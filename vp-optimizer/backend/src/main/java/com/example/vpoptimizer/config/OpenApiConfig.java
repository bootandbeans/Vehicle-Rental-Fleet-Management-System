package com.example.vpoptimizer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI description exposed at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vpOptimizerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product & Volume Point Optimization API")
                        .version("1.0.0")
                        .description("""
                                Finds the best purchase combinations for a target Volume Point (VP).

                                * Products and categories: manage the catalogue.
                                * Pricing: authoritative discount, GST and final unit price calculation.
                                * Optimization: run an optimization over an explicit product selection and
                                  read back ranked solutions plus closest alternatives.
                                * History: every run is stored with a pricing snapshot so old results stay
                                  reproducible.
                                """)
                        .contact(new Contact().name("VP Optimizer"))
                        .license(new License().name("MIT")));
    }
}
