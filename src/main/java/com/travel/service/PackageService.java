package com.travel.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class PackageService {

    private final Map<String, String> packages = new HashMap<>();

    // Initialize file mappings
    {
        // India
        packages.put("kashmir", "Kashmir-3Day.txt");
        packages.put("shimla", "Shimla-4Day.txt");
        packages.put("mumbai", "Mumbai-2Day.txt");
        packages.put("agra", "Agra-3Day.txt");

        // US
        packages.put("newyork", "NewYork-3Day.txt");
        packages.put("losangeles", "LosAngeles-3Day.txt");
        packages.put("miami", "Miami-2Day.txt");

        // Canada
        packages.put("toronto", "Toronto-3Day.txt");
        packages.put("vancouver", "Vancouver-3Day.txt");
        packages.put("niagara", "Niagara-2Day.txt");

        // UK
        packages.put("london", "London-3Day.txt");
        packages.put("edinburgh", "Edinburgh-3Day.txt");
        packages.put("manchester", "Manchester-2Day.txt");
    }

    public String getPackageContent(String place) {

        try {
            String key = place.toLowerCase().replace(" ", "");
            String fileName = packages.getOrDefault(key, "default.txt");

            ClassPathResource resource = new ClassPathResource("packages/" + fileName);

            return new String(resource.getInputStream().readAllBytes());

        } catch (IOException e) {

            return "Tour Package for " + place +
                    "\n\nDay 1: Arrival and welcome tour." +
                    "\nDay 2: Explore main attractions." +
                    "\nDay 3: Free time and departure." +
                    "\n\nPrice: $400/person (includes meals & transport).";
        }
    }
}