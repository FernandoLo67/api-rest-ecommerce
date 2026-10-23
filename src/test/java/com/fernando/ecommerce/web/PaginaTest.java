package com.fernando.ecommerce.web;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PaginaTest {

    private final List<Integer> veinticinco = IntStream.rangeClosed(1, 25).boxed()
            .collect(java.util.stream.Collectors.toUnmodifiableList());

    @Test
    void laPrimeraPaginaDevuelveLosPrimerosElementos() {
        Pagina<Integer> pagina = Pagina.de(veinticinco, 0, 10);

        assertEquals(10, pagina.contenido().size());
        assertEquals(1, pagina.contenido().get(0).intValue());
        assertEquals(25, pagina.totalElementos());
        assertEquals(3, pagina.totalPaginas());
        assertTrue(pagina.haySiguiente());
    }

    @Test
    void laUltimaPaginaPuedeVenirIncompleta() {
        Pagina<Integer> pagina = Pagina.de(veinticinco, 2, 10);

        assertEquals(5, pagina.contenido().size());
        assertEquals(21, pagina.contenido().get(0).intValue());
        assertFalse(pagina.haySiguiente());
    }

    @Test
    void unaPaginaMasAllaDelFinalVieneVacia() {
        Pagina<Integer> pagina = Pagina.de(veinticinco, 99, 10);

        assertTrue(pagina.contenido().isEmpty());
        assertEquals(25, pagina.totalElementos());
        assertFalse(pagina.haySiguiente());
    }

    @Test
    void unaListaVaciaDaCeroPaginas() {
        Pagina<Integer> pagina = Pagina.de(List.of(), 0, 10);

        assertEquals(0, pagina.totalPaginas());
        assertEquals(0, pagina.totalElementos());
        assertFalse(pagina.haySiguiente());
    }

    @Test
    void unaPaginaExactaNoDejaSiguiente() {
        Pagina<Integer> pagina = Pagina.de(veinticinco.subList(0, 20), 1, 10);

        assertEquals(10, pagina.contenido().size());
        assertEquals(2, pagina.totalPaginas());
        assertFalse(pagina.haySiguiente());
    }

    @Test
    void paginaNegativaOTamanoInvalidoSonRechazados() {
        assertThrows(ApiException.class, () -> Pagina.de(veinticinco, -1, 10));
        assertThrows(ApiException.class, () -> Pagina.de(veinticinco, 0, 0));
        assertThrows(ApiException.class, () -> Pagina.de(veinticinco, 0, 101));
    }

    @Test
    void elJsonIncluyeLosMetadatosDePaginacion() {
        var json = Pagina.de(veinticinco, 0, 10).aJson();

        assertEquals(0, json.get("pagina"));
        assertEquals(10, json.get("tamano"));
        assertEquals(25, json.get("totalElementos"));
        assertEquals(3, json.get("totalPaginas"));
        assertEquals(true, json.get("haySiguiente"));
    }
}
