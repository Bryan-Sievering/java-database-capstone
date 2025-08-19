package com.project.back_end.controllers;

import com.project.back_end.models.Admin;
import com.project.back_end.services.AppService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Controller
@RequestMapping("${api.path}admin")
public class AdminController {

    private final AppService appService;

    public AdminController(AppService appService) {
        this.appService = appService;
    }

    // GET /{api.path}admin -> redirect to login page
    @GetMapping
    public String root() {
        // Use a relative redirect so it stays under the class-level path
        return "redirect:login";
    }

    // GET /{api.path}admin/login -> render login page
    @GetMapping("/login")
    public String adminLoginPage() {
        // Render the login template instead of the dashboard
        return "admin/adminLogin";
    }

    // POST /{api.path}admin/login -> process login and redirect
    @PostMapping("/login")
    public String adminLogin(@Valid @ModelAttribute Admin admin,
                             BindingResult bindingResult,
                             Model model) {

        if (bindingResult.hasErrors()) {
            return "admin/adminLogin";
        }

        ResponseEntity<Map<String, String>> response = appService.validateAdmin(admin);

        if (response.getStatusCode().is2xxSuccessful()) {
            // Optional: you can extract the token if needed
            // String token = response.getBody() != null ? response.getBody().get("token") : null;
            return "redirect:dashboard";
        } else {
            String error = "Invalid username or password";
            if (response.getBody() != null && response.getBody().get("error") != null) {
                error = response.getBody().get("error");
            }
            model.addAttribute("error", error);
            return "admin/adminLogin";
        }
    }

    // GET /{api.path}admin/dashboard -> render dashboard page
    @GetMapping("/dashboard")
    public String adminDashboardPage() {
        return "admin/adminDashboard";
    }
}