package edu.unimag.modelo.enumeracion;

import com.cleandev.tpa.api.converter.CodedEnum;
import edu.unimag.modelo.constante.PoliticasAcademicas;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum EstadoInscripcion implements CodedEnum {

    PENDIENTE(1, "Pendiente"),
    ADMITIDA(2, "Admitida"),
    APROBADA(3, "Aprobada"),
    REPROBADA(4, "Reprobada"),
    RETIRADA(5, "Retirada"),
    RECHAZADA(6, "Rechazada");

    private final int code;
    private final String description;

    /**
     * Tabla de transiciones legales por estado de origen.
     * Reprobada, retirada y rechazada son terminales.
     */
    private static final Map<EstadoInscripcion, Set<EstadoInscripcion>> TRANSICIONES
            = crearTablaDeTransiciones();

    private static Map<EstadoInscripcion, Set<EstadoInscripcion>> crearTablaDeTransiciones() {
        Map<EstadoInscripcion, Set<EstadoInscripcion>> tabla
                = new EnumMap<>(EstadoInscripcion.class);

        tabla.put(PENDIENTE, EnumSet.of(ADMITIDA, RECHAZADA));
        tabla.put(ADMITIDA, EnumSet.of(APROBADA, REPROBADA, RETIRADA));
        tabla.put(APROBADA, EnumSet.of(RETIRADA));
        tabla.put(REPROBADA, EnumSet.noneOf(EstadoInscripcion.class));
        tabla.put(RETIRADA, EnumSet.noneOf(EstadoInscripcion.class));
        tabla.put(RECHAZADA, EnumSet.noneOf(EstadoInscripcion.class));

        return Collections.unmodifiableMap(tabla);
    }

    private EstadoInscripcion(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Deriva el estado a partir de la calificación, usando el umbral de
     * aprobación de las políticas académicas.
     */
    public static EstadoInscripcion desdeCalificacion(Integer calificacion) {
        if (calificacion == null) {
            throw new IllegalArgumentException("La calificacion es obligatoria para determinar el estado");
        }
        return (calificacion >= PoliticasAcademicas.NOTA_APROBACION) ? APROBADA : REPROBADA;
    }

    /**
     * Aplica una transición de estado. Rechaza cualquier transición que no
     * esté declarada para el estado actual.
     */
    public EstadoInscripcion cambiarEstadoA(EstadoInscripcion nuevo) {

        if (nuevo == null) {
            throw new IllegalArgumentException("El estado nuevo no puede ser nulo");
        }

        if (TRANSICIONES.get(this).contains(nuevo)) {
            return nuevo;
        }

        throw new IllegalStateException(
                "Transicion invalida: " + this + " -> " + nuevo
        );
    }

    public boolean esTerminal() {
        return TRANSICIONES.get(this).isEmpty();
    }

    public Set<EstadoInscripcion> transicionesPermitidas() {
        return TRANSICIONES.get(this);
    }
}
