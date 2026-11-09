package br.com.nutriexpress.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracoes de seguranca do Spring Security.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;

    public SecurityConfigurations(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    // Rotas publicas de autenticacao
                    req.requestMatchers(HttpMethod.POST, "/auth/login").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/auth/registrar").permitAll();

                    // Documentacao Swagger OpenAPI
                    req.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();

                    // Console do H2
                    req.requestMatchers("/h2-console/**").permitAll();

                    // Leitura do cardapio e categorias publica
                    req.requestMatchers(HttpMethod.GET, "/categorias/**", "/pratos/**").permitAll();

                    // Modificacoes no cardapio apenas para ADMIN
                    req.requestMatchers(HttpMethod.POST, "/categorias/**", "/pratos/**").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.PUT, "/categorias/**", "/pratos/**").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.PATCH, "/categorias/**", "/pratos/**").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.DELETE, "/categorias/**", "/pratos/**").hasRole("ADMIN");

                    // Demais rotas exigem autenticacao
                    req.anyRequest().authenticated();
                })
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // Necessario para o H2 console
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
