package co.edu.unimagdalena.samaria360.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Único punto de entrada del sistema (diagrama de despliegue del SAD). No tiene dominio: enruta, aísla fallos y
 * resuelve aspectos transversales. El JWT lo valida cada servicio (ADR-005).
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
