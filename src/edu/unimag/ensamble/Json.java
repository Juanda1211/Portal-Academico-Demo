package edu.unimag.ensamble;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//Convertidor JSON

public final class Json {

    private Json() {
    }

    // ===================== OBJETO -> JSON =====================

    public static String aJson(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(valor, sb);
        return sb.toString();
    }

    private static void escribir(Object valor, StringBuilder sb) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof BigDecimal numero) {
            sb.append(numero.toPlainString());
        } else if (valor instanceof Number || valor instanceof Boolean) {
            sb.append(valor);
        } else if (valor instanceof Enum<?> e) {
            texto(e.name(), sb);
        } else if (valor instanceof Record r) {
            escribirRecord(r, sb);
        } else if (valor instanceof Map<?, ?> mapa) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                if (!primero) {
                    sb.append(',');
                }
                primero = false;
                texto(String.valueOf(entrada.getKey()), sb);
                sb.append(':');
                escribir(entrada.getValue(), sb);
            }
            sb.append('}');
        } else if (valor instanceof Collection<?> coleccion) {
            sb.append('[');
            boolean primero = true;
            for (Object elemento : coleccion) {
                if (!primero) {
                    sb.append(',');
                }
                primero = false;
                escribir(elemento, sb);
            }
            sb.append(']');
        } else {
            texto(valor.toString(), sb); // UUID, OffsetDateTime, String...
        }
    }

    private static void escribirRecord(Record registro, StringBuilder sb) {
        sb.append('{');
        RecordComponent[] componentes = registro.getClass().getRecordComponents();
        for (int i = 0; i < componentes.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            texto(componentes[i].getName(), sb);
            sb.append(':');
            try {
                escribir(componentes[i].getAccessor().invoke(registro), sb);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("No se pudo leer " + componentes[i].getName(), e);
            }
        }
        sb.append('}');
    }

    private static void texto(String s, StringBuilder sb) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ===================== JSON -> Map =====================

    @SuppressWarnings("unchecked")
    public static Map<String, Object> leerObjeto(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("El cuerpo está vacío");
        }
        Lector lector = new Lector(json);
        Object valor = lector.leerValor();
        lector.verificarFin();
        if (!(valor instanceof Map)) {
            throw new IllegalArgumentException("El cuerpo debe ser JSON");
        }
        return (Map<String, Object>) valor;
    }

    private static final class Lector {

        private final String s;
        private int i = 0;

        Lector(String s) {
            this.s = s;
        }

        void verificarFin() {
            saltarEspacios();
            if (i < s.length()) {
                throw error("Texto sobrante");
            }
        }

        Object leerValor() {
            saltarEspacios();
            if (i >= s.length()) {
                throw error("JSON incompleto");
            }
            char c = s.charAt(i);
            if (c == '{') {
                return leerMapa();
            }
            if (c == '[') {
                return leerLista();
            }
            if (c == '"') {
                return leerCadena();
            }
            if (s.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            if (s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            return leerNumero();
        }

        private Map<String, Object> leerMapa() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            i++; // {
            saltarEspacios();
            if (i < s.length() && s.charAt(i) == '}') {
                i++;
                return mapa;
            }
            while (true) {
                saltarEspacios();
                if (i >= s.length() || s.charAt(i) != '"') {
                    throw error("Se esperaba el nombre de un campo");
                }
                String clave = leerCadena();
                saltarEspacios();
                esperar(':');
                mapa.put(clave, leerValor());
                saltarEspacios();
                if (i < s.length() && s.charAt(i) == ',') {
                    i++;
                } else {
                    esperar('}');
                    return mapa;
                }
            }
        }

        private List<Object> leerLista() {
            List<Object> lista = new ArrayList<>();
            i++; // [
            saltarEspacios();
            if (i < s.length() && s.charAt(i) == ']') {
                i++;
                return lista;
            }
            while (true) {
                lista.add(leerValor());
                saltarEspacios();
                if (i < s.length() && s.charAt(i) == ',') {
                    i++;
                } else {
                    esperar(']');
                    return lista;
                }
            }
        }

        private String leerCadena() {
            StringBuilder sb = new StringBuilder();
            i++; // comilla inicial
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (i >= s.length()) {
                        break;
                    }
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (i + 4 > s.length()) {
                                throw error("Secuencia \\u incompleta");
                            }
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> throw error("Secuencia de escape inválida");
                    }
                } else {
                    sb.append(c);
                }
            }
            throw error("Cadena sin cerrar");
        }

        private Object leerNumero() {
            int inicio = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            String texto = s.substring(inicio, i);
            if (texto.isEmpty()) {
                throw error("Valor inesperado");
            }
            try {
                if (texto.contains(".") || texto.contains("e") || texto.contains("E")) {
                    return Double.valueOf(texto);
                }
                return Long.valueOf(texto);
            } catch (NumberFormatException e) {
                throw error("Número inválido: " + texto);
            }
        }

        private void esperar(char esperado) {
            if (i >= s.length() || s.charAt(i) != esperado) {
                throw error("Se esperaba '" + esperado + "'");
            }
            i++;
        }

        private void saltarEspacios() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        private IllegalArgumentException error(String mensaje) {
            return new IllegalArgumentException("JSON inválido: " + mensaje + " (posición " + i + ")");
        }
    }

    // ===================== Map -> DTO (record) =====================

    /**
     * Arma un record llamando a su constructor, así que las validaciones del
     * DTO (ReglasValidacion) se ejecutan igual que en la consola.
     */
    public static <R extends Record> R aRecord(Class<R> clase, Map<String, Object> datos) {
        RecordComponent[] componentes = clase.getRecordComponents();
        Class<?>[] tipos = new Class<?>[componentes.length];
        Object[] argumentos = new Object[componentes.length];
        for (int i = 0; i < componentes.length; i++) {
            tipos[i] = componentes[i].getType();
            argumentos[i] = convertir(datos.get(componentes[i].getName()), tipos[i], componentes[i].getName());
        }
        try {
            Constructor<R> constructor = clase.getDeclaredConstructor(tipos);
            return constructor.newInstance(argumentos);
        } catch (InvocationTargetException e) {
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException re) {
                throw re; // por ejemplo, IllegalArgumentException de la validación
            }
            throw new IllegalStateException(causa);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo crear " + clase.getSimpleName(), e);
        }
    }

    private static Object convertir(Object valor, Class<?> tipo, String campo) {
        if (valor == null) {
            return null;
        }
        if (tipo.isEnum()) {
            return aEnum(tipo, valor.toString(), campo);
        }
        if (tipo == String.class) {
            return textoSeguro(valor.toString(), campo);
        }
        try {
            if (tipo == Integer.class) {
                return aEntero(valor, campo, Integer.MIN_VALUE, Integer.MAX_VALUE).intValue();
            }
            if (tipo == Long.class) {
                return aEntero(valor, campo, Long.MIN_VALUE, Long.MAX_VALUE);
            }
            if (tipo == Boolean.class) {
                return (valor instanceof Boolean) ? valor : Boolean.valueOf(valor.toString());
            }
            if (tipo == BigDecimal.class) {
                return new BigDecimal(valor.toString());
            }
            if (tipo == UUID.class) {
                return UUID.fromString(valor.toString());
            }
            if (tipo == OffsetDateTime.class) {
                return aFecha(valor.toString());
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new IllegalArgumentException("El campo '" + campo + "' tiene un formato inválido");
        }
        throw new IllegalStateException("Tipo no soportado en el campo " + campo + ": " + tipo.getSimpleName());
    }

    /**
     * Los archivos de persistencia guardan una fila por línea y separan las
     * columnas con '¦'. Un texto que contenga esos caracteres se guarda sin
     * error pero deja la fila ilegible, y desde ese momento todo el listado de
     * la entidad responde 500. Por eso se controla aquí, en la única puerta de
     * entrada de textos de la API: el separador se rechaza (400) y los saltos
     * de línea, que solo pueden venir de un campo multilínea, se vuelven espacio.
     */
    private static String textoSeguro(String texto, String campo) {
        if (texto.indexOf('¦') >= 0) {
            throw new IllegalArgumentException(
                    "El campo '" + campo + "' no puede contener el carácter '¦'");
        }
        return texto.replaceAll("\\R+", " ");
    }

    /** Acepta el nombre de la constante sin importar mayúsculas: "pregrado" -> PREGRADO. */
    private static Object aEnum(Class<?> tipo, String texto, String campo) {
        String buscado = texto.trim().replace(' ', '_');
        for (Object constante : tipo.getEnumConstants()) {
            if (((Enum<?>) constante).name().equalsIgnoreCase(buscado)) {
                return constante;
            }
        }
        StringBuilder validos = new StringBuilder();
        for (Object constante : tipo.getEnumConstants()) {
            if (validos.length() > 0) {
                validos.append(", ");
            }
            validos.append(((Enum<?>) constante).name());
        }
        throw new IllegalArgumentException("El campo '" + campo + "' debe ser uno de: " + validos);
    }

    /** Acepta fecha completa (2024-05-01T10:00:00Z) o solo la fecha (2024-05-01). */
    private static OffsetDateTime aFecha(String texto) {
        String limpio = texto.trim();
        try {
            return OffsetDateTime.parse(limpio);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(limpio).atStartOfDay().atOffset(ZoneOffset.UTC);
        }
    }

    private static Long aEntero(Object valor, String campo, long min, long max) {
        long numero;
        if (valor instanceof Double d) {
            if (d != Math.rint(d)) {
                throw new IllegalArgumentException("El campo '" + campo + "' debe ser un número entero");
            }
            numero = d.longValue();
        } else if (valor instanceof Number n) {
            numero = n.longValue();
        } else {
            String texto = valor.toString().trim();
            try {
                numero = Long.parseLong(texto);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El campo '" + campo + "' debe ser un número entero");
            }
        }
        if (numero < min || numero > max) {
            throw new IllegalArgumentException("El campo '" + campo + "' está fuera de rango");
        }
        return numero;
    }
}
