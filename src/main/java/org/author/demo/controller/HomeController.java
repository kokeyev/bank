package org.author.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/accounts"})
    public String home() {

        return "accounts";
    }

    @GetMapping("/transfers")
    public String transfers() {

        return "transfers";
    }

    @GetMapping("/deposits")
    public String deposits() {

        return "deposits";
    }

    @GetMapping("/loans")
    public String loans() {

        return "loans";
    }

    @GetMapping("/exchange")
    public String exchange() {

        return "exchange";
    }

    @GetMapping("/settings")
    public String settings() {

        return "settings";
    }
}
