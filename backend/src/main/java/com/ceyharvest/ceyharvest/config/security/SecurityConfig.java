package com.ceyharvest.ceyharvest.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Value("${app.cors.allowed-origins:http://localhost:*}")
    private String[] allowedOrigins;

    @Value("${app.dev-endpoints.enabled:false}")
    private boolean devEndpointsEnabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> {
                // Unauthenticated dev/test helpers. Only registered when
                // app.dev-endpoints.enabled=true, which must never be the case
                // in a deployed environment - they seed and reset real data.
                if (devEndpointsEnabled) {
                    authz
                        .requestMatchers("/api/dev/**").permitAll()
                        .requestMatchers("/api/admin/reset/**").permitAll()
                        .requestMatchers("/api/buyer/checkout/test").permitAll()
                        .requestMatchers("/api/buyer/checkout/test-payment-intent").permitAll();
                }

                authz
                // Public authentication endpoints
                .requestMatchers(HttpMethod.POST, "/api/*/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/farmer/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/buyer/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/driver/register").permitAll()
                
                // Unified auth endpoints (public)
                .requestMatchers("/api/auth/**").permitAll()
                
                // Password reset endpoints (public)
                .requestMatchers("/api/auth/forgot-password").permitAll()
                .requestMatchers("/api/auth/reset-password").permitAll()
                .requestMatchers("/api/auth/verify-reset-token/**").permitAll()
                
                // Verification endpoints (public) - for email/SMS verification during registration
                .requestMatchers("/api/verification/**").permitAll()
                
                // Stripe publishable key lookup - needed by the checkout form
                .requestMatchers("/api/buyer/checkout/stripe-config").permitAll()

                // ML/AI endpoints - public for testing (consider securing in production)
                .requestMatchers("/api/yield/**").permitAll()
                
                // Admin endpoints - only for ADMIN role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Farmer endpoints - only for FARMER role
                .requestMatchers("/api/farmer/**").hasRole("FARMER")
                
                // Buyer endpoints - only for BUYER role
                .requestMatchers("/api/buyer/**").hasRole("BUYER")
                
                // Driver endpoints - only for DRIVER role
                .requestMatchers("/api/driver/**").hasRole("DRIVER")
                
                // Health check and actuator endpoints
                .requestMatchers("/actuator/health").permitAll()
                
                // All other requests need authentication
                .anyRequest().authenticated();
            });

        // Add filters in correct order
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Driven by app.cors.allowed-origins (CORS_ALLOWED_ORIGINS). Patterns are
        // used rather than exact origins so entries like https://*.vercel.app work
        // alongside allowCredentials.
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
