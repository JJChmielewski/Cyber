package com.jjchmielewski.cyber.controllers;

import com.jjchmielewski.cyber.classes.PhishingTest;
import com.jjchmielewski.cyber.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping()
    public String check(String uuid) {
        return emailService.checkEmailByUuid(uuid, false);
    }

    @GetMapping("/report")
    public String report(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return "THIS IS NOT A TEST EMAIL. PLEASE REPORT IT TO YOUR ADMINISTRATOR";
        }
        return emailService.checkEmailByUuid(uuid, true);
    }

    @PostMapping()
    public void sendEmail(@RequestBody(required = false) PhishingTest phishingTest, String testName) {
        if (phishingTest != null) {
            emailService.saveTestAndConstruct(phishingTest);
            return;
        }
        if (testName != null && !testName.isEmpty()) {
            emailService.constructAndSendEmails(testName);
            return;
        }
        emailService.constructAndSendEmails();
    }
}
