package mate.academy.accommodationbooking.service.config;

import org.testcontainers.containers.MariaDBContainer;

public class CustomMariaDBContainer extends MariaDBContainer<CustomMariaDBContainer> {
    private static final String DB_IMAGE = "mariadb:latest";

    private static CustomMariaDBContainer mysqlContainer;

    private CustomMariaDBContainer() {
        super(DB_IMAGE);
    }

    public static synchronized CustomMariaDBContainer getInstance() {
        if (mysqlContainer == null) {
            mysqlContainer = new CustomMariaDBContainer();
        }
        return mysqlContainer;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("TEST_DB_URL", mysqlContainer.getJdbcUrl());
        System.setProperty("TEST_DB_USERNAME", mysqlContainer.getUsername());
        System.setProperty("TEST_DB_PASSWORD", mysqlContainer.getPassword());
    }

    @Override
    public void stop() {
    }
}
