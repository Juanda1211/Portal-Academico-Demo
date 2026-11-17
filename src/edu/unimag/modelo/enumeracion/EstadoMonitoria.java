package edu.unimag.modelo.enumeracion;

import com.cleandev.tpa.api.converter.CodedEnum;

public enum EstadoMonitoria implements CodedEnum {

    PENDIENTE(1, "Pendiente"),
    APROBADO(2, "Aprobado"),
    REPROBADO(3, "Reprobado");

    /** La monitoría se califica de 0 a 5; es una escala distinta a la de las inscripciones. */
    public static final int NOTA_MINIMA = 0;
    public static final int NOTA_MAXIMA = 5;
    public static final int NOTA_APROBACION = 3;

    private final int code;
    private final String description;

    private EstadoMonitoria(int code, String description) {
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
     * Deriva el estado a partir de la calificación (0 a 5): aprueba con 3 o más.
     */
    public static EstadoMonitoria desdeCalificacion(Integer calificacion) {
        if (calificacion == null) {
            throw new IllegalArgumentException("La calificacion es obligatoria para determinar el estado");
        }
        if (calificacion < NOTA_MINIMA || calificacion > NOTA_MAXIMA) {
            throw new IllegalArgumentException(
                    "La calificacion debe estar entre " + NOTA_MINIMA + " y " + NOTA_MAXIMA);
        }
        return (calificacion >= NOTA_APROBACION) ? APROBADO : REPROBADO;
    }

    /**
     * Máquina de estados de la monitoría.
     * Permite la reevaluación: aprobado y reprobado pueden intercambiarse y
     * conservar su estado con una nota distinta.
     */
    public EstadoMonitoria cambiarEstadoA(EstadoMonitoria nuevo) {

        if (nuevo == null) {
            throw new IllegalArgumentException("El estado nuevo no puede ser nulo");
        }

        if (this == PENDIENTE && (nuevo == APROBADO || nuevo == REPROBADO)) {
            return nuevo;
        }

        // Reevaluar con otra nota que da el mismo resultado (3 -> 4, o 1 -> 0) es válido:
        // la traza de auditoría conserva la nota anterior.
        if (this != PENDIENTE && this == nuevo) {
            return nuevo;
        }

        if (this == APROBADO && nuevo == REPROBADO) {
            return nuevo;
        }

        if (this == REPROBADO && nuevo == APROBADO) {
            return nuevo;
        }

        throw new IllegalStateException(
                "Transicion invalida: " + this + " -> " + nuevo
        );
    }
}
