package com.travel.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class TripOptimizerService {

    @Value("${groq.api.key}")
    private String apiKey;

    // Groq is OpenAI-compatible, free: 14,400 req/day, 30 req/min
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    public String optimize(String dates, String places, String startLocation,
                           String budget, String style) throws Exception {

        String prompt = buildPrompt(dates, places, startLocation, budget, style);

        String requestBody = "{"
            + "\"model\":\"" + MODEL + "\","
            + "\"messages\":["
            + "{\"role\":\"system\",\"content\":\"You are an expert travel itinerary optimizer. Respond in clean markdown.\"},"
            + "{\"role\":\"user\",\"content\":" + jsonString(prompt) + "}"
            + "],"
            + "\"temperature\":0.5,"
            + "\"max_tokens\":2000"
            + "}";

        URL url = new URL(GROQ_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        conn.disconnect();

        String response = sb.toString();

        if (status == 401) throw new RuntimeException(
            "Invalid Groq API key. Get a free key at https://console.groq.com");
        if (status == 429) throw new RuntimeException(
            "Groq rate limit hit. Please wait a moment and try again.");
        if (status != 200) throw new RuntimeException(
            "Groq API error " + status + ": " + response);

        return extractContent(response);
    }

    private String buildPrompt(String dates, String places, String startLocation,
                                String budget, String style) {
        return "You are an expert travel itinerary optimizer.\n\n"
            + "Optimize this travel plan:\n\n"
            + "**Trip Dates:** " + dates + "\n"
            + "**Places to Visit:** " + places + "\n"
            + "**Starting From:** " + startLocation + "\n"
            + "**Budget:** " + (budget.isBlank() ? "Not specified" : budget) + "\n"
            + "**Travel Style:** " + style + "\n\n"
            + "Provide exactly these 6 sections:\n\n"
            + "## 1. Optimized Itinerary\n"
            + "Day-by-day plan with Location, Activities, and Travel notes.\n\n"
            + "## 2. Route Optimization Summary\n"
            + "What changed and why it is better.\n\n"
            + "## 3. Time-Saving Suggestions\n"
            + "Bullet points.\n\n"
            + "## 4. Cost Optimization Suggestions\n"
            + "Bullet points.\n\n"
            + "## 5. Issues in Original Plan\n"
            + "List any problems found.\n\n"
            + "## 6. Optional Improvements\n"
            + "Better alternatives only if genuinely useful.\n\n"
            + "Rules: Be realistic with travel times. Group geographically close places. "
            + "No unnecessary back-and-forth. Keep it concise and actionable.";
    }

    // Parses "content":"..." from OpenAI-format JSON response
    private String extractContent(String json) {
        String marker = "\"content\":\"";
        int start = json.indexOf(marker);
        if (start == -1) return "Could not parse response: " + json;
        start += marker.length();
        StringBuilder content = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n' -> content.append('\n');
                    case 't' -> content.append('\t');
                    case '"' -> content.append('"');
                    case '\\' -> content.append('\\');
                    default -> content.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                content.append(c);
            }
        }
        return content.toString();
    }

    private String jsonString(String s) {
        return "\"" + s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            + "\"";
    }
}
