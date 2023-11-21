package com.jjchmielewski.cyber.repository;

import com.jjchmielewski.cyber.entities.EmailData;
import org.springframework.data.repository.CrudRepository;


public interface EmailDataRepository extends CrudRepository<EmailData, Integer> {

    public void deleteByUuid(String uuid);
}
