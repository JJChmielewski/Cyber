package com.jjchmielewski.cyber.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjchmielewski.cyber.entities.EmailData;
import com.jjchmielewski.cyber.entities.PhishingTest;
import com.jjchmielewski.cyber.entities.Receiver;
import com.jjchmielewski.cyber.repository.EmailDataRepository;
import com.jjchmielewski.cyber.repository.ReceiverRepository;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.*;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailService {

    private String userDefinedDataRoot;
    private int mailPort;
    private String username;
    private String password;
    private String domain;
    private String host;
    private int maxPoints;
    private boolean development;
    private String emailTemplatesDir;
    private String savedTestsDir;
    private Random random = new Random();
    private String[] properties = new String[]{"subjects", "senders", "points"};
    private ReceiverRepository receiverRepository;
    private EmailDataRepository emailDataRepository;

    @Autowired
    public EmailService(@Value("${cyber.templatesRoot}") String userDefinedDataRoot,
                        @Value("${cyber.mail.port}") int mailPort, @Value("${cyber.mail.username}") String username,
                        @Value("${cyber.mail.password}") String password,@Value("${cyber.mail.domain}") String domain,
                        @Value("${cyber.mail.host}") String host, @Value("${cyber.maxPoints}") int maxPoints,
                        @Value("${cyber.development}") boolean development,
                        ReceiverRepository receiverRepository, EmailDataRepository emailDataRepository) {
        this.userDefinedDataRoot = userDefinedDataRoot;
        this.mailPort = mailPort;
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.host = host;
        this.maxPoints = maxPoints;
        this.development = development;
        this.receiverRepository = receiverRepository;
        this.emailDataRepository = emailDataRepository;
        this.emailTemplatesDir = userDefinedDataRoot + "emails/";
        this.savedTestsDir = userDefinedDataRoot + "tests/";
    }

    @Transactional
    public String checkEmailByUuid(String uuid, boolean wasReported) {
        Receiver receiver = receiverRepository.findByEmailData_Uuid(uuid);
        if (receiver != null) {
            EmailData emailData = receiver.getEmailData().stream().filter(email -> email.getUuid().equals(uuid)).findAny().get();

            if (wasReported) {
                receiver.setPoints(receiver.getPoints() + emailData.getPoints());
            } else {
                int negativePoints = maxPoints - emailData.getPoints();
                receiver.setPoints(receiver.getPoints() - negativePoints);
            }

            receiver.getEmailData().remove(emailData);
            receiverRepository.save(receiver);
            emailDataRepository.delete(emailData);
            return wasReported ? "PASSED" : "FAILED";
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

    private MimeMultipart formatMessage(String message, String uuid, String category) {
        MimeMultipart multipart = new MimeMultipart("related");

        message = message.replace("%message.badUrl", String.format("http://%s/email?uuid=%s", development ? "127.0.0.1:8080" : domain, uuid));
        String uuidElement = String.format("<p style=\"display: none;\">cyberID=\"%s\"</p>", uuid);
        message += uuidElement;

        try {
            MimeBodyPart messageBody = new MimeBodyPart();
            messageBody.setContent(message, "text/html; charset=utf-8");
            multipart.addBodyPart(messageBody);

            File resourcesDir = new File(emailTemplatesDir + category + "/resources/");
            if (resourcesDir.exists() && resourcesDir.isDirectory()) {
                for (File file : resourcesDir.listFiles()) {
                    String imageNumber = file.getName().split("_")[0];

                    BodyPart messageBodyPart = new MimeBodyPart();
                    InputStream imageStream = new FileInputStream(file);
                    DataSource fds = new ByteArrayDataSource(IOUtils.toByteArray(imageStream), "image/gif");
                    messageBodyPart.setDataHandler(new DataHandler(fds));
                    messageBodyPart.setHeader("Content-ID", String.format("<image_%s>", imageNumber));
                    multipart.addBodyPart(messageBodyPart);
                }
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return multipart;
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

    private String getSubject(String message)  {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String subjectsArrayString = getMessagePropertyString(message, "subjects");
            String[] subjects = objectMapper.readValue(subjectsArrayString, String[].class);
            return getRandomFromArray(subjects);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getMessagePropertyString(String message, String property) {
        Pattern pattern = Pattern.compile("%message\\." + property + "=\\{[^}]*\\}");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            String result = matcher.group(0);
            result = result.replace("%message."+property+"={", "");
            result = result.replace("}", "");
            return result;
        }
        return "";
    }

    private String removePropertiesFromMessage(String message) {
        for (String property : properties) {
            Pattern pattern = Pattern.compile("%message\\." + property + "=\\{[^}]*\\}");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                message = matcher.replaceAll("");
            }
        }
        return message;
    }

    private String getEmailCategory() {
        File emailTemplateRootDir = new File(emailTemplatesDir);
        return ((File) getRandomFromArray(Arrays.stream(emailTemplateRootDir.listFiles()).filter(category -> category.isDirectory()).toArray())).getName();
    }

    private String getSender(String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String sendersArrayString = getMessagePropertyString(message, "senders");
            String[] senders = objectMapper.readValue(sendersArrayString, String[].class);
            return getRandomFromArray(senders);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private int getPoints(String message) {
        String pointsString = getMessagePropertyString(message, "points");
        return Integer.parseInt(pointsString);
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

    public void sendEmail(String receiver, String sender, String subject, String message, String uuid, String category) {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.ssl.enable", "true");
        prop.put("mail.smtp.host", host);
        prop.put("mail.smtp.domain", domain);
        prop.put("mail.smtp.port", mailPort);
        prop.put("mail.smtp.ssl.trust", host);

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        MimeMessage mimeMessage = new MimeMessage(session);
        try {
            mimeMessage.setRecipients(Message.RecipientType.TO, receiver);
            mimeMessage.setSubject(subject);
            mimeMessage.setFrom(sender);
            mimeMessage.setContent(formatMessage(message, uuid, category));

            Transport.send(mimeMessage);
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
            String subject = phishingTest.getSubjects()  == null ? getSubject(message) : getRandomFromArray(phishingTest.getSubjects());
            String sender = phishingTest.getSenders() == null ? getSender(message) : getRandomFromArray(phishingTest.getSenders());
            int points = phishingTest.getPoints() == 0 ? getPoints(message) : phishingTest.getPoints();

            message = removePropertiesFromMessage(message);
            EmailData emailData = new EmailData();
            emailData.setUuid(uuid);
            emailData.setEmailReceiver(receiver);
            emailData.setPoints(points);
            emailDataRepository.save(emailData);
            System.out.println(receiver.getEmail() + " | " + sender + " | " + subject + " | " + points +" | \n" + message);
            sendEmail(receiver.getEmail(), sender, subject, message, uuid, category);
        }
    }

    public void saveTestAndConstruct(PhishingTest phishingTest) {
        if (phishingTest.isSaveTest()) {
            phishingTest.saveAsJson(savedTestsDir);
        }
        constructAndSendEmails(phishingTest);
    }
}
