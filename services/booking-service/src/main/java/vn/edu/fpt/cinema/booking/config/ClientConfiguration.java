package vn.edu.fpt.cinema.booking.config;

import java.time.Clock;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.fpt.cinema.booking.client.CatalogClient;

@Configuration
@EnableFeignClients(clients = CatalogClient.class)
public class ClientConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }
}
