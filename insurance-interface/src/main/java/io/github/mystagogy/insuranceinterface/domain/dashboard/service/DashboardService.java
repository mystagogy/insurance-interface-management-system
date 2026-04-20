package io.github.mystagogy.insuranceinterface.domain.dashboard.service;

import io.github.mystagogy.insuranceinterface.domain.dashboard.dto.DashboardSummaryResponse;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    public DashboardSummaryResponse getSummary() {
        return new DashboardSummaryResponse(120L, 108L, 12L);
    }
}

