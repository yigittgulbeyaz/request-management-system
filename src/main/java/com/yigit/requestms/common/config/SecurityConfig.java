package com.yigit.requestms.common.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import com.yigit.requestms.auth.view.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

// VaadinSecurityConfigurer wires the CSRF and routing rules Vaadin needs.
// Route-level access is declared per view with @RolesAllowed rather than as URL
// patterns here, so a new view cannot be left unprotected by omission.
//
// EnableMethodSecurity turns on the second layer. A view that forgets its
// annotation is one mistake; a service that can be called by anyone who reaches
// it is a hole that outlives the screen in front of it.
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(),
                configurer -> configurer.loginView(LoginView.class));
        return http.build();
    }
}