package sg.edu.nus.iss.shopsmart_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Home Controller", description = "Home controller for Shopsmart Backend")
public class HomeController {

    Logger logger = LoggerFactory.getLogger(HomeController.class);

    @RequestMapping("/")
    public String home() {
        logger.info("{\"message\": \"Welcome to Central Hub"+"\"}");
        return "Welcome to Central Hub!";
    }


    @RequestMapping("/home")
    public String homeMethod() {
        logger.info("{\"message\": \"Welcome to Central Hub Home"+"\"}");
        return "Welcome to Central Hub Home!";
        return "Welcome to Central Hub!";
    }
}
