package com.muledetector.core.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
 * Who may do what. Three callers, three roles:
 * CHANNEL (mobile app, ATM, branch: moves money for customers), INVESTIGATOR (fraud staff: reads, freezes,
 * unfreezes) and RULE_ENGINE (may only freeze; lifting a freeze always takes a person).
 *
 * shortcut: HTTP Basic over plain HTTP with users from config, so the app still listens on 127.0.0.1 only.
 * Before exposing it: TLS, and tokens from the bank's identity provider (OIDC) instead of passwords.
 * shortcut: CHANNEL is trusted to act for any customer; a real channel would carry the customer's own
 * login and the bank would check the account is theirs.
 */
@Configuration
public class SecurityConfig {

    public static final String CHANNEL = "CHANNEL";
    public static final String INVESTIGATOR = "INVESTIGATOR";
    public static final String RULE_ENGINE = "RULE_ENGINE";

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
                .csrf(c -> c.disable())  // no cookies or sessions: every request carries its own credentials
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // A bare 401 without WWW-Authenticate, so the dashboard shows its own login, not the browser's dialog
                .httpBasic(b -> b.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/accounts/*/freeze").hasAnyRole(INVESTIGATOR, RULE_ENGINE)
                        .requestMatchers("/api/admin/**").hasRole(INVESTIGATOR)
                        .requestMatchers(HttpMethod.GET, "/api/accounts/**").hasAnyRole(CHANNEL, INVESTIGATOR)
                        .requestMatchers("/api/me").authenticated()
                        .requestMatchers("/api/**").hasRole(CHANNEL)
                        .anyRequest().denyAll())
                .build();
    }

    // shortcut: bcrypt cost 4 (about 1 ms) because Basic checks the password on every request and a replay
    // sends 55,000 of them; the default cost 10 would add over an hour. Tokens remove this trade-off.
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder,
                             @Value("${mulebank.passwords.channel}") String channel,
                             @Value("${mulebank.passwords.investigator}") String investigator,
                             @Value("${mulebank.passwords.rule-engine}") String ruleEngine) {
        return new InMemoryUserDetailsManager(
                User.withUsername("channel").password(encoder.encode(channel)).roles(CHANNEL).build(),
                User.withUsername("investigator").password(encoder.encode(investigator)).roles(INVESTIGATOR).build(),
                User.withUsername("rule-engine").password(encoder.encode(ruleEngine)).roles(RULE_ENGINE).build());
    }
}
