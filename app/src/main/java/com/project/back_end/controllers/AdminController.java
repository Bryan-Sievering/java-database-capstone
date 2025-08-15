package com.project.back_end.controllers;

import com.project.back_end.models.Admin;
import com.project.back_end.services.AppService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

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
        return "redirect:admin/login";
    }

    // GET /{api.path}admin/login -> render login page
    @GetMapping("/login")
    public String adminLoginPage() {
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

        boolean valid = appService.validateAdminLogin(admin);
        if (valid) {
            // Relative redirect keeps controller-level base path
            return "redirect:dashboard";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "admin/adminLogin";
        }
    }

    // GET /{api.path}admin/dashboard -> render dashboard page
    @GetMapping("/dashboard")
    public String adminDashboardPage() {
        return "admin/adminDashboard";
    }
}