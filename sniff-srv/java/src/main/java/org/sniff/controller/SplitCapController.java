package org.sniff.controller;

import org.sniff.service.SplitCapService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/splitCap")
public class SplitCapController {

    @Resource
    private SplitCapService splitCapService;

    // 嗅探服务器
    @PostMapping(value = "/{file}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void split(@PathVariable("file") String file, HttpServletRequest request) {
        try {
            splitCapService.splitCap(request.getInputStream());
        } catch (Exception e) {
            System.err.println(file);
            e.printStackTrace();
        }
    }
}
