package io.github.mystagogy.insuranceinterface.domain.api.service;

import io.github.mystagogy.insuranceinterface.domain.api.dto.InsuranceLookupResponse;
import org.springframework.stereotype.Service;

@Service
public class InterfaceService {

    public InsuranceLookupResponse lookupInsurance(String policyNumber) {
        return new InsuranceLookupResponse(policyNumber, "PENDING");
    }
}

