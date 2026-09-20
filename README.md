# Fintech App & GestoPago Integration

Este repositorio contiene la implementación Full-Stack (Spring Boot + Flutter Web) de una aplicación Fintech que se integra con el proveedor de servicios externos **GestoPago**.

### 1. Configuración de propiedades
Se implementó un manejo seguro de las propiedades de entorno en `application.properties`. Se aislaron parámetros como contraseñas, tokens JWT, y URLs del proveedor.
- Se utilizan anotaciones `@Value` en lugar de *hardcodear* credenciales en el código fuente.
- Configuración independiente de bases de datos para **PostgreSQL** y **Redis**.

### 2. Cliente de integración
Se utilizó **Spring Cloud OpenFeign** para la orquestación y consumo de la API de Gestopago.
- `GestoPagoServiceClient`: Interfaz Feign encargada de mapear el endpoint `GET /sistema/service/getProductList.do`.
- **Autenticación (Bearer Token)**: Se construyó el interceptor `GestoPagoFeignConfig` que se encarga de inyectar dinámicamente el `Authorization: Bearer <token>` en cada petición de manera automática y transparente.

### 3. Servicio de negocio (Decisionés Técnicas y Fallbacks)
El servicio `GestoPagoProductServiceImpl` implementa un sistema robusto de Tolerancia a Fallos y caché de tres niveles:
1. **Memoria Ultrarrápida (Redis)**: Se intenta recuperar el catálogo de productos primero desde Redis.
2. **Base de Datos (Postgres)**: Si Redis cae o la llave expira, consulta la base de datos relacional y repuebla el caché de Redis.
3. **Servicio API Directo (GestoPago)**: Solo en el peor de los casos (Redis y Postgres sin datos), se consume el API de GestoPago.

Adicionalmente, se configuró un **Cron Job** (`GestoPagoSyncServiceImpl`) que se ejecuta de manera periódica, parsea los resultados y solo inserta datos en BD si el proveedor entregó un volumen de productos válido (evitando sobreescribir la base de datos local con un catálogo vacío por un fallo de GestoPago).

### 4. DTOs y Modelos de Respuesta
Toda comunicación hacia bases de datos y APIs fue encapsulada utilizando el patrón Data Transfer Object y Entidades JPA.
- `GestoPagoProduct`: Modelo de datos para guardar el catálogo de recargas/servicios.
- Se implementó un parser de XML a Java Custom (`GestoPagoXmlParser`) que limpia caracteres ilegales y la declaración inicial para no romper el DOM cuando el proveedor responde formatos inválidos.

### 5. Pruebas unitarias
Se incluyeron **Pruebas Unitarias** exhaustivas en `GestoPagoProductServiceImplTest` usando **JUnit 5** y **Mockito**.
- Validan que los flujos de Fallback de Base de Datos y Caché funcionen sin llamar al cliente de Feign cuando no es necesario.
- Verifican los lanzamientos de excepciones controladas (Código 1 para error local/timeout, Código 2 para errores del lado del servidor de GestoPago).

### 6. Breve documentación explicando la solución (Arquitectura)
La aplicación mantiene estrictamente la convención de Capas dictada:
* `Controller`: Controladores REST para Autenticación e invocación de productos.
* `Service`: Lógica de negocios compleja (Login, Registro, Fallbacks de GestoPago, Sincronización).
* `Client`: Definición de Feign Clients para GestoPago.
* `Config`: Manejo Global de Excepciones y políticas CORS.
* `Frontend (Flutter)`: Aplicación Web complementaria en la carpeta `/flutter_frontend` que consume localmente el backend manteniendo la persistencia de sesión a través del JWT y mostrando números de cuenta generados.
