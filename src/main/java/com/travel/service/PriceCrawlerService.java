package com.travel.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

@Service
public class PriceCrawlerService {

    // Destination → search keyword used on travel sites
    private static final Map<String, String> KEYWORDS = Map.ofEntries(
        Map.entry("kashmir", "Kashmir"),
        Map.entry("shimla", "Shimla"),
        Map.entry("mumbai", "Mumbai"),
        Map.entry("agra", "Agra"),
        Map.entry("newyork", "New York"),
        Map.entry("losangeles", "Los Angeles"),
        Map.entry("miami", "Miami"),
        Map.entry("toronto", "Toronto"),
        Map.entry("vancouver", "Vancouver"),
        Map.entry("niagara", "Niagara Falls"),
        Map.entry("london", "London"),
        Map.entry("edinburgh", "Edinburgh"),
        Map.entry("manchester", "Manchester")
    );

    public List<Map<String, Object>> crawlPrices(String place, int people) {
        String key = place.toLowerCase().replace(" ", "");
        String keyword = KEYWORDS.getOrDefault(key, place);

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<Map<String, Object>>> futures = List.of(
            executor.submit(() -> crawlMakeMyTrip(keyword, people)),
            executor.submit(() -> crawlYatra(keyword, people)),
            executor.submit(() -> crawlCleartrip(keyword, people)),
            executor.submit(() -> crawlGoibibo(keyword, people))
        );

        List<Map<String, Object>> results = new ArrayList<>();
        for (Future<Map<String, Object>> f : futures) {
            try {
                Map<String, Object> r = f.get(10, TimeUnit.SECONDS);
                if (r != null && r.containsKey("pricePerPerson")) results.add(r);
            } catch (Exception ignored) {}
        }
        executor.shutdown();

        // If no results at all, return empty — frontend will show error
        if (results.isEmpty()) return results;

        // Sort by price
        results.sort(Comparator.comparingInt(r -> (int) r.get("pricePerPerson")));

        // TravelHub price = min competitor price - 4% (feasibility floor: never below 5000)
        int minCompetitor = (int) results.get(0).get("pricePerPerson");
        int travelhubPrice = Math.max(5000, (int)(minCompetitor * 0.96));
        // Round to X,999 pattern
        travelhubPrice = ((travelhubPrice / 1000) * 1000) + 999;
        // Make sure it's actually lower
        if (travelhubPrice >= minCompetitor) travelhubPrice = minCompetitor - 500;

        Map<String, Object> travelhub = new LinkedHashMap<>();
        travelhub.put("platform", "TravelHub");
        travelhub.put("pricePerPerson", travelhubPrice);
        travelhub.put("totalPrice", travelhubPrice * people);
        travelhub.put("rating", 4.9);
        travelhub.put("reviews", "2,341");
        travelhub.put("badge", "BEST_PRICE");
        travelhub.put("scraped", true);
        travelhub.put("bookUrl", "/booking/traveller/" + place);
        results.add(0, travelhub);

        // Re-sort after adding TravelHub
        results.sort(Comparator.comparingInt(r -> (int) r.get("pricePerPerson")));
        results.get(0).put("badge", "BEST_PRICE");

        return results;
    }

    // ─── MakeMyTrip ───────────────────────────────────────────────────────────
    // MMT exposes a search suggestions + listing JSON endpoint
    private Map<String, Object> crawlMakeMyTrip(String keyword, int people) {
        Map<String, Object> result = baseResult("MakeMyTrip", 4.5, "18,420",
            "https://www.makemytrip.com/holidays-india/");
        try {
            // MMT holiday listing page — scrape the JSON embedded in __NEXT_DATA__ or window.__STATE__
            String url = "https://www.makemytrip.com/holidays-india/" +
                keyword.toLowerCase().replace(" ", "-") + "-tour-packages/";
            String html = fetchHtml(url);

            // Try to find price in JSON blobs embedded in the page
            int price = extractPriceFromJson(html,
                new String[]{"\"discountedPrice\":(\\d+)", "\"price\":(\\d+)",
                             "\"startingPrice\":(\\d+)", "\"minPrice\":(\\d+)",
                             "\"salePrice\":(\\d+)", "\"amount\":(\\d+)"});

            if (price > 0) {
                result.put("pricePerPerson", price);
                result.put("totalPrice", price * people);
                result.put("scraped", true);
            } else {
                // Try the search page
                String searchUrl = "https://www.makemytrip.com/holidays-india/search/?searchText=" +
                    keyword.replace(" ", "+") + "+tour+package";
                String searchHtml = fetchHtml(searchUrl);
                price = extractPriceFromJson(searchHtml,
                    new String[]{"\"discountedPrice\":(\\d+)", "\"price\":(\\d+)",
                                 "\"startingPrice\":(\\d+)", "\"minPrice\":(\\d+)"});
                if (price > 0) {
                    result.put("pricePerPerson", price);
                    result.put("totalPrice", price * people);
                    result.put("scraped", true);
                } else {
                    return null; // no price found
                }
            }
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    // ─── Yatra ────────────────────────────────────────────────────────────────
    private Map<String, Object> crawlYatra(String keyword, int people) {
        Map<String, Object> result = baseResult("Yatra", 4.3, "9,870",
            "https://www.yatra.com/holidays/");
        try {
            String url = "https://www.yatra.com/holidays/india/" +
                keyword.toLowerCase().replace(" ", "-") + "-packages";
            String html = fetchHtml(url);

            int price = extractPriceFromJson(html,
                new String[]{"\"price\":(\\d+)", "\"packagePrice\":(\\d+)",
                             "\"startingFrom\":(\\d+)", "\"minPrice\":(\\d+)",
                             "\"discountedPrice\":(\\d+)", "\"amount\":(\\d+)"});

            if (price > 0) {
                result.put("pricePerPerson", price);
                result.put("totalPrice", price * people);
                result.put("scraped", true);
            } else {
                // Try Yatra search
                String searchUrl = "https://www.yatra.com/holidays/search?destination=" +
                    keyword.replace(" ", "%20");
                String searchHtml = fetchHtml(searchUrl);
                price = extractPriceFromJson(searchHtml,
                    new String[]{"\"price\":(\\d+)", "\"packagePrice\":(\\d+)",
                                 "\"startingFrom\":(\\d+)", "\"minPrice\":(\\d+)"});
                if (price > 0) {
                    result.put("pricePerPerson", price);
                    result.put("totalPrice", price * people);
                    result.put("scraped", true);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    // ─── Cleartrip ────────────────────────────────────────────────────────────
    private Map<String, Object> crawlCleartrip(String keyword, int people) {
        Map<String, Object> result = baseResult("Cleartrip", 4.4, "7,230",
            "https://www.cleartrip.com/holidays/");
        try {
            String url = "https://www.cleartrip.com/holidays/india/packages/" +
                keyword.toLowerCase().replace(" ", "-") + "/";
            String html = fetchHtml(url);

            int price = extractPriceFromJson(html,
                new String[]{"\"price\":(\\d+)", "\"discountedPrice\":(\\d+)",
                             "\"startingPrice\":(\\d+)", "\"minPrice\":(\\d+)",
                             "\"salePrice\":(\\d+)", "\"totalPrice\":(\\d+)"});

            if (price > 0) {
                result.put("pricePerPerson", price);
                result.put("totalPrice", price * people);
                result.put("scraped", true);
            } else {
                String searchUrl = "https://www.cleartrip.com/holidays/india/packages/?q=" +
                    keyword.replace(" ", "+");
                String searchHtml = fetchHtml(searchUrl);
                price = extractPriceFromJson(searchHtml,
                    new String[]{"\"price\":(\\d+)", "\"discountedPrice\":(\\d+)",
                                 "\"startingPrice\":(\\d+)", "\"minPrice\":(\\d+)"});
                if (price > 0) {
                    result.put("pricePerPerson", price);
                    result.put("totalPrice", price * people);
                    result.put("scraped", true);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    // ─── Goibibo ──────────────────────────────────────────────────────────────
    private Map<String, Object> crawlGoibibo(String keyword, int people) {
        Map<String, Object> result = baseResult("Goibibo", 4.4, "11,540",
            "https://www.goibibo.com/holidays/");
        try {
            String url = "https://www.goibibo.com/holidays/" +
                keyword.toLowerCase().replace(" ", "-") + "-tour-packages/";
            String html = fetchHtml(url);

            int price = extractPriceFromJson(html,
                new String[]{"\"price\":(\\d+)", "\"discountedPrice\":(\\d+)",
                             "\"startingPrice\":(\\d+)", "\"minPrice\":(\\d+)",
                             "\"salePrice\":(\\d+)", "\"amount\":(\\d+)"});

            if (price > 0) {
                result.put("pricePerPerson", price);
                result.put("totalPrice", price * people);
                result.put("scraped", true);
            } else {
                String searchUrl = "https://www.goibibo.com/holidays/search/?q=" +
                    keyword.replace(" ", "+");
                String searchHtml = fetchHtml(searchUrl);
                price = extractPriceFromJson(searchHtml,
                    new String[]{"\"price\":(\\d+)", "\"discountedPrice\":(\\d+)",
                                 "\"startingPrice\":(\\d+)", "\"minPrice\":(\\d+)"});
                if (price > 0) {
                    result.put("pricePerPerson", price);
                    result.put("totalPrice", price * people);
                    result.put("scraped", true);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String fetchHtml(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-IN,en;q=0.9");
        conn.setRequestProperty("Accept-Encoding", "identity");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();
        if (status == 403 || status == 429 || status == 503) {
            throw new Exception("Blocked: HTTP " + status);
        }

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        // Read max 500KB to avoid memory issues
        int chars = 0;
        while ((line = reader.readLine()) != null && chars < 500_000) {
            sb.append(line);
            chars += line.length();
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Scans raw HTML/JSON for price patterns using regex.
     * Finds the LOWEST valid price (most likely the starting/minimum package price).
     */
    private int extractPriceFromJson(String html, String[] patterns) {
        int lowestPrice = 0;
        for (String pattern : patterns) {
            try {
                Matcher m = Pattern.compile(pattern).matcher(html);
                while (m.find()) {
                    String numStr = m.group(1);
                    if (numStr.length() > 8) continue; // skip huge numbers
                    int val = Integer.parseInt(numStr);
                    // Valid INR package price range: ₹3,000 – ₹5,00,000
                    if (val >= 3000 && val <= 500000) {
                        if (lowestPrice == 0 || val < lowestPrice) {
                            lowestPrice = val;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return lowestPrice;
    }

    private Map<String, Object> baseResult(String platform, double rating,
                                            String reviews, String bookUrl) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("platform", platform);
        r.put("rating", rating);
        r.put("reviews", reviews);
        r.put("badge", "");
        r.put("scraped", false);
        r.put("bookUrl", bookUrl);
        return r;
    }
}
