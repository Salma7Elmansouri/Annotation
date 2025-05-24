package org.example.annotation.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    //Login Errors & Logout

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Nom d'utilisateur ou mot de passe invalide, ou compte désactivé.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Vous avez été déconnecté avec succès.");
        }
        return "login";
    }

   // Redirection after Login

    @GetMapping("/default")
    public String redirectAfterLogin(Authentication authentication) {
        if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/admin_dashboard";
        } else if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ANNOTATOR"))) {
            return "redirect:/annotator/annotator_dashboard";
        }
        return "redirect:/login?error";
    }
}
