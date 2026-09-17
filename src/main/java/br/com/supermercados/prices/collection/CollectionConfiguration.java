package br.com.supermercados.prices.collection;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import br.com.supermercados.prices.collection.nagumo.NagumoProperties;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(NagumoProperties.class)
public class CollectionConfiguration {
}
