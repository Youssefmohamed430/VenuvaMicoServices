# Design Patterns Implementation Plan

Excellent choices! From your list, I will apply the **Singleton Pattern** and the **Builder Pattern** to address the flaws in the API Gateway. We will also include the fix for the CORS `OPTIONS` bug to ensure cross-origin requests don't fail.

## 1. Singleton Pattern (JWT Utility)
**The Problem:** Currently, the `JwtAuthenticationFilter` re-creates the cryptographic HMAC key (`Keys.hmacShaKeyFor(...)`) on *every single request* during token validation. This is highly inefficient in a high-traffic gateway.
**The Solution:** We will implement a `JwtUtil` class using the **Singleton Pattern** (managed by Spring). It will instantiate the secret key exactly once in memory upon startup and reuse it for all incoming requests, significantly optimizing performance and separating the validation logic from the routing filter.

## 2. Builder Pattern (Structured Error Responses)
**The Problem:** When an unhandled request occurs (like hitting an undefined function/route) or authentication fails, the gateway returns an empty body or HTML. This crashes the frontend.
**The Solution:** We will implement an `ErrorResponse` class utilizing the **Builder Pattern**. We will also create a Global Exception Handler. When an error occurs, the Gateway will use the Builder Pattern to flexibly construct a standardized JSON response (step-by-step: adding the timestamp, HTTP status, path, and error message). This ensures the frontend always receives predictable JSON.

## Proposed Changes

### `api-gateway`

#### [NEW] [JwtUtil.java](file:///home/yusef/Desktop/VenuvaMicoServices/api-gateway/src/main/java/com/example/gateway/config/JwtUtil.java)
- Implement the **Singleton Pattern** to initialize the secret key once.
- Move the `validateToken` method here from the filter.

#### [NEW] [ErrorResponse.java](file:///home/yusef/Desktop/VenuvaMicoServices/api-gateway/src/main/java/com/example/gateway/config/ErrorResponse.java)
- Implement the **Builder Pattern** for this class, allowing flexible creation of error payloads (e.g., `ErrorResponse.builder().status(404).message("Not Found").build()`).

#### [NEW] [GatewayExceptionHandler.java](file:///home/yusef/Desktop/VenuvaMicoServices/api-gateway/src/main/java/com/example/gateway/config/GatewayExceptionHandler.java)
- Create the global error handler that catches routing errors and utilizes the `ErrorResponse` Builder to return JSON.

#### [MODIFY] [JwtAuthenticationFilter.java](file:///home/yusef/Desktop/VenuvaMicoServices/api-gateway/src/main/java/com/example/gateway/config/JwtAuthenticationFilter.java)
- Update the filter to inject and use the `JwtUtil` Singleton.
- Fix the bug where CORS `OPTIONS` preflight requests were being blocked.

## Verification Plan
1. Test token validation: Ensure requests are successfully validated using the new Singleton utility.
2. Send an `OPTIONS` request: Verify it returns `200 OK` without requiring an Authorization header.
3. Hit an undefined route: Verify the Builder Pattern correctly generates a structured `404 Not Found` JSON response.
