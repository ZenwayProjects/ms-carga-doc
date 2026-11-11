package co.com.bancolombia.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class ApiRestTest {

    private WebTestClient client;

    @BeforeEach
    void setup() {
        client = WebTestClient.bindToController(new ApiRest()).build();
    }

    @Test
    void shouldReturnValueFromUseCaseVersion1() {
        client.get()
                .uri("/api/usecase/path")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(String.class)
                .isEqualTo("Value from use case v1");
    }


    @Test
    void shouldReturnNotFoundForInvalidPath() {
        client.get()
                .uri("/api/usecase/invalid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }
}
