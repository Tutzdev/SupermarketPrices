package br.com.supermercados.prices.collection.mercafacil;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.supermercados.prices.collection.SupermarketCollector;
import tools.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnProperty(name = "app.collection.regional.enabled", havingValue = "true", matchIfMissing = true)
public class MercafacilConfiguration {

    @Bean
    SupermarketCollector pameVoltaRedonda(ObjectMapper mapper, Clock clock) {
        return new MercafacilCollector(new MercafacilStoreDefinition("pame_santo_agostinho", "Pame",
                "Pame Santo Agostinho", URI.create("https://www.pamesupermercados.com.br"), "loja-01",
                "6647c2d35aa7f3f538ab875b", "26185052000173",
                "Rua Mil e Cinquenta e Seis, 9 - Santo Agostinho - Volta Redonda/RJ"), mapper, clock, Duration.ofSeconds(1));
    }

    @Bean
    SupermarketCollector villeVoltaRedonda(ObjectMapper mapper, Clock clock) {
        return new MercafacilCollector(new MercafacilStoreDefinition("ville_sessenta", "Ville",
                "Ville Sessenta", URI.create("https://www.villesupermercado.com.br"), "loja",
                "655662fde8f60d001eedf70c", "32877857000180",
                "Rua Trinta e Cinco, 48 - Sessenta - Volta Redonda/RJ"), mapper, clock, Duration.ofSeconds(1));
    }
}
