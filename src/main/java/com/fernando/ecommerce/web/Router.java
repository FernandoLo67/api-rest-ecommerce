package com.fernando.ecommerce.web;

import com.fernando.ecommerce.json.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Tabla de rutas. Resuelve una solicitud al manejador correspondiente y
 * distingue 404 (la ruta no existe) de 405 (existe, pero no para ese metodo).
 */
public class Router {

    public static final class Entrada {
        private final Ruta ruta;
        private final Function<Solicitud, Respuesta> manejador;

        public Entrada(Ruta ruta, Function<Solicitud, Respuesta> manejador) {
            this.ruta = ruta;
            this.manejador = manejador;
        }

        public Ruta ruta() {
            return ruta;
        }

        public Function<Solicitud, Respuesta> manejador() {
            return manejador;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Entrada)) {
                return false;
            }
            Entrada that = (Entrada) o;
            return java.util.Objects.equals(ruta, that.ruta) && java.util.Objects.equals(manejador, that.manejador);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(ruta, manejador);
        }

        @Override
        public String toString() {
            return "Entrada[ruta=" + ruta + ", manejador=" + manejador + "]";
        }
    }

    private final List<Entrada> entradas = new ArrayList<>();

    public Router registrar(Metodo metodo, String plantilla, Function<Solicitud, Respuesta> manejador) {
        entradas.add(new Entrada(new Ruta(metodo, plantilla), manejador));
        return this;
    }

    public Router get(String plantilla, Function<Solicitud, Respuesta> manejador) {
        return registrar(Metodo.GET, plantilla, manejador);
    }

    public Router post(String plantilla, Function<Solicitud, Respuesta> manejador) {
        return registrar(Metodo.POST, plantilla, manejador);
    }

    public Router put(String plantilla, Function<Solicitud, Respuesta> manejador) {
        return registrar(Metodo.PUT, plantilla, manejador);
    }

    public Router delete(String plantilla, Function<Solicitud, Respuesta> manejador) {
        return registrar(Metodo.DELETE, plantilla, manejador);
    }

    public Respuesta despachar(Metodo metodo, String ruta, Map<String, String> consulta, String cuerpo) {
        for (Entrada entrada : entradas) {
            Optional<Map<String, String>> variables = entrada.ruta().emparejar(metodo, ruta);
            if (variables.isPresent()) {
                try {
                    return entrada.manejador().apply(
                            new Solicitud(metodo, ruta, variables.get(), consulta, cuerpo));
                } catch (ApiException e) {
                    return Respuesta.error(e.getCodigo(), e.getMessage());
                } catch (Json.JsonException e) {
                    // Un cuerpo mal formado o con campos faltantes es culpa del
                    // cliente, no del servidor: 400, nunca 500.
                    return Respuesta.error(400, e.getMessage());
                } catch (IllegalArgumentException | ArithmeticException e) {
                    return Respuesta.error(400, e.getMessage());
                } catch (RuntimeException e) {
                    return Respuesta.error(500, "Error interno: " + e.getMessage());
                }
            }
        }
        boolean rutaExiste = entradas.stream().anyMatch(e -> e.ruta().coincideLaRuta(ruta));
        if (rutaExiste) {
            return Respuesta.error(405, "El metodo " + metodo + " no esta permitido en " + ruta);
        }
        return Respuesta.error(404, "No existe la ruta " + ruta);
    }

    public List<String> rutasRegistradas() {
        return entradas.stream().map(e -> e.ruta().toString())
                .collect(java.util.stream.Collectors.toUnmodifiableList());
    }
}
