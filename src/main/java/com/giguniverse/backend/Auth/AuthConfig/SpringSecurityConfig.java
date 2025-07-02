package com.giguniverse.backend.Auth.AuthConfig;

import com.giguniverse.backend.Auth.Service.CustomOAuth2FreelancerHandler;
import com.giguniverse.backend.Auth.Service.CustomOAuth2FreelancerFailureHandler;
import com.giguniverse.backend.Auth.Service.CustomOAuth2FreelancerService;
import com.giguniverse.backend.Auth.Service.EmployerOAuth2LoginFailureHandler;
import com.giguniverse.backend.Auth.Service.EmployerOAuth2LoginSuccessHandler;
import com.giguniverse.backend.Auth.Service.FreelancerOAuth2LoginFailureHandler;
import com.giguniverse.backend.Auth.Service.FreelancerOAuth2LoginSuccessHandler;
import com.giguniverse.backend.Auth.Service.CustomOAuth2EmployerHandler;
import com.giguniverse.backend.Auth.Service.CustomOAuth2EmployerFailureHandler;
import com.giguniverse.backend.Auth.Service.CustomOAuth2EmployerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    // === FREELANCER REGISTRATION COMPONENTS ===
    @Autowired
    private CustomOAuth2FreelancerHandler freelancerHandler;

    @Autowired
    private CustomOAuth2FreelancerFailureHandler freelancerFailHandler;

    @Autowired
    private CustomOAuth2FreelancerService freelancerService;

    // === EMPLOYER REGISTRATION COMPONENTS ===
    @Autowired
    private CustomOAuth2EmployerHandler employerHandler;

    @Autowired
    private CustomOAuth2EmployerFailureHandler employerFailHandler;

    @Autowired
    private CustomOAuth2EmployerService employerService;

    // === FREELANCER REGISTRATION CHAIN ===
    @Bean
    @Order(1)
    public SecurityFilterChain freelancerChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/login/oauth2/code/google-freelancer", "/oauth2/authorization/google-freelancer")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth -> oauth
                .loginPage("/oauth2/authorization/google-freelancer")
                .userInfoEndpoint(info -> info.oidcUserService(freelancerService))
                .successHandler(freelancerHandler)
                .failureHandler(freelancerFailHandler)
            );

        return http.build();
    }

    // === EMPLOYER REGISTRATION CHAIN ===
    @Bean
    @Order(2)
    public SecurityFilterChain employerChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/login/oauth2/code/google-employer", "/oauth2/authorization/google-employer")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth -> oauth
                .loginPage("/oauth2/authorization/google-employer")
                .userInfoEndpoint(info -> info.oidcUserService(employerService))
                .successHandler(employerHandler)
                .failureHandler(employerFailHandler)
            );

        return http.build();
    }

    // === FREELANCER LOGIN COMPONENTS ===
    @Autowired private FreelancerOAuth2LoginSuccessHandler freelancerLoginSuccessHandler;
    @Autowired private FreelancerOAuth2LoginFailureHandler freelancerLoginFailHandler;
    
    // === FREELANCER LOGIN CHAIN ===
    @Bean
    @Order(3)
    public SecurityFilterChain freelancerLoginChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/login/oauth2/code/google-freelancer-login", "/oauth2/authorization/google-freelancer-login")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth -> oauth
                .loginPage("/oauth2/authorization/google-freelancer-login")
                .userInfoEndpoint(info -> info.oidcUserService(new OidcUserService()))
                .successHandler(freelancerLoginSuccessHandler)
                .failureHandler(freelancerLoginFailHandler)
            );

        return http.build();
    }

    // === EMPLOYER LOGIN COMPONENTS ===
    @Autowired private EmployerOAuth2LoginSuccessHandler employerLoginSuccessHandler;
    @Autowired private EmployerOAuth2LoginFailureHandler employerLoginFailHandler;

    @Bean
    @Order(4)
    public SecurityFilterChain employerLoginChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/login/oauth2/code/google-employer-login", "/oauth2/authorization/google-employer-login")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth -> oauth
                .loginPage("/oauth2/authorization/google-employer-login")
                .userInfoEndpoint(info -> info.oidcUserService(new OidcUserService()))
                .successHandler(employerLoginSuccessHandler)
                .failureHandler(employerLoginFailHandler)
            );

        return http.build();
    }

    // === DEFAULT FILTER CHAIN ===
    @Bean
    @Order(99)
    public SecurityFilterChain defaultSecurity(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", 
                    "/check/postgres",
                    "/check/mongo",                
                    "/register/**", 
                    "/api/auth/**", 
                    "/oauth2/**", 
                    "/login/oauth2/code/**",
                    "/oauth2-init/**",
                    "/error"
                ).permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
