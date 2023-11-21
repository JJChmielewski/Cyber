package com.jjchmielewski.cyber.controllers;

import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.awt.*;
import java.io.*;

@Controller
@RequestMapping("/resource")
public class ResourceController {

    private String userDefinedDataRoot = "C:/Users/Chmielu/IdeaProjects/Cyber/src/main/resources/static/";
    private String emailTemplatesDir = userDefinedDataRoot + "emails/";

    @GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getImage(@RequestParam("category") String category, @RequestParam("name") String name) throws IOException {
        File image = new File(emailTemplatesDir + category + "/resources/" + name);
        InputStream inputStream = new FileInputStream(image);
        return IOUtils.toByteArray(inputStream);
    }

}
