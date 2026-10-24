package com.fernando.ecommerce.api;

import com.fernando.ecommerce.dominio.Pedido;
import com.fernando.ecommerce.dominio.Producto;
import com.fernando.ecommerce.json.Json;
import com.fernando.ecommerce.repositorio.RepositorioEnMemoria;
import com.fernando.ecommerce.servicio.ServicioCatalogo;
import com.fernando.ecommerce.servicio.ServicioPedidos;
import com.fernando.ecommerce.web.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Adaptador entre el servidor HTTP del JDK y el Router.
 *
 * El Router no sabe nada de HttpExchange: recibe metodo, ruta, parametros y
 * cuerpo, y devuelve una Respuesta. Eso permite probar el enrutado y los
 * controladores sin levantar un servidor.
 */
public class ServidorApi implements AutoCloseable {

    private final HttpServer servidor;
    private final ExecutorService pool;
    private final Router router;
    private final ServicioCatalogo catalogo;
    private final ServicioPedidos pedidos;

    public ServidorApi(int puerto) throws IOException {
        this(puerto, Instant::now);
    }

    public ServidorApi(int puerto, Supplier<Instant> reloj) throws IOException {
        RepositorioEnMemoria<Producto> repoProductos = new RepositorioEnMemoria<>("SKU");
        RepositorioEnMemoria<Pedido> repoPedidos = new RepositorioEnMemoria<>("PED");

        this.catalogo = new ServicioCatalogo(repoProductos);
        this.pedidos = new ServicioPedidos(repoPedidos, catalogo, reloj);

        this.router = new Router();
        new ControladorProductos(catalogo).registrarEn(router);
        new ControladorPedidos(pedidos).registrarEn(router);
        router.get("/api/salud", s -> Respuesta.ok(Map.of("estado", "ok")));

        this.servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        this.pool = Executors.newFixedThreadPool(8, tarea -> {
            Thread hilo = new Thread(tarea, "api-http");
            hilo.setDaemon(true);
            return hilo;
        });
        this.servidor.setExecutor(pool);
        this.servidor.createContext("/", this::atender);
    }

    public ServidorApi conDatosDeEjemplo() {
        catalogo.crear("TEC-01", "Teclado mecanico", "perifericos", new BigDecimal("450.00"), 10);
        catalogo.crear("MOU-01", "Mouse inalambrico", "perifericos", new BigDecimal("180.00"), 25);
        catalogo.crear("MON-01", "Monitor 24 pulgadas", "pantallas", new BigDecimal("1200.00"), 5);
        return this;
    }

    public void iniciar() {
        servidor.start();
    }

    public int getPuerto() {
        return servidor.getAddress().getPort();
    }

    public Router getRouter() {
        return router;
    }

    public ServicioCatalogo getCatalogo() {
        return catalogo;
    }

    public ServicioPedidos getPedidos() {
        return pedidos;
    }

    private void atender(HttpExchange intercambio) throws IOException {
        agregarCabecerosCors(intercambio);

        if ("OPTIONS".equalsIgnoreCase(intercambio.getRequestMethod())) {
            intercambio.sendResponseHeaders(204, -1);
            intercambio.close();
            return;
        }

        try {
            Metodo metodo;
            try {
                metodo = Metodo.valueOf(intercambio.getRequestMethod().toUpperCase());
            } catch (IllegalArgumentException e) {
                responder(intercambio, Respuesta.error(405, "Metodo no soportado: "
                        + intercambio.getRequestMethod()));
                return;
            }

            String ruta = intercambio.getRequestURI().getPath();
            Map<String, String> consulta = interpretarConsulta(intercambio.getRequestURI().getRawQuery());
            String cuerpo;
            try (InputStream entrada = intercambio.getRequestBody()) {
                cuerpo = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            }

            responder(intercambio, router.despachar(metodo, ruta, consulta, cuerpo));
        } catch (RuntimeException e) {
            responder(intercambio, Respuesta.error(500, "Error interno: " + e.getMessage()));
        }
    }

    private void agregarCabecerosCors(HttpExchange intercambio) {
        intercambio.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        intercambio.getResponseHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        intercambio.getResponseHeaders().add("Access-Control-Allow-Headers",
                "Content-Type, Authorization");
    }

    static Map<String, String> interpretarConsulta(String crudo) {
        Map<String, String> parametros = new LinkedHashMap<>();
        if (crudo == null || crudo.isBlank()) {
            return parametros;
        }
        for (String par : crudo.split("&")) {
            if (par.isBlank()) {
                continue;
            }
            int igual = par.indexOf('=');
            if (igual < 0) {
                parametros.put(decodificar(par), "");
            } else {
                parametros.put(decodificar(par.substring(0, igual)), decodificar(par.substring(igual + 1)));
            }
        }
        return parametros;
    }

    private static String decodificar(String texto) {
        return URLDecoder.decode(texto, StandardCharsets.UTF_8);
    }

    private void responder(HttpExchange intercambio, Respuesta respuesta) throws IOException {
        respuesta.cabeceras().forEach((clave, valor) ->
                intercambio.getResponseHeaders().add(clave, valor));
        intercambio.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");

        if (respuesta.cuerpo() == null) {
            intercambio.sendResponseHeaders(respuesta.codigo(), -1);
            intercambio.close();
            return;
        }
        byte[] datos = Json.escribir(respuesta.cuerpo()).getBytes(StandardCharsets.UTF_8);
        intercambio.sendResponseHeaders(respuesta.codigo(), datos.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(datos);
        }
    }

    @Override
    public void close() {
        servidor.stop(0);
        pool.shutdownNow();
    }
}
