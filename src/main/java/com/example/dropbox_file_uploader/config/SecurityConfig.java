package com.example.dropbox_file_uploader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Configuration class for Spring Security settings.
 * Defines security filter chains for different parts of the application.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures security for API endpoints.
     * This filter chain has higher precedence (Order 1) and applies to specific API endpoints.
     * It disables CSRF protection and allows all requests to the specified endpoints without authentication.
     *
     * @param http The HttpSecurity object to configure
     * @return A SecurityFilterChain configured for API endpoints
     * @throws Exception If an error occurs during security configuration
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/upload", "/api/upload", "/test-connection")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * Configures security for web endpoints.
     * This filter chain has lower precedence (Order 2) and applies to all remaining endpoints.
     * It enables CSRF protection with non-HttpOnly cookies, permits access to static resources,
     * requires authentication for all other requests, disables form login, and configures logout.
     *
     * @param http The HttpSecurity object to configure
     * @return A SecurityFilterChain configured for web endpoints
     * @throws Exception If an error occurs during security configuration
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(LogoutConfigurer::permitAll);

        return http.build();
    }
}