package io.github.mystagogy.insuranceinterface.domain.api.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiInfoRepository;
import io.github.mystagogy.insuranceinterface.domain.stat.service.CarInsuranceContractStatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ApiInfoInitializer implements ApplicationRunner {

    private final ApiInfoRepository apiInfoRepository;
    private final String carInsuranceContractBaseUrl;
    private final int carInsuranceContractTimeoutMs;

    public ApiInfoInitializer(
        ApiInfoRepository apiInfoRepository,
        @Value("${external.car-insurance-contract.base-url}") String carInsuranceContractBaseUrl,
        @Value("${external.car-insurance-contract.timeout-ms:5000}") int carInsuranceContractTimeoutMs
    ) {
        this.apiInfoRepository = apiInfoRepository;
        this.carInsuranceContractBaseUrl = carInsuranceContractBaseUrl;
        this.carInsuranceContractTimeoutMs = carInsuranceContractTimeoutMs;
    }

    @Override
    public void run(ApplicationArguments args) {
        apiInfoRepository.findByApiName(CarInsuranceContractStatService.API_NAME)
            .orElseGet(() -> apiInfoRepository.save(
                new ApiInfo(
                    CarInsuranceContractStatService.API_NAME,
                    "금융위원회",
                    carInsuranceContractBaseUrl,
                    carInsuranceContractTimeoutMs
                )
            ));
    }
}
