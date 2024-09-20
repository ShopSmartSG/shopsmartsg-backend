package sg.edu.nus.iss.shopsmart_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class ShopsmartBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopsmartBackendApplication.class, args);
	}

}
