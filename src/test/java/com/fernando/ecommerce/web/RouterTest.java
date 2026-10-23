package com.fernando.ecommerce.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouterTest {

    private Router router;

    @BeforeEach
    void prepararRouter() {
        router = new Router();
        router.get("/api/productos", s -> Respuesta.ok(Map.of("total", 0)))
              .get("/api/productos/{sku}", s -> Respuesta.ok(Map.of("sku", s.variable("sku"))))
              .post("/api/productos", s -> Respuesta.creado(Map.of("creado", true), "/api/productos/X"))
              .delete("/api/productos/{sku}", s -> Respuesta.sinContenido());
    }

    @Test
    void despachaAlManejadorCorrecto() {
        Respuesta respuesta = router.despachar(Metodo.GET, "/api/productos/TEC-01", Map.of(), null);

        assertEquals(200, respuesta.codigo());
        assertEquals(Map.of("sku", "TEC-01"), respuesta.cuerpo());
    }

    @Test
    void unaRutaInexistenteDevuelve404() {
        assertEquals(404, router.despachar(Metodo.GET, "/api/inventado", Map.of(), null).codigo());
    }

    @Test
    void unaRutaQueExisteConOtroMetodoDevuelve405() {
        assertEquals(405, router.despachar(Metodo.PUT, "/api/productos/TEC-01", Map.of(), null).codigo());
    }

    @Test
    void distingueRutasConYSinVariable() {
        assertEquals(200, router.despachar(Metodo.GET, "/api/productos", Map.of(), null).codigo());
        assertEquals(201, router.despachar(Metodo.POST, "/api/productos", Map.of(), null).codigo());
        assertEquals(204, router.despachar(Metodo.DELETE, "/api/productos/X", Map.of(), null).codigo());
    }

    @Test
    void unaApiExceptionSeConvierteEnSuCodigoHttp() {
        router.get("/api/falla", s -> {
            throw ApiException.conflicto("ya existe");
        });

        Respuesta respuesta = router.despachar(Metodo.GET, "/api/falla", Map.of(), null);

        assertEquals(409, respuesta.codigo());
        assertTrue(Respuesta.error(409, "ya existe").cuerpo().toString().contains("ya existe"));
    }

    @Test
    void unIllegalArgumentSeConvierteEn400() {
        router.get("/api/mal", s -> {
            throw new IllegalArgumentException("dato invalido");
        });
        assertEquals(400, router.despachar(Metodo.GET, "/api/mal", Map.of(), null).codigo());
    }

    @Test
    void unErrorInesperadoSeConvierteEn500YNoSePropaga() {
        router.get("/api/explota", s -> {
            throw new IllegalStateException("boom");
        });
        assertEquals(500, router.despachar(Metodo.GET, "/api/explota", Map.of(), null).codigo());
    }

    @Test
    void losParametrosDeConsultaLleganAlManejador() {
        router.get("/api/buscar", s -> Respuesta.ok(Map.of(
                "q", s.parametro("q").orElse("(vacio)"),
                "pagina", s.parametroEntero("pagina", 0))));

        Respuesta respuesta = router.despachar(Metodo.GET, "/api/buscar",
                Map.of("q", "teclado", "pagina", "2"), null);

        assertEquals(Map.of("q", "teclado", "pagina", 2), respuesta.cuerpo());
    }

    @Test
    void unParametroNumericoInvalidoDevuelve400() {
        router.get("/api/buscar", s -> Respuesta.ok(Map.of("p", s.parametroEntero("pagina", 0))));
        assertEquals(400, router.despachar(Metodo.GET, "/api/buscar", Map.of("pagina", "abc"), null).codigo());
    }

    @Test
    void listaLasRutasRegistradas() {
        assertTrue(router.rutasRegistradas().contains("GET /api/productos/{sku}"));
        assertEquals(4, router.rutasRegistradas().size());
    }
}
