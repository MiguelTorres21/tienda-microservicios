package com.tienda.inventory.integration;

import org.testcontainers.containers.PostgreSQLContainer;

public final class SharedPostgresContainer {

    private static final PostgreSQLContainer<?> INSTANCE;

    static {
        INSTANCE = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("inventory_test")
                .withUsername("test_user")
                .withPassword("test_pass");
        INSTANCE.start();
    }

    private SharedPostgresContainer() {
    }

    public static PostgreSQLContainer<?> getInstance() {
        return INSTANCE;
    }
}
