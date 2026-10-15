package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.error.ProblemDetailAccessDeniedHandler;
import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.error.ProblemDetailAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API sin estado con token Bearer emitido por ms-auth, validado aquí con su JWKS (ADR-005): el gateway solo lo
 * reenvía. La disponibilidad es pública; reservar, consultar y cancelar exigen el rol Turista.
 */
@Configuration
public class SecurityConfig {

    /** El JWKS se descarga al validar el primer token: el servicio arranca aunque ms-auth no esté disponible. */
    @Bean
    public JwtDecoder jwtDecoder(SeguridadPropiedades propiedades) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(propiedades.jwkSetUri())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(propiedades.emisor()));
        return decoder;
    }

    @Bean
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

    /** Claim {@code rol: Turista} → authority {@code ROLE_Turista} (RNF06). */
    private static JwtAuthenticationConverter conversorDeRol() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}
