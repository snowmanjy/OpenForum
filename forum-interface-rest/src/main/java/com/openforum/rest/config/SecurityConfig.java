package com.openforum.rest.config;

import com.openforum.rest.auth.MemberJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for JWT-based authentication.
 *
 * Implements the "Trusted Parent" pattern:
 * - JWT tokens are issued by a parent SaaS platform
 * - We validate the signature using a configured public key or JWKS endpoint
 * - JIT provisioning creates Members automatically from JWT claims
 *
 * Configuration (application.yml):
 *   spring.security.oauth2.resourceserver.jwt.public-key-location: classpath:public-key.pem
 *   OR
 *   spring.security.oauth2.resourceserver.jwt.jwk-set-uri: https://auth.example.com/.well-known/jwks.json
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final MemberJwtAuthenticationConverter memberJwtAuthenticationConverter;

    public SecurityConfig(MemberJwtAuthenticationConverter memberJwtAuthenticationConverter) {
        this.memberJwtAuthenticationConverter = memberJwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(memberJwtAuthenticationConverter))
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // TODO: Configure specific origins in production
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
