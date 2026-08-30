package com.group5.lostandfoundjava.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockController {
    @GetMapping("/api/auth/mockapi")
    public ResponseEntity<String> mockApi() {
        return ResponseEntity.ok("MOCK CONTROLLER TESTED lerk ti2");
    }
}
