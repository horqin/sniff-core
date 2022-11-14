package org.sniff.feign;

import org.sniff.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(name = "forecast", url = "http://debian:5000/forecast")
public interface ForecastFeign {

    @GetMapping("/{session}")
    R forecast(@PathVariable("session") String session);
}
