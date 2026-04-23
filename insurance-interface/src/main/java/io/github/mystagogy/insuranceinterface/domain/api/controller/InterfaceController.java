package io.github.mystagogy.insuranceinterface.domain.api.controller;

import io.github.mystagogy.insuranceinterface.common.response.ApiResponse;
import io.github.mystagogy.insuranceinterface.domain.api.dto.InsuranceLookupResponse;
import io.github.mystagogy.insuranceinterface.domain.api.service.InterfaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interfaces")
public class InterfaceController {

    private final InterfaceService interfaceService;

    public InterfaceController(InterfaceService interfaceService) {
        this.interfaceService = interfaceService;
    }

    @GetMapping("/insurance")
    public ApiResponse<InsuranceLookupResponse> lookupInsurance(@RequestParam String policyNumber) {
        return ApiResponse.ok(interfaceService.lookupInsurance(policyNumber));
    }
}

