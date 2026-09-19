package br.com.supermercados.prices.collection.flyer;

import java.math.BigDecimal;
import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.supermercados.prices.collection.CollectedStore;
import tools.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnProperty(name = "app.collection.reviewed-flyers.enabled", havingValue = "true", matchIfMissing = true)
public class ReviewedFlyerConfiguration {

    @Bean
    ReviewedAtacadaoFlyerCollector atacadaoSaoGeraldo(ObjectMapper mapper, Clock clock) {
        return new ReviewedAtacadaoFlyerCollector(815, "atacadao_sao_geraldo_flyer",
                new CollectedStore("Atacadão São Geraldo",
                        "Rodovia dos Metalúrgicos, 1085 - São Geraldo - Volta Redonda/RJ",
                        new BigDecimal("-22.521388"), new BigDecimal("-44.080095"), true), mapper, clock);
    }

    @Bean
    ReviewedAtacadaoFlyerCollector atacadaoVilaRica(ObjectMapper mapper, Clock clock) {
        return new ReviewedAtacadaoFlyerCollector(289, "atacadao_vila_rica_flyer",
                new CollectedStore("Atacadão Vila Rica",
                        "Avenida Dois, 10 - Jardim Vila Rica - Volta Redonda/RJ",
                        new BigDecimal("-22.54283"), new BigDecimal("-44.07364"), true), mapper, clock);
    }
}
