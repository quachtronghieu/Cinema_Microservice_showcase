package vn.edu.fpt.cinema.cinema.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CatalogMovieClient {

    private final RestClient restClient;

    /** Spring Boot's builder is the one that sends traceparent; RestClient.builder() would break the trace here. */
    public CatalogMovieClient(@Value("${services.catalog.url:http://localhost:8082}") String catalogBaseUrl,
                              RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(catalogBaseUrl)
                .build();
    }

    public MovieMetadataDTO getMovieById(String movieId) {
        try {
            return restClient.get()
                    .uri("/api/v1/movies/{id}", movieId)
                    .retrieve()
                    .body(MovieMetadataDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    public record MovieMetadataDTO(
        String movieId,
        String title,
        int duration,
        String genre,
        String img,
        String ageRating
    ) {}
}
