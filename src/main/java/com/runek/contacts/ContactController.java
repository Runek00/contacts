package com.runek.contacts;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.regex.Pattern;

@Controller
public class ContactController {

    private final ContactRepository contactRepository;
    private SseEmitter sseEmitter;

    @Autowired
    public ContactController(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/contacts";
    }

    @GetMapping("/contacts")
    String contacts(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestHeader(name = "HX-Trigger", required = false) String trigger,
            Model model) {
        int pageLength = 10;
        model.addAttribute("q", q);
        model.addAttribute("page", page);
        model.addAttribute(
                "contacts",
                contactRepository.findAllByName(q, pageLength, page * pageLength));
        model.addAttribute("archiver", Archiver.get());
        if (page == 0 && !"search".equals(trigger)) {
            return "index";
        } else {
            return "rows";
        }
    }

    @DeleteMapping("/contacts")
    String bulkDeleteContacts(@RequestParam HashSet<Long> selectedContactIds, Model model) {
        contactRepository.deleteAllById(selectedContactIds);
        model.addAttribute("contacts", contactRepository.findAll());
        model.addAttribute("archiver", Archiver.get());
        return "index";
    }

    @GetMapping("/contacts/count")
    @ResponseBody
    String contactsCount() {
        long count = contactRepository.count();
        return "(" + count + " total contacts)";
    }

    @GetMapping("/contacts/{id}")
    String viewContact(@PathVariable Long id, Model model) {
        model.addAttribute(
                "contact",
                contactRepository.findById(id)
                        .orElse(new Contact(null, null, null, null, null)));
        return "show";
    }

    @GetMapping("/contacts/new")
    String newContact(Model model) {
        model.addAttribute(
                "contact",
                new Contact(null, null, null, null, null));
        return "new_contact";
    }

    @PostMapping("/contacts/new")
    String newContactPost(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phone) {
        Contact c = new Contact(null, firstName, lastName, email, phone);
        try {
            String validationErrors = validateEmailFinal(null, email);
            if (!"".equals(validationErrors)) {
                throw new IllegalArgumentException(validationErrors);
            }
            contactRepository.save(c);
            return "redirect:/contacts";
        } catch (Exception ex) {
            return "new_contact";
        }
    }

    @GetMapping("/contacts/{id}/edit")
    String editContact(@PathVariable Long id, Model model) {
        model.addAttribute(
                "contact",
                contactRepository.findById(id)
                        .orElse(new Contact(null, null, null, null, null)));
        return "edit";
    }

    @PostMapping("/contacts/{id}/edit")
    String editContactPost(
            @PathVariable Long id,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phone,
            Model model) {
        Contact c = new Contact(id, firstName, lastName, email, phone);
        try {
            String validationErrors = validateEmailFinal(id, email);
            if (!"".equals(validationErrors)) {
                throw new IllegalArgumentException(validationErrors);
            }
            contactRepository.save(c);
            return "redirect:/contacts";
        } catch (Exception ex) {
            model.addAttribute("contact", c);
            return "edit";
        }
    }

    @DeleteMapping("/contacts/{id}")
    @ResponseBody
    Object deleteContactPost(
            @PathVariable Long id,
            @RequestHeader(name = "HX-Trigger", required = false) String trigger) {
        contactRepository.deleteById(id);
        if ("delete-btn".equals(trigger)) {
            RedirectView rView = new RedirectView("/contacts");
            rView.setStatusCode(HttpStatusCode.valueOf(303));
            return rView;
        } else {
            return "";
        }
    }

    @GetMapping("/contacts/{id}/email")
    @ResponseBody
    String validateEmail(@PathVariable Long id, @RequestParam String email) {
        return validateEmailInline(id, email);
    }

    private String validateEmailInline(Long id, String email) {
        String error = "";
        boolean repeats = contactRepository.findAll()
                .stream()
                .filter(c -> !c.id().equals(id))
                .map(Contact::email)
                .anyMatch(e -> e.equals(email));
        if (repeats) {
            error += "Email must be unique!\n";
        }
        return error;
    }

    @PostMapping("/contacts/archive")
    String archiveStart(Model model) {
        Archiver archiver = Archiver.get();
        archiver.run();
        model.addAttribute("archiver", archiver);
        return "archive";
    }

    @GetMapping("/contacts/archive")
    String archiveCheckStatus(Model model) {
        Archiver archiver = Archiver.get();
        model.addAttribute("archiver", archiver);
        return "archive";
    }

    @DeleteMapping("/contacts/archive")
    String archiveReset(Model model) {
        Archiver archiver = Archiver.get();
        archiver.reset();
        model.addAttribute("archiver", archiver);
        return "archive";
    }

    @GetMapping("/contacts/archive/file")
    void archiveFile(HttpServletResponse response) throws IOException {
        Archiver archiver = Archiver.get();
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment; filename=file.txt");
        response.getOutputStream().write(Files.readAllBytes(archiver.archiveFilePath()));
    }

    @GetMapping("/contacts/change_emitter")
    public SseEmitter contactsChanged() {
        return this.sseEmitter = new SseEmitter(Long.MAX_VALUE);
    }

    private String validateEmailFinal(Long id, String email) {
        String error = "";
        String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        if (!Pattern.compile(regexPattern).matcher(email).matches()) {
            error += "Wrong email pattern!\n";
        }
        boolean repeats = contactRepository.findAll()
                .stream()
                .filter(c -> !c.id().equals(id))
                .map(Contact::email)
                .anyMatch(e -> e.equals(email));
        if (repeats) {
            error += "Email must be unique!\n";
        }
        return error;
    }

    public void emitChange(String msg) {
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .data(msg);
            this.sseEmitter.send(event);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
