package org.sniff.controller;

import org.sniff.service.SplitService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/split")
public class SplitController {

    @Resource
    private SplitService splitService;

    // 嗅探服务器
    @PostMapping(consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void split(HttpServletRequest request) {
        try {
            splitService.split(request.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
