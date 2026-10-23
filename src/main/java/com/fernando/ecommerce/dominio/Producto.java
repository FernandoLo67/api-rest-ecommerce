package com.fernando.ecommerce.dominio;

import com.fernando.ecommerce.json.Json;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class Producto implements Json.JsonSerializable {

    private final String sku;
    private String nombre;
    private String categoria;
    private BigDecimal precio;
    private int stock;

    public Producto(String sku, String nombre, String categoria, BigDecimal precio, int stock) {
        this.sku = exigirTexto(sku, "sku");
        this.nombre = exigirTexto(nombre, "nombre");
        this.categoria = exigirTexto(categoria, "categoria");
        establecerPrecio(precio);
        establecerStock(stock);
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio");
        }
        return valor.trim();
    }

    public final void establecerPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a 0");
        }
        this.precio = precio;
    }

    public final void establecerStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        this.stock = stock;
    }

    public void renombrar(String nombre) {
        this.nombre = exigirTexto(nombre, "nombre");
    }

    public void recategorizar(String categoria) {
        this.categoria = exigirTexto(categoria, "categoria");
    }

    /** Reserva unidades para un pedido. Devuelve false si no alcanza el stock. */
    public synchronized boolean reservar(int unidades) {
        if (unidades <= 0) {
            throw new IllegalArgumentException("Las unidades deben ser mayores a 0");
        }
        if (unidades > stock) {
            return false;
        }
        stock -= unidades;
        return true;
    }

    public synchronized void liberar(int unidades) {
        if (unidades <= 0) {
            throw new IllegalArgumentException("Las unidades deben ser mayores a 0");
        }
        stock += unidades;
    }

    public String getSku() { return sku; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPrecio() { return precio; }
    public synchronized int getStock() { return stock; }

    @Override
    public Map<String, Object> aJson() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("sku", sku);
        mapa.put("nombre", nombre);
        mapa.put("categoria", categoria);
        mapa.put("precio", precio);
        mapa.put("stock", getStock());
        return mapa;
    }
}
