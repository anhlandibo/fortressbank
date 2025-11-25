package com.uit.userservice.config;

import com.uit.userservice.security.ParseUserInfoFilter;
import com.uit.userservice.security.RoleCheckInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {
    
    private final ParseUserInfoFilter parseUserInfoFilter;
    private final RoleCheckInterceptor roleCheckInterceptor;
    
    public SecurityConfig(ParseUserInfoFilter parseUserInfoFilter, RoleCheckInterceptor roleCheckInterceptor) {
        this.parseUserInfoFilter = parseUserInfoFilter;
        this.roleCheckInterceptor = roleCheckInterceptor;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(parseUserInfoFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // Kong already authenticated
            );
        return http.build();
    }
    
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(roleCheckInterceptor);
    }
}