package io.github.mystagogy.insuranceinterface.domain.stat.client;

import io.github.mystagogy.insuranceinterface.domain.stat.dto.external.IndemnityInsuranceExternalItem;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.GenderType;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class IndemnityInsuranceApiClient {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter BASIC_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String serviceKey;
    private final int timeoutMs;
    private final int numOfRows;

    public IndemnityInsuranceApiClient(
        WebClient webClient,
        ObjectMapper objectMapper,
        @Value("${external.indemnity-insurance.base-url}") String baseUrl,
        @Value("${external.indemnity-insurance.service-key:}") String serviceKey,
        @Value("${external.indemnity-insurance.timeout-ms:5000}") int timeoutMs,
        @Value("${external.indemnity-insurance.num-of-rows:300}") int numOfRows
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
        this.timeoutMs = timeoutMs;
        this.numOfRows = numOfRows;
    }

    public List<IndemnityInsuranceExternalItem> fetch(String fromYm, String toYm) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("실손보험 가입정보 API Decoding service key가 설정되지 않았습니다.");
        }

        List<IndemnityInsuranceExternalItem> results = new ArrayList<>();
        YearMonth current = YearMonth.parse(fromYm, YEAR_MONTH_FORMATTER);
        YearMonth end = YearMonth.parse(toYm, YEAR_MONTH_FORMATTER);
        while (!current.isAfter(end)) {
            results.addAll(fetchByMonth(current.format(YEAR_MONTH_FORMATTER)));
            current = current.plusMonths(1);
        }
        return results;
    }

    private List<IndemnityInsuranceExternalItem> fetchByMonth(String yearMonth) {
        List<IndemnityInsuranceExternalItem> results = new ArrayList<>();
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;
        int fetchedRowCount = 0;

        while (fetchedRowCount < totalCount) {
            int currentPageNo = pageNo;
            String requestUri = buildRequestUri(yearMonth, currentPageNo);
            JsonNode bodyNode = requestBody(requestUri);
            totalCount = bodyNode.path("totalCount").asInt(0);
            List<JsonNode> itemNodes = extractItemNodes(bodyNode.path("items").path("item"));
            for (JsonNode itemNode : itemNodes) {
                results.addAll(mapItems(itemNode));
            }
            fetchedRowCount += itemNodes.size();

            if (itemNodes.isEmpty()) {
                break;
            }
            pageNo++;
        }

        return results;
    }

    private String buildRequestUri(String yearMonth, int pageNo) {
        return baseUrl + "/getInsuranceInfo"
            + "?serviceKey=" + encode(serviceKey)
            + "&pageNo=" + pageNo
            + "&numOfRows=" + numOfRows
            + "&resultType=json"
            + "&likeBasDt=" + encode(yearMonth + "01");
    }

    private JsonNode requestBody(String requestUri) {
        String responseBody = webClient.get()
            .uri(URI.create(requestUri))
            .retrieve()
            .bodyToMono(String.class)
            .block(Duration.ofMillis(timeoutMs));

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("실손보험 가입정보 API 응답이 비어 있습니다.");
        }

        JsonNode root = readTree(responseBody);
        JsonNode responseNode = root.path("response");
        JsonNode headerNode = responseNode.path("header");
        String resultCode = headerNode.path("resultCode").asText("");
        if (!"00".equals(resultCode)) {
            String resultMessage = headerNode.path("resultMsg").asText("알 수 없는 외부 API 오류");
            throw new IllegalStateException("실손보험 가입정보 API 오류: " + resultMessage);
        }
        return responseNode.path("body");
    }

    private JsonNode readTree(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("실손보험 가입정보 API 응답 파싱에 실패했습니다.", exception);
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

    private List<IndemnityInsuranceExternalItem> mapItems(JsonNode itemNode) {
        LocalDate statDate = parseBasDt(readText(itemNode, "basDt"));
        String ageGroup = defaultText(readText(itemNode, "age"), "미상");
        String indemnityType = defaultText(readText(itemNode, "ptrn"), "미상");
        String coverageItem = defaultText(readText(itemNode, "mog"), "미상");
        BigDecimal malePremium = readDecimal(itemNode, "mlInsRt");
        BigDecimal femalePremium = readDecimal(itemNode, "fmlInsRt");

        return List.of(
            new IndemnityInsuranceExternalItem(
                statDate,
                ageGroup,
                GenderType.MALE,
                indemnityType,
                coverageItem,
                malePremium != null ? malePremium : BigDecimal.ZERO,
                itemNode.toString()
            ),
            new IndemnityInsuranceExternalItem(
                statDate,
                ageGroup,
                GenderType.FEMALE,
                indemnityType,
                coverageItem,
                femalePremium != null ? femalePremium : BigDecimal.ZERO,
                itemNode.toString()
            )
        );
    }

    private LocalDate parseBasDt(String basDt) {
        if (basDt == null || basDt.isBlank()) {
            throw new IllegalStateException("실손보험 가입정보 API 기준일자가 비어 있습니다.");
        }

        try {
            String value = basDt.trim();
            if (value.length() == 8) {
                return LocalDate.parse(value, BASIC_DATE_FORMATTER);
            }
            if (value.length() == 6) {
                return YearMonth.parse(value, YEAR_MONTH_FORMATTER).atDay(1);
            }
            throw new DateTimeParseException("invalid basDt format", value, 0);
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("실손보험 가입정보 API 기준일자 파싱에 실패했습니다.", exception);
        }
    }

    private BigDecimal readDecimal(JsonNode node, String fieldName) {
        String text = readText(node, fieldName);
        if (!text.isBlank()) {
            String normalized = normalizeNumber(text);
            if (!normalized.isBlank()) {
                return new BigDecimal(normalized);
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
