package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.GestionarFavoritosUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.GestionarFavoritosUseCase.Guardado;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoOferta;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaFavorito;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaLista;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.error.DatosInvalidosException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * CU «Guardar favoritos» (RF54; DEC-80, DEC-86). Solo el rol Turista (ver {@code SecurityConfig}); el turista es siempre
 * el del claim {@code sub}. {@code PUT} y {@code DELETE} son idempotentes. Contrato en {@code docs/sad/api/ms-catalog.md}.
 */
@RestController
@RequestMapping("/api/v1/catalogo/favoritos")
public class FavoritoController {

    private final GestionarFavoritosUseCase favoritos;

    public FavoritoController(GestionarFavoritosUseCase favoritos) {
        this.favoritos = favoritos;
    }

    @GetMapping
    public RespuestaLista<RespuestaFavorito> listar(@AuthenticationPrincipal Jwt jwt) {
        return new RespuestaLista<>(favoritos.listar(turista(jwt)).stream().map(RespuestaFavorito::de).toList());
    }

    /** 201 si se agregó ahora; 200 si ya estaba. */
    @PutMapping("/{tipo}/{id}")
    public ResponseEntity<RespuestaFavorito> guardar(@AuthenticationPrincipal Jwt jwt, @PathVariable String tipo,
                                                     @PathVariable String id) {
        Guardado guardado = favoritos.guardar(turista(jwt), tipo(tipo), id);
        return ResponseEntity.status(guardado.creado() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(RespuestaFavorito.de(guardado.favorito()));
    }

    /** 204 aunque no existiera. */
    @DeleteMapping("/{tipo}/{id}")
    public ResponseEntity<Void> quitar(@AuthenticationPrincipal Jwt jwt, @PathVariable String tipo,
                                       @PathVariable String id) {
        favoritos.quitar(turista(jwt), tipo(tipo), id);
        return ResponseEntity.noContent().build();
    }

    private static UUID turista(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    /** {@code Servicio} o {@code Atractivo}, exactos (DEC-80); cualquier otro valor es 400 con {@code campos.tipo}. */
    private static TipoOferta tipo(String tipo) {
        for (TipoOferta valor : TipoOferta.values()) {
            if (valor.name().equals(tipo)) {
                return valor;
            }
        }
        throw new DatosInvalidosException(Map.of("tipo", "NO_PERMITIDO"));
    }
}
