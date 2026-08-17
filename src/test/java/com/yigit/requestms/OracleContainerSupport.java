package com.yigit.requestms;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.oracle.OracleContainer;

// A container of its own rather than the shared company schema. Locking tests
// hold rows on purpose, and one that ends badly can leave a row locked; doing
// that where other people work is not a risk worth taking.
//
// The container is static and never stopped: it is reused across every test
// class that extends this one, and Ryuk removes it when the JVM exits. Starting
// Oracle costs most of a minute, so starting it once matters.
public abstract class OracleContainerSupport {

    static final OracleContainer ORACLE = new OracleContainer("gvenzl/oracle-free:latest")
            .withUsername("rms")
            .withPassword("rms");

    static {
        ORACLE.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ORACLE::getJdbcUrl);
        registry.add("spring.datasource.username", ORACLE::getUsername);
        registry.add("spring.datasource.password", ORACLE::getPassword);

        // The container starts empty, so Hibernate builds the schema here.
        // Everywhere else this stays at validate: the DDL scripts are the
        // authority and Hibernate only checks the entities agree with them.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}