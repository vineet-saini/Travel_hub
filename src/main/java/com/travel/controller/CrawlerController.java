package com.travel.controller;

import com.travel.service.PriceCrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    @Autowired
    private PriceCrawlerService crawlerService;

    @GetMapping("/prices")
    public ResponseEntity<?> getPrices(
            @RequestParam String place,
            @RequestParam(defaultValue = "1") int people,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {

        List<Map<String, Object>> prices = crawlerService.crawlPrices(place, people);

        if (prices.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "error", "Could not fetch live prices from any platform. Sites may be blocking crawlers.",
                "prices", List.of(),
                "location", buildLocation(city, country, lat, lng)
            ));
        }

        int cheapest = prices.stream()
            .mapToInt(p -> (int) p.get("pricePerPerson"))
            .min().orElse(0);
        int mostExpensive = prices.stream()
            .mapToInt(p -> (int) p.get("pricePerPerson"))
            .max().orElse(cheapest);

        Map<String, Object> best = prices.get(0);
        int savings = (mostExpensive - cheapest) * people;

        return ResponseEntity.ok(Map.of(
            "prices", prices,
            "bestPlatform", best.get("platform"),
            "bestPrice", best.get("pricePerPerson"),
            "savings", savings,
            "people", people,
            "location", buildLocation(city, country, lat, lng),
            "scrapedAt", new java.util.Date().toString()
        ));
    }

    private Map<String, Object> buildLocation(String city, String country, Double lat, Double lng) {
        Map<String, Object> loc = new LinkedHashMap<>();
        loc.put("city", city != null ? city : "Unknown");
        loc.put("country", country != null ? country : "Unknown");
        loc.put("lat", lat);
        loc.put("lng", lng);
        return loc;
    }
}
