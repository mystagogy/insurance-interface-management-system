package io.github.mystagogy.insuranceinterface.domain.history.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiCallHistoryRepository;
import io.github.mystagogy.insuranceinterface.domain.history.dto.HistoryItemResponse;
import io.github.mystagogy.insuranceinterface.domain.history.dto.HistoryQueryRequest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private ApiCallHistoryRepository apiCallHistoryRepository;

    @InjectMocks
    private HistoryService historyService;

    @Test
    void getRecentMapsHistoryToResponseDto() {
        ApiInfo successApiInfo = new ApiInfo("CAR_INSURANCE_CONTRACT", "provider", "http://example.com", 3000);
        ApiInfo failApiInfo = new ApiInfo("LIFE_INSURANCE_SUBSCRIPTION", "provider", "http://example.com", 3000);

        ApiCallHistory successHistory = new ApiCallHistory(successApiInfo, null, "from=2024");
        ReflectionTestUtils.setField(successHistory, "id", 10L);
        ReflectionTestUtils.setField(successHistory, "requestTime", LocalDateTime.of(2024, 4, 1, 10, 20, 30));
        successHistory.completeSuccess(200, "ok");

        ApiCallHistory failHistory = new ApiCallHistory(failApiInfo, null, "from=2024");
        ReflectionTestUtils.setField(failHistory, "id", 11L);
        ReflectionTestUtils.setField(failHistory, "requestTime", LocalDateTime.of(2024, 4, 2, 11, 22, 33));
        failHistory.completeFailure(500, "error", "failed");

        when(apiCallHistoryRepository.findByRequestTimeGreaterThanEqualAndRequestTimeLessThanOrderByRequestTimeDesc(
            LocalDate.of(2024, 4, 1).atStartOfDay(),
            LocalDate.of(2024, 5, 1).atStartOfDay()
        ))
            .thenReturn(List.of(failHistory, successHistory));

        List<HistoryItemResponse> responses = historyService.getRecent(new HistoryQueryRequest("20240401", "20240430"));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).requestId()).isEqualTo("REQ-11");
        assertThat(responses.get(0).requestTime()).isEqualTo("2024-04-02 11:22:33");
        assertThat(responses.get(0).interfaceName()).isEqualTo("생명보험 가입 통계 조회");
        assertThat(responses.get(0).status()).isEqualTo("FAIL");
        assertThat(responses.get(0).errorMessage()).isEqualTo("error");
        assertThat(responses.get(1).requestId()).isEqualTo("REQ-10");
        assertThat(responses.get(1).requestTime()).isEqualTo("2024-04-01 10:20:30");
        assertThat(responses.get(1).interfaceName()).isEqualTo("자동차보험 계약 통계 조회");
        assertThat(responses.get(1).status()).isEqualTo("SUCCESS");
        assertThat(responses.get(1).errorMessage()).isNull();
        verify(apiCallHistoryRepository).findByRequestTimeGreaterThanEqualAndRequestTimeLessThanOrderByRequestTimeDesc(
            LocalDate.of(2024, 4, 1).atStartOfDay(),
            LocalDate.of(2024, 5, 1).atStartOfDay()
        );
    }

    @Test
    void getRecentUsesApiNameWhenInterfaceTypeIsUnknown() {
        ApiInfo apiInfo = new ApiInfo("UNKNOWN_API_NAME", "provider", "http://example.com", 3000);
        ApiCallHistory history = new ApiCallHistory(apiInfo, null, "from=2024");
        ReflectionTestUtils.setField(history, "id", 12L);
        ReflectionTestUtils.setField(history, "requestTime", LocalDateTime.of(2024, 4, 3, 12, 23, 34));
        history.completeFailure(500, "error", "failed");

        when(apiCallHistoryRepository.findByRequestTimeGreaterThanEqualAndRequestTimeLessThanOrderByRequestTimeDesc(any(), any()))
            .thenReturn(List.of(history));

        List<HistoryItemResponse> responses = historyService.getRecent(new HistoryQueryRequest("20240401", "20240430"));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).interfaceName()).isEqualTo("UNKNOWN_API_NAME");
    }

    @Test
    void getRecentReturnsEmptyListWhenNoHistoryExists() {
        when(apiCallHistoryRepository.findByRequestTimeGreaterThanEqualAndRequestTimeLessThanOrderByRequestTimeDesc(any(), any()))
            .thenReturn(List.of());

        List<HistoryItemResponse> responses = historyService.getRecent(new HistoryQueryRequest("20240401", "20240430"));

        assertThat(responses).isEmpty();
        verify(apiCallHistoryRepository).findByRequestTimeGreaterThanEqualAndRequestTimeLessThanOrderByRequestTimeDesc(
            LocalDate.of(2024, 4, 1).atStartOfDay(),
            LocalDate.of(2024, 5, 1).atStartOfDay()
        );
    }

    @Test
    void getRecentUsesDefaultLastWeekRangeWhenDateQueryMissing() {
        when(apiCallHistoryRepository.findByRequestTimeGreaterThanEqualAndRequestTimeLessThanOrderByRequestTimeDesc(any(), any()))
            .thenReturn(List.of());

        historyService.getRecent(new HistoryQueryRequest(null, null));

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(apiCallHistoryRepository).findByRequestTimeGreaterThanEqualAndRequestTimeLessThanOrderByRequestTimeDesc(
            fromCaptor.capture(),
            toCaptor.capture()
        );

        assertThat(Duration.between(fromCaptor.getValue(), toCaptor.getValue())).isEqualTo(Duration.ofDays(7));
        assertThat(toCaptor.getValue().toLocalDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void getRecentRejectsWhenFromDateIsAfterToDate() {
        assertThatThrownBy(() -> historyService.getRecent(new HistoryQueryRequest("20240502", "20240501")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("조회 시작일자는 종료일자보다 늦을 수 없습니다.");
    }

    @Test
    void getRecentRejectsInvalidDateValue() {
        assertThatThrownBy(() -> historyService.getRecent(new HistoryQueryRequest("20240231", "20240301")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("조회 시작일자는 유효한 yyyyMMdd 날짜여야 합니다.");
    }

    @Test
    void getRecentUsesFutureToDateAsIs() {
        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(5);

        when(apiCallHistoryRepository.findByRequestTimeGreaterThanEqualAndRequestTimeLessThanOrderByRequestTimeDesc(any(), any()))
            .thenReturn(List.of());

        historyService.getRecent(new HistoryQueryRequest(
            today.minusDays(2).format(DateTimeFormatter.BASIC_ISO_DATE),
            future.format(DateTimeFormatter.BASIC_ISO_DATE)
        ));

        verify(apiCallHistoryRepository).findByRequestTimeGreaterThanEqualAndRequestTimeLessThanOrderByRequestTimeDesc(
            today.minusDays(2).atStartOfDay(),
            future.plusDays(1).atStartOfDay()
        );
    }
}
