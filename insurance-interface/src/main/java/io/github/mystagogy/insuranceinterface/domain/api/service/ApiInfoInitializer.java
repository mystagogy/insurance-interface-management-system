package io.github.mystagogy.insuranceinterface.domain.api.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiInfoRepository;
import io.github.mystagogy.insuranceinterface.domain.stat.service.CarInsuranceContractStatService;
import io.github.mystagogy.insuranceinterface.domain.stat.service.IndemnityInsuranceStatService;
import io.github.mystagogy.insuranceinterface.domain.stat.service.LifeInsuranceStatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ApiInfoInitializer implements ApplicationRunner {

    private final ApiInfoRepository apiInfoRepository;
    private final String carInsuranceContractBaseUrl;
    private final int carInsuranceContractTimeoutMs;
    private final String lifeInsuranceBaseUrl;
    private final int lifeInsuranceTimeoutMs;
    private final String indemnityInsuranceBaseUrl;
    private final int indemnityInsuranceTimeoutMs;

    public ApiInfoInitializer(
        ApiInfoRepository apiInfoRepository,
        @Value("${external.car-insurance-contract.base-url}") String carInsuranceContractBaseUrl,
        @Value("${external.car-insurance-contract.timeout-ms:5000}") int carInsuranceContractTimeoutMs,
        @Value("${external.life-insurance.base-url}") String lifeInsuranceBaseUrl,
        @Value("${external.life-insurance.timeout-ms:5000}") int lifeInsuranceTimeoutMs,
        @Value("${external.indemnity-insurance.base-url}") String indemnityInsuranceBaseUrl,
        @Value("${external.indemnity-insurance.timeout-ms:5000}") int indemnityInsuranceTimeoutMs
    ) {
        this.apiInfoRepository = apiInfoRepository;
        this.carInsuranceContractBaseUrl = carInsuranceContractBaseUrl;
        this.carInsuranceContractTimeoutMs = carInsuranceContractTimeoutMs;
        this.lifeInsuranceBaseUrl = lifeInsuranceBaseUrl;
        this.lifeInsuranceTimeoutMs = lifeInsuranceTimeoutMs;
        this.indemnityInsuranceBaseUrl = indemnityInsuranceBaseUrl;
        this.indemnityInsuranceTimeoutMs = indemnityInsuranceTimeoutMs;
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
        apiInfoRepository.findByApiName(LifeInsuranceStatService.API_NAME)
            .orElseGet(() -> apiInfoRepository.save(
                new ApiInfo(
                    LifeInsuranceStatService.API_NAME,
                    "금융위원회",
                    lifeInsuranceBaseUrl,
                    lifeInsuranceTimeoutMs
                )
            ));
        apiInfoRepository.findByApiName(IndemnityInsuranceStatService.API_NAME)
            .orElseGet(() -> apiInfoRepository.save(
                new ApiInfo(
                    IndemnityInsuranceStatService.API_NAME,
                    "금융위원회",
                    indemnityInsuranceBaseUrl,
                    indemnityInsuranceTimeoutMs
                )
            ));
    }
}
