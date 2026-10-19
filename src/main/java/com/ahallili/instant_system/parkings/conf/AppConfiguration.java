package com.ahallili.instant_system.parkings.conf;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ahallili.instant_system.parkings.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * AppConfiguration
 * The userDetailsService() defines how to retrieve the user using the
 * UserRepository that is injected. The passwordEncoder() creates an instance of
 * the BCryptPasswordEncoder() used to encode the plain user password. The
 * authenticationProvider() sets the new strategy to perform the authentication.
 * 
 */
@Configuration
@EnableCaching
@PropertySource("classpath:application.properties")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class AppConfiguration {

    private final UserRepository userRepository;

    public AppConfiguration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    /**
     * this alllow unauthenticated users/client to call these endpoints
     * namely : login and sining up
     * 
     * @param http
     * @return
     */
    @Bean
    SecurityFilterChain manageAutorisation(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
            SecurityErrorHandler securityErrorHandler) {
        WebExpressionAuthorizationManager.Builder authz = WebExpressionAuthorizationManager.withDefaults();
        http.authorizeHttpRequests((authorize) -> authorize
                .requestMatchers("/api/auth/**", "/api/hello", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .access(authz.expression("permitAll"))
                .anyRequest().authenticated())
                // .ignoringRequestMatchers("/api/auth/**", "/api/hello")
                // in case of specific endpoint
                .csrf((csrf) -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                // register the token filter first
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public Caffeine caffeineConfig() {
        // j'ai fait le choix de mettre 30 secondes car l'api poitiers met à jour les
        // places en temps réél toutes les 30 secondes
        return Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS);
    }

    @Bean
    public CacheManager cacheManager(Caffeine caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }

}
