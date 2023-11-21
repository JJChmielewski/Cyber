package com.jjchmielewski.cyber.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class EmailData {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private int id;
    private String uuid;
    private int points = 0;

    @ManyToOne()
    @JoinColumn(name = "receiver_id")
    private Receiver emailReceiver;

    public EmailData() {

    }
}
