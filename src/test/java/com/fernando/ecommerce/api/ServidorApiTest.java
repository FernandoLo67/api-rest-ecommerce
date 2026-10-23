package com.fernando.ecommerce.api;

import com.fernando.ecommerce.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integracion reales: levantan el servidor en un puerto libre
 * y le hacen peticiones HTTP de verdad sobre loopback.
 */
class ServidorApiTest {

    private ServidorApi servidor;
    private HttpClient cliente;
    private String base;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = new ServidorApi(0, () -> Instant.parse("2026-08-24T10:00:00Z")).conDatosDeEjemplo();
        servidor.iniciar();
        cliente = HttpClient.newHttpClient();
        base = "http://127.0.0.1:" + servidor.getPuerto();
    }

    @AfterEach
    void apagarServidor() {
        servidor.close();
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String cuerpo) throws Exception {
        HttpRequest.BodyPublisher publicador = cuerpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(cuerpo);
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(base + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, publicador)
                .build();
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void elEndpointDeSaludResponde200() throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/salud", null);

        assertEquals(200, respuesta.statusCode());
        assertEquals("ok", Json.buscar(Json.parsear(respuesta.body()), "estado"));
        assertTrue(respuesta.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }

    @Test
    void listarProductosDevuelveLosDeEjemploPaginados() throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/productos", null);
        Object json = Json.parsear(respuesta.body());

        assertEquals(200, respuesta.statusCode());
        assertEquals(new BigDecimal("3"), Json.buscar(json, "totalElementos"));
        assertEquals("MON-01", Json.buscar(json, "contenido.0.sku"), "Deben venir ordenados por SKU");
    }

    @Test
    void laPaginacionRespetaPaginaYTamano() throws Exception {
        Object json = Json.parsear(pedir("GET", "/api/productos?pagina=1&tamano=2", null).body());

        assertEquals(new BigDecimal("1"), Json.buscar(json, "pagina"));
        assertEquals(new BigDecimal("2"), Json.buscar(json, "totalPaginas"));
        assertEquals(false, Json.buscar(json, "haySiguiente"));
    }

    @Test
    void sePuedeFiltrarPorCategoriaYPorTexto() throws Exception {
        Object porCategoria = Json.parsear(pedir("GET", "/api/productos?categoria=pantallas", null).body());
        assertEquals(new BigDecimal("1"), Json.buscar(porCategoria, "totalElementos"));

        Object porTexto = Json.parsear(pedir("GET", "/api/productos?buscar=mouse", null).body());
        assertEquals("MOU-01", Json.buscar(porTexto, "contenido.0.sku"));
    }

    @Test
    void obtenerUnProductoInexistenteDevuelve404() throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/productos/NO-EXISTE", null);

        assertEquals(404, respuesta.statusCode());
        assertEquals(new BigDecimal("404"), Json.buscar(Json.parsear(respuesta.body()), "codigo"));
    }

    @Test
    void crearUnProductoDevuelve201ConCabeceraLocation() throws Exception {
        HttpResponse<String> respuesta = pedir("POST", "/api/productos",
                "{\"sku\":\"AUR-01\",\"nombre\":\"Audifonos\",\"categoria\":\"audio\",\"precio\":320.00,\"stock\":7}");

        assertEquals(201, respuesta.statusCode());
        assertEquals("/api/productos/AUR-01", respuesta.headers().firstValue("Location").orElseThrow());
        assertEquals("Audifonos", Json.buscar(Json.parsear(respuesta.body()), "nombre"));
    }

    @Test
    void crearUnSkuRepetidoDevuelve409() throws Exception {
        String cuerpo = "{\"sku\":\"TEC-01\",\"nombre\":\"Otro\",\"categoria\":\"x\",\"precio\":1,\"stock\":1}";
        assertEquals(409, pedir("POST", "/api/productos", cuerpo).statusCode());
    }

    @Test
    void crearSinCamposObligatoriosDevuelve400() throws Exception {
        assertEquals(400, pedir("POST", "/api/productos", "{\"sku\":\"X\"}").statusCode());
    }

    @Test
    void unCuerpoJsonInvalidoDevuelve400() throws Exception {
        assertEquals(400, pedir("POST", "/api/productos", "{esto no es json").statusCode());
    }

    @Test
    void actualizarCambiaSoloLosCamposEnviados() throws Exception {
        HttpResponse<String> respuesta = pedir("PUT", "/api/productos/TEC-01", "{\"precio\":499.99}");
        Object json = Json.parsear(respuesta.body());

        assertEquals(200, respuesta.statusCode());
        assertEquals("Teclado mecanico", Json.buscar(json, "nombre"), "El nombre no se toco");
        assertEquals(new BigDecimal("499.99"), Json.buscar(json, "precio"));
    }

    @Test
    void eliminarDevuelve204YLuegoElProductoYaNoEsta() throws Exception {
        assertEquals(204, pedir("DELETE", "/api/productos/MOU-01", null).statusCode());
        assertEquals(404, pedir("GET", "/api/productos/MOU-01", null).statusCode());
        assertEquals(404, pedir("DELETE", "/api/productos/MOU-01", null).statusCode());
    }

    @Test
    void crearUnPedidoDescuentaStockYCalculaTotales() throws Exception {
        HttpResponse<String> respuesta = pedir("POST", "/api/pedidos",
                "{\"cliente\":\"daniel\",\"articulos\":[{\"sku\":\"TEC-01\",\"cantidad\":2}]}");
        Object json = Json.parsear(respuesta.body());

        assertEquals(201, respuesta.statusCode());
        assertEquals("PENDIENTE", Json.buscar(json, "estado"));
        assertEquals(new BigDecimal("900.00"), Json.buscar(json, "subtotal"));
        assertEquals(new BigDecimal("108.00"), Json.buscar(json, "iva"));
        assertEquals(new BigDecimal("1008.00"), Json.buscar(json, "total"));

        Object producto = Json.parsear(pedir("GET", "/api/productos/TEC-01", null).body());
        assertEquals(new BigDecimal("8"), Json.buscar(producto, "stock"));
    }

    @Test
    void unPedidoSinStockDevuelve409YNoTocaElInventario() throws Exception {
        HttpResponse<String> respuesta = pedir("POST", "/api/pedidos",
                "{\"cliente\":\"daniel\",\"articulos\":["
                        + "{\"sku\":\"TEC-01\",\"cantidad\":2},"
                        + "{\"sku\":\"MON-01\",\"cantidad\":99}]}");

        assertEquals(409, respuesta.statusCode());

        Object teclado = Json.parsear(pedir("GET", "/api/productos/TEC-01", null).body());
        assertEquals(new BigDecimal("10"), Json.buscar(teclado, "stock"),
                "El stock del teclado debe quedar intacto si el pedido completo falla");
    }

    @Test
    void cancelarUnPedidoDevuelveElStock() throws Exception {
        String id = (String) Json.buscar(Json.parsear(pedir("POST", "/api/pedidos",
                "{\"cliente\":\"ana\",\"articulos\":[{\"sku\":\"MON-01\",\"cantidad\":3}]}").body()), "id");

        HttpResponse<String> cancelacion = pedir("PATCH", "/api/pedidos/" + id + "/estado",
                "{\"estado\":\"CANCELADO\"}");

        assertEquals(200, cancelacion.statusCode());
        assertEquals("CANCELADO", Json.buscar(Json.parsear(cancelacion.body()), "estado"));

        Object monitor = Json.parsear(pedir("GET", "/api/productos/MON-01", null).body());
        assertEquals(new BigDecimal("5"), Json.buscar(monitor, "stock"));
    }

    @Test
    void unaTransicionDeEstadoInvalidaDevuelve409() throws Exception {
        String id = (String) Json.buscar(Json.parsear(pedir("POST", "/api/pedidos",
                "{\"cliente\":\"ana\",\"articulos\":[{\"sku\":\"MOU-01\",\"cantidad\":1}]}").body()), "id");

        // PENDIENTE no puede saltar directo a ENTREGADO
        assertEquals(409, pedir("PATCH", "/api/pedidos/" + id + "/estado",
                "{\"estado\":\"ENTREGADO\"}").statusCode());
    }

    @Test
    void unEstadoDesconocidoDevuelve400() throws Exception {
        String id = (String) Json.buscar(Json.parsear(pedir("POST", "/api/pedidos",
                "{\"cliente\":\"ana\",\"articulos\":[{\"sku\":\"MOU-01\",\"cantidad\":1}]}").body()), "id");

        assertEquals(400, pedir("PATCH", "/api/pedidos/" + id + "/estado",
                "{\"estado\":\"BAILANDO\"}").statusCode());
    }

    @Test
    void sePuedenFiltrarLosPedidosPorCliente() throws Exception {
        pedir("POST", "/api/pedidos", "{\"cliente\":\"daniel\",\"articulos\":[{\"sku\":\"MOU-01\",\"cantidad\":1}]}");
        pedir("POST", "/api/pedidos", "{\"cliente\":\"ana\",\"articulos\":[{\"sku\":\"MOU-01\",\"cantidad\":1}]}");

        Object json = Json.parsear(pedir("GET", "/api/pedidos?cliente=DANIEL", null).body());
        assertEquals(new BigDecimal("1"), Json.buscar(json, "totalElementos"),
                "El filtro por cliente debe ignorar mayusculas");
    }

    @Test
    void unaRutaInexistenteDevuelve404YUnMetodoNoPermitido405() throws Exception {
        assertEquals(404, pedir("GET", "/api/inventado", null).statusCode());
        assertEquals(405, pedir("DELETE", "/api/salud", null).statusCode());
    }

    @Test
    void losParametrosDeConsultaSeDecodifican() {
        Map<String, String> parametros = ServidorApi.interpretarConsulta("buscar=teclado%20mecanico&pagina=2");

        assertEquals("teclado mecanico", parametros.get("buscar"));
        assertEquals("2", parametros.get("pagina"));
        assertTrue(ServidorApi.interpretarConsulta(null).isEmpty());
        assertTrue(ServidorApi.interpretarConsulta("").isEmpty());
    }
}
