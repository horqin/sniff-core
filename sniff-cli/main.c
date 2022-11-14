#define _GNU_SOURCE

#include <signal.h>
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
#include <sodium/utils.h>
#include <zookeeper/zookeeper.h>

#include "utils/thpool.h"

static void service();


/*
 * 监视器部分
 */
static int pid;

static char device[64];
static char filter[64];
static char server[64];

static void watcher(zhandle_t *zh, int type, int stat, const char *path, void *ctx);

int main(int argc, const char *argv[]) {
    char *host, *path;
    if ((host = getenv("ZK_HOST")) == NULL || (path = getenv("ZK_PATH")) == NULL) {
#ifdef _DEBUG
        fprintf(stderr, "usage: ZK_HOST=<host> ZK_PATH=<path> %s\n", argv[0]);
#endif
        exit(-1);
    }

#ifndef _DEBUG
    close(0), close(1), close(2);
#endif

    zhandle_t *zh;
    if ((zh = zookeeper_init(host, NULL, 2000, NULL, NULL, 0)) == NULL) {
#ifdef _DEBUG
        perror("zookeeper_init");
#endif
        exit(1);
    }

    watcher(zh, 0, 0, path, NULL);

    for (;;) {
        if ((pid = fork()) == 0) service();

        int stat;
        (void)waitpid(pid, &stat, 0);
        if (WIFEXITED(stat) && WEXITSTATUS(stat)) {
            exit(1);
        }
    }
}

void watcher(zhandle_t *zh, int type, int stat, const char *path, void *ctx) {
    char buf[256];
    int buf_size = sizeof buf;
    int rc;
    if ((rc = zoo_wget(zh, path, watcher, NULL, buf, &buf_size, NULL)) != ZOK) {
#ifdef _DEBUG
        switch (rc) {
        case ZNONODE:
            fprintf(stderr, "zoo_wget: the node does not exist\n");
            break;
        case ZNOAUTH:
            fprintf(stderr, "zoo_wget: the client does not have permission\n");
            break;
        case ZBADARGUMENTS:
            fprintf(stderr, "zoo_wget: invalid input parameters\n");
            break;
        default:
            fprintf(stderr, "zoo_wget: failed to marshall a request; possibly, out of memory\n");
        }
#endif
        exit(1);
    }
    cJSON *obj = cJSON_Parse(buf);
    strcpy(device, cJSON_GetObjectItem(obj, "device")->valuestring);
    strcpy(filter, cJSON_GetObjectItem(obj, "filter")->valuestring);
    strcpy(server, cJSON_GetObjectItem(obj, "server")->valuestring);
    cJSON_Delete(obj);

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
#define NCPUS 4
#define NPKTS 100

static void *pcap;
static void *pool;

static void handler_ctrl(int sig);
static void handler_pcap(unsigned char *arg, const struct pcap_pkthdr *pcap_pkthdr, const unsigned char *bytes);
static void handler_curl(void *arg);

void service() {
    curl_global_init(CURL_GLOBAL_ALL);

    sigset_t newset, oldset;
    sigemptyset(&newset);
    sigaddset(&newset, SIGINT);
    pthread_sigmask(SIG_BLOCK, &newset, &oldset);
    pool = thpool_init(2*NCPUS+1);
    pthread_sigmask(SIG_SETMASK, &oldset, NULL);

    signal(SIGINT, handler_ctrl);

    bpf_u_int32 addr, mask;
    char errbuf[PCAP_ERRBUF_SIZE];
    if (pcap_lookupnet(device, &addr, &mask, errbuf) == PCAP_ERROR ||
            (pcap = pcap_open_live(device, 65535, 0, 200, errbuf)) == NULL) {
#ifdef _DEBUG
        fprintf(stderr, "pcap_lookupnet, pcap_open_live: %s", errbuf);
#endif
        exit(1);
    }
    struct bpf_program bpf_program;
    if (pcap_compile(pcap, &bpf_program, filter, 0, addr) == PCAP_ERROR ||
            pcap_setfilter(pcap, &bpf_program) == PCAP_ERROR) {
#ifdef _DEBUG
        pcap_perror(pcap, "pcap_compile, pcap_setfilter");
#endif
        exit(1);
    }
    (void)pcap_loop(pcap, -1, handler_pcap, NULL);
}

void handler_ctrl(int sig) {
    pcap_close(pcap);

    thpool_wait(pool), thpool_destroy(pool);

    curl_global_cleanup();

    exit(0);
}

void handler_pcap(unsigned char *arg, const struct pcap_pkthdr *pcap_pkthdr, const unsigned char *bytes) {
    static pcap_dumper_t *pcap_dumper = NULL;

    static int i = 0;
    if ((i = ((i + 1) % NPKTS)) == 1) {
        if (pcap_dumper != NULL) {
            pcap_dump_flush(pcap_dumper);
            thpool_add_work(pool, handler_curl, pcap_dumper);
        }
        pcap_dumper = pcap_dump_fopen(pcap, tmpfile());
    }
    pcap_dump((unsigned char *)pcap_dumper, pcap_pkthdr, bytes);
}

void handler_curl(void *arg) {
    FILE *fp = (FILE *)arg;

    struct stat stat;
    fstat(fileno(fp), &stat);

    void *ptr;
    if ((ptr = mmap(NULL, stat.st_size, PROT_READ, MAP_PRIVATE, fileno(fp), 0)) == (void *)-1) {
#ifdef _DEBUG
        perror("mmap");
#endif
        exit(1);
    }

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
    
    munmap(ptr, stat.st_size);

    fclose(fp);
}
