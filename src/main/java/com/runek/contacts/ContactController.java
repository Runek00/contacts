package com.runek.contacts;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class ContactController {

    private final ContactRepository contactRepository;

    @Autowired
    public ContactController(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @GetMapping("/")
    @ResponseBody
    public RedirectView index() {
        return new RedirectView("/contacts");
    }

    @GetMapping("/contacts")
    String contacts(HttpServletRequest request, Model model) {
        String q = request.getParameter("q");
        model.addAttribute("q", q);
        return "index";
    }

    @GetMapping("/contacts/new")
    String newContact(HttpServletRequest request, Model model) {
        return "new_contact";
    }
}
