package org.sniff.controller;

import org.sniff.service.SplitCapService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/splitCap")
public class SplitCapController {

    @Resource
    private SplitCapService splitCapService;

    // 嗅探服务器
    @PostMapping(consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void split(HttpServletRequest request, @RequestParam String file) {
        try {
            splitCapService.splitCap(request.getInputStream());
        } catch (Exception e) {
            System.err.println(file);
            e.printStackTrace();
        }
    }
}
