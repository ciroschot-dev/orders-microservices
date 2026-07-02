# Interview Prep — orders-microservices

> Q&A drawn from the actual decisions made while building this project. It grows one
> section per phase. Answers are kept tight and on-purpose — enough to explain the *why*
> out loud in an interview, not a textbook. Spanish is fine for studying; the technical
> terms are the ones you'd use in the room.

---

## 1. Microservices fundamentals

**¿Por qué microservicios y no un monolito?**
Cada servicio se deploya, escala y falla de forma independiente. Podés escalar solo la parte
que lo necesita (ej: inventory en un pico) sin tocar el resto, y un equipo trabaja en un
servicio sin pisar a otro. El costo: complejidad operativa (red, latencia, consistencia
eventual, observabilidad). Para un CRUD chico es sobre-ingeniería; se justifica cuando el
dominio y el equipo crecen.

**¿Qué es "database-per-service" y por qué?**
Cada servicio es dueño de su base y nadie más la toca. Si compartieran tablas, un cambio de
schema de uno rompería al otro y se perdería la independencia de deploy. Además cada servicio
elige su tecnología (Postgres en order/inventory, MongoDB en notification). El acoplamiento
queda en fronteras explícitas: la **API** (Feign) y los **eventos** (RabbitMQ), no las tablas.
El costo: perdés los JOINs entre servicios → tenés que pedir el dato por HTTP o vía evento.

---

## 2. Service discovery (Eureka)

**¿Qué problema resuelve el service discovery?**
En micro, los servicios son procesos separados en hosts/puertos que cambian (contenedores que
mueren y se recrean, varias instancias para escalar). Hardcodear IPs se rompe. Eureka es un
registro vivo: cada servicio se anota y se busca **por nombre**, no por dirección.

**Server vs client de Eureka.**
El `@EnableEurekaServer` es el registro central. Cada servicio es un **client**: al arrancar se
*registra* (nombre + host:puerto) y manda un *heartbeat* cada ~30s. Si deja de latir, Eureka lo
saca de la lista.

**¿Por qué en el server pusiste `register-with-eureka: false` y `fetch-registry: false`?**
El server también trae adentro un client (para clusters de varios Eureka que se replican). En un
standalone de una sola instancia no querés que se registre en sí mismo ni se baje su propio
registro — es la fuente de verdad. Sin eso, warnings de conexión a peers inexistentes.

**¿Qué es el self-preservation mode (el banner rojo)?**
Cuando los heartbeats recibidos caen debajo de un umbral, Eureka sospecha que el problema es la
**red** (no que se cayeron todos los servicios de golpe) y **deja de expirar instancias** por las
dudas. En prod con varias instancias te salva de un blip de red que te vacíe el registro. En dev
con un solo cliente salta siempre (renews < threshold) — es esperable, no un error.

**Client-side vs server-side discovery.**
Client-side (lo de acá): el cliente le pregunta a Eureka las instancias vivas y **él** elige a
cuál llamar (con Spring Cloud LoadBalancer). Server-side: un balanceador intermedio resuelve y
reenvía (ej: un LB de infra). Client-side = menos saltos, más lógica en el cliente.

---

## 3. Diseño de API / REST

**¿Qué es `ProblemDetail` (RFC 7807) y por qué usarlo?**
Formato estándar de respuesta de error HTTP (`type`, `title`, `status`, `detail`, más campos
propios). Da errores consistentes y machine-readable en todos los servicios, en vez de que cada
uno invente su JSON de error. En Spring: `ProblemDetail.forStatus(...)` + `setProperty(...)` para
extenderlo (ej: mapa de errores de validación campo→mensaje).

**Status codes: ¿cuándo cada uno?**
- **201 Created** — POST que crea un recurso.
- **204 No Content** — DELETE ok / respuesta sin body.
- **404 Not Found** — el recurso no existe.
- **409 Conflict** — conflicto con el estado actual (SKU duplicado, stock insuficiente). Clave:
  **no todo error es 400.** El 400 es input mal formado; el 409 es "tu pedido es válido pero
  choca con el estado del sistema".
- **400 Bad Request** — validación de input fallida (`@Valid` → `MethodArgumentNotValidException`).

**PUT vs PATCH.**
PUT = reemplazo **completo** del recurso (mandás el objeto entero, todos los campos requeridos).
PATCH = actualización **parcial** (solo los campos a cambiar). Si tu request exige todos los
campos y pisás todo, el verbo correcto es PUT.

**¿Path variable o query param para el id?**
El id **identifica** el recurso → va en el path (`DELETE /products/5`). Los query params son para
filtros/opciones (`?status=PENDING`), no para señalar cuál recurso.

---

## 4. Persistencia / JPA

**¿Por qué DTOs y no exponer las entidades JPA?**
Desacopla la API del modelo de persistencia: podés cambiar la tabla sin romper el contrato HTTP,
evitás exponer campos internos o relaciones lazy (y sus errores de serialización), y validás el
input en un objeto pensado para eso. El pasaje entidad⇄DTO lo hace **MapStruct** (genera el
mapper en compile-time, sin reflection en runtime).

**¿Qué es dirty checking?**
Dentro de una transacción, una entidad *managed* que modificás con setters se sincroniza sola a
la DB en el commit — no hace falta llamar `save()`. Por eso el `update` trae la entidad, la muta
y listo.

**¿Para qué `@Transactional(readOnly = true)` en las lecturas?**
Le avisa a Hibernate/DB que no va a escribir → puede optimizar (sin dirty checking, hints al
driver). Documenta la intención y previene escrituras accidentales.

**Testcontainers, ¿por qué?**
Levanta una DB real (Postgres) efímera en Docker para los tests de integración, en vez de un H2
en memoria que se comporta distinto. Los tests corren contra el mismo motor que producción.

---

## 5. Comunicación entre servicios (OpenFeign)

**¿Qué es OpenFeign?**
Un cliente HTTP **declarativo**: declarás una interfaz con las firmas de los endpoints remotos
(anotaciones Spring MVC) y Spring genera la implementación. Elimina el boilerplate de
`RestTemplate`/`WebClient` (armar URL, serializar, parsear).

**¿Cómo resuelve la dirección sin hardcodear host:puerto?**
En `@FeignClient(name = "inventory-service")`, el `name` es el **nombre en Eureka**. Feign le
pregunta a Eureka las instancias vivas y Spring Cloud LoadBalancer reparte entre ellas
(client-side load balancing). Si el servicio se mueve o escala, el cliente se entera solo.

**¿Por qué order define su propio `ProductResponse` en vez de importar el de inventory?**
Database-per-service también vale para el código: los servicios no comparten clases. Order define
un DTO propio con solo los campos que le interesan del JSON; el contrato es la forma del payload,
no una dependencia de código compartida.

---

## 6. Resiliencia (Resilience4j) — *se completa al cerrar Phase 3*

<!-- pendiente: circuit breaker (estados closed/open/half-open), fallback, por qué un servicio
     lento/caído no debe tumbar al que lo llama, timeouts, retries. -->

---

## 7. Mensajería asíncrona (RabbitMQ) — *Phase 5*

<!-- pendiente: AMQP, exchange/queue/binding, producer/consumer, idempotencia, dead-letter queue,
     sync vs async y por qué el descuento de stock va por evento y no por Feign. -->
