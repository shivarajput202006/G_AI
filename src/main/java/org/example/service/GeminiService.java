package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    public GeminiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(4000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String askGeminiText(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );
            return callGemini(body);
        } catch (Exception e) {
            return "• **MSMED Act 2006 Section 15 & 16:** Payment beyond 45 days attracts mandatory compound interest at 3x RBI Bank Rate (~20.25% p.a.).\n"
                    + "• **Immediate Action:** Send a 7-day formal Demand Notice via WhatsApp/Email.\n"
                    + "• **MSME Samadhaan Filing:** File online claim at samadhaan.msme.gov.in with Udyam Certificate & Invoice copy for MSEFC arbitration.";
        }
    }

    public String extractInvoiceFromFile(MultipartFile file) {
        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String mime = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            String prompt = "Extract invoice details and return ONLY raw JSON with keys: customerName, customerPhone, invoiceNumber, amount, dueDate (YYYY-MM-DD).";

            Map<String, Object> inlineData = Map.of("mimeType", mime, "data", base64);
            Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(
                    Map.of("text", prompt),
                    Map.of("inlineData", inlineData)
            ))));

            String text = callGemini(body);
            return text.replace("```json", "").replace("```", "").trim();
        } catch (Exception e) {
            return "{\"error\": \"Invoice scan failed: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String callGemini(Map<String, Object> body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> res = restTemplate.postForEntity(URL + apiKey.trim(), new HttpEntity<>(body, headers), String.class);
        JsonNode root = objectMapper.readTree(res.getBody());
        return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
    }
}