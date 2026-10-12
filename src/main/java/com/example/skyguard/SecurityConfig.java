package com.example.skyguard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User u = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            return new org.springframework.security.core.userdetails.User(
                    u.username,
                    u.password,
                    u.enabled,
                    true,
                    true,
                    true,
                    Collections.singletonList(new SimpleGrantedAuthority(u.role))
            );
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disabled for REST API architecture
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // Static resources & frontend pages
                .requestMatchers("/", "/index.html", "/favicon.ico", "/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                // Authentication API
                .requestMatchers("/api/auth/**").permitAll()
                // Public read-only operational telemetry for dashboard & alerts
                .requestMatchers(HttpMethod.GET, "/api/summary", "/api/alerts").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/aircraft/**", "/api/flights/**", "/api/maintenance/**", "/api/incidents/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/ai/ask").permitAll()
                // Incident reporting can be submitted by staff or public reporting
                .requestMatchers(HttpMethod.POST, "/api/incidents").permitAll()
                // Admin-only destructive operations
                .requestMatchers(HttpMethod.DELETE, "/api/aircraft/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                // All other operations (adding flights, aircraft updates, maintenance) require authentication
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}
