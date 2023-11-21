package com.jjchmielewski.cyber.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    private String roleName;
    @ManyToMany(mappedBy = "roles")
    private List<Receiver> receiver;
}
