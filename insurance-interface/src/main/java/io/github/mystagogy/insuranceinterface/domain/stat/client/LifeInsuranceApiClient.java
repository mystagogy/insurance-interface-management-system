package io.github.mystagogy.insuranceinterface.domain.stat.client;

import io.github.mystagogy.insuranceinterface.domain.stat.dto.external.LifeInsuranceExternalItem;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.GenderType;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class LifeInsuranceApiClient {

    private static final Set<String> DIMENSION_FIELDS = Set.of(
        "sttsAccmlTrgtYr",
        "areaNm",
        "sexNm",
        "rchnAggr",
        "aggr",
        "isuKindNm",
        "isuItmsNm"
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String serviceKey;
    private final int timeoutMs;
    private final int numOfRows;

    public LifeInsuranceApiClient(
        WebClient webClient,
        ObjectMapper objectMapper,
        @Value("${external.life-insurance.base-url}") String baseUrl,
        @Value("${external.life-insurance.service-key:}") String serviceKey,
        @Value("${external.life-insurance.timeout-ms:5000}") int timeoutMs,
        @Value("${external.life-insurance.num-of-rows:300}") int numOfRows
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
        this.timeoutMs = timeoutMs;
        this.numOfRows = numOfRows;
    }

    public List<LifeInsuranceExternalItem> fetch(String fromYear, String toYear) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("생명보험 가입정보 API Decoding service key가 설정되지 않았습니다.");
        }

        List<LifeInsuranceExternalItem> results = new ArrayList<>();
        int currentYear = Integer.parseInt(fromYear);
        int endYear = Integer.parseInt(toYear);
        while (currentYear <= endYear) {
            results.addAll(fetchByYear(String.valueOf(currentYear)));
            currentYear++;
        }
        return results;
    }

    private List<LifeInsuranceExternalItem> fetchByYear(String year) {
        List<LifeInsuranceExternalItem> results = new ArrayList<>();
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;

        while (results.size() < totalCount) {
            int currentPageNo = pageNo;
            String requestUri = buildRequestUri(year, currentPageNo);
            JsonNode bodyNode = requestBody(requestUri);
            totalCount = bodyNode.path("totalCount").asInt(0);
            List<JsonNode> itemNodes = extractItemNodes(bodyNode.path("items").path("item"));
            for (JsonNode itemNode : itemNodes) {
                results.add(mapItem(year, itemNode));
            }

            if (itemNodes.isEmpty()) {
                break;
            }
            pageNo++;
        }

        return results;
    }

    private String buildRequestUri(String year, int pageNo) {
        return baseUrl + "/getLifeInsuJoinStatus"
            + "?serviceKey=" + encode(serviceKey)
            + "&pageNo=" + pageNo
            + "&numOfRows=" + numOfRows
            + "&resultType=json"
            + "&likeSttsAccmlTrgtYr=" + encode(year);
    }

    private JsonNode requestBody(String requestUri) {
        String responseBody = webClient.get()
            .uri(URI.create(requestUri))
            .retrieve()
            .bodyToMono(String.class)
            .block(Duration.ofMillis(timeoutMs));

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("생명보험 가입정보 API 응답이 비어 있습니다.");
        }

        JsonNode root = readTree(responseBody);
        JsonNode responseNode = root.path("response");
        JsonNode headerNode = responseNode.path("header");
        String resultCode = headerNode.path("resultCode").asText("");
        if (!"00".equals(resultCode)) {
            String resultMessage = headerNode.path("resultMsg").asText("알 수 없는 외부 API 오류");
            throw new IllegalStateException("생명보험 가입정보 API 오류: " + resultMessage);
        }
        return responseNode.path("body");
    }

    private JsonNode readTree(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("생명보험 가입정보 API 응답 파싱에 실패했습니다.", exception);
        }
    }

    private List<JsonNode> extractItemNodes(JsonNode itemNode) {
        if (itemNode == null || itemNode.isMissingNode() || itemNode.isNull()) {
            return List.of();
        }
        if (itemNode.isArray()) {
            List<JsonNode> nodes = new ArrayList<>();
            itemNode.forEach(nodes::add);
            return nodes;
        }
        return List.of(itemNode);
    }

    private LifeInsuranceExternalItem mapItem(String requestYear, JsonNode itemNode) {
        String statYear = defaultText(readText(itemNode, "sttsAccmlTrgtYr", "baseYear", "year"), requestYear);
        long subscriptionCount = readLong(itemNode, "subscriptionCount", "sbcnCnt", "joinCnt", "insuJoinCnt");
        BigDecimal subscriptionRate = readDecimal(
            itemNode,
            "subscriptionRate",
            "joinRt",
            "joinRto",
            "sbcnRt",
            "insuJoinRt",
            "jngpRt"
        );

        if (subscriptionCount == 0L || subscriptionRate == null) {
            List<BigDecimal> unknownMetrics = readUnknownNumericValues(itemNode);
            if (subscriptionCount == 0L && !unknownMetrics.isEmpty()) {
                subscriptionCount = unknownMetrics.get(0).longValue();
            }
            if (subscriptionRate == null && unknownMetrics.size() >= 2) {
                subscriptionRate = unknownMetrics.get(1);
            } else if (subscriptionRate == null && subscriptionCount != 0L && unknownMetrics.size() == 1) {
                subscriptionRate = unknownMetrics.get(0);
            }
        }

        return new LifeInsuranceExternalItem(
            LocalDate.of(Integer.parseInt(statYear), 1, 1),
            defaultText(readText(itemNode, "areaNm", "areaName"), "미상"),
            defaultText(readText(itemNode, "rchnAggr", "aggr", "ageGroup"), "미상"),
            GenderType.fromLabel(readText(itemNode, "sexNm", "gender")),
            defaultText(readText(itemNode, "isuKindNm", "isuItmsNm", "insuranceType"), "미상"),
            subscriptionCount,
            subscriptionRate != null ? subscriptionRate : BigDecimal.ZERO,
            itemNode.toString()
        );
    }

    private List<BigDecimal> readUnknownNumericValues(JsonNode itemNode) {
        List<BigDecimal> values = new ArrayList<>();
        Set<String> handled = new HashSet<>(DIMENSION_FIELDS);
        handled.add("subscriptionCount");
        handled.add("subscriptionRate");
        handled.add("sbcnCnt");
        handled.add("joinCnt");
        handled.add("insuJoinCnt");
        handled.add("joinRt");
        handled.add("joinRto");
        handled.add("sbcnRt");
        handled.add("insuJoinRt");
        handled.add("jngpRt");

        for (String fieldName : itemNode.propertyNames()) {
            if (handled.contains(fieldName)) {
                continue;
            }
            JsonNode valueNode = itemNode.path(fieldName);
            if (!valueNode.isValueNode()) {
                continue;
            }
            String text = valueNode.asText("");
            if (text.isBlank()) {
                continue;
            }
            String normalized = normalizeNumber(text);
            if (normalized.matches("-?\\d+(\\.\\d+)?")) {
                values.add(new BigDecimal(normalized));
            }
        }
        return values;
    }

    private long readLong(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String text = readText(node, fieldName);
            if (!text.isBlank()) {
                String normalized = normalizeNumber(text);
                if (!normalized.isBlank()) {
                    return new BigDecimal(normalized).longValue();
                }
            }
        }
        return 0L;
    }

    private BigDecimal readDecimal(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String text = readText(node, fieldName);
            if (!text.isBlank()) {
                String normalized = normalizeNumber(text);
                if (!normalized.isBlank()) {
                    return new BigDecimal(normalized);
                }
            }
        }
        return null;
    }

    private String normalizeNumber(String value) {
        return value.replace(",", "").replace(" ", "").trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String readText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                String value = valueNode.asText("").trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
