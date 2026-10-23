package com.fernando.ecommerce.dominio;

import com.fernando.ecommerce.json.Json;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Pedido implements Json.JsonSerializable {

    public static final BigDecimal IVA = new BigDecimal("0.12");

    private final String id;
    private final String cliente;
    private final List<LineaPedido> lineas;
    private final Instant creado;
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    public Pedido(String id, String cliente, List<LineaPedido> lineas, Instant creado) {
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("Un pedido necesita al menos una linea");
        }
        if (cliente == null || cliente.isBlank()) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }
        this.id = id;
        this.cliente = cliente.trim();
        this.lineas = List.copyOf(lineas);
        this.creado = creado;
    }

    public void cambiarEstado(EstadoPedido destino) {
        if (!estado.puedeTransicionarA(destino)) {
            throw new IllegalStateException("No se puede pasar de " + estado + " a " + destino);
        }
        this.estado = destino;
    }

    public BigDecimal subtotal() {
        return lineas.stream()
                .map(LineaPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal iva() {
        return subtotal().multiply(IVA).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal total() {
        return subtotal().add(iva());
    }

    public String getId() { return id; }
    public String getCliente() { return cliente; }
    public List<LineaPedido> getLineas() { return lineas; }
    public EstadoPedido getEstado() { return estado; }
    public Instant getCreado() { return creado; }

    @Override
    public Map<String, Object> aJson() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", id);
        mapa.put("cliente", cliente);
        mapa.put("estado", estado.name());
        mapa.put("creado", creado.toString());
        mapa.put("lineas", lineas);
        mapa.put("subtotal", subtotal());
        mapa.put("iva", iva());
        mapa.put("total", total());
        return mapa;
    }
}
