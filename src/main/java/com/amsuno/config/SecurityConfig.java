package com.amsuno.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain seguridad(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(reglas -> reglas
                .requestMatchers("/", "/reservar", "/login", "/acceso-denegado", "/error").permitAll()
                .requestMatchers("/css/**", "/js/**", "/webjars/**").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/peliculas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/snacks/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/consultas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reservas/**").hasAnyRole("ADMIN", "CAJERO")
                .requestMatchers("/api/**").hasRole("ADMIN")

                .requestMatchers("/admin/estadisticas", "/admin/graficos", "/admin/configuracion").hasRole("ADMIN")
                .requestMatchers("/admin/*/eliminar/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "CAJERO")

                .anyRequest().authenticated())
            .formLogin(login -> login
                .loginPage("/login")
                .defaultSuccessUrl("/admin/dashboard", true)
                .permitAll())
            .httpBasic(Customizer.withDefaults())
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll())
            .exceptionHandling(errores -> errores.accessDeniedPage("/acceso-denegado"));

        return http.build();
    }
}
