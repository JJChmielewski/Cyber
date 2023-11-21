package com.jjchmielewski.cyber.controllers;

import com.jjchmielewski.cyber.entities.PhishingTest;
import com.jjchmielewski.cyber.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/email")
    public String check(String uuid) {
        return emailService.checkEmailByUuid(uuid);
    }

    @PostMapping("/email")
    public void sendEmail(@RequestBody(required = false) PhishingTest phishingTest, String testName) {
        if (phishingTest != null) {
            emailService.constructAndSendEmails(phishingTest);
            return;
        }
        if (testName != null && !testName.isEmpty()) {
            emailService.constructAndSendEmails(testName);
            return;
        }
        emailService.constructAndSendEmails();
    }
}
