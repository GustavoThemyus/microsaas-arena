package br.com.cobranca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CobrancaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CobrancaApplication.class, args);
    }
}
