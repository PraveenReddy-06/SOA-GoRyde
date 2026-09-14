package com.goryde.gateway.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayJwtFilter implements GlobalFilter {
    private final Function<String, Mono<AuthValidationResponse>> tokenValidator;

    @Autowired
    public GatewayJwtFilter(WebClient.Builder webClientBuilder) {
        WebClient authClient = webClientBuilder.baseUrl("http://AUTH-SERVICE").build();
        this.tokenValidator = token -> validateWithAuth(authClient, token);
    }

    GatewayJwtFilter(Function<String, Mono<AuthValidationResponse>> tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }
        if (isPublicAuthEndpoint(exchange)) {
            return chain.filter(exchange);
        }
        if (path.startsWith("/internal/")) {
            return writeError(exchange, HttpStatus.NOT_FOUND, "Not found");
        }
        if (!isProtectedPath(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String token = authorization.substring(7);
        return tokenValidator.apply(token)
            .switchIfEmpty(Mono.error(new InvalidTokenException()))
                .flatMap(identity -> {
                    if (!isAllowed(path, exchange.getRequest().getMethod().name(), identity.role())) {
                        return writeError(exchange, HttpStatus.FORBIDDEN, "Insufficient role");
                    }
                    ServerWebExchange authenticatedExchange = exchange.mutate()
                            .request(request -> request.headers(headers -> {
                                headers.remove("X-User-Id");
                                headers.remove("X-Authenticated-User-Id");
                                headers.remove("X-Authenticated-Role");
                                headers.set("X-Authenticated-User-Id", identity.userId().toString());
                                headers.set("X-Authenticated-Role", identity.role());
                            }))
                            .build();
                    return chain.filter(authenticatedExchange);
                })
                .onErrorResume(InvalidTokenException.class,
                    error -> writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token"))
                .onErrorResume(error -> writeError(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                    "Authentication service unavailable"));
    }

    private Mono<AuthValidationResponse> validateWithAuth(WebClient authClient, String token) {
        return authClient.post()
                .uri("/internal/auth/validate")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(token)
                .retrieve()
                .onStatus(status -> status.value() == 401,
                        response -> Mono.error(new InvalidTokenException()))
                .bodyToMono(AuthValidationResponse.class);
    }

    private boolean isPublicAuthEndpoint(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        return "POST".equals(exchange.getRequest().getMethod().name())
                && ("/api/auth/register".equals(path) || "/api/auth/login".equals(path));
    }

    private boolean isProtectedPath(String path) {
        return path.startsWith("/api/rides/") || "/api/rides".equals(path)
                || path.startsWith("/api/drivers/") || "/api/drivers".equals(path)
                || path.startsWith("/api/payments/")
                || "/api/payments".equals(path)
                || "/api/auth/me".equals(path);
    }

    private static class InvalidTokenException extends RuntimeException {
    }

    private boolean isAllowed(String path, String method, String role) {
        if ("ADMIN".equals(role)) {
            return true;
        }
        if (path.startsWith("/api/drivers/") || "/api/drivers".equals(path)) {
            return "DRIVER".equals(role);
        }
        if (path.startsWith("/api/payments/")) {
            return "PASSENGER".equals(role);
        }
        if ("/api/auth/me".equals(path)) {
            return true;
        }
        if (path.startsWith("/api/rides/driver/")) {
            return "DRIVER".equals(role);
        }
        if (path.endsWith("/start") || path.endsWith("/complete")) {
            return "DRIVER".equals(role);
        }
        if ("GET".equals(method) && path.matches("/api/rides/\\d+")) {
            return "PASSENGER".equals(role) || "DRIVER".equals(role);
        }
        return "PASSENGER".equals(role);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"status\":" + status.value() + ",\"error\":\"" + message + "\"}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
