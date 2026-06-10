package com.example.UPImesh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.UPImesh.security.JWTfilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JWTfilter jwtFilter;

    public SecurityConfig(JWTfilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Publicly accessible static resources
                        .requestMatchers("/", "/index.html", "/styles.css", "/app.js", "/sw.js", "/manifest.json").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        // Public API endpoints
                        .requestMatchers(HttpMethod.GET, "/api/demo/generate-packet").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Protected API endpoints (Requires ROLE_USER)
                        .requestMatchers(HttpMethod.POST, "/api/bridge/ingest").hasRole("USER")
                        .anyRequest().authenticated())
                // Add JWT filter before the standard authentication filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
