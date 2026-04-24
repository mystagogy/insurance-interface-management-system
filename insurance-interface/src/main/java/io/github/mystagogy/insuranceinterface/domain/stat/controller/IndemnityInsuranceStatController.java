package io.github.mystagogy.insuranceinterface.domain.stat.controller;

import io.github.mystagogy.insuranceinterface.common.response.ApiResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.IndemnityInsuranceStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.IndemnityInsuranceStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.service.IndemnityInsuranceStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "실손보험 가입정보")
@Validated
@RestController
@RequestMapping("/api/v1/stats/indemnity-insurance/subscriptions")
public class IndemnityInsuranceStatController {

    private final IndemnityInsuranceStatService indemnityInsuranceStatService;

    public IndemnityInsuranceStatController(IndemnityInsuranceStatService indemnityInsuranceStatService) {
        this.indemnityInsuranceStatService = indemnityInsuranceStatService;
    }

    @Operation(
        summary = "실손보험 가입 통계 조회",
        description = """
            조회 기간(fromYm~toYm)을 기준으로 실손보험 가입 통계를 조회합니다.
            sync=true 이면 외부 API를 호출해 최신 데이터를 동기화한 뒤 통계를 반환합니다.
            sync=false 이면 저장된 통계 데이터만 조회합니다.
            """
    )
    @GetMapping
    public ApiResponse<IndemnityInsuranceStatResponse> getSubscriptionStats(
        @ParameterObject @Valid IndemnityInsuranceStatQueryRequest request,
        @Parameter(description = "true: 외부 API 동기화 후 조회, false: 저장된 데이터만 조회")
        @RequestParam(defaultValue = "false") boolean sync
    ) {
        if (sync) {
            return ApiResponse.ok(indemnityInsuranceStatService.refreshSubscriptionStats(request));
        }
        return ApiResponse.ok(indemnityInsuranceStatService.getStoredSubscriptionStats(request));
    }
}
