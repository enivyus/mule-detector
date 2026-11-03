package com.muledetector.core;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
// Stable page JSON: {content, page: {size, number, totalElements, totalPages}} instead of raw PageImpl.
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
// Swagger UI's "Authorize" button sends HTTP Basic credentials (see SecurityConfig)
@SecurityScheme(name = "basic", type = SecuritySchemeType.HTTP, scheme = "basic")
@OpenAPIDefinition(security = @SecurityRequirement(name = "basic"))
public class CoreBankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreBankingApplication.class, args);
    }
}
