package com.runek.contacts;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
public class ContactController {

    private final ContactRepository contactRepository;

    @Autowired
    public ContactController(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/contacts";
    }

    @GetMapping("/contacts")
    String contacts(HttpServletRequest request, Model model) {
        String q = request.getParameter("q");
        q = q == null ? "" : q;
        int page = Integer.parseInt(request.getParameterMap().getOrDefault("page", new String[]{"0"})[0]);
        model.addAttribute("q", q);
        model.addAttribute("page", page);
        model.addAttribute(
                "contacts",
                contactRepository.findAllByFirstName(q, 10, page * 10));
        model.addAttribute("archiver", Archiver.get());
        if (page == 0 && !"search".equals(request.getHeader("HX-Trigger"))) {
            return "index";
        } else {
            return "rows";
        }
    }

    @DeleteMapping("/contacts")
    String bulkDeleteContacts(HttpServletRequest request, Model model) {
        String[] stringIds = request.getParameterValues("selected_contact_ids");
        Set<Long> ids = Arrays.stream(stringIds).map(Long::parseLong).collect(Collectors.toSet());
        contactRepository.deleteAllById(ids);
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
    String newContactPost(HttpServletRequest request) {
        Contact c = new Contact(
                null,
                request.getParameter("first_name"),
                request.getParameter("last_name"),
                request.getParameter("email"),
                request.getParameter("phone"));
        try {
            String validationErrors = validateEmailFinal(null, request.getParameter("email"));
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
    String editContactPost(@PathVariable Long id, HttpServletRequest request, Model model) {
        Contact c = new Contact(
                id,
                request.getParameter("first_name"),
                request.getParameter("last_name"),
                request.getParameter("email"),
                request.getParameter("phone"));
        try {
            String validationErrors = validateEmailFinal(id, request.getParameter("email"));
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
    Object deleteContactPost(@PathVariable Long id, HttpServletRequest request) {
        contactRepository.deleteById(id);
        if ("delete-btn".equals(request.getHeader("HX-Trigger"))) {
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
    void archiveFile(Model model, HttpServletResponse response) throws IOException {
        Archiver archiver = Archiver.get();
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment; filename=file.txt");
        response.getOutputStream().write(Files.readAllBytes(archiver.archiveFilePath()));
    }

    private String validateEmailFinal(Long id, String email) {
        String error = "";
        String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        if (Pattern.compile(regexPattern).matcher(email).matches()) {
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
}
