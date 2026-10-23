package com.fernando.ecommerce.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class Respuesta {

    private final int codigo;
    private final Object cuerpo;
    private final Map<String, String> cabeceras;

    public Respuesta(int codigo, Object cuerpo, Map<String, String> cabeceras) {
        this.codigo = codigo;
        this.cuerpo = cuerpo;
        this.cabeceras = cabeceras;
    }

    public int codigo() {
        return codigo;
    }

    public Object cuerpo() {
        return cuerpo;
    }

    public Map<String, String> cabeceras() {
        return cabeceras;
    }

    public static Respuesta ok(Object cuerpo) {
        return new Respuesta(200, cuerpo, Map.of());
    }

    public static Respuesta creado(Object cuerpo, String ubicacion) {
        return new Respuesta(201, cuerpo, Map.of("Location", ubicacion));
    }

    public static Respuesta sinContenido() {
        return new Respuesta(204, null, Map.of());
    }

    public static Respuesta error(int codigo, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", mensaje);
        cuerpo.put("codigo", codigo);
        return new Respuesta(codigo, cuerpo, Map.of());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Respuesta)) {
            return false;
        }
        Respuesta that = (Respuesta) o;
        return codigo == that.codigo
                && Objects.equals(cuerpo, that.cuerpo)
                && Objects.equals(cabeceras, that.cabeceras);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo, cuerpo, cabeceras);
    }

    @Override
    public String toString() {
        return "Respuesta[codigo=" + codigo + ", cuerpo=" + cuerpo + ", cabeceras=" + cabeceras + "]";
    }
}
