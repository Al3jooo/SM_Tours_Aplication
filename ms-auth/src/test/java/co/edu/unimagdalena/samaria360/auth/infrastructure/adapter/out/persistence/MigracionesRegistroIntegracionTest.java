package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.auth.infrastructure.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migraciones del CU Registrarse sobre PostgreSQL real: V4 (CHECK de categorías), V5 (razón social opcional), V6 y V7
 * (semilla), V8 (país ISO) y V9 (aceptación de políticas del prestador). V1, V2 y V3 no se editaron.
 */
@PruebaIntegracion
class MigracionesRegistroIntegracionTest {

    private static final UUID ID_TURISTA = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID ID_PRESTADOR = UUID.fromString("00000000-0000-4000-8000-000000000002");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void v4_laBaseSoloAceptaLasTresCategorias() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO turista_categoria_interes (id_usuario, categoria) VALUES (?, 'Playa')", ID_TURISTA))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_turista_categoria_interes_categoria");
    }

    @Test
    void v5_laRazonSocialEsOpcional() {
        assertThat(columna("prestador", "razon_social")).containsEntry("is_nullable", "YES");
    }

    @Test
    void v6_elPrestadorDeLaSemillaQuedoValidado() {
        assertThat(jdbc.queryForObject("SELECT estado_validacion FROM prestador WHERE id_usuario = ?", String.class,
                ID_PRESTADOR)).isEqualTo("Activo");
    }

    @Test
    void v7yV8_elPaisSeGuardaComoCodigoIsoDeDosLetras() {
        assertThat(jdbc.queryForObject("SELECT pais_origen FROM turista WHERE id_usuario = ?", String.class,
                ID_TURISTA)).isEqualTo("CO");
        assertThat(columna("turista", "pais_origen"))
                .containsEntry("data_type", "character varying")
                .containsEntry("character_maximum_length", 2);
        for (String invalido : new String[]{"co", "C1"}) {
            assertThatThrownBy(() -> jdbc.update("UPDATE turista SET pais_origen = ? WHERE id_usuario = ?", invalido,
                    ID_TURISTA)).isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("ck_turista_pais_origen");
        }
    }

    @Test
    void v9_laAceptacionDelPrestadorEsObligatoriaYSinValorPorDefecto() {
        assertThat(columna("prestador", "acepto_politicas"))
                .containsEntry("is_nullable", "NO")
                .containsEntry("column_default", null);
        // Las filas anteriores quedaron en false: nadie aceptó nada en un formulario.
        assertThat(jdbc.queryForObject("SELECT acepto_politicas FROM prestador WHERE id_usuario = ?", Boolean.class,
                ID_PRESTADOR)).isFalse();
    }

    @Test
    void v10_elCatalogoDeRolesExisteSinDuplicados() {
        assertThat(jdbc.queryForList("SELECT nombre FROM rol ORDER BY id_rol", String.class))
                .containsExactly("Turista", "Prestador", "Gestor", "Administrador");
    }

    private Map<String, Object> columna(String tabla, String columna) {
        return jdbc.queryForMap("SELECT is_nullable, data_type, character_maximum_length, column_default"
                + " FROM information_schema.columns WHERE table_name = ? AND column_name = ?", tabla, columna);
    }
}
