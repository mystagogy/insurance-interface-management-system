package io.github.mystagogy.insuranceinterface.domain.dashboard.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.CallStatus;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiCallHistoryRepository;
import io.github.mystagogy.insuranceinterface.domain.dashboard.dto.DashboardSummaryResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ApiCallHistoryRepository apiCallHistoryRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummaryReturnsAggregatedCountsForToday() {
        when(apiCallHistoryRepository.countByRequestTimeGreaterThanEqualAndRequestTimeLessThan(any(), any()))
            .thenReturn(25L);
        when(apiCallHistoryRepository.countByRequestTimeGreaterThanEqualAndRequestTimeLessThanAndCallStatus(
            any(),
            any(),
            any()
        ))
            .thenReturn(20L, 5L);

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.todayTotal()).isEqualTo(25L);
        assertThat(summary.successCount()).isEqualTo(20L);
        assertThat(summary.failCount()).isEqualTo(5L);
    }

    @Test
    void getSummaryUsesTodayRangeAndStatusFilters() {
        when(apiCallHistoryRepository.countByRequestTimeGreaterThanEqualAndRequestTimeLessThan(any(), any()))
            .thenReturn(0L);
        when(apiCallHistoryRepository.countByRequestTimeGreaterThanEqualAndRequestTimeLessThanAndCallStatus(
            any(),
            any(),
            any()
        ))
            .thenReturn(0L);

        dashboardService.getSummary();

        ArgumentCaptor<LocalDateTime> totalStartCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> totalEndCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(apiCallHistoryRepository).countByRequestTimeGreaterThanEqualAndRequestTimeLessThan(
            totalStartCaptor.capture(),
            totalEndCaptor.capture()
        );

        ArgumentCaptor<LocalDateTime> successStartCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> successEndCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(apiCallHistoryRepository).countByRequestTimeGreaterThanEqualAndRequestTimeLessThanAndCallStatus(
            successStartCaptor.capture(),
            successEndCaptor.capture(),
            eq(CallStatus.SUCCESS)
        );

        ArgumentCaptor<LocalDateTime> failStartCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> failEndCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(apiCallHistoryRepository).countByRequestTimeGreaterThanEqualAndRequestTimeLessThanAndCallStatus(
            failStartCaptor.capture(),
            failEndCaptor.capture(),
            eq(CallStatus.FAIL)
        );

        assertThat(totalEndCaptor.getValue()).isEqualTo(totalStartCaptor.getValue().plusDays(1));
        assertThat(successStartCaptor.getValue()).isEqualTo(totalStartCaptor.getValue());
        assertThat(successEndCaptor.getValue()).isEqualTo(totalEndCaptor.getValue());
        assertThat(failStartCaptor.getValue()).isEqualTo(totalStartCaptor.getValue());
        assertThat(failEndCaptor.getValue()).isEqualTo(totalEndCaptor.getValue());
    }
}
