package com.fernando.ecommerce.api;

import com.fernando.ecommerce.dominio.Producto;
import com.fernando.ecommerce.json.Json;
import com.fernando.ecommerce.servicio.ServicioCatalogo;
import com.fernando.ecommerce.web.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ControladorProductos {

    private final ServicioCatalogo catalogo;

    public ControladorProductos(ServicioCatalogo catalogo) {
        this.catalogo = catalogo;
    }

    public void registrarEn(Router router) {
        router.get("/api/productos", this::listar)
              .get("/api/productos/{sku}", this::obtener)
              .post("/api/productos", this::crear)
              .put("/api/productos/{sku}", this::actualizar)
              .delete("/api/productos/{sku}", this::eliminar);
    }

    private Respuesta listar(Solicitud solicitud) {
        List<Producto> productos = catalogo.listar(
                solicitud.parametro("categoria").orElse(null),
                solicitud.parametro("buscar").orElse(null));
        Pagina<Producto> pagina = Pagina.de(productos,
                solicitud.parametroEntero("pagina", 0),
                solicitud.parametroEntero("tamano", 20));
        return Respuesta.ok(pagina.aJson());
    }

    private Respuesta obtener(Solicitud solicitud) {
        return Respuesta.ok(catalogo.obtener(solicitud.variable("sku")));
    }

    private Respuesta crear(Solicitud solicitud) {
        Map<String, Object> cuerpo = solicitud.cuerpoJson();
        Producto producto = catalogo.crear(
                Json.textoDe(cuerpo, "sku"),
                Json.textoDe(cuerpo, "nombre"),
                Json.textoDe(cuerpo, "categoria"),
                Json.decimalDe(cuerpo, "precio"),
                Json.enteroDe(cuerpo, "stock"));
        return Respuesta.creado(producto, "/api/productos/" + producto.getSku());
    }

    private Respuesta actualizar(Solicitud solicitud) {
        Map<String, Object> cuerpo = solicitud.cuerpoJson();
        Producto producto = catalogo.actualizar(
                solicitud.variable("sku"),
                cuerpo.containsKey("nombre") ? Json.textoDe(cuerpo, "nombre") : null,
                cuerpo.containsKey("categoria") ? Json.textoDe(cuerpo, "categoria") : null,
                cuerpo.containsKey("precio") ? Json.decimalDe(cuerpo, "precio") : (BigDecimal) null,
                cuerpo.containsKey("stock") ? Json.enteroDe(cuerpo, "stock") : null);
        return Respuesta.ok(producto);
    }

    private Respuesta eliminar(Solicitud solicitud) {
        catalogo.eliminar(solicitud.variable("sku"));
        return Respuesta.sinContenido();
    }
}
