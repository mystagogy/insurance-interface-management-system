package io.github.mystagogy.insuranceinterface.domain.history.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HistoryPageController {

    @GetMapping("/history")
    public String historyPage() {
        return "forward:/history.html";
    }
}
