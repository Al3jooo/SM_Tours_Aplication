package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.CuentaRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.GestionServiciosEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.ResultadoActualizacion;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Borradores;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Cuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.RolCuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DEC-142 (b), ajuste 5: la otra mitad de la carrera entre «Registrar servicio» y RF22. El relevo entrega «Inactivo» y
 * {@code ActualizarCuentaService} corre su primer {@code updateMulti} cuando el servicio todavía no existe; el registro
 * lee la proyección aún Activa, inserta y relee (sigue Activa); recién después se guarda la proyección. La segunda
 * fijación, después de guardar, oculta el servicio.
 */
class CarreraRegistroInactivacionTest {

    private static final UUID PRESTADOR = UUID.fromString("00000000-0000-4000-8000-0000000000d2");

    @Test
    void siLaProyeccionSeGuardaDespuesDelInsertLaSegundaFijacionOcultaElServicio() {
        GestionServiciosEnMemoria servicios = new GestionServiciosEnMemoria();
        AtomicReference<ServicioComercial> registrado = new AtomicReference<>();
        RelojFijo reloj = new RelojFijo(Instant.parse("2026-10-24T15:00:00Z"));
        CuentaRepositoryEnMemoria cuentas = new CuentaRepositoryEnMemoria() {
            private boolean registrando;

            @Override
            public boolean guardarSiEsMasNueva(Cuenta cuenta) {
                if (!registrando && registrado.get() == null) {
                    // El prestador registra entre el primer updateMulti y el guardado de la proyección.
                    registrando = true;
                    registrado.set(new RegistrarServicioService(servicios, this, reloj)
                            .registrar(PRESTADOR, Borradores.tour()));
                }
                return super.guardarSiEsMasNueva(cuenta);
            }
        };
        cuentas.agregar(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Activo, 3, "RNT-1"));
        ActualizarCuentaService actualizar = new ActualizarCuentaService(cuentas, servicios.visibilidad());

        ResultadoActualizacion resultado = actualizar.actualizar(
                new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Inactivo, 4, "RNT-1"));

        assertThat(resultado).isEqualTo(ResultadoActualizacion.APLICADA);
        // El registro vio la cuenta Activa en sus dos lecturas: devolvió el servicio activo.
        assertThat(registrado.get().isPrestadorActivo()).isTrue();
        // Pero la segunda fijación, después de guardar la proyección, lo dejó oculto.
        ServicioComercial guardado = servicios.guardado(registrado.get().getId().orElseThrow());
        assertThat(guardado.isPrestadorActivo()).isFalse();
        assertThat(guardado.esVisibleEnBusquedaPublica()).isFalse();
    }
}
