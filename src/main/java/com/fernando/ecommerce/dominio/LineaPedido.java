package com.fernando.ecommerce.dominio;

import com.fernando.ecommerce.json.Json;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Linea de pedido con el precio congelado al momento de la compra. */
public final class LineaPedido implements Json.JsonSerializable {

    private final String sku;
    private final String nombre;
    private final BigDecimal precioUnitario;
    private final int cantidad;

    public LineaPedido(String sku, String nombre, BigDecimal precioUnitario, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        this.sku = sku;
        this.nombre = nombre;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
    }

    public String sku() {
        return sku;
    }

    public String nombre() {
        return nombre;
    }

    public BigDecimal precioUnitario() {
        return precioUnitario;
    }

    public int cantidad() {
        return cantidad;
    }

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    @Override
    public Map<String, Object> aJson() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("sku", sku);
        mapa.put("nombre", nombre);
        mapa.put("precioUnitario", precioUnitario);
        mapa.put("cantidad", cantidad);
        mapa.put("subtotal", subtotal());
        return mapa;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LineaPedido)) {
            return false;
        }
        LineaPedido that = (LineaPedido) o;
        return cantidad == that.cantidad
                && Objects.equals(sku, that.sku)
                && Objects.equals(nombre, that.nombre)
                && Objects.equals(precioUnitario, that.precioUnitario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, nombre, precioUnitario, cantidad);
    }

    @Override
    public String toString() {
        return "LineaPedido[sku=" + sku + ", nombre=" + nombre + ", precioUnitario=" + precioUnitario
                + ", cantidad=" + cantidad + "]";
    }
}
