package sg.edu.nus.iss.shopsmart_backend;

import org.springframework.web.bind.annotation.*;

@RestController
public class HomeController {
    @RequestMapping("/")
    public String home() {
        return "Welcome to Shopsmart!";
    }
}
