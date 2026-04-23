package io.github.mystagogy.insuranceinterface.domain.stat.client;

import io.github.mystagogy.insuranceinterface.domain.stat.dto.external.LifeInsuranceExternalItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class LifeInsuranceApiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final LifeInsuranceApiClient lifeInsuranceApiClient = new LifeInsuranceApiClient(
        WebClient.builder().build(),
        objectMapper,
        "http://example.com",
        "service-key",
        5000,
        300
    );

    @Test
    void mapsSingleUnknownMetricToSubscriptionRateWhenCountIsAlreadyParsed() throws Exception {
        JsonNode itemNode = objectMapper.readTree("""
            {
              "sttsAccmlTrgtYr": "2024",
              "areaNm": "대전",
              "sexNm": "여자",
              "rchnAggr": "30대",
              "isuKindNm": "연금",
              "joinCnt": "9567",
              "unexpectedRate": "9.5"
            }
            """);

        LifeInsuranceExternalItem item = ReflectionTestUtils.invokeMethod(
            lifeInsuranceApiClient,
            "mapItem",
            "2024",
            itemNode
        );

        assertThat(item.subscriptionCount()).isEqualTo(9567L);
        assertThat(item.subscriptionRate()).isEqualByComparingTo(new BigDecimal("9.5"));
    }
}
