package io.github.mystagogy.insuranceinterface.domain.stat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndemnityInsurancePageController {

    @GetMapping("/indemnity-insurance")
    public String indemnityInsurancePage() {
        return "forward:/indemnity-insurance.html";
    }
}
