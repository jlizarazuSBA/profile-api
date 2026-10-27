package dev.lizarazu.profileapi.sam;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class SamEntityClient {

    private final RestClient restClient;
    private final String apiKey;

    public SamEntityClient(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public SamEntityResponse findByUei(String uei) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/entity-information/v3/entities")
                        .queryParam("api_key", apiKey)
                        .queryParam("ueiSAM", uei)
                        .queryParam("includeSections", "entityRegistration")
                        .queryParam("sensitivity", "public")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(SamEntityResponse.class);
    }
}