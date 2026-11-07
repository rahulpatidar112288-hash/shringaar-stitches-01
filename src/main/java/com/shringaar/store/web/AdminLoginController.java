package com.shringaar.store.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminLoginController {

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }

    /** Customers don't need accounts; /login is a shortcut to the staff sign-in. */
    @GetMapping("/login")
    public String loginShortcut() {
        return "redirect:/admin/login";
    }
}
