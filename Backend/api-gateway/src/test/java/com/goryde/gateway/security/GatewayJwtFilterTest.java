package com.goryde.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayJwtFilterTest {
    private GatewayFilterChain chain;
    private GatewayJwtFilter filter;

    @BeforeEach
    void setUp() {
        WebClient.Builder builder = mock(WebClient.Builder.class);
        when(builder.baseUrl("http://AUTH-SERVICE")).thenReturn(builder);
        when(builder.build()).thenReturn(mock(WebClient.class));
        filter = new GatewayJwtFilter(builder);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());
    }

    @Test
    void rejectsMissingTokenForProtectedRoute() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/rides").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void allowsPublicAuthEndpointsWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login").build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void allowsOptionsPreflightWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/rides")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void blocksInternalEndpointsAtGateway() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/internal/rides/1/payment-completed").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(chain, never()).filter(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acceptsValidTokenAndAddsTrustedIdentityHeaders() {
        filter = new GatewayJwtFilter(token -> Mono.just(new AuthValidationResponse(7L, "driver@example.com", "DRIVER")));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/rides/10")
                        .header("Authorization", "Bearer valid-token")
                        .build());

        filter.filter(exchange, chain).block();

        var forwarded = org.mockito.ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(forwarded.capture());
        assertThat(forwarded.getValue().getRequest().getHeaders().getFirst("X-Authenticated-User-Id"))
                .isEqualTo("7");
    }

    @Test
    void rejectsInvalidOrExpiredToken() {
        filter = new GatewayJwtFilter(token -> Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/rides").header("Authorization", "Bearer expired").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsPassengerFromDriverRoute() {
        filter = new GatewayJwtFilter(token -> Mono.just(new AuthValidationResponse(7L, "passenger@example.com", "PASSENGER")));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/drivers/7").header("Authorization", "Bearer valid").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void allowsDriverOnDriverRidesRoute() {
        filter = new GatewayJwtFilter(token -> Mono.just(new AuthValidationResponse(14L, "driver@example.com", "DRIVER")));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/rides/driver/my-rides")
                        .header("Authorization", "Bearer valid").build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsPassengerFromDriverRidesRoute() {
        filter = new GatewayJwtFilter(token -> Mono.just(new AuthValidationResponse(7L, "passenger@example.com", "PASSENGER")));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/rides/driver/my-rides")
                        .header("Authorization", "Bearer valid").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void allowsPassengerOnPassengerRidesRoute() {
        filter = new GatewayJwtFilter(token -> Mono.just(new AuthValidationResponse(7L, "passenger@example.com", "PASSENGER")));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/rides/my-rides")
                        .header("Authorization", "Bearer valid").build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(org.mockito.ArgumentMatchers.any());
    }
}
