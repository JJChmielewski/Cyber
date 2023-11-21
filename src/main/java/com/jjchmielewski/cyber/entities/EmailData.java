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
    private Long id;
    private String uuid;

    @ManyToOne()
    @JoinColumn(name = "receiver_id")
    private Receiver emailReceiver;

    public EmailData() {

    }
}
