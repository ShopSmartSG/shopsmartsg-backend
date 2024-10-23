package sg.edu.nus.iss.shopsmart_backend;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Home Controller", description = "Home controller for Shopsmart Backend")
public class HomeController {
    @RequestMapping("/")
    public String home() {
        return "Welcome to Central Hub!";
    }
}
