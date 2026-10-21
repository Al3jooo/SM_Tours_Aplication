package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import co.edu.unimagdalena.samaria360.booking.application.port.in.VerificarSesionUseCase;
import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.error.ProblemDetailAccessDeniedHandler;
import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.error.ProblemDetailAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API sin estado con token Bearer emitido por ms-auth, validado aquí con su JWKS (ADR-005): el gateway solo lo
 * reenvía. La disponibilidad es pública; reservar, consultar y cancelar exigen el rol Turista.
 * <p>
 * Dos cadenas (ADR-008): {@code /internal/**} solo para el JWT de servicio de ms-auth ({@code rol = SERVICIO},
 * {@code aud = ms-booking}); todo lo demás, para tokens de usuario cuyo claim {@code ver} no sea menor que la versión de
 * la proyección {@code cuenta} (DEC-29).
 */
@Configuration
public class SecurityConfig {

    /** Tokens de usuario: firma, iss, exp y, después, {@code ver}. */
    @Bean
    public JwtDecoder jwtDecoder(SeguridadPropiedades propiedades, VerificarSesionUseCase sesiones) {
        return decoder(propiedades, new ValidadorVersionSesion(sesiones));
    }

    /** El JWKS se descarga al validar el primer token: el servicio arranca aunque ms-auth no esté disponible. */
    private static JwtDecoder decoder(SeguridadPropiedades propiedades, OAuth2TokenValidator<Jwt> adicional) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(propiedades.jwkSetUri())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(propiedades.emisor()), adicional));
        return decoder;
    }

    /** Endpoints internos: solo el JWT de servicio de ms-auth. Un token de usuario recibe 403; una {@code aud} ajena, 401. */
    @Bean
    @Order(1)
    public SecurityFilterChain cadenaInterna(HttpSecurity http, SeguridadPropiedades propiedades,
                                             ProblemDetailAuthenticationEntryPoint entryPoint,
                                             ProblemDetailAccessDeniedHandler accesoDenegado) throws Exception {
        JwtDecoder decoderDeServicio = decoder(propiedades, new ValidadorTokenServicio(propiedades.audiencia()));
        http
                .securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(acceso -> acceso.anyRequest().hasRole("SERVICIO"))
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.decoder(decoderDeServicio).jwtAuthenticationConverter(conversorDeRol()))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accesoDenegado))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accesoDenegado));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, Environment entorno, JwtDecoder jwtDecoder,
                                                 ProblemDetailAuthenticationEntryPoint entryPoint,
                                                 ProblemDetailAccessDeniedHandler accesoDenegado) throws Exception {
        boolean dev = entorno.acceptsProfiles(Profiles.of("dev"));
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(acceso -> {
                    acceso.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    acceso.requestMatchers(HttpMethod.GET, "/api/v1/reservas/disponibilidad",
                            "/api/v1/reservas/disponibilidad/resumen").permitAll();
                    acceso.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    if (dev) {
                        acceso.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    }
                    acceso.requestMatchers("/api/v1/reservas/**").hasRole("Turista");
                    acceso.anyRequest().authenticated();
                })
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(conversorDeRol()))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accesoDenegado))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accesoDenegado));
        return http.build();
    }

    /** Claim {@code rol: Turista} → authority {@code ROLE_Turista}; {@code rol: SERVICIO} → {@code ROLE_SERVICIO} (RNF06). */
    private static JwtAuthenticationConverter conversorDeRol() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}
