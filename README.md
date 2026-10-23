# API REST de E-commerce

API REST completa de e-commerce —catalogo de productos y pedidos con control de stock— construida **sobre el servidor HTTP que ya trae el JDK** (`com.sun.net.httpserver`), sin Spring y sin ninguna dependencia externa.

## Endpoints

| Metodo | Ruta | Que hace |
|---|---|---|
| `GET` | `/api/salud` | Health check |
| `GET` | `/api/productos` | Lista paginada. Filtros: `?categoria=`, `?buscar=`, `?pagina=`, `?tamano=` |
| `GET` | `/api/productos/{sku}` | Un producto |
| `POST` | `/api/productos` | Crea (201 + cabecera `Location`) |
| `PUT` | `/api/productos/{sku}` | Actualiza solo los campos enviados |
| `DELETE` | `/api/productos/{sku}` | Elimina (204) |
| `GET` | `/api/pedidos` | Lista paginada. Filtro: `?cliente=` |
| `GET` | `/api/pedidos/{id}` | Un pedido |
| `POST` | `/api/pedidos` | Crea el pedido y reserva stock |
| `PATCH` | `/api/pedidos/{id}/estado` | Avanza la maquina de estados |

## Como ejecutar

```bash
mvn package
java -jar target/api-rest-ecommerce-1.0.0.jar 8080
```

```bash
curl http://localhost:8080/api/productos
curl -X POST http://localhost:8080/api/pedidos \
     -H 'Content-Type: application/json' \
     -d '{"cliente":"daniel","articulos":[{"sku":"TEC-01","cantidad":2}]}'
```

## Arquitectura

```
web/          Mini framework HTTP: Ruta, Router, Solicitud, Respuesta, Pagina
json/         Parser y serializador de JSON escritos a mano
dominio/      Producto, Pedido, LineaPedido, EstadoPedido
repositorio/  Contrato Repositorio<T> + implementacion concurrente en memoria
servicio/     Reglas de negocio (catalogo, pedidos)
api/          Controladores + adaptador al HttpServer del JDK
```

El punto de diseño que importa: **el `Router` no conoce `HttpExchange`**. Recibe metodo, ruta, parametros y cuerpo, y devuelve una `Respuesta`. `ServidorApi` es el unico archivo que habla el lenguaje del JDK. Por eso el enrutado y los controladores se prueban sin levantar un servidor, y cambiar de servidor HTTP tocaria un solo archivo.

## Que muestra este proyecto

- **Un mini framework de enrutado**: plantillas tipo `/api/productos/{sku}` compiladas a expresiones regulares, con extraccion de variables. Distingue **404** (la ruta no existe) de **405** (existe, pero no para ese metodo), que es la clase de detalle que casi siempre se omite.
- **Codigos HTTP correctos**: 200, 201 con `Location`, 204 sin cuerpo, 400 (JSON invalido o campos faltantes), 404, 405, 409 (SKU repetido, sin stock, transicion de estado invalida), 500 solo para errores de verdad inesperados.
- **Maquina de estados en el enum**: `EstadoPedido.puedeTransicionarA()` define las transiciones legales. Un pedido `PENDIENTE` no puede saltar a `ENTREGADO`.
- **Reserva de stock todo-o-nada**: si una linea del pedido falla, se libera el stock ya reservado de las anteriores. La prueba lo verifica explicitamente.
- **Concurrencia**: `ConcurrentHashMap` en el repositorio y metodos `synchronized` en el stock, porque el servidor atiende varias peticiones a la vez.
- **Paginacion** con metadatos completos (`totalPaginas`, `haySiguiente`) y validacion de limites.
- **Parser de JSON propio** con serializacion via la interfaz `JsonSerializable`.

## Pruebas

**57 pruebas**, incluyendo pruebas de integracion que levantan el servidor en un puerto libre y le hacen peticiones HTTP reales sobre loopback: verifican codigos de estado, cabeceras, paginacion, filtros y que el inventario quede consistente despues de un pedido fallido.

```bash
mvn test
```
