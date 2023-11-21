package com.jjchmielewski.cyber.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class Receiver {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    private String email;
    private String name;
    private String surname;
    private int points;
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "receiver_roles",
            joinColumns = @JoinColumn(name = "receiver_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<Role> roles;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "emailReceiver")
    private List<EmailData> emailData;
}
