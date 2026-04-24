package io.github.mystagogy.insuranceinterface.domain.stat.controller;

import io.github.mystagogy.insuranceinterface.common.response.ApiResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.CarInsuranceContractStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.CarInsuranceContractStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.service.CarInsuranceContractStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "자동차보험 계약정보")
@Validated
@RestController
@RequestMapping("/api/v1/stats/car-insurance/contracts")
public class CarInsuranceContractStatController {

    private final CarInsuranceContractStatService carInsuranceContractStatService;

    public CarInsuranceContractStatController(CarInsuranceContractStatService carInsuranceContractStatService) {
        this.carInsuranceContractStatService = carInsuranceContractStatService;
    }

    @Operation(
        summary = "자동차보험 계약 통계 조회",
        description = """
            조회 기간(fromYm~toYm)을 기준으로 자동차보험 계약 통계를 조회합니다.
            sync=true 이면 외부 API를 호출해 최신 데이터를 동기화한 뒤 통계를 반환합니다.
            sync=false 이면 저장된 통계 데이터만 조회합니다.
            """
    )
    @GetMapping
    public ApiResponse<CarInsuranceContractStatResponse> getContractStats(
        @Valid CarInsuranceContractStatQueryRequest request,
        @Parameter(description = "true: 외부 API 동기화 후 조회, false: 저장된 데이터만 조회")
        @RequestParam(defaultValue = "false") boolean sync
    ) {
        if (sync) {
            return ApiResponse.ok(carInsuranceContractStatService.refreshContractStats(request));
        }
        return ApiResponse.ok(carInsuranceContractStatService.getStoredContractStats(request));
    }
}
