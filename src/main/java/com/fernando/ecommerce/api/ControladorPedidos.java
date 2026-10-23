package com.fernando.ecommerce.api;

import com.fernando.ecommerce.dominio.Pedido;
import com.fernando.ecommerce.json.Json;
import com.fernando.ecommerce.servicio.ServicioPedidos;
import com.fernando.ecommerce.web.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ControladorPedidos {

    private final ServicioPedidos pedidos;

    public ControladorPedidos(ServicioPedidos pedidos) {
        this.pedidos = pedidos;
    }

    public void registrarEn(Router router) {
        router.get("/api/pedidos", this::listar)
              .get("/api/pedidos/{id}", this::obtener)
              .post("/api/pedidos", this::crear)
              .registrar(Metodo.PATCH, "/api/pedidos/{id}/estado", this::cambiarEstado);
    }

    private Respuesta listar(Solicitud solicitud) {
        List<Pedido> encontrados = pedidos.listar(solicitud.parametro("cliente").orElse(null));
        Pagina<Pedido> pagina = Pagina.de(encontrados,
                solicitud.parametroEntero("pagina", 0),
                solicitud.parametroEntero("tamano", 20));
        return Respuesta.ok(pagina.aJson());
    }

    private Respuesta obtener(Solicitud solicitud) {
        return Respuesta.ok(pedidos.obtener(solicitud.variable("id")));
    }

    @SuppressWarnings("unchecked")
    private Respuesta crear(Solicitud solicitud) {
        Map<String, Object> cuerpo = solicitud.cuerpoJson();
        String cliente = Json.textoDe(cuerpo, "cliente");

        Object crudo = cuerpo.get("articulos");
        if (!(crudo instanceof List<?>) || ((List<?>) crudo).isEmpty()) {
            throw ApiException.solicitudInvalida("Se espera 'articulos' como lista no vacia");
        }
        List<?> lista = (List<?>) crudo;
        Map<String, Integer> articulos = new LinkedHashMap<>();
        for (Object elemento : lista) {
            if (!(elemento instanceof Map)) {
                throw ApiException.solicitudInvalida("Cada articulo debe ser un objeto {sku, cantidad}");
            }
            Map<String, Object> articulo = (Map<String, Object>) elemento;
            articulos.merge(Json.textoDe(articulo, "sku"), Json.enteroDe(articulo, "cantidad"), Integer::sum);
        }
        Pedido pedido = pedidos.crear(cliente, articulos);
        return Respuesta.creado(pedido, "/api/pedidos/" + pedido.getId());
    }

    private Respuesta cambiarEstado(Solicitud solicitud) {
        Map<String, Object> cuerpo = solicitud.cuerpoJson();
        return Respuesta.ok(pedidos.cambiarEstado(
                solicitud.variable("id"), Json.textoDe(cuerpo, "estado")));
    }
}
