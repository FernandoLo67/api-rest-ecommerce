package com.fernando.ecommerce.repositorio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementacion en memoria y segura para concurrencia: el servidor HTTP
 * atiende varias peticiones a la vez sobre el mismo repositorio.
 */
public class RepositorioEnMemoria<T> implements Repositorio<T> {

    private final Map<String, T> datos = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong(1);
    private final String prefijo;

    public RepositorioEnMemoria(String prefijo) {
        this.prefijo = prefijo;
    }

    public String siguienteId() {
        return prefijo + "-" + secuencia.getAndIncrement();
    }

    @Override
    public void guardar(String id, T entidad) {
        datos.put(id, entidad);
    }

    @Override
    public Optional<T> buscar(String id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public List<T> listar() {
        return List.copyOf(datos.values());
    }

    @Override
    public boolean eliminar(String id) {
        return datos.remove(id) != null;
    }

    @Override
    public int cantidad() {
        return datos.size();
    }

    public boolean existe(String id) {
        return datos.containsKey(id);
    }
}
