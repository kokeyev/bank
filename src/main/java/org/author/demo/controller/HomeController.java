package org.author.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/accounts"})
    public String home() {

        return "accounts/index";
    }

    @GetMapping("/login")
    public String login() {

        return "bank/login";
    }

    @PostMapping("/login")
    public String createLogin() {

        return "redirect:/accounts";
    }

    @GetMapping("/register")
    public String register() {

        return "bank/register";
    }

    @PostMapping("/register")
    public String createRegister() {

        return "redirect:/login";
    }

    @GetMapping("/accounts/open")
    public String openAccount() {

        return "accounts/open";
    }

    @PostMapping("/accounts/open")
    public String createAccount() {

        return "redirect:/accounts";
    }

    @GetMapping("/transfers")
    public String transfers() {

        return "transfers/index";
    }

    @GetMapping("/transfers/between-accounts")
    public String transferBetweenAccounts() {

        return "transfers/between-accounts";
    }

    @PostMapping("/transfers/between-accounts")
    public String createTransferBetweenAccounts() {

        return "redirect:/transfers";
    }

    @GetMapping("/transfers/by-phone")
    public String transferByPhone() {

        return "transfers/by-phone";
    }

    @PostMapping("/transfers/by-phone")
    public String createTransferByPhone() {

        return "redirect:/transfers";
    }

    @GetMapping("/transfers/deposit-top-up")
    public String depositTopUp() {

        return "transfers/deposit-top-up";
    }

    @PostMapping("/transfers/deposit-top-up")
    public String createDepositTopUp() {

        return "redirect:/transfers";
    }

    @GetMapping("/transfers/loan-payment")
    public String loanPayment() {

        return "transfers/loan-payment";
    }

    @PostMapping("/transfers/loan-payment")
    public String createLoanPayment() {

        return "redirect:/transfers";
    }

    @GetMapping("/transfers/currency-exchange")
    public String currencyExchange() {

        return "transfers/currency-exchange";
    }

    @PostMapping("/transfers/currency-exchange")
    public String createCurrencyExchange() {

        return "redirect:/transfers";
    }

    @GetMapping("/deposits")
    public String deposits() {

        return "deposits/index";
    }

    @GetMapping("/deposits/kopilka")
    public String kopilkaDeposit() {

        return "deposits/kopilka";
    }

    @PostMapping("/deposits/kopilka")
    public String createKopilkaDeposit() {

        return "redirect:/deposits";
    }

    @GetMapping("/deposits/strategy")
    public String strategyDeposit() {

        return "deposits/strategy";
    }

    @PostMapping("/deposits/strategy")
    public String createStrategyDeposit() {

        return "redirect:/deposits";
    }

    @GetMapping("/deposits/capital")
    public String capitalDeposit() {

        return "deposits/capital";
    }

    @PostMapping("/deposits/capital")
    public String createCapitalDeposit() {

        return "redirect:/deposits";
    }

    @GetMapping("/loans")
    public String loans() {

        return "loans/index";
    }

    @GetMapping("/loans/purpose")
    public String purposeLoan() {

        return "loans/purpose";
    }

    @PostMapping("/loans/purpose")
    public String createPurposeLoan() {

        return "redirect:/loans";
    }

    @GetMapping("/loans/auto")
    public String autoLoan() {

        return "loans/auto";
    }

    @PostMapping("/loans/auto")
    public String createAutoLoan() {

        return "redirect:/loans";
    }

    @GetMapping("/loans/mortgage")
    public String mortgageLoan() {

        return "loans/mortgage";
    }

    @PostMapping("/loans/mortgage")
    public String createMortgageLoan() {

        return "redirect:/loans";
    }

    @GetMapping("/exchange")
    public String exchange() {

        return "exchange/index";
    }

    @GetMapping("/settings")
    public String settings() {

        return "settings/index";
    }
}
