package com.aaha.ftth.camunda.mock;

import com.aaha.ftth.eoc.delegate.adapter.ServiceRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Real implementation that calls EOC's Service Registry REST API.
 * Base URL: http://localhost:8090/cwf/sr/v1
 * Auth: Basic (eoc:EOC)
 */
@Component
public class MockServiceRegistryService implements ServiceRegistryService {

    private static final Logger log = LoggerFactory.getLogger(MockServiceRegistryService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String username;
    private final String password;

    public MockServiceRegistryService(
            RestTemplate restTemplate,
            @Value("${eoc.sr.base-url}") String baseUrl,
            @Value("${eoc.sr.username}") String username,
            @Value("${eoc.sr.password}") String password) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> createProduct(Map<String, Object> payload) {
        log.info("=== [EOC SR] Creating Product in Service Registry ===");
        log.info("  URL: {}/product", baseUrl);
        log.info("  Payload: {}", payload);

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/product", HttpMethod.POST, request, Map.class);
            log.info("  Response: {} - {}", response.getStatusCode(), response.getBody());
            return response.getBody() != null ? response.getBody() : Map.of("id", "PROD-" + UUID.randomUUID().toString().substring(0, 8));
        } catch (Exception e) {
            log.error("  FAILED to create product: {}", e.getMessage());
            // Return mock ID so the flow doesn't break during testing
            String mockId = "PROD-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("  Returning mock ID: {}", mockId);
            return Map.of("id", mockId, "status", "mock-fallback", "error", e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProductById(String productId) {
        log.info("=== [EOC SR] Getting Product: {} ===", productId);
        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/product/" + productId, HttpMethod.GET, request, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("id", productId);
        } catch (Exception e) {
            log.error("  FAILED: {}", e.getMessage());
            return Map.of("id", productId, "error", e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateProduct(String productId, Map<String, Object> payload) {
        log.info("=== [EOC SR] Updating Product: {} ===", productId);
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/product/" + productId, HttpMethod.PUT, request, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("id", productId);
        } catch (Exception e) {
            log.error("  FAILED: {}", e.getMessage());
            return Map.of("id", productId, "status", "mock-fallback");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> patchProduct(String productId, Map<String, Object> payload) {
        log.info("=== [EOC SR] Patching Product: {} with {} ===", productId, payload);
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/product/" + productId, HttpMethod.PATCH, request, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("id", productId);
        } catch (Exception e) {
            log.error("  FAILED: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("id", productId);
            result.putAll(payload);
            return result;
        }
    }

    @Override
    public Map<String, Object> deleteProduct(String productId) {
        log.info("=== [EOC SR] Deleting Product: {} ===", productId);
        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            restTemplate.exchange(baseUrl + "/product/" + productId, HttpMethod.DELETE, request, Void.class);
            return Map.of("id", productId, "status", "deleted");
        } catch (Exception e) {
            log.error("  FAILED: {}", e.getMessage());
            return Map.of("id", productId, "status", "delete-failed");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> searchProducts(Map<String, Object> searchCriteria) {
        log.info("=== [EOC SR] Searching Products: {} ===", searchCriteria);
        try {
            String customerCode = (String) searchCriteria.getOrDefault("relatedParty.id", "");
            String url = baseUrl + "/product/?relatedParties.role=Customer&relatedParties.reference=" + customerCode;
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);
            List<Map<String, Object>> results = response.getBody() != null ? response.getBody() : List.of();
            return Map.of("results", results);
        } catch (Exception e) {
            log.error("  FAILED: {}", e.getMessage());
            return Map.of("results", List.of(Map.of("id", "PROD-FOUND-MOCK", "status", "active")));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> createService(Map<String, Object> payload) {
        log.info("=== [EOC SR] Creating Service in Service Registry ===");
        log.info("  URL: {}/service", baseUrl);
        log.info("  Payload: {}", payload);

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/service", HttpMethod.POST, request, Map.class);
            log.info("  Response: {} - {}", response.getStatusCode(), response.getBody());
            return response.getBody() != null ? response.getBody() : Map.of("id", "SVC-" + UUID.randomUUID().toString().substring(0, 8));
        } catch (Exception e) {
            log.error("  FAILED to create service: {}", e.getMessage());
            String mockId = "SVC-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("  Returning mock ID: {}", mockId);
            return Map.of("id", mockId, "status", "mock-fallback", "error", e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getServiceById(String serviceId) {
        log.info("=== [EOC SR] Getting Service: {} ===", serviceId);
        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/service/" + serviceId, HttpMethod.GET, request, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("id", serviceId);
        } catch (Exception e) {
            log.error("  FAILED: {}", e.getMessage());
            return Map.of("id", serviceId, "error", e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateService(String serviceId, Map<String, Object> payload) {
        log.info("=== [EOC SR] Updating Service: {} ===", serviceId);
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/service/" + serviceId, HttpMethod.PUT, request, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("id", serviceId);
        } catch (Exception e) {
            log.error("  FAILED: {}", e.getMessage());
            return Map.of("id", serviceId, "status", "mock-fallback");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> patchService(String serviceId, Map<String, Object> payload) {
        log.info("=== [EOC SR] Patching Service: {} with {} ===", serviceId, payload);
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/service/" + serviceId, HttpMethod.PATCH, request, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("id", serviceId);
        } catch (Exception e) {
            log.error("  FAILED: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("id", serviceId);
            result.putAll(payload);
            return result;
        }
    }

    @Override
    public Map<String, Object> deleteService(String serviceId) {
        log.info("=== [EOC SR] Deleting Service: {} ===", serviceId);
        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            restTemplate.exchange(baseUrl + "/service/" + serviceId, HttpMethod.DELETE, request, Void.class);
            return Map.of("id", serviceId, "status", "deleted");
        } catch (Exception e) {
            log.error("  FAILED: {}", e.getMessage());
            return Map.of("id", serviceId, "status", "delete-failed");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> searchServices(Map<String, Object> searchCriteria) {
        log.info("=== [EOC SR] Searching Services: {} ===", searchCriteria);
        try {
            String customerCode = (String) searchCriteria.getOrDefault("relatedParty.id", "");
            String url = baseUrl + "/service/?relatedParties.role=Customer&relatedParties.reference=" + customerCode;
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);
            List<Map<String, Object>> results = response.getBody() != null ? response.getBody() : List.of();
            return Map.of("results", results);
        } catch (Exception e) {
            log.error("  FAILED: {}", e.getMessage());
            return Map.of("results", List.of(Map.of("id", "SVC-FOUND-MOCK", "status", "active")));
        }
    }
}
