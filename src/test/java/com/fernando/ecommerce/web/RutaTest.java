package com.fernando.ecommerce.web;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RutaTest {

    @Test
    void unaRutaSinVariablesEmparejaExacto() {
        Ruta ruta = new Ruta(Metodo.GET, "/api/productos");

        assertTrue(ruta.emparejar(Metodo.GET, "/api/productos").isPresent());
        assertTrue(ruta.emparejar(Metodo.GET, "/api/pedidos").isEmpty());
    }

    @Test
    void extraeUnaVariableDeLaRuta() {
        Ruta ruta = new Ruta(Metodo.GET, "/api/productos/{sku}");

        Optional<Map<String, String>> variables = ruta.emparejar(Metodo.GET, "/api/productos/TEC-01");

        assertTrue(variables.isPresent());
        assertEquals("TEC-01", variables.get().get("sku"));
    }

    @Test
    void extraeVariasVariables() {
        Ruta ruta = new Ruta(Metodo.GET, "/api/clientes/{cliente}/pedidos/{id}");

        Map<String, String> variables = ruta.emparejar(Metodo.GET, "/api/clientes/ana/pedidos/PED-9").orElseThrow();

        assertEquals("ana", variables.get("cliente"));
        assertEquals("PED-9", variables.get("id"));
        assertIterableEquals(java.util.List.of("cliente", "id"), ruta.getNombresDeVariables());
    }

    @Test
    void unaVariableNoAtraviesaLasBarras() {
        Ruta ruta = new Ruta(Metodo.GET, "/api/productos/{sku}");
        assertTrue(ruta.emparejar(Metodo.GET, "/api/productos/TEC-01/extra").isEmpty());
    }

    @Test
    void elMetodoTieneQueCoincidir() {
        Ruta ruta = new Ruta(Metodo.GET, "/api/productos");

        assertTrue(ruta.emparejar(Metodo.POST, "/api/productos").isEmpty());
        assertTrue(ruta.coincideLaRuta("/api/productos"), "La ruta coincide aunque el metodo no");
    }

    @Test
    void laBarraFinalSeIgnora() {
        Ruta ruta = new Ruta(Metodo.GET, "/api/productos");
        assertTrue(ruta.emparejar(Metodo.GET, "/api/productos/").isPresent());
    }

    @Test
    void losCaracteresEspecialesDeLaPlantillaSeTratanComoTexto() {
        Ruta ruta = new Ruta(Metodo.GET, "/api/v1.0/productos");

        assertTrue(ruta.emparejar(Metodo.GET, "/api/v1.0/productos").isPresent());
        assertTrue(ruta.emparejar(Metodo.GET, "/api/v1X0/productos").isEmpty(),
                "El punto no debe funcionar como comodin");
    }

    @Test
    void laRepresentacionEnTextoMuestraMetodoYPlantilla() {
        assertEquals("GET /api/productos/{sku}", new Ruta(Metodo.GET, "/api/productos/{sku}").toString());
    }
}
