package com.muledetector.graph;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Suspects, trails and communities name suspected accounts, so every endpoint needs a login: investigators (the dashboard).
 * Same set-up as core-banking's SecurityConfig, including its shortcuts (Basic over HTTP, users from config).
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
                .csrf(c -> c.disable())  // no cookies or sessions: every request carries its own credentials
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(a -> a.requestMatchers("/error").permitAll().anyRequest().authenticated())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);  // cost 4: Basic checks the password on every request
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder, @Value("${mulebank.passwords.investigator}") String investigator) {
        return new InMemoryUserDetailsManager(
                User.withUsername("investigator").password(encoder.encode(investigator)).roles("INVESTIGATOR").build());
    }
}
