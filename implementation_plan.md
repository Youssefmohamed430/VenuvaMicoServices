# Venuva Microservices — Inter-Service Communication Plan

## Problem Summary

The microservices currently have **cross-database access violations** — particularly `registration-service` which imports `Event` and `UserEntity` models and queries them via JPA repositories (`EventRepository`, `UserRepository`) that hit **other services' databases**. This breaks microservice isolation. We need to replace all cross-database queries with **REST API calls** (synchronous) and **RabbitMQ messages** (asynchronous), plus apply AOP across all services and add configurable service URLs.

## Current Service Ports & Databases

| Service | Port | Database | Spring Boot |
|---|---|---|---|
| Auth Service | 8081 | `authdb` | 3.5.11 |
| Event Service | 8088 | `event_service` | 3.5.14 |
| Registration Service | 8088 ⚠️ | `venuvadb` | 4.0.6 |
| Notification Service | 9090 | `NotifsDb` | 4.0.6 |
| Payment Service | 9099 | `paymentdb` | 4.0.6 |

> [!WARNING]
> Registration Service and Event Service both use port **8088** — one of them must change. The plan assigns Registration to port **8085**.

---

## Communication Map (Sync vs Async)

```mermaid
graph LR
    subgraph "Synchronous REST API Calls"
        RS["Registration Service"] -->|GET /api/auth/users/{id}| AS["Auth Service"]
        RS -->|GET /api/events/internal/{id}| ES["Event Service"]
        PS["Payment Service"] -->|PUT /api/registrations/status| RS
        ES -->|GET /api/auth/users/{id}| AS
    end

    subgraph "Asynchronous RabbitMQ"
        ES -->|event.created| NS["Notification Service"]
        RS -->|registration.created| NS
        PS -->|payment.success / payment.failed| RS
        PS -->|payment.success| NS
    end
```

### Decision Criteria — Sync vs Async

| Call | Type | Reason |
|---|---|---|
| Registration → Auth (get user) | **Sync** | Need user data immediately to validate & build response |
| Registration → Event (get event) | **Sync** | Need event data immediately to validate capacity & build response |
| Event → Auth (get organizer name) | **Sync** | Need organizer name immediately for response DTO |
| Payment callback → Registration (update status) | **Async (RabbitMQ)** | Fire-and-forget; payment callback must return 200 fast |
| Event created → Notification | **Async (RabbitMQ)** | Already implemented; fire-and-forget |
| Registration created → Notification | **Async (RabbitMQ)** | Fire-and-forget; user doesn't wait for notification |
| Payment success → Notification | **Async (RabbitMQ)** | Fire-and-forget; notify user asynchronously |

---

## Open Questions

> [!IMPORTANT]
> **Port Conflict**: Registration and Event both use 8088. I propose changing Registration to **8085**. Is this acceptable?

> [!IMPORTANT]
> **RabbitMQ Host**: Currently event-service connects to `localhost:5672` (default). Should all services use the same RabbitMQ instance? If you have a different host/port, please provide it.

> [!IMPORTANT]  
> **Internal API Security**: The `GET /api/events/internal/{id}` endpoint currently has no auth. Should internal service-to-service calls pass the JWT token, use a shared service key, or remain unauthenticated?

---

## Proposed Changes

### Phase 0: Cross-Cutting — Service URL Configuration

Add configurable service URLs to every service so you can change them between dev and production.

---

#### [MODIFY] [application.properties](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/authservice/src/main/resources/application.properties)

Add service URLs section:
```properties
# ===== MICROSERVICE URLs =====
services.event.url=${EVENT_SERVICE_URL:http://localhost:8088}
services.registration.url=${REGISTRATION_SERVICE_URL:http://localhost:8085}
services.notification.url=${NOTIFICATION_SERVICE_URL:http://localhost:9090}
services.payment.url=${PAYMENT_SERVICE_URL:http://localhost:9099}
```

#### [MODIFY] application.properties for each of: event-service, registration-service, notif-service, paymentservice

Each service gets the same block (with its own URL excluded) in `application.properties` and corresponding `.env` entries.

#### [NEW] `application-prod.properties` in each service

Create a production profile for each service under `src/main/resources/`:
```properties
# Production URLs — override via environment variables
services.auth.url=${AUTH_SERVICE_URL:http://auth-service:8081}
services.event.url=${EVENT_SERVICE_URL:http://event-service:8088}
# ... etc
```

Activate with: `SPRING_PROFILES_ACTIVE=prod`

---

### Phase 1: Auth Service — Add Missing Endpoints

The Auth Service is the **source of truth** for user data. Other services need to fetch user info. Currently there is no `GET /api/auth/users/{id}` endpoint.

---

#### [MODIFY] [AuthController.java](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/authservice/src/main/java/com/example/authservice/Controllers/AuthController.java)

Add two new internal endpoints:

```java
// GET /api/auth/users/{id} — Used by Registration, Event, Notif services
@GetMapping("/users/{id}")
@HandleException
public ResponseEntity<?> getUserById(@PathVariable int id) {
    log.info("[INTERNAL] AuthController.getUserById() — id={}", id);
    var user = authService.getUserById(id);
    return ResponseEntity.ok(user);
}

// GET /api/auth/organizers — Used by Admin frontend
// GET /api/auth/organizers/{id} — Used by Event Service to get organizer name
@GetMapping("/organizers/{id}")
@HandleException
public ResponseEntity<?> getOrganizerById(@PathVariable int id) {
    log.info("[INTERNAL] AuthController.getOrganizerById() — id={}", id);
    var user = authService.getUserById(id);
    return ResponseEntity.ok(user);
}
```

#### [MODIFY] [AuthService.java](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/authservice/src/main/java/com/example/authservice/Services/AuthService.java)

Add method:
```java
public UserResponseDto getUserById(int id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));
    return new UserResponseDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
}
```

#### [NEW] `UserResponseDto.java` in `authservice/AuthDtos/`

```java
@Data @AllArgsConstructor @NoArgsConstructor
public class UserResponseDto {
    private int id;
    private String name;
    private String email;
    private String role;
}
```

---

### Phase 2: Registration Service — Replace Cross-DB Access with API Calls

This is the **biggest change**. Currently `RegistrationService` uses `EventRepository` and `UserRepository` which hit other databases directly. We must:

1. **Delete** `EventRepository`, `UserRepository`, and models `Event`, `UserEntity`
2. **Create** HTTP client classes (`AuthServiceClient`, `EventServiceClient`)
3. **Create** DTOs for API responses (`UserDto`, `EventDto`)
4. **Rewrite** `RegistrationService` to call APIs instead of repositories

---

#### [DELETE] `registration-service/.../Repos/UserRepository.java`
#### [DELETE] `registration-service/.../Repos/EventRepository.java` (if exists as file)
#### [DELETE] `registration-service/.../Models/UserEntity.java` (if exists as file)
#### [DELETE] `registration-service/.../Models/Event.java` (if exists as file)

> [!NOTE]
> These model/repo files are imported but may not physically exist yet (compilation would fail). We remove the imports and replace with API client calls.

#### [NEW] `registration-service/.../Client/AuthServiceClient.java`

```java
@Component
public class AuthServiceClient {
    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public AuthServiceClient(RestTemplate restTemplate,
            @Value("${services.auth.url}") String authServiceUrl) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
    }

    public UserDto getUserById(int userId) {
        return restTemplate.getForObject(
            authServiceUrl + "/api/auth/users/" + userId, UserDto.class);
    }
}
```

#### [NEW] `registration-service/.../Client/EventServiceClient.java`

```java
@Component
public class EventServiceClient {
    private final RestTemplate restTemplate;
    private final String eventServiceUrl;

    public EventServiceClient(RestTemplate restTemplate,
            @Value("${services.event.url}") String eventServiceUrl) {
        this.restTemplate = restTemplate;
        this.eventServiceUrl = eventServiceUrl;
    }

    public EventDto getEventById(int eventId) {
        return restTemplate.getForObject(
            eventServiceUrl + "/api/events/internal/" + eventId, EventDto.class);
    }
}
```

#### [NEW] `registration-service/.../Client/NotificationServiceClient.java`

For publishing registration events (or use RabbitMQ — see Phase 5).

#### [NEW] `registration-service/.../DTOs/UserDto.java`

```java
@Data @NoArgsConstructor @AllArgsConstructor
public class UserDto {
    private int id;
    private String name;
    private String email;
    private String role;
}
```

#### [NEW] `registration-service/.../DTOs/EventDto.java`

```java
@Data @NoArgsConstructor @AllArgsConstructor
public class EventDto {
    private int id;
    private String title;
    private String description;
    private LocalDateTime date;
    private String location;
    private int maxAttendance;
    private String eventStatus;
    private boolean paymentRequired;
    private BigDecimal price;
    private int organizerId;
}
```

#### [MODIFY] [RegistrationService.java](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/registration-service/src/main/java/com/example/registration_service/Services/RegistrationService.java)

**Before (cross-DB):**
```java
private final UserRepository userRepository;
private final EventRepository eventRepository;
// ...
Event event = eventRepository.findById(requestDto.getEventId()).orElse(null);
UserEntity user = userRepository.findById(requestDto.getUserId()).orElse(null);
```

**After (API calls):**
```java
private final AuthServiceClient authClient;
private final EventServiceClient eventClient;
// ...
EventDto event = eventClient.getEventById(requestDto.getEventId());
UserDto user = authClient.getUserById(requestDto.getUserId());
```

Full rewrite of all methods: `registerUserToEvent`, `getUserRegistrations`, `cancelRegistration`, `getTotalSpents` — replacing every `eventRepository` / `userRepository` call with the corresponding client call.

#### [MODIFY] `.env` — Change port to **8085** and add service URLs:

```properties
SERVER_PORT=8085

# ===== MICROSERVICE URLs =====
AUTH_SERVICE_URL=http://localhost:8081
EVENT_SERVICE_URL=http://localhost:8088
NOTIFICATION_SERVICE_URL=http://localhost:9090
PAYMENT_SERVICE_URL=http://localhost:9099
```

#### [NEW] `application.properties` for registration-service

Currently missing `src/main/resources/`. Create the directory and file with full configuration including service URLs.

#### [MODIFY] `pom.xml` — Remove `mssql-jdbc` dependency (not needed), ensure `spring-boot-starter-amqp` is present for RabbitMQ.

---

### Phase 3: Event Service — Fix AuthServiceClient & Add AOP

The Event Service already has an `AuthServiceClient` but it's **broken** — the constructor hardcodes `"authServiceUrl"` as a literal string instead of using the injected value.

---

#### [MODIFY] [AuthServiceClient.java](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/event-service/src/main/java/com/example/event_service/client/AuthServiceClient.java)

**Fix the broken constructor:**
```diff
- this.authServiceUrl = "authServiceUrl";
+ this.authServiceUrl = authServiceUrl;
```

**Fix the API path** to use the new endpoint:
```diff
- authServiceUrl + "/api/admin/organizers/" + organizerId,
+ authServiceUrl + "/api/auth/users/" + organizerId,
```

#### [MODIFY] [application.properties](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/event-service/src/main/resources/application.properties)

Add service URLs and JWT config:
```properties
# ===== MICROSERVICE URLs =====
services.auth.url=${AUTH_SERVICE_URL:http://localhost:8081}
services.registration.url=${REGISTRATION_SERVICE_URL:http://localhost:8085}
services.notification.url=${NOTIFICATION_SERVICE_URL:http://localhost:9090}

# ===== JWT =====
app.jwt.secret=${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
```

#### [MODIFY] `pom.xml` — Add AOP dependencies:
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aop</artifactId>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
</dependency>
<dependency>
    <groupId>io.github.microservices</groupId>
    <artifactId>aop-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### [MODIFY] [EventController.java](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/event-service/src/main/java/com/example/event_service/controller/EventController.java)

Add `@HandleException` annotation to all endpoints (currently missing).

---

### Phase 4: Notification Service — Add CreateNotification Endpoint & RabbitMQ Consumer

Currently the Notification Service only has GET and PUT endpoints. Other services need to **create** notifications. Also, it needs a RabbitMQ consumer to listen for `event.created` messages from Event Service.

---

#### [MODIFY] [NotificationController.java](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/notif-service/src/main/java/com/example/notif_service/Controllers/NotificationController.java)

Add POST endpoint:
```java
// POST /api/notifications — Create notification for a user
@PostMapping
public ResponseEntity<?> createNotification(@RequestBody @Valid CreateNotificationDto dto) {
    Result<?> result = notifService.createNotification(dto);
    return ResponseUtility.toResponse(result, HttpStatus.CREATED);
}
```

#### [MODIFY] [INotifService.java](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/notif-service/src/main/java/com/example/notif_service/Services/INotifService.java)

Add method: `Result<NotifDTO> createNotification(CreateNotificationDto dto);`

#### [MODIFY] [NotifService.java](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/notif-service/src/main/java/com/example/notif_service/Services/NotifService.java)

Implement `createNotification`:
```java
@Override
public Result<NotifDTO> createNotification(CreateNotificationDto dto) {
    Notification notif = new Notification();
    notif.setMessage(dto.getMessage());
    notif.setDate(LocalDateTime.now());
    notifRepo.save(notif);

    UserNotification userNotif = UserNotification.builder()
        .userId(dto.getUserId())
        .notifId(notif.getNotifId())
        .isRead(false)
        .build();
    userNotifRepoGeneric.save(userNotif);

    return Result.success(new NotifDTO(notif.getNotifId(), notif.getMessage(),
        notif.getDate(), dto.getUserId(), "Unknown", false));
}
```

#### [NEW] `notif-service/.../Messaging/RabbitMQConfig.java`

Configure queue, exchange, and binding to listen for `event.created`:
```java
@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "venuva.events";
    public static final String EVENT_CREATED_QUEUE = "notif.event.created";
    public static final String PAYMENT_SUCCESS_QUEUE = "notif.payment.success";
    public static final String REGISTRATION_CREATED_QUEUE = "notif.registration.created";
    // Beans: queues, exchange, bindings
}
```

#### [NEW] `notif-service/.../Messaging/EventConsumer.java`

```java
@Component
@RabbitListener(queues = "notif.event.created")
public class EventConsumer {
    private final INotifService notifService;
    private final AuthServiceClient authClient;

    @RabbitHandler
    public void handleEventCreated(EventCreatedMessage message) {
        // Get all users from auth service, create notification for each
        notifService.sendNotification(message.getMessage());
    }
}
```

#### [NEW] `notif-service/.../Client/AuthServiceClient.java`

For fetching user names when creating notifications.

#### [MODIFY] `pom.xml` — Add `spring-boot-starter-amqp` and AOP dependencies.

#### [MODIFY] `.env` — Add RabbitMQ and service URL config.

#### [MODIFY] `application.properties` — Add RabbitMQ connection and service URLs.

---

### Phase 5: Payment Service — Add RabbitMQ Publisher & API Clients

After successful payment callback, the Payment Service needs to:
1. **Publish** `payment.success` event to RabbitMQ → Registration & Notification listen
2. Optionally call Registration Service to update registration status

---

#### [NEW] `paymentservice/.../Messaging/RabbitMQConfig.java`

```java
@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "venuva.events";
    // Define exchange bean
}
```

#### [NEW] `paymentservice/.../Messaging/PaymentPublisher.java`

```java
@Component
public class PaymentPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        rabbitTemplate.convertAndSend("venuva.events", "payment.success", event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        rabbitTemplate.convertAndSend("venuva.events", "payment.failed", event);
    }
}
```

#### [NEW] `paymentservice/.../DTOs/PaymentSuccessEvent.java`

```java
@Data
public class PaymentSuccessEvent {
    private int userId;
    private int eventId;
    private int orderId;
    private BigDecimal amount;
}
```

#### [MODIFY] [PayMobService.java](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/paymentservice/src/main/java/com/example/paymentservice/Services/PayMobService.java)

In the `paymobCallback` method, after successful payment, publish RabbitMQ event:
```java
if (Boolean.TRUE.equals(obj.success)) {
    PaymentSuccessEvent event = new PaymentSuccessEvent();
    event.setUserId(userId);
    event.setEventId(eventId);
    event.setOrderId(obj.order.id);
    event.setAmount(payment.getAmount());
    paymentPublisher.publishPaymentSuccess(event);
}
```

#### [MODIFY] `pom.xml` — Add `spring-boot-starter-amqp` and AOP dependencies.

#### [MODIFY] `.env` — Add RabbitMQ config and service URLs.

#### [MODIFY] `application.properties` — Add RabbitMQ connection and service URLs.

#### [MODIFY] [PaymobController.java](file:///c:/Users/Gaber/Desktop/VenuvaMicoServices/paymentservice/src/main/java/com/example/paymentservice/Controller/PaymobController.java)

Add `@HandleException` AOP annotations.

---

### Phase 6: Registration Service — Add RabbitMQ Consumer & Publisher

Registration Service needs to:
1. **Listen** for `payment.success` → update registration status from PENDING to PAID
2. **Publish** `registration.created` → Notification Service creates notification

---

#### [NEW] `registration-service/.../Messaging/RabbitMQConfig.java`

```java
@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "venuva.events";
    public static final String PAYMENT_SUCCESS_QUEUE = "registration.payment.success";
    // Queue, Exchange, Binding beans
}
```

#### [NEW] `registration-service/.../Messaging/PaymentEventConsumer.java`

```java
@Component
public class PaymentEventConsumer {
    private final RegistrationRepository repository;

    @RabbitListener(queues = "registration.payment.success")
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        Registration reg = repository.findByEventIdAndUserId(event.getEventId(), event.getUserId());
        if (reg != null) {
            reg.setRegistrationStatus(RegistrationStatus.PAID);
            repository.save(reg);
        }
    }
}
```

#### [NEW] `registration-service/.../Messaging/RegistrationPublisher.java`

```java
@Component
public class RegistrationPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishRegistrationCreated(RegistrationCreatedEvent event) {
        rabbitTemplate.convertAndSend("venuva.events", "registration.created", event);
    }
}
```

#### [MODIFY] `RegistrationService.java` — After successful registration, publish event:
```java
registrationPublisher.publishRegistrationCreated(new RegistrationCreatedEvent(
    user.getId(), user.getName(), event.getId(), event.getTitle()));
```

---

### Phase 7: Apply AOP Module to All Services

Currently only **Auth Service** and **Registration Service** have the AOP dependency. Add it to **Event**, **Notification**, and **Payment** services.

---

#### For each of: event-service, notif-service, paymentservice

**[MODIFY] `pom.xml`** — Add:
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aop</artifactId>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
</dependency>
<dependency>
    <groupId>io.github.microservices</groupId>
    <artifactId>aop-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**[MODIFY] Controllers** — Add `@HandleException` and `@Loggable` annotations to all endpoints.

---

## Summary of New/Modified Files Per Service

### Auth Service (3 files)
| Action | File |
|---|---|
| MODIFY | `AuthController.java` — add `getUserById`, `getOrganizerById` |
| MODIFY | `AuthService.java` — add `getUserById` method |
| NEW | `AuthDtos/UserResponseDto.java` |
| MODIFY | `application.properties` — add service URLs |
| MODIFY | `.env` — add service URL vars |

### Registration Service (12+ files)
| Action | File |
|---|---|
| DELETE | `Repos/UserRepository.java`, `Repos/EventRepository.java` |
| DELETE | `Models/UserEntity.java`, `Models/Event.java` (if exist) |
| NEW | `Client/AuthServiceClient.java` |
| NEW | `Client/EventServiceClient.java` |
| NEW | `DTOs/UserDto.java`, `DTOs/EventDto.java` |
| NEW | `Messaging/RabbitMQConfig.java` |
| NEW | `Messaging/PaymentEventConsumer.java` |
| NEW | `Messaging/RegistrationPublisher.java` |
| MODIFY | `Services/RegistrationService.java` — full rewrite |
| MODIFY | `Services/IRegistrationService.java` |
| NEW | `src/main/resources/application.properties` |
| MODIFY | `.env`, `pom.xml` |

### Event Service (4 files)
| Action | File |
|---|---|
| MODIFY | `client/AuthServiceClient.java` — fix broken constructor & URL |
| MODIFY | `controller/EventController.java` — add AOP annotations |
| MODIFY | `application.properties` — add service URLs |
| MODIFY | `pom.xml` — add AOP deps |

### Notification Service (7+ files)
| Action | File |
|---|---|
| MODIFY | `Controllers/NotificationController.java` — add POST |
| MODIFY | `Services/INotifService.java` — add method |
| MODIFY | `Services/NotifService.java` — implement createNotification |
| NEW | `Messaging/RabbitMQConfig.java` |
| NEW | `Messaging/EventConsumer.java` |
| NEW | `Client/AuthServiceClient.java` |
| MODIFY | `application.properties`, `.env`, `pom.xml` |

### Payment Service (6+ files)
| Action | File |
|---|---|
| NEW | `Messaging/RabbitMQConfig.java` |
| NEW | `Messaging/PaymentPublisher.java` |
| NEW | `DTOs/PaymentSuccessEvent.java` |
| MODIFY | `Services/PayMobService.java` — publish events |
| MODIFY | `Controller/PaymobController.java` — add AOP |
| MODIFY | `application.properties`, `.env`, `pom.xml` |

---

## Verification Plan

### Automated Tests
1. Build each service: `./mvnw compile` in each service directory
2. Verify no compilation errors from removed cross-DB imports
3. Run existing tests: `./mvnw test`

### Manual Verification
1. Start RabbitMQ (Docker: `docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:management`)
2. Start all services in order: Auth → Event → Notification → Registration → Payment
3. Test Auth: `GET /api/auth/users/1` returns user data
4. Test Registration: `POST /api/registrations/register` calls Auth + Event APIs
5. Test Event creation publishes to RabbitMQ → Notification creates record
6. Test Payment callback publishes `payment.success` → Registration status updated to PAID
7. Verify `application-prod.properties` profile works with `SPRING_PROFILES_ACTIVE=prod`
