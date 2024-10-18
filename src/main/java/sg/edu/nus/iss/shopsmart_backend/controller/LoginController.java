package sg.edu.nus.iss.shopsmart_backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
@Tag(name = "Login", description = "Handle login for customers and merchants via APIs")
public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
}
