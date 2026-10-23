package com.fernando.ecommerce.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Resultado paginado, listo para serializar a JSON. */
public final class Pagina<T> {

    private final List<T> contenido;
    private final int pagina;
    private final int tamano;
    private final int totalElementos;

    public Pagina(List<T> contenido, int pagina, int tamano, int totalElementos) {
        this.contenido = contenido;
        this.pagina = pagina;
        this.tamano = tamano;
        this.totalElementos = totalElementos;
    }

    public List<T> contenido() {
        return contenido;
    }

    public int pagina() {
        return pagina;
    }

    public int tamano() {
        return tamano;
    }

    public int totalElementos() {
        return totalElementos;
    }

    public static <T> Pagina<T> de(List<T> todos, int pagina, int tamano) {
        if (pagina < 0) {
            throw ApiException.solicitudInvalida("La pagina no puede ser negativa");
        }
        if (tamano <= 0 || tamano > 100) {
            throw ApiException.solicitudInvalida("El tamano debe estar entre 1 y 100");
        }
        int desde = Math.min(pagina * tamano, todos.size());
        int hasta = Math.min(desde + tamano, todos.size());
        return new Pagina<>(List.copyOf(todos.subList(desde, hasta)), pagina, tamano, todos.size());
    }

    public int totalPaginas() {
        return totalElementos == 0 ? 0 : (int) Math.ceil((double) totalElementos / tamano);
    }

    public boolean haySiguiente() {
        return pagina + 1 < totalPaginas();
    }

    public Map<String, Object> aJson() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("contenido", contenido);
        mapa.put("pagina", pagina);
        mapa.put("tamano", tamano);
        mapa.put("totalElementos", totalElementos);
        mapa.put("totalPaginas", totalPaginas());
        mapa.put("haySiguiente", haySiguiente());
        return mapa;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pagina)) {
            return false;
        }
        Pagina<?> that = (Pagina<?>) o;
        return pagina == that.pagina
                && tamano == that.tamano
                && totalElementos == that.totalElementos
                && Objects.equals(contenido, that.contenido);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contenido, pagina, tamano, totalElementos);
    }

    @Override
    public String toString() {
        return "Pagina[contenido=" + contenido + ", pagina=" + pagina + ", tamano=" + tamano
                + ", totalElementos=" + totalElementos + "]";
    }
}
