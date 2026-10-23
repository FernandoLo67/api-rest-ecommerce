package com.fernando.ecommerce;

import com.fernando.ecommerce.api.ServidorApi;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        ServidorApi servidor = new ServidorApi(puerto).conDatosDeEjemplo();
        servidor.iniciar();

        System.out.println("=== API REST de E-commerce ===");
        System.out.println("Escuchando en http://localhost:" + servidor.getPuerto());
        System.out.println();
        System.out.println("Rutas disponibles:");
        servidor.getRouter().rutasRegistradas().forEach(ruta -> System.out.println("  " + ruta));
        System.out.println();
        System.out.println("Prueba:");
        System.out.println("  curl http://localhost:" + servidor.getPuerto() + "/api/productos");
        System.out.println("  curl -X POST http://localhost:" + servidor.getPuerto() + "/api/pedidos \\");
        System.out.println("       -d '{\"cliente\":\"daniel\",\"articulos\":[{\"sku\":\"TEC-01\",\"cantidad\":2}]}'");
        System.out.println();
        System.out.println("Ctrl+C para detener.");

        Runtime.getRuntime().addShutdownHook(new Thread(servidor::close));
    }
}
