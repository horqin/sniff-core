package org.sniff.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(name = "forecast", url = "http://centos:5000/forecast")
public interface ForecastFeign {

    // 深度学习远程调用接口
    @GetMapping("/{session}")
    String forecast(@PathVariable("session") String session);
}
