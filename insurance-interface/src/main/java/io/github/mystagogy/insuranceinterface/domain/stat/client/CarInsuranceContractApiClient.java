package io.github.mystagogy.insuranceinterface.domain.stat.client;

import io.github.mystagogy.insuranceinterface.domain.stat.dto.external.CarInsuranceContractExternalItem;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.GenderType;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
public class CarInsuranceContractApiClient {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private static final Set<String> DIMENSION_FIELDS = Set.of(
        "isuCmpyOfrYm",
        "isuItmsNm",
        "mogClsfNm",
        "sexNm",
        "aggr",
        "atmbPlorNm",
        "kncrNm"
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String serviceKey;
    private final int timeoutMs;
    private final int numOfRows;

    public CarInsuranceContractApiClient(
        WebClient webClient,
        ObjectMapper objectMapper,
        @Value("${external.car-insurance-contract.base-url}") String baseUrl,
        @Value("${external.car-insurance-contract.service-key:}") String serviceKey,
        @Value("${external.car-insurance-contract.timeout-ms:5000}") int timeoutMs,
        @Value("${external.car-insurance-contract.num-of-rows:300}") int numOfRows
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
        this.timeoutMs = timeoutMs;
        this.numOfRows = numOfRows;
    }

    public List<CarInsuranceContractExternalItem> fetch(String fromYm, String toYm) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("자동차보험 계약정보 API service key가 설정되지 않았습니다.");
        }

        List<CarInsuranceContractExternalItem> results = new ArrayList<>();
        YearMonth current = YearMonth.parse(fromYm, YEAR_MONTH_FORMATTER);
        YearMonth end = YearMonth.parse(toYm, YEAR_MONTH_FORMATTER);
        while (!current.isAfter(end)) {
            results.addAll(fetchByMonth(current.format(YEAR_MONTH_FORMATTER)));
            current = current.plusMonths(1);
        }
        return results;
    }

    private List<CarInsuranceContractExternalItem> fetchByMonth(String yearMonth) {
        List<CarInsuranceContractExternalItem> results = new ArrayList<>();
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;

        while (results.size() < totalCount) {
            int currentPageNo = pageNo;
            String requestUri = buildRequestUri(yearMonth, currentPageNo);
            JsonNode bodyNode = requestBody(requestUri);
            totalCount = bodyNode.path("totalCount").asInt(0);
            List<JsonNode> itemNodes = extractItemNodes(bodyNode.path("items").path("item"));
            for (JsonNode itemNode : itemNodes) {
                results.add(mapItem(itemNode));
            }

            if (itemNodes.isEmpty()) {
                break;
            }
            pageNo++;
        }

        return results;
    }

    private String buildRequestUri(String yearMonth, int pageNo) {
        return baseUrl + "/getContractInfo"
            + "?serviceKey=" + encode(serviceKey)
            + "&pageNo=" + pageNo
            + "&numOfRows=" + numOfRows
            + "&resultType=json"
            + "&likeIsuCmpyOfrYm=" + encode(yearMonth);
    }

    private JsonNode requestBody(String requestUri) {
        String responseBody = webClient.get()
            .uri(URI.create(requestUri))
            .retrieve()
            .bodyToMono(String.class)
            .block(Duration.ofMillis(timeoutMs));

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("자동차보험 계약정보 API 응답이 비어 있습니다.");
        }

        JsonNode root = readTree(responseBody);
        JsonNode responseNode = root.path("response");
        JsonNode headerNode = responseNode.path("header");
        String resultCode = headerNode.path("resultCode").asText("");
        if (!"00".equals(resultCode)) {
            String resultMessage = headerNode.path("resultMsg").asText("알 수 없는 외부 API 오류");
            throw new IllegalStateException("자동차보험 계약정보 API 오류: " + resultMessage);
        }
        return responseNode.path("body");
    }

    private JsonNode readTree(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("자동차보험 계약정보 API 응답 파싱에 실패했습니다.", exception);
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

    private CarInsuranceContractExternalItem mapItem(JsonNode itemNode) {
        BigDecimal earnedPremium = readDecimal(itemNode, "earnedPremium", "erndInsPrm", "pmmiPrem", "premAmt", "elpsInpm");
        long contractCount = readLong(itemNode, "contractCount", "sbcnCnt", "joinCnt", "insuJoinCnt");

        if (contractCount == 0L || earnedPremium == null) {
            List<BigDecimal> unknownMetrics = readUnknownNumericValues(itemNode);
            if (contractCount == 0L && !unknownMetrics.isEmpty()) {
                contractCount = unknownMetrics.get(0).longValue();
            }
            if (earnedPremium == null) {
                if (unknownMetrics.size() >= 2) {
                    earnedPremium = unknownMetrics.get(1);
                } else if (unknownMetrics.size() == 1) {
                    earnedPremium = unknownMetrics.get(0);
                }
            }
        }

        return new CarInsuranceContractExternalItem(
            readText(itemNode, "isuCmpyOfrYm"),
            readText(itemNode, "isuItmsNm"),
            readText(itemNode, "mogClsfNm"),
            GenderType.fromLabel(readText(itemNode, "sexNm")),
            defaultText(readText(itemNode, "aggr"), "미상"),
            defaultText(readText(itemNode, "atmbPlorNm"), "미상"),
            defaultText(readText(itemNode, "kncrNm"), "미상"),
            contractCount,
            earnedPremium != null ? earnedPremium : BigDecimal.ZERO,
            itemNode.toString()
        );
    }

    private List<BigDecimal> readUnknownNumericValues(JsonNode itemNode) {
        List<BigDecimal> values = new ArrayList<>();
        Set<String> handled = new HashSet<>(DIMENSION_FIELDS);
        handled.add("contractCount");
        handled.add("earnedPremium");
        handled.add("erndInsPrm");
        handled.add("pmmiPrem");
        handled.add("premAmt");
        handled.add("elpsInpm");
        handled.add("sbcnCnt");
        handled.add("joinCnt");
        handled.add("insuJoinCnt");

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

    private String readText(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        return valueNode.isMissingNode() || valueNode.isNull() ? "" : valueNode.asText("").trim();
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
