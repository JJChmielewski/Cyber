package com.jjchmielewski.cyber.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjchmielewski.cyber.entities.EmailData;
import com.jjchmielewski.cyber.entities.PhishingTest;
import com.jjchmielewski.cyber.entities.Receiver;
import com.jjchmielewski.cyber.repository.EmailDataRepository;
import com.jjchmielewski.cyber.repository.ReceiverRepository;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;

@Service
public class EmailService {

    //TODO get it from properties
    private String userDefinedDataRoot = "C:/Users/Chmielu/IdeaProjects/Cyber/src/main/resources/static/";
    private String emailTemplatesDir = userDefinedDataRoot + "emails/";
    private String savedTestsDir = userDefinedDataRoot + "tests/";
    private Random random = new Random();

    @Autowired
    private ReceiverRepository receiverRepository;

    @Autowired
    private EmailDataRepository emailDataRepository;

    public String test() {
        return "TEST";
    }

    @Transactional
    public String checkEmailByUuid(String uuid) {
        Receiver receiver = receiverRepository.findByEmailData_Uuid(uuid);
        if (receiver != null) {
            receiver.setPoints(receiver.getPoints() - 5);
            receiverRepository.save(receiver);
            emailDataRepository.deleteByUuid(uuid);
            return "FAILED";
        }
        return "NOTHING HAPPENED";
    }

    private String getMessage(String emailCategory) {
        File emailCategoryDir = new File(emailTemplatesDir + emailCategory);
        return getMessage(emailCategory, (String) getRandomFromArray(Arrays.stream(emailCategoryDir.list()).filter(template -> template.endsWith(".html")).toArray()));
    }

    private String getMessage(String emailCategory, String fileName) {
        File htmlFile = new File(emailTemplatesDir + emailCategory + "/" + fileName);
        return getFileContent(htmlFile);
    }

    private String formatMessage(String message, String uuid) {
        message = message.replace("$message.badUrl", "http://localhost:8080/email?uuid="+ uuid);
        //TODO save hash to DB
        return message;
    }

    private String getFileContent(File file) {
        String fileContent = "";
        if (file.exists() && file.isFile()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    fileContent += line + "\n";
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return fileContent;
    }

    private <T> T getRandomFromArray(T[] array) {
        return array[random.nextInt(array.length)];
    }

    private String getSubject(String emailCategory) {
        File subjectsFile = new File(emailTemplatesDir + emailCategory + "/subjects.txt");
        return getRandomFromArray(getFileContent(subjectsFile).split("\n"));
    }

    private String getEmailCategory() {
        File emailTemplateRootDir = new File(emailTemplatesDir);
        return ((File) getRandomFromArray(Arrays.stream(emailTemplateRootDir.listFiles()).filter(category -> category.isDirectory()).toArray())).getName();
    }

    private String getSender(String emailCategory) {
        File sendersFile = new File(emailTemplatesDir + emailCategory + "/senders.txt");
        String sendersFileContent = getFileContent(sendersFile);
        if (sendersFileContent.isEmpty()) {
            sendersFile = new File(emailTemplatesDir + "senders.txt");
            sendersFileContent = getFileContent(sendersFile);
        }
        return getRandomFromArray(sendersFileContent.split("\n"));
    }

    private PhishingTest getPhishingTest(String testName) {
        File phishingTestJson = new File(savedTestsDir + testName);
        try {
            return new ObjectMapper().readValue(phishingTestJson, PhishingTest.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendEmail(String receiver, String sender, String subject, String message) {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.host", "smtp.mailtrap.io");
        prop.put("mail.smtp.port", "2525");
        prop.put("mail.smtp.ssl.trust", "smtp.mailtrap.io");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("d4e8e653c426cc", "038c8a3a7de408");
            }
        });
        MimeMessage mimeMessage = new MimeMessage(session);
        try {
            mimeMessage.setRecipients(Message.RecipientType.TO, receiver);
            mimeMessage.setSubject(subject);
            mimeMessage.setFrom(sender);
            mimeMessage.setContent(message, "text/html; charset=utf-8");

            //Transport.send(mimeMessage);
        } catch (MessagingException messagingException) {
            messagingException.printStackTrace();
        }
    }

    public void constructAndSendEmails() {
        constructAndSendEmails(new PhishingTest());
    }

    public void constructAndSendEmails(String testName) {
        constructAndSendEmails(getPhishingTest(testName));
    }

    public List<Receiver> getRandomReceivers() {
        return getRandomReceivers(receiverRepository.findAll());
    }

    public List<Receiver> getRandomReceivers(String[] roles) {
        return getRandomReceivers(receiverRepository.findAllByRoles_RoleNameIn(roles));
    }

    public List<Receiver> getRandomReceivers(List<Receiver> receivers) {
        int numberOfReceivers = random.nextInt(receivers.size() + 1);
        List<Receiver> chosenReceivers = new ArrayList<>();

        for (int i = 0; i < numberOfReceivers; i++) {
            int receiverIndex = random.nextInt(receivers.size());
            Receiver temp = receivers.get(receiverIndex);
            chosenReceivers.add(temp);
            receivers.remove(receiverIndex);
        }
        return chosenReceivers;
    }

    @Transactional
    public void constructAndSendEmails(PhishingTest phishingTest) {

        if (phishingTest.isSaveTest()) {
            phishingTest.saveAsJson(savedTestsDir);
        }

        List<Receiver> receivers = receiverRepository.findAllByEmailIn(phishingTest.getReceivers());
        if (receivers.isEmpty()) {
            String[] receiverRoles = phishingTest.getReceiverRoles();
            if (receiverRoles != null) {
                receivers = getRandomReceivers(receiverRoles);
            } else {
                receivers = getRandomReceivers();
            }
        }

        for (Receiver receiver : receivers) {
            String uuid = UUID.randomUUID().toString();
            String category = phishingTest.getMessageCategory() == null ? getEmailCategory() : phishingTest.getMessageCategory();
            String message = phishingTest.getMessage() == null ? getMessage(category) : getMessage(category, phishingTest.getMessage());
            String subject = phishingTest.getSubjects()  == null ? getSubject(category) : getRandomFromArray(phishingTest.getSubjects());
            String sender = phishingTest.getSenders() == null ? getSender(category) : getRandomFromArray(phishingTest.getSenders());

            message = formatMessage(message, uuid);
            EmailData emailData = new EmailData();
            emailData.setUuid(uuid);
            emailData.setEmailReceiver(receiver);
            emailDataRepository.save(emailData);

            System.out.println(receiver.getEmail() + " | " + sender + " | " + subject + " :\n" + message);
            //sendEmail(receiver, sender, subject, message);
        }
    }
}
