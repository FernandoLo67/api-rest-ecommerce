package com.fernando.ecommerce.web;

/** Error que ya sabe con que codigo HTTP debe responderse. */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int codigo;

    public ApiException(int codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(404, mensaje);
    }

    public static ApiException solicitudInvalida(String mensaje) {
        return new ApiException(400, mensaje);
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(409, mensaje);
    }
}
