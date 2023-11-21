package com.jjchmielewski.cyber.repository;

import com.jjchmielewski.cyber.entities.Receiver;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


public interface ReceiverRepository extends CrudRepository<Receiver, Integer> {

    public List<Receiver> findAllByEmailIn(String[] emails);
    public List<Receiver> findAll();
    public List<Receiver> findAllByRoles_RoleNameIn(String[] roles);
    public Receiver findByEmailData_Uuid(String uuid);
}
