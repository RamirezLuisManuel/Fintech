# Arquitectura y Diseño Técnico: Integración de Pagos GestoPago (`sendTx.do`)

Este documento describe la arquitectura, análisis de riesgos, justificación de decisiones de diseño y el flujo integral de la integración de pagos y recargas con **GestoPago**, cumpliendo con los estándares de seguridad financiera, concurrencia, idempotencia y alta disponibilidad.

---

## 1. Diagrama del Flujo Integral de la Operación

El flujo se divide en dos fases principales mediante un esquema de **Autenticación de Dos Factores (2FA)** y **Two-Phase Commit (Retención Preventiva)**:

```
[Cliente / Postman]
       |
       |  1. POST /api/v1/gestopago/transactions/challenge
       |     (Token JWT, Idempotency-Key, numeroCuenta, idProducto, monto, telefono/referencia)
       v
[GestoPagoTransactionController]
       |
       v
[GestoPagoPaymentService & Validator]
       |-- a) Valida cuenta, usuario y cliente activos y autenticados.
       |-- b) Valida saldo disponible: (saldo - saldo_retenido) >= monto.
       |-- c) Valida restricciones de producto:
       |        * Recarga (tipoFront=1): monto == precio_producto && telefono 10 dígitos.
       |        * Servicio (tipoFront=2, 4): formato de referencia (1-100 chars alfanumérico).
       |        * Si tipoFront=4: ejecuta verifyReference.do con GestoPago.
       |-- d) Valida Idempotency-Key (evita colisiones o duplicados).
       |-- e) Guarda transacción en BD local (estado: OTP_PENDING, upc único de 15 chars).
       |-- f) Genera OTP de 6 dígitos (BCrypt en Redis con TTL de 5 min) y envía correo por Gmail.
       v
[Respuesta: HTTP 202 Accepted] (operationId, estado: OTP_PENDING, correoDestino enmascarado)
       |
       |  2. El cliente consulta su correo Gmail y obtiene el código OTP de 6 dígitos.
       |
       |  3. POST /api/v1/gestopago/transactions/confirm
       |     (operationId, codigo OTP)
       v
[GestoPagoPaymentService]
       |-- a) Valida y consume el OTP contra Redis (máx 5 intentos).
       |-- b) Bloqueo pesimista (Pessimistic Lock FOR UPDATE) de la transacción.
       |-- c) RESERVA ATÓMICA DE SALDO en Postgres:
       |        UPDATE cuentas SET saldo_retenido = saldo_retenido + :monto
       |        WHERE id = :id AND activo = TRUE AND (saldo - saldo_retenido) >= :monto
       |-- d) Estado pasa a: PROCESSING.
       |-- e) Cifra el payload JSON con AES/CBC/PKCS5Padding (método oficial GestoPago Java).
       |-- f) Inyecta token Bearer activo mediante GestoPagoFeignConfig.
       |-- g) Envía a GestoPago: POST /sistema/service/sendTx.do
       |
       +-------------------------------+-------------------------------+
       |                               |                               |
  [ÉXITO: Código "01"]         [FALLO: Código != "01"]       [TIMEOUT / RED / "06"]
       |                               |                               |
       v                               v                               v
[Captura Definitiva]           [Liberación de Saldo]         [Estado: UNKNOWN]
* UPDATE cuentas SET           * UPDATE cuentas SET          * Saldo permanece retenido.
    saldo = saldo - monto,         saldo_retenido =          * Scheduler cada 30 seg
    saldo_retenido =                 saldo_retenido - monto    ejecuta confirmTx.do:
    saldo_retenido - monto     * Estado: FAILED              * Si aplicada -> Captura.
* Estado: SUCCEEDED                                          * Si no aplicada -> Libera.
```

---

## 2. Matriz de Riesgos y Escenarios de Fallo Contemplados

| Escenario de Riesgo | Posible Impacto | Estrategia de Mitigación Implementada |
| :--- | :--- | :--- |
| **1. Concurrencia y Doble Gasto** | Dos solicitudes casi simultáneas podrían sobregirar la cuenta del cliente. | **Atomic Update a nivel Base de Datos** (`WHERE (saldo - saldo_retenido) >= :monto`) y bloqueo pesimista `SELECT ... FOR UPDATE` en `GestoPagoTransactionStateService`. Ninguna solicitud puede gastar saldo no disponible. |
| **2. Timeout / Caída de Red en `sendTx.do`** | Asumir fallo provocaría pérdida si el proveedor sí procesó la recarga; asumir éxito causaría pérdidas si no se aplicó. | **Estado `UNKNOWN` + Conciliación Asíncrona**. El saldo se mantiene en `saldo_retenido`. Un cron programado (`@Scheduled`) consulta periódicamente a `confirmTx.do` hasta obtener una confirmación concluyente antes de tocar el saldo final. |
| **3. Peticiones Duplicadas (Reintentos del Cliente)** | Cobro doble por presionar dos veces el botón de pago. | **Idempotencia Obligatoria**: Header `Idempotency-Key` único indexado en la tabla `gestopago_transactions`. Si la petición ya existe, retorna el estado actual sin volver a procesar el cobro. |
| **4. Montos Manipulados o Parámetros Inválidos** | Un atacante intenta pagar $1.00 por una recarga de $100.00. | **Validación en catálogo local (`GestoPagoPaymentValidator`)**: Para recargas móviles fijas (`tipoFront = 1`), el monto solicitado debe ser idéntico al precio oficial del catálogo (`monto == product.precio`). En servicios en línea (`tipoFront = 4`), se valida la referencia con `verifyReference.do`. |
| **5. Exposición de Credenciales y Tokens** | Fuga de contraseñas de distribuidor o tokens Bearer en código o logs. | **Seguridad 12-Factor App**: Se admiten variables de entorno (`GESTOPAGO_PASSWORD`, `GESTOPAGO_SECRET_KEY`, etc.). El interceptor Feign inyecta el token en memoria sin loguearlo ni exponerlo en HTTP Responses. El OTP se guarda con hash BCrypt en Redis. |

---

## 3. Justificación de Decisiones de Diseño

### A. Estrategia de Retención Preventiva (Two-Phase Commit)
* **¿Por qué no descontar antes de llamar a GestoPago?** Si GestoPago falla, habría que reembolsar el dinero, generando inconsistencias contables y confusión en el usuario.
* **¿Por qué no descontar después de recibir la respuesta?** Si el usuario hace otra compra en el lapso de espera (de 3 a 8 segundos), podría vaciar su saldo y GestoPago aplicaría el cargo sobre una cuenta sin fondos.
* **Solución**: Se usa `saldo_retenido`. Al iniciar la operación, el saldo disponible disminuye (`saldo - saldo_retenido`), pero el dinero sigue en la cuenta. Solo se debita formalmente (`saldo = saldo - monto`) cuando GestoPago confirma con código `"01"`.

### B. Máquina de Estados Finita en Transacciones
La tabla `gestopago_transactions` implementa una máquina de estados determinista:
* `OTP_PENDING`: Esperando confirmación de segundo factor por correo.
* `PROCESSING`: Saldo retenido, petición en vuelo hacia GestoPago.
* `SUCCEEDED`: GestoPago confirmó código "01". Saldo capturado definitivamente.
* `FAILED`: Proveedor rechazó la operación o expiró OTP. Saldo retenido liberado.
* `UNKNOWN`: Incertidumbre de red / timeout. Saldo protegido en espera de conciliación.
* `MANUAL_REVIEW`: Si en 24 horas no se resuelve la conciliación, se escala a auditoría humana.

### C. Arquitectura Limpia y Principios SOLID
* **Single Responsibility Principle (SRP)**:
  * [GestoPagoTransactionController.java](file:///c:/Datos/UTNG/10mo/WEB%20progresivas/Fintech/src/main/java/com/proyecto/servicios/controller/GestoPagoTransactionController.java): Solo maneja HTTP y delega. 0% `@PathVariable`.
  * [GestoPagoPaymentValidator.java](file:///c:/Datos/UTNG/10mo/WEB%20progresivas/Fintech/src/main/java/com/proyecto/servicios/service/GestoPagoPaymentValidator.java): Concentra las reglas de negocio y validaciones.
  * [GestoPagoPaymentService.java](file:///c:/Datos/UTNG/10mo/WEB%20progresivas/Fintech/src/main/java/com/proyecto/servicios/service/GestoPagoPaymentService.java): Orquesta el ciclo de vida del pago.
  * [GestoPagoTransactionStateService.java](file:///c:/Datos/UTNG/10mo/WEB%20progresivas/Fintech/src/main/java/com/proyecto/servicios/service/GestoPagoTransactionStateService.java): Control de persistencia y bloqueos atómicos.
  * [GestoPagoEncryptionService.java](file:///c:/Datos/UTNG/10mo/WEB%20progresivas/Fintech/src/main/java/com/proyecto/servicios/service/GestoPagoEncryptionService.java): Cifrado simétrico AES/CBC oficial Java.

---

## 4. Respuestas HTTP y Formato Estandarizado

Toda la API responde bajo el formato unificado `GenericResponse<T>` con los códigos acordados:
* `codigo: 0`: Operación exitosa (HTTP 200 / 202).
* `codigo: 1`: Error de validación de entrada (HTTP 400 - Bean Validation `@Valid`).
* `codigo: 2`: Error de negocio o del sistema (HTTP 401, 403, 404, 409, 410, 503).