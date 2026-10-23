package com.fernando.ecommerce.web;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Una ruta declarada como "/api/productos/{sku}" que se compila a una expresion
 * regular y sabe extraer las variables de una URL concreta.
 */
public class Ruta {

    private static final Pattern VARIABLE = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)\\}");

    private final Metodo metodo;
    private final String plantilla;
    private final Pattern patron;
    private final List<String> nombresDeVariables = new ArrayList<>();

    public Ruta(Metodo metodo, String plantilla) {
        this.metodo = Objects.requireNonNull(metodo);
        this.plantilla = normalizar(plantilla);

        StringBuilder regex = new StringBuilder("^");
        Matcher matcher = VARIABLE.matcher(this.plantilla);
        int ultimo = 0;
        while (matcher.find()) {
            regex.append(Pattern.quote(this.plantilla.substring(ultimo, matcher.start())));
            regex.append("([^/]+)");
            nombresDeVariables.add(matcher.group(1));
            ultimo = matcher.end();
        }
        regex.append(Pattern.quote(this.plantilla.substring(ultimo)));
        regex.append("$");
        this.patron = Pattern.compile(regex.toString());
    }

    /** Devuelve las variables si la ruta coincide, o vacio si no. */
    public Optional<Map<String, String>> emparejar(Metodo metodo, String ruta) {
        if (this.metodo != metodo) {
            return Optional.empty();
        }
        Matcher matcher = patron.matcher(normalizar(ruta));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        Map<String, String> variables = new LinkedHashMap<>();
        for (int i = 0; i < nombresDeVariables.size(); i++) {
            variables.put(nombresDeVariables.get(i), matcher.group(i + 1));
        }
        return Optional.of(variables);
    }

    /** true si la ruta coincide ignorando el metodo (sirve para responder 405). */
    public boolean coincideLaRuta(String ruta) {
        return patron.matcher(normalizar(ruta)).matches();
    }

    public Metodo getMetodo() {
        return metodo;
    }

    public String getPlantilla() {
        return plantilla;
    }

    public List<String> getNombresDeVariables() {
        return List.copyOf(nombresDeVariables);
    }

    /** Quita la barra final salvo que la ruta sea solo "/". */
    private static String normalizar(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return "/";
        }
        String limpia = ruta.trim();
        if (!limpia.startsWith("/")) {
            limpia = "/" + limpia;
        }
        while (limpia.length() > 1 && limpia.endsWith("/")) {
            limpia = limpia.substring(0, limpia.length() - 1);
        }
        return limpia;
    }

    @Override
    public String toString() {
        return metodo + " " + plantilla;
    }
}
