package io.github.mystagogy.insuranceinterface.domain.dashboard.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.CallStatus;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiCallHistoryRepository;
import io.github.mystagogy.insuranceinterface.domain.dashboard.dto.DashboardSummaryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final ApiCallHistoryRepository apiCallHistoryRepository;

    public DashboardService(ApiCallHistoryRepository apiCallHistoryRepository) {
        this.apiCallHistoryRepository = apiCallHistoryRepository;
    }

    public DashboardSummaryResponse getSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        long todayTotal = apiCallHistoryRepository.countByRequestTimeGreaterThanEqualAndRequestTimeLessThan(
            startOfToday,
            startOfTomorrow
        );
        long successCount = apiCallHistoryRepository.countByRequestTimeGreaterThanEqualAndRequestTimeLessThanAndCallStatus(
            startOfToday,
            startOfTomorrow,
            CallStatus.SUCCESS
        );
        long failCount = apiCallHistoryRepository.countByRequestTimeGreaterThanEqualAndRequestTimeLessThanAndCallStatus(
            startOfToday,
            startOfTomorrow,
            CallStatus.FAIL
        );

        return new DashboardSummaryResponse(todayTotal, successCount, failCount);
    }
}
