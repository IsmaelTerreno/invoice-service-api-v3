package com.remotejob.planservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configures our application with Spring Security to restrict access to our API endpoints.
 */
@Configuration
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * A final instance of {@code JwtFilter} that is responsible for intercepting HTTP requests
     * to validate and process JWT tokens for authentication.
     * <p>
     * This filter checks for a valid JWT token in the Authorization header of incoming requests,
     * extracts the claims if the token is valid, creates an authentication object, and sets it
     * in the SecurityContextHolder for subsequent security checks within the application.
     */
    private final JwtFilter jwtFilter;
    /**
     * A string representing the origins allowed for CORS (Cross-Origin Resource Sharing).
     * This is typically configured through an external property, such as application.yml or application.properties.
     * It is used to initialize the CORS configuration within the {@code SecurityConfig} class.
     */
    private final String corsAllowedOrigins;

    public SecurityConfig(JwtFilter jwtFilter, @Value("${cors.allowed.origins}") String corsAllowedOrigins) {
        this.jwtFilter = jwtFilter;
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    /**
     * Sets up and returns a CORS (Cross-Origin Resource Sharing) configuration.
     * This configuration allows all origins and specifies the permitted methods
     * and credentials policies.
     *
     * @return a {@code CorsConfiguration} object containing the CORS settings
     */
    private CorsConfiguration getCorsConfiguration() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        List<String> allowedOrigins = List.of(
                corsAllowedOrigins.split(",")
        );
        corsConfiguration.setAllowedOrigins(allowedOrigins);
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE"));
        corsConfiguration.applyPermitDefaultValues();
        UrlBasedCorsConfigurationSource corsSource = new UrlBasedCorsConfigurationSource();
        corsSource.registerCorsConfiguration("/**", corsConfiguration);
        return corsConfiguration;
    }

    /**
     * Configures HTTP security settings for the application.
     *
     * @param http the {@code HttpSecurity} to modify
     * @return a {@code SecurityFilterChain} describing the security filter chain for the application
     * @throws Exception if an error occurs while configuring the {@code HttpSecurity}
     */
    @Bean
    public SecurityFilterChain configureHttpSecurity(HttpSecurity http) throws Exception {
        // Define CORS configuration
        CorsConfiguration corsConfiguration = getCorsConfiguration();
        return http
                // Disable default security settings
                .httpBasic(AbstractHttpConfigurer::disable)
                // Disable default security settings
                .csrf(AbstractHttpConfigurer::disable)
                // Configure session management with stateless sessions
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Authorize requests
                .authorizeHttpRequests((authorize) -> authorize
                        // Allow Swagger/OpenAPI docs publicly
                        .requestMatchers(
                                "/doc",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/plan/{id}").permitAll()
                        // Removed unused search endpoint and enforced auth for user path
                        .requestMatchers(HttpMethod.GET, "/api/v1/plan/user/{userId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/plan").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/plan").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/plan/{id}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/plan/{id}").authenticated()
                        .requestMatchers("/actuator/health/**").permitAll()
                )
                // Add JWT filter after UsernamePasswordAuthenticationFilter
                .addFilterAfter(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // Configure CORS
                .cors(cors -> cors.configurationSource(request -> corsConfiguration))
                .build();
    }

    /**
     * Creates and returns a BCryptPasswordEncoder instance.
     * This encoder is used to hash passwords using the BCrypt strong hashing function.
     *
     * @return a PasswordEncoder that uses the BCrypt hashing algorithm
     */
    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }
}