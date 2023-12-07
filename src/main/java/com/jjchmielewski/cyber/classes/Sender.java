package com.jjchmielewski.cyber.classes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sender {

    private int mailPort;
    private String username;
    private String password;
    private String domain;
    private String host;

    public Sender(String username, String password, String domain, String host, int mailPort) {
        this.mailPort = mailPort;
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.host = host;
    }
}
