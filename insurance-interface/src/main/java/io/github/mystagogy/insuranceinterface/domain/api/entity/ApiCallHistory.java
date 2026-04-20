package io.github.mystagogy.insuranceinterface.domain.api.entity;

import io.github.mystagogy.insuranceinterface.common.entity.BaseTimeEntity;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
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
    name = "api_call_history",
    indexes = {
        @Index(name = "idx_api_call_history_request_time", columnList = "request_time"),
        @Index(name = "idx_api_call_history_success_request_time", columnList = "success_yn, request_time"),
        @Index(name = "idx_api_call_history_api_request_time", columnList = "api_id, request_time"),
        @Index(name = "idx_api_call_history_status_request_time", columnList = "call_status, request_time")
    }
)
public class ApiCallHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_id", nullable = false)
    private ApiInfo apiInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @Column(name = "request_time", nullable = false)
    private LocalDateTime requestTime;

    @Column(name = "response_time")
    private LocalDateTime responseTime;

    @Column(name = "processing_ms")
    private Long processingMs;

    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "success_yn", nullable = false)
    private boolean successYn;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_status", nullable = false, length = 20)
    private CallStatus callStatus;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "response_summary", columnDefinition = "TEXT")
    private String responseSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retry_of_history_id")
    private ApiCallHistory retryOfHistory;

    @Column(name = "retry_seq", nullable = false)
    private int retrySeq;

    protected ApiCallHistory() {
    }

    public ApiCallHistory(ApiInfo apiInfo, User requestedBy, String requestParams) {
        this(apiInfo, requestedBy, requestParams, null, 0);
    }

    public ApiCallHistory(
        ApiInfo apiInfo,
        User requestedBy,
        String requestParams,
        ApiCallHistory retryOfHistory,
        int retrySeq
    ) {
        this.apiInfo = apiInfo;
        this.requestedBy = requestedBy;
        this.requestParams = requestParams;
        this.retryOfHistory = retryOfHistory;
        this.retrySeq = retrySeq;
        this.requestTime = LocalDateTime.now();
        this.successYn = false;
        this.callStatus = CallStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public ApiInfo getApiInfo() {
        return apiInfo;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public LocalDateTime getResponseTime() {
        return responseTime;
    }

    public Long getProcessingMs() {
        return processingMs;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public boolean isSuccessYn() {
        return successYn;
    }

    public CallStatus getCallStatus() {
        return callStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public ApiCallHistory getRetryOfHistory() {
        return retryOfHistory;
    }

    public int getRetrySeq() {
        return retrySeq;
    }

    public void completeSuccess(int statusCode, String responseSummary) {
        this.responseTime = LocalDateTime.now();
        this.processingMs = java.time.Duration.between(this.requestTime, this.responseTime).toMillis();
        this.statusCode = statusCode;
        this.successYn = true;
        this.callStatus = CallStatus.SUCCESS;
        this.errorMessage = null;
        this.responseSummary = responseSummary;
    }

    public void completeFailure(int statusCode, String errorMessage, String responseSummary) {
        this.responseTime = LocalDateTime.now();
        this.processingMs = java.time.Duration.between(this.requestTime, this.responseTime).toMillis();
        this.statusCode = statusCode;
        this.successYn = false;
        this.callStatus = CallStatus.FAIL;
        this.errorMessage = errorMessage;
        this.responseSummary = responseSummary;
    }

    public void markRetried() {
        this.callStatus = CallStatus.RETRY;
        this.successYn = false;
    }
}
