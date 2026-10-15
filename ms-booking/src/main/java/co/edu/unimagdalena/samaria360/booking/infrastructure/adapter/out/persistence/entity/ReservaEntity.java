package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.persistence.entity;

import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoReserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.UnidadCobro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Tabla {@code reserva} (V1). El id lo asigna el dominio: se inserta con {@code EntityManager.persist} y nunca con
 * {@code save}, que con un id ya asignado haría un merge con un SELECT previo.
 */
@Entity
@Table(name = "reserva")
public class ReservaEntity {

    @Id
    @Column(name = "id_reserva", nullable = false)
    private UUID idReserva;

    @Column(name = "id_turista", nullable = false)
    private UUID idTurista;

    @Column(name = "id_prestador", nullable = false)
    private UUID idPrestador;

    @Column(name = "id_servicio_mongo", nullable = false, length = 24)
    private String idServicioMongo;

    @Column(name = "nombre_servicio", nullable = false, length = 150)
    private String nombreServicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 20)
    private CategoriaReservable categoria;

    @Column(name = "id_unidad", length = 24)
    private String idUnidad;

    @Column(name = "nombre_unidad", length = 100)
    private String nombreUnidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "cantidad_solicitada", nullable = false)
    private int cantidadSolicitada;

    @Column(name = "personas", nullable = false)
    private int personas;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_cobro", nullable = false, length = 20)
    private UnidadCobro unidadCobro;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "fecha_entrada", nullable = false)
    private LocalDate fechaEntrada;

    @Column(name = "fecha_salida")
    private LocalDate fechaSalida;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private Instant fechaFin;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_cancelacion")
    private Instant fechaCancelacion;

    @Column(name = "clave_idempotencia", nullable = false)
    private UUID claveIdempotencia;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "hash_solicitud", nullable = false, length = 64)
    private String hashSolicitud;

    protected ReservaEntity() {
    }

    public ReservaEntity(UUID idReserva, UUID idTurista, UUID idPrestador, String idServicioMongo,
                         String nombreServicio, CategoriaReservable categoria, String idUnidad, String nombreUnidad,
                         EstadoReserva estado, int cantidadSolicitada, int personas, BigDecimal precioUnitario,
                         UnidadCobro unidadCobro, BigDecimal montoTotal, String moneda, LocalDate fechaEntrada,
                         LocalDate fechaSalida, LocalTime horaInicio, Instant fechaInicio, Instant fechaFin,
                         Instant fechaCreacion, Instant fechaCancelacion, UUID claveIdempotencia,
                         String hashSolicitud) {
        this.idReserva = idReserva;
        this.idTurista = idTurista;
        this.idPrestador = idPrestador;
        this.idServicioMongo = idServicioMongo;
        this.nombreServicio = nombreServicio;
        this.categoria = categoria;
        this.idUnidad = idUnidad;
        this.nombreUnidad = nombreUnidad;
        this.estado = estado;
        this.cantidadSolicitada = cantidadSolicitada;
        this.personas = personas;
        this.precioUnitario = precioUnitario;
        this.unidadCobro = unidadCobro;
        this.montoTotal = montoTotal;
        this.moneda = moneda;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.horaInicio = horaInicio;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.fechaCreacion = fechaCreacion;
        this.fechaCancelacion = fechaCancelacion;
        this.claveIdempotencia = claveIdempotencia;
        this.hashSolicitud = hashSolicitud;
    }

    public UUID getIdReserva() {
        return idReserva;
    }

    public UUID getIdTurista() {
        return idTurista;
    }

    public UUID getIdPrestador() {
        return idPrestador;
    }

    public String getIdServicioMongo() {
        return idServicioMongo;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public CategoriaReservable getCategoria() {
        return categoria;
    }

    public String getIdUnidad() {
        return idUnidad;
    }

    public String getNombreUnidad() {
        return nombreUnidad;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public int getCantidadSolicitada() {
        return cantidadSolicitada;
    }

    public int getPersonas() {
        return personas;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public UnidadCobro getUnidadCobro() {
        return unidadCobro;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public String getMoneda() {
        return moneda;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public Instant getFechaInicio() {
        return fechaInicio;
    }

    public Instant getFechaFin() {
        return fechaFin;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaCancelacion() {
        return fechaCancelacion;
    }

    public UUID getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public String getHashSolicitud() {
        return hashSolicitud;
    }
}
