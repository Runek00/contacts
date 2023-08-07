package com.runek.contacts;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("q", q);
        model.addAttribute("contacts", contactRepository.findAll().stream().filter(c -> c.firstName().contains(q == null ? "" : q)).toList());
        return "index";
    }

    @GetMapping("/contacts/{id}")
    String viewContact(@PathVariable Long id,  Model model) {
        model.addAttribute("contact", contactRepository.findById(id).get());
        return "show";
    }

    @GetMapping("/contacts/new")
    String newContact(HttpServletRequest request, Model model) {
        model.addAttribute("contact", new Contact(null, null, null, null, null));
        return "new_contact";
    }

    @PostMapping("/contacts/new")
    String newContactPost(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Contact c = new Contact(
                null,
                request.getParameter("first_name"),
                request.getParameter("last_name"),
                request.getParameter("email"),
                request.getParameter("phone"));
        try {
            contactRepository.save(c);
            redirectAttributes.addFlashAttribute("Contact added!");
            return "redirect:/contacts";
        } catch (Exception ex) {
            return "new_contact";
        }
    }
}
