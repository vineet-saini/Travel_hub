package com.travel.controller;

import com.travel.service.TripOptimizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/optimizer")
public class TripOptimizerController {

    @Autowired
    private TripOptimizerService optimizerService;

    @PostMapping("/optimize")
    public ResponseEntity<?> optimize(@RequestBody Map<String, String> req) {
        try {
            String result = optimizerService.optimize(
                req.getOrDefault("dates", ""),
                req.getOrDefault("places", ""),
                req.getOrDefault("startLocation", ""),
                req.getOrDefault("budget", ""),
                req.getOrDefault("style", "balanced")
            );
            return ResponseEntity.ok(Map.of("result", result));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }
}
