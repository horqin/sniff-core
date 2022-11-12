package org.sniff.controller;

import cn.hutool.core.codec.Base64;
import org.sniff.service.SplitService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class SplitController {

    @Resource
    private SplitService splitService;

    // 嗅探服务器
    @PostMapping("/split")
    public void split(@RequestBody String bytes) {
        try {
            bytes = bytes.replaceAll("-", "+").replaceAll("_", "/");
            splitService.split(Base64.decode(bytes));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
