package br.com.supermercados.prices.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CollectionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionScheduler.class);

    private final CollectionCoordinator coordinator;
    private final boolean enabled;

    public CollectionScheduler(
            CollectionCoordinator coordinator,
            @Value("${app.collection.enabled:true}") boolean enabled) {
        this.coordinator = coordinator;
        this.enabled = enabled;
    }

    @Scheduled(
            cron = "${app.collection.cron:0 30 5 * * *}",
            zone = "${app.collection.zone:America/Sao_Paulo}")
    public void collectDaily() {
        if (!enabled) {
            return;
        }
        try {
            coordinator.collectAll();
        } catch (RuntimeException exception) {
            LOGGER.error("Não foi possível iniciar a coleta agendada: {}", exception.getMessage(), exception);
        }
    }
}
