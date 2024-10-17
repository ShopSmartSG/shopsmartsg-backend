package sg.edu.nus.iss.shopsmart_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
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
    }
}
