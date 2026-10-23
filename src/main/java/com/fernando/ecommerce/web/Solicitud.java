package com.fernando.ecommerce.web;

import com.fernando.ecommerce.json.Json;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Solicitud ya interpretada: variables de ruta, parametros de consulta y cuerpo. */
public final class Solicitud {

    private final Metodo metodo;
    private final String ruta;
    private final Map<String, String> variables;
    private final Map<String, String> consulta;
    private final String cuerpo;

    public Solicitud(Metodo metodo, String ruta, Map<String, String> variables,
                      Map<String, String> consulta, String cuerpo) {
        this.metodo = metodo;
        this.ruta = ruta;
        this.variables = variables;
        this.consulta = consulta;
        this.cuerpo = cuerpo;
    }

    public Metodo metodo() {
        return metodo;
    }

    public String ruta() {
        return ruta;
    }

    public Map<String, String> variables() {
        return variables;
    }

    public Map<String, String> consulta() {
        return consulta;
    }

    public String cuerpo() {
        return cuerpo;
    }

    public String variable(String nombre) {
        String valor = variables.get(nombre);
        if (valor == null) {
            throw new ApiException(500, "La ruta no declara la variable '" + nombre + "'");
        }
        return valor;
    }

    public Optional<String> parametro(String nombre) {
        return Optional.ofNullable(consulta.get(nombre));
    }

    public int parametroEntero(String nombre, int porDefecto) {
        String valor = consulta.get(nombre);
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "El parametro '" + nombre + "' debe ser un numero entero");
        }
    }

    public Map<String, Object> cuerpoJson() {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new ApiException(400, "Se esperaba un cuerpo JSON");
        }
        try {
            return Json.parsearObjeto(cuerpo);
        } catch (Json.JsonException e) {
            throw new ApiException(400, "JSON invalido: " + e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Solicitud)) {
            return false;
        }
        Solicitud that = (Solicitud) o;
        return metodo == that.metodo
                && Objects.equals(ruta, that.ruta)
                && Objects.equals(variables, that.variables)
                && Objects.equals(consulta, that.consulta)
                && Objects.equals(cuerpo, that.cuerpo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metodo, ruta, variables, consulta, cuerpo);
    }

    @Override
    public String toString() {
        return "Solicitud[metodo=" + metodo + ", ruta=" + ruta + ", variables=" + variables
                + ", consulta=" + consulta + ", cuerpo=" + cuerpo + "]";
    }
}
