package io.github.mystagogy.insuranceinterface.domain.dashboard.dto;

public record DashboardSummaryResponse(long todayTotal, long successCount, long failCount) {
}

