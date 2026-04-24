package io.github.mystagogy.insuranceinterface.domain.stat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LifeInsurancePageController {

    @GetMapping("/life-insurance")
    public String lifeInsurancePage() {
        return "forward:/life-insurance.html";
    }
}
