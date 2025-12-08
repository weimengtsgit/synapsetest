package com.synapsetest.testmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration
 * Implements authentication and authorization framework
 * Uses SecurityFilterChain (Spring Security 5.7+ / Spring Boot 2.7+)
 * instead of deprecated WebSecurityConfigurerAdapter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                // System endpoints (no version)
                .antMatchers("/health", "/actuator/**").permitAll()
                // Swagger/OpenAPI endpoints
                .antMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                // Authentication endpoints
                .antMatchers("/auth/**").permitAll()
                // API v1 endpoints - permit all for development/testing
                // In production, you should add proper authentication
                .antMatchers("/api/v1/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated();
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
