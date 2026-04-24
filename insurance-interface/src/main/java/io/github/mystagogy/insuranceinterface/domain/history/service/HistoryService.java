package io.github.mystagogy.insuranceinterface.domain.history.service;

import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiCallHistoryRepository;
import io.github.mystagogy.insuranceinterface.domain.history.dto.HistoryItemResponse;
import io.github.mystagogy.insuranceinterface.domain.history.dto.HistoryQueryRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HistoryService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_LOOKBACK_DAYS = 6;

    private final ApiCallHistoryRepository apiCallHistoryRepository;

    public HistoryService(ApiCallHistoryRepository apiCallHistoryRepository) {
        this.apiCallHistoryRepository = apiCallHistoryRepository;
    }

    public List<HistoryItemResponse> getRecent(HistoryQueryRequest request) {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = parseDateOrDefault(
            request.from(),
            today.minusDays(DEFAULT_LOOKBACK_DAYS),
            "조회 시작일자는 유효한 yyyyMMdd 날짜여야 합니다."
        );
        LocalDate toDate = parseDateOrDefault(
            request.to(),
            today,
            "조회 종료일자는 유효한 yyyyMMdd 날짜여야 합니다."
        );
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("조회 시작일자는 종료일자보다 늦을 수 없습니다.");
        }

        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTimeExclusive = toDate.plusDays(1).atStartOfDay();

        return apiCallHistoryRepository.findByRequestTimeGreaterThanEqualAndRequestTimeLessThanOrderByRequestTimeDesc(
                fromDateTime,
                toDateTimeExclusive
            )
            .stream()
            .map(history -> new HistoryItemResponse(
                "REQ-" + history.getId(),
                history.getRequestTime().format(DATE_TIME_FORMATTER),
                toInterfaceName(history.getApiInfo().getApiName()),
                history.getCallStatus().name(),
                resolveErrorMessage(history.getCallStatus().name(), history.getErrorMessage())
            ))
            .toList();
    }

    private LocalDate parseDateOrDefault(String raw, LocalDate defaultDate, String errorMessage) {
        if (raw == null || raw.isBlank()) {
            return defaultDate;
        }
        try {
            return LocalDate.parse(raw, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String toInterfaceName(String apiName) {
        return switch (apiName) {
            case "CAR_INSURANCE_CONTRACT" -> "자동차보험 계약 통계 조회";
            case "LIFE_INSURANCE_SUBSCRIPTION" -> "생명보험 가입 통계 조회";
            case "INDEMNITY_INSURANCE_SUBSCRIPTION" -> "실손보험 가입 통계 조회";
            default -> apiName;
        };
    }

    private String resolveErrorMessage(String status, String errorMessage) {
        if (!"FAIL".equals(status)) {
            return null;
        }
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        return errorMessage;
    }
}
