package org.eci.ZwingBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZwingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZwingBackendApplication.class, args);
	}

}
