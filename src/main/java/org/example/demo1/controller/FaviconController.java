package org.example.demo1.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaviconController {
//todo加入con图标
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noCache())
                .build();
    }
}
