package com.fernando.ecommerce.servicio;

import com.fernando.ecommerce.dominio.*;
import com.fernando.ecommerce.repositorio.RepositorioEnMemoria;
import com.fernando.ecommerce.web.ApiException;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public class ServicioPedidos {

    private final RepositorioEnMemoria<Pedido> pedidos;
    private final ServicioCatalogo catalogo;
    private final Supplier<Instant> reloj;

    public ServicioPedidos(RepositorioEnMemoria<Pedido> pedidos, ServicioCatalogo catalogo, Supplier<Instant> reloj) {
        this.pedidos = pedidos;
        this.catalogo = catalogo;
        this.reloj = reloj;
    }

    /**
     * Crea el pedido reservando stock. Si una linea falla, se devuelve el stock
     * ya reservado de las anteriores: o se reserva todo, o no se reserva nada.
     */
    public Pedido crear(String cliente, Map<String, Integer> articulos) {
        if (articulos == null || articulos.isEmpty()) {
            throw ApiException.solicitudInvalida("El pedido debe tener al menos un articulo");
        }

        List<LineaPedido> lineas = new ArrayList<>();
        List<Producto> reservados = new ArrayList<>();
        List<Integer> cantidadesReservadas = new ArrayList<>();

        try {
            for (Map.Entry<String, Integer> entrada : new TreeMap<>(articulos).entrySet()) {
                String sku = entrada.getKey();
                int cantidad = entrada.getValue();
                if (cantidad <= 0) {
                    throw ApiException.solicitudInvalida("La cantidad de " + sku + " debe ser mayor a 0");
                }
                Producto producto = catalogo.obtener(sku);
                if (!producto.reservar(cantidad)) {
                    throw ApiException.conflicto(
                            "Stock insuficiente de " + sku + ": quedan " + producto.getStock());
                }
                reservados.add(producto);
                cantidadesReservadas.add(cantidad);
                lineas.add(new LineaPedido(sku, producto.getNombre(), producto.getPrecio(), cantidad));
            }
        } catch (RuntimeException e) {
            for (int i = 0; i < reservados.size(); i++) {
                reservados.get(i).liberar(cantidadesReservadas.get(i));
            }
            throw e;
        }

        String id = pedidos.siguienteId();
        Pedido pedido = new Pedido(id, cliente, lineas, reloj.get());
        pedidos.guardar(id, pedido);
        return pedido;
    }

    public Pedido obtener(String id) {
        return pedidos.buscar(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el pedido " + id));
    }

    public Pedido cambiarEstado(String id, String nuevoEstado) {
        Pedido pedido = obtener(id);
        EstadoPedido destino;
        try {
            destino = EstadoPedido.valueOf(nuevoEstado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.solicitudInvalida("Estado desconocido: " + nuevoEstado);
        }
        EstadoPedido anterior = pedido.getEstado();
        try {
            pedido.cambiarEstado(destino);
        } catch (IllegalStateException e) {
            throw ApiException.conflicto(e.getMessage());
        }
        if (destino == EstadoPedido.CANCELADO && anterior != EstadoPedido.CANCELADO) {
            for (LineaPedido linea : pedido.getLineas()) {
                catalogo.obtener(linea.sku()).liberar(linea.cantidad());
            }
        }
        return pedido;
    }

    public List<Pedido> listar(String cliente) {
        return pedidos.listar().stream()
                .filter(p -> cliente == null || p.getCliente().equalsIgnoreCase(cliente))
                .sorted(Comparator.comparing(Pedido::getId))
                .collect(java.util.stream.Collectors.toUnmodifiableList());
    }
}
