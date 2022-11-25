package org.sniff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
@TableName("session")
public class SessionEntity {

    // 主键
    private Long    id;
    // 协议
    private String  protocol;
    // 源 IP
    private String  srcIp;
    // 源端口
    private Integer srcPort;
    // 目的 IP
    private String  dstIp;
    // 目的端口
    private Integer dstPort;
    // 预测结果
    private Integer forecast;
    // 创建时间
    private Date    createDate;

    public static String encode(String protocol, String srcIp, Integer srcPort, String dstIp, Integer dstPort) {
        String srcHost = String.join("@", srcIp, srcPort.toString());
        String dstHost = String.join("@", dstIp, dstPort.toString());
        return srcHost.compareTo(dstHost) < 0
                ? String.join("@", protocol, srcHost, dstHost)
                : String.join("@", protocol, dstHost, srcHost);
    }

    public static SessionEntity decode(String session, Integer forecast, Date createDate) {
        String[] s = session.split("@");
        return new SessionEntity(null, s[0], s[1], Integer.parseInt(s[2]), s[3], Integer.parseInt(s[4]),
                forecast, createDate);
    }
}
