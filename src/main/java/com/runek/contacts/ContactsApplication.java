package com.runek.contacts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

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
    @Query("select * from Contact where first_name like '%' || :first_name || '%' limit :limit offset :offset")
    List<Contact> findAllByFirstName(@Param("first_name") String firstName, @Param("limit")Integer limit, @Param("offset") Integer offset);
}
