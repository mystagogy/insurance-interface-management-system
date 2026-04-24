package io.github.mystagogy.insuranceinterface.domain.stat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CarInsurancePageController {

    @GetMapping("/car-insurance")
    public String carInsurancePage() {
        return "forward:/car-insurance.html";
    }
}
