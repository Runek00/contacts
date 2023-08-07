package com.runek.contacts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@SpringBootApplication
public class ContactsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContactsApplication.class, args);
    }

}

record Contact(@Id Long id, String firstName, String lastName, String email, String phone) {
}

interface ContactRepository extends CrudRepository<Contact, Long> {
    List<Contact> findAll();
}
