package edu.unimag.modelo;

import com.cleandev.tpa.api.annotations.TpaConvert;
import com.cleandev.tpa.api.annotations.TpaId;
import edu.unimag.modelo.convertidor.EstadoProfesorConverter;
import edu.unimag.modelo.convertidor.TipoAsignacionConverter;
import edu.unimag.modelo.enumeracion.EstadoProfesorCurso;
import edu.unimag.modelo.enumeracion.TipoAsignacion;
import edu.unimag.modelo.pk.CursoProfesorPk;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CursoProfesor {

    @TpaId
    private CursoProfesorPk idCursoProfesorPeriodo;
    private OffsetDateTime fechaAsignacion;
    @TpaConvert(converter = TipoAsignacionConverter.class)
    private TipoAsignacion tipoAsignacion;
    @TpaConvert(converter = EstadoProfesorConverter.class)
    private EstadoProfesorCurso estado;

    //reflexion
    protected CursoProfesor() {
    }

    //creacion
    public CursoProfesor(
            CursoProfesorPk idCursoProfesorPeriodo,
            TipoAsignacion tipoAsignacion
    ) {
        this.idCursoProfesorPeriodo = idCursoProfesorPeriodo;
        fechaAsignacion = OffsetDateTime.now();
        this.tipoAsignacion = tipoAsignacion;
        estado = EstadoProfesorCurso.ACTIVO;
    }

    //hidratacion
    public CursoProfesor(
            CursoProfesorPk idCursoProfesorPeriodo,
            OffsetDateTime fechaAsignacion,
            TipoAsignacion tipoAsignacion,
            EstadoProfesorCurso estado
    ) {
        this.idCursoProfesorPeriodo = idCursoProfesorPeriodo;
        this.tipoAsignacion = tipoAsignacion;
        this.fechaAsignacion = (fechaAsignacion != null) ? fechaAsignacion : OffsetDateTime.now();
        this.estado = (estado == null) ? EstadoProfesorCurso.ACTIVO : estado;
    }

    public void cambiarTipoAsignacion(TipoAsignacion nuevo) {
        if (nuevo == tipoAsignacion) {
            throw new IllegalArgumentException("no realizaste ningun cambio");
        }
        tipoAsignacion = nuevo;
    }

    public void retirar() {
        estado = estado.cambiarEstado(EstadoProfesorCurso.RETIRADO);
    }

    public void reactivar() {

        estado = estado.cambiarEstado(EstadoProfesorCurso.ACTIVO);
    }

    public Boolean estaActivo() {
        return estado == EstadoProfesorCurso.ACTIVO;
    }

    public OffsetDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public TipoAsignacion getTipoAsignacion() {
        return tipoAsignacion;
    }

    public EstadoProfesorCurso getEstado() {
        return estado;
    }

    public Long getidProfesor() {
        return idCursoProfesorPeriodo.getIdProfesor();
    }

    public UUID getIdCursoPeriodo() {
        return idCursoProfesorPeriodo.getIdCursoPeriodo();
    }

    public Profesor getProfesor() {
        return idCursoProfesorPeriodo.getProfesor();
    }

    public CursoPeriodo getCursoPeriodo() {
        return idCursoProfesorPeriodo.getCursoPeriodo();
    }

    public CursoProfesorPk getIdCursoProfesorPeriodo() {
        return idCursoProfesorPeriodo;
    }

}
