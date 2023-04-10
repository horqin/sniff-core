package org.sniff.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
@TableName("tb_session")
public class Session {
    private Long    id;

    // 协议
    private String  protocol;
    // 源 IP 地址
    private String  srcIp;
    // 源端口
    private Integer srcPort;
    // 目的 IP 地址
    private String  dstIp;
    // 目的端口
    private Integer dstPort;
    // 预测标签
    private Integer forecast;

    // 创建时间
    private Date    createdTime;
    // 修改时间
    private Date    modifiedTime;

    public static String encode(String protocol, String srcIp, Integer srcPort, String dstIp, Integer dstPort) {
        String srcHost = String.join("@", srcIp, srcPort.toString());
        String dstHost = String.join("@", dstIp, dstPort.toString());
        return srcHost.compareTo(dstHost) < 0
                ? String.join("@", protocol, srcHost, dstHost)
                : String.join("@", protocol, dstHost, srcHost);
    }

    public static Session decode(String session, Integer forecast) {
        String[] s = session.split("@");
        return new Session(null, s[0], s[1], Integer.parseInt(s[2]), s[3], Integer.parseInt(s[4]),
                forecast, null, null);
    }
}
