package pl.viksi.catsmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "pl.viksi.catsmatch.backend")
@EntityScan("pl.viksi.catsmatch.backend")
@EnableJpaRepositories("pl.viksi.catsmatch.backend")
public class CatsmatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatsmatchApplication.class, args);
	}


}
