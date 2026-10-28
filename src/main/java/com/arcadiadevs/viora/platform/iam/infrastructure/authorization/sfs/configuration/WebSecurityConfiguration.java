package com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.configuration;

import com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import com.arcadiadevs.viora.platform.iam.infrastructure.hashing.argon2.Argon2HashingService;
import com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import com.arcadiadevs.viora.platform.shared.infrastructure.deeplinks.configuration.DeepLinkProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * Web Security Configuration.
 *
 * <p>Configures Spring Security filter chains, stateless JWT session management,
 * Argon2 password encoding, and authorization request filters.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {

    private final UserDetailsService userDetailsService;
    private final BearerTokenService tokenService;
    private final Argon2HashingService hashingService;
    private final AuthenticationEntryPoint unauthorizedRequestHandler;
    private final DeepLinkProperties deepLinkProperties;
    private final List<String> allowedOrigins;

    public WebSecurityConfiguration(
            @Qualifier("defaultUserDetailsService") UserDetailsService userDetailsService,
            BearerTokenService tokenService,
            Argon2HashingService hashingService,
            AuthenticationEntryPoint unauthorizedRequestHandler,
            DeepLinkProperties deepLinkProperties,
            @Value("${web.cors.allowed-origins:*}") List<String> allowedOrigins
    ) {
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.hashingService = hashingService;
        this.unauthorizedRequestHandler = unauthorizedRequestHandler;
        this.deepLinkProperties = deepLinkProperties;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public BearerAuthorizationRequestFilter authorizationRequestFilter() {
        return new BearerAuthorizationRequestFilter(tokenService, userDetailsService, unauthorizedRequestHandler,
                deepLinkProperties.passwordResetPath());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(hashingService);
        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return hashingService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(configurer -> configurer.configurationSource(request -> {
            var cors = new CorsConfiguration();
            // CORS_ALLOWED_ORIGINS (comma separated) restricts the browsers that may call the API; the app is not a
            // browser and is not affected
            cors.setAllowedOrigins(allowedOrigins);
            cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
            cors.setAllowedHeaders(List.of("*"));
            return cors;
        }));

        http.csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(unauthorizedRequestHandler))
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        // Deny by default: only the routes below answer without a session. Any other route needs a
                        // valid token even when its controller forgets to resolve the current user.
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/.well-known/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/email/**",
                                "/error",
                                deepLinkProperties.passwordResetPath() + "/**",
                                "/payments/return/**").permitAll()
                        // The plan catalog is shown before signing up, and a sealed dossier is checked by third parties
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/subscription-plans/**",
                                "/api/v1/agronomic-dossiers/verifications/**").permitAll()
                        // Mercado Pago signs its notifications; the controller verifies that signature
                        .requestMatchers(HttpMethod.POST, "/api/v1/payment-notifications/mercado-pago").permitAll()
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authorizationRequestFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
