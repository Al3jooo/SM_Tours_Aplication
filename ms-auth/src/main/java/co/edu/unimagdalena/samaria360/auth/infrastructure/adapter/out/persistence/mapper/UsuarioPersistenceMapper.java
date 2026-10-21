package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.mapper;

import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

@Component
public class UsuarioPersistenceMapper {

    public Usuario aDominio(UsuarioEntity entidad) {
        return new Usuario(
                entidad.getIdUsuario(),
                entidad.getCorreo(),
                entidad.getContrasenaHash(),
                Rol.valueOf(entidad.getRol().getNombre()),
                entidad.getEstadoCuenta(),
                entidad.getIntentosFallidos(),
                entidad.getBloqueadoHasta(),
                entidad.getVersionCuenta());
    }

    /** Copia al registro los campos que puede cambiar el dominio. */
    public void actualizar(UsuarioEntity entidad, Usuario usuario) {
        entidad.setIntentosFallidos(usuario.getIntentosFallidos());
        entidad.setBloqueadoHasta(usuario.getBloqueadoHasta().orElse(null));
        entidad.setEstadoCuenta(usuario.getEstadoCuenta());
    }
}
