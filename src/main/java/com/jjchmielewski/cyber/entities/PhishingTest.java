package com.jjchmielewski.cyber.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

@Getter
@Setter
@Component
public class PhishingTest {

    private String testName = "";
    private String[] receivers = null;
    private String[] receiverRoles = null;

    private String[] senders = null;
    private String[] subjects = null;

    private String message = null;
    private String messageCategory = null;
    private boolean saveTest = false;
    private int points = 0;

    public PhishingTest() {
    }

    public void saveAsJson(String destination) {
        String pathToJson = this.testName == null ? destination + LocalDateTime.now() : destination + this.testName;
        pathToJson += ".json";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pathToJson))){
            String json = new ObjectMapper().writeValueAsString(this);
            writer.write(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
