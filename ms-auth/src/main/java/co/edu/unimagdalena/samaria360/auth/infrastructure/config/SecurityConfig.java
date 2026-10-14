package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.error.ProblemDetailAccessDeniedHandler;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.error.ProblemDetailAuthenticationEntryPoint;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.security.JwtTokenProviderAdapter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/** API sin estado con token Bearer: ms-auth emite el JWT y también lo valida con su propia clave pública. */
@Configuration
public class SecurityConfig {

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
                    // Un error interno no debe convertirse en 401 al despachar /error.
                    acceso.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    acceso.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll();
                    // Registro (RF01, RF02) y su lista de países: anónimos.
                    acceso.requestMatchers(HttpMethod.POST, "/api/v1/auth/registro/turista",
                            "/api/v1/auth/registro/prestador").permitAll();
                    acceso.requestMatchers(HttpMethod.GET, "/api/v1/auth/paises").permitAll();
                    acceso.requestMatchers(HttpMethod.GET, "/api/v1/auth/.well-known/jwks.json").permitAll();
                    acceso.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    if (dev) {
                        acceso.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    }
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
        autoridades.setAuthoritiesClaimName(JwtTokenProviderAdapter.CLAIM_ROL);
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}
