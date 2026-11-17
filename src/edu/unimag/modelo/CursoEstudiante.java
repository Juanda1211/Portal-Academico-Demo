package edu.unimag.modelo;

import com.cleandev.tpa.api.annotations.TpaConvert;
import com.cleandev.tpa.api.annotations.TpaId;
import edu.unimag.modelo.convertidor.EstadoInscripcionConverter;
import edu.unimag.modelo.enumeracion.EstadoInscripcion;
import edu.unimag.modelo.pk.CursoEstudiantePk;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CursoEstudiante {

    @TpaId
    private CursoEstudiantePk idCursoEstudiante;

    @TpaConvert(converter = EstadoInscripcionConverter.class)
    private EstadoInscripcion estadoInscripcion;

    private Integer calificacionCursoEstudiante;
    private OffsetDateTime fechaInscripcion;
    private OffsetDateTime fechaEvaluacion;

    // Reflexión
    protected CursoEstudiante() {
    }

    // Creación
    public CursoEstudiante(CursoEstudiantePk pk) {
        if (pk == null) {
            throw new IllegalArgumentException("La clave de la inscripcion es obligatoria");
        }
        this.idCursoEstudiante = pk;
        this.estadoInscripcion = EstadoInscripcion.PENDIENTE;
        this.calificacionCursoEstudiante = null;
        this.fechaInscripcion = OffsetDateTime.now();
        this.fechaEvaluacion = null;
    }

    // Hidratación
    public CursoEstudiante(
            CursoEstudiantePk pk,
            EstadoInscripcion estado,
            Integer calificacion,
            OffsetDateTime fechaInscripcion,
            OffsetDateTime fechaEvaluacion
    ) {
        if (pk == null) {
            throw new IllegalArgumentException("La clave de la inscripcion es obligatoria en hidratacion");
        }
        this.idCursoEstudiante = pk;
        this.estadoInscripcion = (estado != null) ? estado : EstadoInscripcion.PENDIENTE;
        this.calificacionCursoEstudiante = calificacion;
        this.fechaInscripcion = (fechaInscripcion != null) ? fechaInscripcion : OffsetDateTime.now();
        this.fechaEvaluacion = fechaEvaluacion;
    }

    // Métodos de comportamiento
    public void admitir() {
        this.estadoInscripcion = this.estadoInscripcion.cambiarEstadoA(EstadoInscripcion.ADMITIDA);
    }

    public void evaluar(Integer calificacion) {
        EstadoInscripcion nuevoEstado = EstadoInscripcion.desdeCalificacion(calificacion);
        this.estadoInscripcion = this.estadoInscripcion.cambiarEstadoA(nuevoEstado);
        this.calificacionCursoEstudiante = calificacion;
        this.fechaEvaluacion = OffsetDateTime.now();
    }

    public void retirar() {
        this.estadoInscripcion = this.estadoInscripcion.cambiarEstadoA(EstadoInscripcion.RETIRADA);
    }

    public void rechazar() {
        this.estadoInscripcion = this.estadoInscripcion.cambiarEstadoA(EstadoInscripcion.RECHAZADA);
    }

    // Métodos de consulta
    public boolean estaPendiente() {
        return this.estadoInscripcion == EstadoInscripcion.PENDIENTE;
    }

    public boolean estaAdmitida() {
        return this.estadoInscripcion == EstadoInscripcion.ADMITIDA;
    }

    public boolean estaAprobada() {
        return this.estadoInscripcion == EstadoInscripcion.APROBADA;
    }

    public boolean estaReprobada() {
        return this.estadoInscripcion == EstadoInscripcion.REPROBADA;
    }

    public boolean estaRetirada() {
        return this.estadoInscripcion == EstadoInscripcion.RETIRADA;
    }

    public boolean estaRechazada() {
        return this.estadoInscripcion == EstadoInscripcion.RECHAZADA;
    }

    public boolean estaEvaluada() {
        return this.calificacionCursoEstudiante != null;
    }

    // Getters
    public CursoEstudiantePk getIdCursoEstudiante() {
        return idCursoEstudiante;
    }

    public EstadoInscripcion getEstadoInscripcion() {
        return estadoInscripcion;
    }

    public Integer getCalificacionCursoEstudiante() {
        return calificacionCursoEstudiante;
    }

    public OffsetDateTime getFechaInscripcion() {
        return fechaInscripcion;
    }

    public OffsetDateTime getFechaEvaluacion() {
        return fechaEvaluacion;
    }

    // Getters de conveniencia (Ley de Demeter)
    public UUID getIdCursoPeriodo() {
        return this.idCursoEstudiante.getIdCursoPeriodo();
    }

    public UUID getIdEstudiante() {
        return this.idCursoEstudiante.getIdEstudiante();
    }

    public CursoPeriodo getCursoPeriodo() {
        return this.idCursoEstudiante.getCursoPeriodo();
    }

    public Estudiante getEstudiante() {
        return this.idCursoEstudiante.getEstudiante();
    }
}
