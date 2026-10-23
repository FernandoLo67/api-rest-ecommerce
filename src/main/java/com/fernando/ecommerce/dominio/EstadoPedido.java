package com.fernando.ecommerce.dominio;

import java.util.Set;

public enum EstadoPedido {
    PENDIENTE,
    PAGADO,
    ENVIADO,
    ENTREGADO,
    CANCELADO;

    /** Transiciones permitidas: la maquina de estados vive en el propio enum. */
    public boolean puedeTransicionarA(EstadoPedido destino) {
        switch (this) {
            case PENDIENTE:
                return Set.of(PAGADO, CANCELADO).contains(destino);
            case PAGADO:
                return Set.of(ENVIADO, CANCELADO).contains(destino);
            case ENVIADO:
                return Set.of(ENTREGADO).contains(destino);
            case ENTREGADO:
            case CANCELADO:
                return false;
            default:
                throw new IllegalStateException("Estado desconocido: " + this);
        }
    }

    public boolean esFinal() {
        return this == ENTREGADO || this == CANCELADO;
    }
}
