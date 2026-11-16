package br.com.cobranca.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    public WebConfig(AppProperties props) {
        this.props = props;
    }

    /** Relógio injetável: permite testar "atrasado" sem depender da data real. */
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Recife"));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.allowedOrigin())
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
