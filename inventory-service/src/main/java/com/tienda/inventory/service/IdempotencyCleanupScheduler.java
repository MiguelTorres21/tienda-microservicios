package com.tienda.inventory.service;

import com.tienda.inventory.repository.IdempotencyRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Component
public class IdempotencyCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupScheduler.class);

    private final IdempotencyRecordRepository repository;

    /**
     * @param repository
     */
    public IdempotencyCleanupScheduler(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedRateString = "PT1H")
    @Transactional
    public void cleanup() {
        int deleted = repository.deleteExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Limpieza idempotencia: {} registros expirados eliminados", deleted);
        }
    }
}
