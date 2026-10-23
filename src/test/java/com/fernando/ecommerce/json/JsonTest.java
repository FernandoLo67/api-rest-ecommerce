package com.fernando.ecommerce.json;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonTest {

    @Test
    void parseaYSerializaUnObjetoIdaYVuelta() {
        String original = "{\"a\":1,\"b\":\"hola\",\"c\":true,\"d\":null}";
        assertEquals(original, Json.escribir(Json.parsear(original)));
    }

    @Test
    void serializaMapasListasYNumeros() {
        Map<String, Object> objeto = new LinkedHashMap<>();
        objeto.put("nombre", "Teclado");
        objeto.put("precio", new BigDecimal("450.00"));
        objeto.put("etiquetas", List.of("a", "b"));

        assertEquals("{\"nombre\":\"Teclado\",\"precio\":450.00,\"etiquetas\":[\"a\",\"b\"]}",
                Json.escribir(objeto));
    }

    @Test
    void escapaLosCaracteresEspeciales() {
        assertEquals("\"linea1\\nlinea2\\t\\\"cita\\\"\"", Json.escribir("linea1\nlinea2\t\"cita\""));
        assertEquals("\"barra\\\\invertida\"", Json.escribir("barra\\invertida"));
    }

    @Test
    void escapaLosCaracteresDeControl() {
        assertEquals("\"\\u0001\"", Json.escribir("\u0001"));
    }

    @Test
    void serializaObjetosQueImplementanJsonSerializable() {
        Json.JsonSerializable objeto = () -> Map.of("x", 1);
        assertEquals("{\"x\":1}", Json.escribir(objeto));
    }

    @Test
    void losDecimalesConservanSuEscala() {
        assertEquals("450.00", Json.escribir(new BigDecimal("450.00")));
    }

    @Test
    void parseaAnidamientoYPermiteBuscarPorRuta() {
        Object raiz = Json.parsear("{\"datos\":{\"items\":[{\"id\":7}]}}");
        assertEquals(new BigDecimal("7"), Json.buscar(raiz, "datos.items.0.id"));
        assertNull(Json.buscar(raiz, "datos.items.5.id"));
    }

    @Test
    void interpretaEscapesUnicodeAlLeer() {
        assertEquals("Mañana", Json.buscar(Json.parsear("{\"t\":\"Ma\\u00f1ana\"}"), "t"));
    }

    @Test
    void textoDeExigeUnCampoNoVacio() {
        Map<String, Object> objeto = Json.parsearObjeto("{\"a\":\"valor\",\"b\":\"  \"}");

        assertEquals("valor", Json.textoDe(objeto, "a"));
        assertThrows(Json.JsonException.class, () -> Json.textoDe(objeto, "b"));
        assertThrows(Json.JsonException.class, () -> Json.textoDe(objeto, "inexistente"));
    }

    @Test
    void decimalDeAceptaNumerosYCadenasNumericas() {
        Map<String, Object> objeto = Json.parsearObjeto("{\"a\":12.5,\"b\":\"3.25\",\"c\":\"x\"}");

        assertEquals(0, new BigDecimal("12.5").compareTo(Json.decimalDe(objeto, "a")));
        assertEquals(0, new BigDecimal("3.25").compareTo(Json.decimalDe(objeto, "b")));
        assertThrows(Json.JsonException.class, () -> Json.decimalDe(objeto, "c"));
    }

    @Test
    void enteroDeRechazaLosDecimalesConParteFraccionaria() {
        Map<String, Object> objeto = Json.parsearObjeto("{\"a\":5,\"b\":5.5}");

        assertEquals(5, Json.enteroDe(objeto, "a"));
        assertThrows(ArithmeticException.class, () -> Json.enteroDe(objeto, "b"));
    }

    @Test
    void parsearObjetoRechazaLoQueNoEsObjeto() {
        assertThrows(Json.JsonException.class, () -> Json.parsearObjeto("[1,2]"));
    }

    @Test
    void jsonMalFormadoLanzaJsonException() {
        assertThrows(Json.JsonException.class, () -> Json.parsear("{\"a\":1"));
        assertThrows(Json.JsonException.class, () -> Json.parsear("{\"a\":1} sobra"));
        assertThrows(Json.JsonException.class, () -> Json.parsear(null));
    }
}
