package com.runek.contacts;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class ContactJsonController {

    private final ContactRepository contactRepository;
    private final ContactController frontendController;

    @Autowired
    public ContactJsonController(ContactRepository contactRepository,
                                 ContactController contactController) {
        this.contactRepository = contactRepository;
        this.frontendController = contactController;
    }

    @GetMapping("/contacts")
    List<Contact> contacts() {
        return this.contactRepository.findAll();
    }

    @PostMapping("/contacts")
    Contact addContact(@RequestParam String firstName,
                       @RequestParam String lastName,
                       @RequestParam String email,
                       @RequestParam String phone) {
        Contact contact = new Contact(null, firstName, lastName, email, phone);
        frontendController.emitChange(firstName + " " + lastName + " added");
        return this.contactRepository.save(contact);
    }

    @PutMapping("/contacts/{id}")
    Contact editContact(@PathVariable Long id,
                        @RequestParam String firstName,
                        @RequestParam String lastName,
                        @RequestParam String email,
                        @RequestParam String phone,
                        HttpServletResponse response) throws IOException {
        if (contactRepository.existsById(id)) {
            Contact contact = new Contact(id, firstName, lastName, email, phone);
            frontendController.emitChange(firstName + " " + lastName + " changed");
            return this.contactRepository.save(contact);
        } else {
            response.getOutputStream().write(("Contact with id " + id + " not present").getBytes());
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.flushBuffer();
            return null;
        }
    }

    @DeleteMapping("/contacts/{id}")
    Boolean deleteContact(@PathVariable Long id) {
        Optional<Contact> contact = contactRepository.findById(id);
        if (contact.isPresent()) {
            this.contactRepository.deleteById(id);
            frontendController.emitChange(
                    contact.get().firstName() + " " +
                            contact.get().lastName() +
                            " deleted");
            return true;
        } else {
            return false;
        }
    }
}
