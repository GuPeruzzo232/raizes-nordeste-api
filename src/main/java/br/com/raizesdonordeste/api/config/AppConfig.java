package br.com.raizesdonordeste.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    /** Relógio injetável: permite testar regras de data (itens sazonais) sem depender do dia real. */
    @Bean
    Clock clock(@Value("${app.fuso:America/Recife}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("Raízes do Nordeste API").version("1.0.0")
                        .description("API única para app, totem, balcão, pick-up e web da rede Raízes do Nordeste."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
