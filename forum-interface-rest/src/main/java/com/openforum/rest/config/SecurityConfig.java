package com.openforum.rest.config;

import com.openforum.rest.auth.HybridJwtAuthenticationConverter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final HybridJwtAuthenticationConverter hybridConverter;
    private final RSAPublicKey saasPublicKey;

    public SecurityConfig(HybridJwtAuthenticationConverter hybridConverter, RSAPublicKey saasPublicKey) {
        this.hybridConverter = hybridConverter;
        this.saasPublicKey = saasPublicKey;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.web.cors.CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/tenants/lookup/**")
                        .permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/threads/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/posts/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(authenticationManagerResolver));

        return http.build();
    }

    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
        Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();

        // Clerk Decoder & Manager
        JwtDecoder clerkDecoder = NimbusJwtDecoder.withIssuerLocation("https://shining-skink-92.clerk.accounts.dev")
                .build();
        JwtAuthenticationProvider clerkProvider = new JwtAuthenticationProvider(clerkDecoder);
        clerkProvider.setJwtAuthenticationConverter(hybridConverter);
        authenticationManagers.put("https://shining-skink-92.clerk.accounts.dev", clerkProvider::authenticate);

        // SaaS Decoder & Manager
        JwtDecoder saasDecoder = NimbusJwtDecoder.withPublicKey(saasPublicKey).build();
        JwtAuthenticationProvider saasProvider = new JwtAuthenticationProvider(saasDecoder);
        saasProvider.setJwtAuthenticationConverter(hybridConverter);
        // Assuming issuer is openforum-saas or null? Usually better to enforce issuer.
        // If SaaS token doesn't have issuer, JwtIssuerAuthenticationManagerResolver
        // might fail to pick it.
        // We will assume "openforum-saas" for now as per plan.
        authenticationManagers.put("openforum-saas", saasProvider::authenticate);

        return new JwtIssuerAuthenticationManagerResolver(authenticationManagers::get);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // Allow all for now
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/swagger-ui/**", "/v3/api-docs/**");
    }

}
