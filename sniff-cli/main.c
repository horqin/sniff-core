#define _GNU_SOURCE

#define LAMBDA(r, p) ({r _ p _;})

#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include <cjson/cJSON.h>
#include <curl/curl.h>
#include <pcap/pcap.h>
#include <uv.h>
#include <zookeeper/zookeeper.h>

static void service();


/*
 * 监视器部分
 */
static int pid; // 嗅探器 PID

static char device[64]; // 嗅探的网络设备
static char filter[64]; // BPF 表达式
static char server[64]; // 服务器的地址

static void watcher(zhandle_t *zh, int type, int stat, const char *path, void *ctx);

int main(int argc, const char *argv[]) {
    // 获取 ZK 数据库的地址和路径
    char *host, *path;
    if ((host = getenv("ZK_HOST")) == NULL || (path = getenv("ZK_PATH")) == NULL) {
#ifdef _DEBUG
        fprintf(stderr, "usage: ZK_HOST=<host> ZK_PATH=<path> %s\n", argv[0]);
#endif
        exit(-1);
    }

    // 设置 ZK 客户端的 DEBUG 等级
#ifdef _DEBUG
    zoo_set_debug_level(3);
#else
    zoo_set_debug_level(0);
#endif

    // 连接 ZK 数据库
    zhandle_t *zh;
    if ((zh = zookeeper_init(host, NULL, 2000, NULL, NULL, 0)) == NULL) {
        exit(1);
    }

    // 获取 ZK 数据库指定节点记录的配置信息，并且监视器的子线程持续监听是否发生配置信息的更新
    watcher(zh, 0, 0, path, NULL);

    // 创建嗅探器之后，监听器的主线程等待嗅探器退出
    // 注意：`fork` 函数只会复制主进程的主线程的虚拟内存，因此子进程不会触发 `watcher` 函数
    for (;;) {
        // 子进程
        if ((pid = fork()) == 0) service();

        // 主进程
        int stat;
        (void)waitpid(pid, &stat, 0);
        // 如果嗅探器错误地退出，那么监听器同样退出
        if (WIFEXITED(stat) && WEXITSTATUS(stat)) {
            exit(1);
        }
    }
}

// 注意：监视器的主线程只会执行一次，相反，子线程则会执行多次
void watcher(zhandle_t *zh, int type, int stat, const char *path, void *ctx) {
    char buf[256];
    int buf_size = sizeof buf;
    // 获取配置信息
    if (zoo_wget(zh, path, watcher, NULL, buf, &buf_size, NULL) != ZOK) {
        exit(1);
    }
    // 解析配置信息，记录到全局变量中
    cJSON *obj = cJSON_Parse(buf);
    strcpy(device, cJSON_GetObjectItem(obj, "device")->valuestring);
    strcpy(filter, cJSON_GetObjectItem(obj, "filter")->valuestring);
    strcpy(server, cJSON_GetObjectItem(obj, "server")->valuestring);
    cJSON_Delete(obj);

    // 此处只有监视器的子线程才会执行，其作用是结束嗅探器的运行
    static int i = 0;
    if (i == 0) {
        i = 1;
    } else {
        kill(pid, SIGINT);
    }
}


/*
 * 嗅探器部分
 */
#define NPKTS 100 // 每次上传的数据包数量

static void *loop; // 线程池句柄
static void *pcap; // PCAP 句柄

static void handler_ctrl(int sig);
static void handler_pcap(unsigned char *arg, const struct pcap_pkthdr *pcap_pkthdr, const unsigned char *bytes);
static void handler_curl(uv_work_t *req);

void service() {
    // CURL 全局初始化
    curl_global_init(CURL_GLOBAL_ALL);

    // 创建线程池
    // 注意：线程池创建期间屏蔽信号，使得子线程在派生后屏蔽信号，从而只有主线程能够触发相应的操作
    sigset_t newset, oldset;
    sigemptyset(&newset);
    sigaddset(&newset, SIGINT);
    pthread_sigmask(SIG_BLOCK, &newset, &oldset);
    loop = uv_default_loop();
    pthread_sigmask(SIG_SETMASK, &oldset, NULL);

    // 注册信号处理函数
    signal(SIGINT, handler_ctrl);

    // 嗅探
    bpf_u_int32 addr, mask;
    char errbuf[PCAP_ERRBUF_SIZE];
    if (pcap_lookupnet(device, &addr, &mask, errbuf) == PCAP_ERROR ||
            (pcap = pcap_open_live(device, 65535, 0, 200, errbuf)) == NULL) {
#ifdef _DEBUG
        fprintf(stderr, "pcap_lookupnet, pcap_open_live: %s\n", errbuf);
#endif
        exit(1);
    }
    struct bpf_program bpf_program;
    if (pcap_compile(pcap, &bpf_program, filter, 0, addr) == PCAP_ERROR ||
            pcap_setfilter(pcap, &bpf_program) == PCAP_ERROR) {
#ifdef _DEBUG
        pcap_perror(pcap, "pcap_compile, pcap_compile");
#endif
        exit(1);
    }
    (void)pcap_loop(pcap, -1, handler_pcap, NULL);
}

void handler_ctrl(int sig) {
    // 首先，关闭 PCAP 句柄，使得在线程池中不再持续堆积任务
    pcap_close(pcap);

    // 然后，执行线程池中尚未完成的任务
    (void)uv_run(loop, UV_RUN_DEFAULT);

    // 接着，CURL 全局清理
    curl_global_cleanup();

    // 最后，退出
    // 注意：在退出时，尚未处理的临时文件将会自动删除
    exit(0);
}

void handler_pcap(unsigned char *arg, const struct pcap_pkthdr *pcap_pkthdr, const unsigned char *bytes) {
    static pcap_dumper_t *pcap_dumper = NULL;

    // 每收集 NPKTS 份数据包，便向目标服务器上传
    // 注意：网络流量存储在临时文件中，当文件关闭或者进程结束时，自动删除
    static int i = 0;
    if ((i = ((i + 1) % NPKTS)) == 1) {
        if (pcap_dumper != NULL) {
            pcap_dump_flush(pcap_dumper);

            uv_work_t *req = malloc(sizeof(uv_work_t));
            req->data = (void *)pcap_dumper;
            uv_queue_work(loop, req, handler_curl, LAMBDA(void, (uv_work_t *r, int s) { free(r); }));
        }
        pcap_dumper = pcap_dump_fopen(pcap, tmpfile());
    }
    pcap_dump((unsigned char *)pcap_dumper, pcap_pkthdr, bytes);
}

void handler_curl(uv_work_t *req) {
    FILE *fp = (FILE *)req->data;

    // 查询临时文件的大小
    struct stat stat;
    fstat(fileno(fp), &stat);

    // 生成临时文件对应的内存映射
    void *ptr;
    if ((ptr = mmap(NULL, stat.st_size, PROT_READ, MAP_PRIVATE, fileno(fp), 0)) == (void *)-1) {
#ifdef _DEBUG
        perror("mmap");
#endif
        exit(1);
    }

    // 上传网络流量到目标服务器上，使用二进制 POST 的方式
    CURL *curl = curl_easy_init();
    struct curl_slist *curl_slist = curl_slist_append(NULL, "Content-Type:application/octet-stream");
    curl_easy_setopt(curl, CURLOPT_BUFFERSIZE, 102400L);
    curl_easy_setopt(curl, CURLOPT_URL, server);
    curl_easy_setopt(curl, CURLOPT_NOPROGRESS, 1L);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, ptr);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE_LARGE, (curl_off_t)stat.st_size);
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, curl_slist);
    curl_easy_setopt(curl, CURLOPT_USERAGENT, "curl/7.85.0");
    curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 50L);
    curl_easy_setopt(curl, CURLOPT_HTTP_VERSION, (long)CURL_HTTP_VERSION_2TLS);
    curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "POST");
    curl_easy_setopt(curl, CURLOPT_FTP_SKIP_PASV_IP, 1L);
    curl_easy_setopt(curl, CURLOPT_TCP_KEEPALIVE, 1L);
    CURLcode rc;
    if ((rc = curl_easy_perform(curl)) != CURLE_OK) {
#ifdef _DEBUG
        fprintf(stderr, "curl_easy_perform: %s\n", curl_easy_strerror(rc));
#endif
        exit(1);
    }
    curl_slist_free_all(curl_slist);
    curl_easy_cleanup(curl);

    // 关闭内存映射
    munmap(ptr, stat.st_size);

    // 关闭临时文件，从而实现文件自动删除
    fclose(fp);
}
