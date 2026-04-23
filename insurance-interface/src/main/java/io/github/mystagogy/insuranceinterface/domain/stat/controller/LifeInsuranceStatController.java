package io.github.mystagogy.insuranceinterface.domain.stat.controller;

import io.github.mystagogy.insuranceinterface.common.response.ApiResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.LifeInsuranceStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.LifeInsuranceStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.service.LifeInsuranceStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "생명보험 가입정보")
@Validated
@RestController
@RequestMapping("/api/v1/stats/life-insurance/subscriptions")
public class LifeInsuranceStatController {

    private final LifeInsuranceStatService lifeInsuranceStatService;

    public LifeInsuranceStatController(LifeInsuranceStatService lifeInsuranceStatService) {
        this.lifeInsuranceStatService = lifeInsuranceStatService;
    }

    @Operation(
        summary = "생명보험 가입 통계 조회",
        description = """
            조회 기간(fromYear~toYear)을 기준으로 생명보험 가입 통계를 조회합니다.
            요청 시마다 외부 API를 호출해 최신 데이터를 동기화한 뒤 저장된 통계를 반환합니다.
            """
    )
    @GetMapping
    public ApiResponse<LifeInsuranceStatResponse> getSubscriptionStats(
        @ParameterObject @Valid LifeInsuranceStatQueryRequest request
    ) {
        return ApiResponse.ok(lifeInsuranceStatService.getSubscriptionStats(request));
    }
}
