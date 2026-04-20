package io.github.mystagogy.insuranceinterface.domain.log.entity;

import io.github.mystagogy.insuranceinterface.common.entity.BaseTimeEntity;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "error_log",
    indexes = {
        @Index(name = "idx_error_log_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_error_log_api_occurred_at", columnList = "api_id, occurred_at")
    }
)
public class ErrorLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_id", nullable = false)
    private ApiInfo apiInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id")
    private ApiCallHistory apiCallHistory;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", nullable = false, length = 30)
    private ErrorType errorType;

    @Column(name = "error_message", nullable = false, length = 2000)
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    protected ErrorLog() {
    }

    public ErrorLog(
        ApiInfo apiInfo,
        ApiCallHistory apiCallHistory,
        ErrorType errorType,
        String errorMessage,
        String stackTrace
    ) {
        this.apiInfo = apiInfo;
        this.apiCallHistory = apiCallHistory;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.occurredAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ApiInfo getApiInfo() {
        return apiInfo;
    }

    public ApiCallHistory getApiCallHistory() {
        return apiCallHistory;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
