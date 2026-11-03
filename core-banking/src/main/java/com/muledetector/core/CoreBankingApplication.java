package com.muledetector.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
// Stable page JSON: {content, page: {size, number, totalElements, totalPages}} instead of raw PageImpl.
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class CoreBankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreBankingApplication.class, args);
    }
}
