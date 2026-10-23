package com.fernando.ecommerce.json;

import java.math.BigDecimal;
import java.util.*;

/**
 * Parser y serializador de JSON escritos a mano (sin Gson ni Jackson).
 *
 * Tipos que maneja: Map<String,Object>, List<Object>, String, Number, Boolean, null.
 */
public final class Json {

    private final String texto;
    private int posicion;

    private Json(String texto) {
        this.texto = texto;
    }

    // ---------------- lectura ----------------

    public static Object parsear(String json) {
        if (json == null) {
            throw new JsonException("El JSON no puede ser nulo");
        }
        Json parser = new Json(json);
        parser.saltarEspacios();
        Object valor = parser.leerValor();
        parser.saltarEspacios();
        if (parser.posicion < parser.texto.length()) {
            throw new JsonException("Contenido sobrante en la posicion " + parser.posicion);
        }
        return valor;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parsearObjeto(String json) {
        Object valor = parsear(json);
        if (!(valor instanceof Map)) {
            throw new JsonException("Se esperaba un objeto JSON");
        }
        return (Map<String, Object>) valor;
    }

    // ---------------- escritura ----------------

    public static String escribir(Object valor) {
        StringBuilder salida = new StringBuilder();
        escribirEn(salida, valor);
        return salida.toString();
    }

    private static void escribirEn(StringBuilder salida, Object valor) {
        if (valor == null) {
            salida.append("null");
        } else if (valor instanceof String) {
            escribirTexto(salida, (String) valor);
        } else if (valor instanceof BigDecimal) {
            salida.append(((BigDecimal) valor).toPlainString());
        } else if (valor instanceof Number || valor instanceof Boolean) {
            salida.append(valor);
        } else if (valor instanceof Map<?, ?>) {
            Map<?, ?> mapa = (Map<?, ?>) valor;
            salida.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                if (!primero) {
                    salida.append(',');
                }
                primero = false;
                escribirTexto(salida, String.valueOf(entrada.getKey()));
                salida.append(':');
                escribirEn(salida, entrada.getValue());
            }
            salida.append('}');
        } else if (valor instanceof Collection<?>) {
            Collection<?> lista = (Collection<?>) valor;
            salida.append('[');
            boolean primero = true;
            for (Object elemento : lista) {
                if (!primero) {
                    salida.append(',');
                }
                primero = false;
                escribirEn(salida, elemento);
            }
            salida.append(']');
        } else if (valor instanceof JsonSerializable) {
            escribirEn(salida, ((JsonSerializable) valor).aJson());
        } else {
            escribirTexto(salida, String.valueOf(valor));
        }
    }

    private static void escribirTexto(StringBuilder salida, String texto) {
        salida.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"':
                    salida.append("\\\"");
                    break;
                case '\\':
                    salida.append("\\\\");
                    break;
                case '\n':
                    salida.append("\\n");
                    break;
                case '\r':
                    salida.append("\\r");
                    break;
                case '\t':
                    salida.append("\\t");
                    break;
                case '\b':
                    salida.append("\\b");
                    break;
                case '\f':
                    salida.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        salida.append(String.format("\\u%04x", (int) c));
                    } else {
                        salida.append(c);
                    }
            }
        }
        salida.append('"');
    }

    /** Objetos que saben convertirse a una estructura serializable. */
    public interface JsonSerializable {
        Map<String, Object> aJson();
    }

    // ---------------- utilidades de lectura ----------------

    @SuppressWarnings("unchecked")
    public static Object buscar(Object raiz, String ruta) {
        Object actual = raiz;
        for (String tramo : ruta.split("\\.")) {
            if (actual == null) {
                return null;
            }
            if (actual instanceof Map<?, ?>) {
                actual = ((Map<String, Object>) actual).get(tramo);
            } else if (actual instanceof List<?>) {
                List<?> lista = (List<?>) actual;
                try {
                    int indice = Integer.parseInt(tramo);
                    actual = (indice >= 0 && indice < lista.size()) ? lista.get(indice) : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return actual;
    }

    public static String textoDe(Map<String, Object> objeto, String clave) {
        Object valor = objeto.get(clave);
        if (!(valor instanceof String) || ((String) valor).isBlank()) {
            throw new JsonException("Falta el campo de texto '" + clave + "'");
        }
        return (String) valor;
    }

    public static BigDecimal decimalDe(Map<String, Object> objeto, String clave) {
        Object valor = objeto.get(clave);
        if (valor instanceof Number) {
            return new BigDecimal(valor.toString());
        }
        if (valor instanceof String) {
            String texto = (String) valor;
            try {
                return new BigDecimal(texto);
            } catch (NumberFormatException e) {
                throw new JsonException("El campo '" + clave + "' no es un numero valido");
            }
        }
        throw new JsonException("Falta el campo numerico '" + clave + "'");
    }

    public static int enteroDe(Map<String, Object> objeto, String clave) {
        return decimalDe(objeto, clave).intValueExact();
    }

    // ---------------- descenso recursivo ----------------

    private Object leerValor() {
        saltarEspacios();
        if (posicion >= texto.length()) {
            throw new JsonException("JSON incompleto");
        }
        char c = texto.charAt(posicion);
        switch (c) {
            case '{':
                return leerObjeto();
            case '[':
                return leerArreglo();
            case '"':
                return leerTexto();
            case 't':
            case 'f':
                return leerBooleano();
            case 'n':
                return leerNulo();
            default:
                return leerNumero();
        }
    }

    private Map<String, Object> leerObjeto() {
        Map<String, Object> objeto = new LinkedHashMap<>();
        consumir('{');
        saltarEspacios();
        if (mirar() == '}') {
            consumir('}');
            return objeto;
        }
        while (true) {
            saltarEspacios();
            String clave = leerTexto();
            saltarEspacios();
            consumir(':');
            objeto.put(clave, leerValor());
            saltarEspacios();
            char siguiente = mirar();
            if (siguiente == ',') {
                consumir(',');
            } else if (siguiente == '}') {
                consumir('}');
                return objeto;
            } else {
                throw new JsonException("Se esperaba ',' o '}' en la posicion " + posicion);
            }
        }
    }

    private List<Object> leerArreglo() {
        List<Object> arreglo = new ArrayList<>();
        consumir('[');
        saltarEspacios();
        if (mirar() == ']') {
            consumir(']');
            return arreglo;
        }
        while (true) {
            arreglo.add(leerValor());
            saltarEspacios();
            char siguiente = mirar();
            if (siguiente == ',') {
                consumir(',');
            } else if (siguiente == ']') {
                consumir(']');
                return arreglo;
            } else {
                throw new JsonException("Se esperaba ',' o ']' en la posicion " + posicion);
            }
        }
    }

    private String leerTexto() {
        consumir('"');
        StringBuilder resultado = new StringBuilder();
        while (true) {
            if (posicion >= texto.length()) {
                throw new JsonException("Cadena sin cerrar");
            }
            char c = texto.charAt(posicion++);
            if (c == '"') {
                return resultado.toString();
            }
            if (c == '\\') {
                char escape = texto.charAt(posicion++);
                switch (escape) {
                    case '"':
                        resultado.append('"');
                        break;
                    case '\\':
                        resultado.append('\\');
                        break;
                    case '/':
                        resultado.append('/');
                        break;
                    case 'b':
                        resultado.append('\b');
                        break;
                    case 'f':
                        resultado.append('\f');
                        break;
                    case 'n':
                        resultado.append('\n');
                        break;
                    case 'r':
                        resultado.append('\r');
                        break;
                    case 't':
                        resultado.append('\t');
                        break;
                    case 'u':
                        resultado.append((char) Integer.parseInt(texto.substring(posicion, posicion + 4), 16));
                        posicion += 4;
                        break;
                    default:
                        throw new JsonException("Escape desconocido: \\" + escape);
                }
            } else {
                resultado.append(c);
            }
        }
    }

    private Object leerNumero() {
        int inicio = posicion;
        if (mirar() == '-') {
            posicion++;
        }
        while (posicion < texto.length()
                && (Character.isDigit(texto.charAt(posicion)) || "+-.eE".indexOf(texto.charAt(posicion)) >= 0)) {
            posicion++;
        }
        String crudo = texto.substring(inicio, posicion);
        try {
            return new BigDecimal(crudo);
        } catch (NumberFormatException e) {
            throw new JsonException("Numero invalido: '" + crudo + "'");
        }
    }

    private Boolean leerBooleano() {
        if (texto.startsWith("true", posicion)) {
            posicion += 4;
            return Boolean.TRUE;
        }
        if (texto.startsWith("false", posicion)) {
            posicion += 5;
            return Boolean.FALSE;
        }
        throw new JsonException("Booleano invalido en la posicion " + posicion);
    }

    private Object leerNulo() {
        if (texto.startsWith("null", posicion)) {
            posicion += 4;
            return null;
        }
        throw new JsonException("Valor invalido en la posicion " + posicion);
    }

    private char mirar() {
        if (posicion >= texto.length()) {
            throw new JsonException("JSON incompleto");
        }
        return texto.charAt(posicion);
    }

    private void consumir(char esperado) {
        if (posicion >= texto.length() || texto.charAt(posicion) != esperado) {
            throw new JsonException("Se esperaba '" + esperado + "' en la posicion " + posicion);
        }
        posicion++;
    }

    private void saltarEspacios() {
        while (posicion < texto.length() && Character.isWhitespace(texto.charAt(posicion))) {
            posicion++;
        }
    }

    public static class JsonException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        public JsonException(String mensaje) {
            super(mensaje);
        }
    }
}
