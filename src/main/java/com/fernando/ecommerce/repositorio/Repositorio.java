package com.fernando.ecommerce.repositorio;

import java.util.List;
import java.util.Optional;

/** Contrato generico de persistencia. Permite cambiar la implementacion sin tocar los servicios. */
public interface Repositorio<T> {

    void guardar(String id, T entidad);

    Optional<T> buscar(String id);

    List<T> listar();

    boolean eliminar(String id);

    int cantidad();
}
