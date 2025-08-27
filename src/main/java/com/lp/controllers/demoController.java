package com.lp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
public class demoController {

    @GetMapping
    public ResponseEntity<String> demoEndpoint() {
        return ResponseEntity.ok("hello from secured endpoint");
    }
}
