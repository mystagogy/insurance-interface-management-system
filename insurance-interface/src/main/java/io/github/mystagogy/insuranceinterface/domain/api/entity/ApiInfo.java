package io.github.mystagogy.insuranceinterface.domain.api.entity;

import io.github.mystagogy.insuranceinterface.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "api_info",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_api_info_api_name", columnNames = "api_name")
    }
)
public class ApiInfo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "api_id")
    private Long id;

    @Column(name = "api_name", nullable = false, length = 100)
    private String apiName;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "timeout_ms", nullable = false)
    private int timeoutMs;

    @Column(name = "use_yn", nullable = false)
    private boolean useYn;

    protected ApiInfo() {
    }

    public ApiInfo(String apiName, String providerName, String baseUrl, int timeoutMs) {
        this.apiName = apiName;
        this.providerName = providerName;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.useYn = true;
    }

    public Long getId() {
        return id;
    }

    public String getApiName() {
        return apiName;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public boolean isUseYn() {
        return useYn;
    }

    public void deactivate() {
        this.useYn = false;
    }
}
