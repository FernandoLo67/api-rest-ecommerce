package com.fernando.ecommerce.servicio;

import com.fernando.ecommerce.dominio.Producto;
import com.fernando.ecommerce.repositorio.RepositorioEnMemoria;
import com.fernando.ecommerce.web.ApiException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class ServicioCatalogo {

    private final RepositorioEnMemoria<Producto> repositorio;

    public ServicioCatalogo(RepositorioEnMemoria<Producto> repositorio) {
        this.repositorio = repositorio;
    }

    public Producto crear(String sku, String nombre, String categoria, BigDecimal precio, int stock) {
        if (repositorio.existe(sku)) {
            throw ApiException.conflicto("Ya existe un producto con el SKU " + sku);
        }
        Producto producto = new Producto(sku, nombre, categoria, precio, stock);
        repositorio.guardar(sku, producto);
        return producto;
    }

    public Producto obtener(String sku) {
        return repositorio.buscar(sku)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el producto " + sku));
    }

    public Producto actualizar(String sku, String nombre, String categoria, BigDecimal precio, Integer stock) {
        Producto producto = obtener(sku);
        if (nombre != null) {
            producto.renombrar(nombre);
        }
        if (categoria != null) {
            producto.recategorizar(categoria);
        }
        if (precio != null) {
            producto.establecerPrecio(precio);
        }
        if (stock != null) {
            producto.establecerStock(stock);
        }
        return producto;
    }

    public void eliminar(String sku) {
        if (!repositorio.eliminar(sku)) {
            throw ApiException.noEncontrado("No existe el producto " + sku);
        }
    }

    /** Listado ordenado por SKU, opcionalmente filtrado por categoria y texto. */
    public List<Producto> listar(String categoria, String busqueda) {
        return repositorio.listar().stream()
                .filter(p -> categoria == null || p.getCategoria().equalsIgnoreCase(categoria))
                .filter(p -> busqueda == null
                        || p.getNombre().toLowerCase().contains(busqueda.toLowerCase()))
                .sorted(Comparator.comparing(Producto::getSku))
                .collect(java.util.stream.Collectors.toUnmodifiableList());
    }
}
