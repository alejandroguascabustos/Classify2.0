package com.classify20.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class ClassifyDataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ClassifyDataSourceConfig.class);

    @Bean
    @Primary
    public DataSource classifyDataSource(ClassifyDatabaseProperties properties) {
        ConnectionTarget target = resolveTarget(properties);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(target.url());
        dataSource.setUsername(target.username());
        dataSource.setPassword(target.password());
        dataSource.setDriverClassName(resolveDriverClassName(target.url()));
        dataSource.setPoolName(target.fallback() ? "ClassifyH2Pool" : "ClassifyPrimaryPool");
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(1);

        logger.info("JPA usara la base de datos {}: {}", target.fallback() ? "de respaldo" : "principal", target.url());
        return dataSource;
    }

    private ConnectionTarget resolveTarget(ClassifyDatabaseProperties properties) {
        ConnectionTarget primary = new ConnectionTarget(
                properties.getUrl(),
                properties.getUsername(),
                properties.getPassword(),
                false
        );

        if (canConnect(primary)) {
            return primary;
        }

        ConnectionTarget fallback = new ConnectionTarget(
                properties.getFallbackUrl(),
                properties.getFallbackUsername(),
                properties.getFallbackPassword(),
                true
        );

        if (canConnect(fallback)) {
            return fallback;
        }

        throw new IllegalStateException("No fue posible conectar ni con la base principal ni con la base de respaldo.");
    }

    private boolean canConnect(ConnectionTarget target) {
        try {
            Class.forName(resolveDriverClassName(target.url()));
            try (Connection ignored = DriverManager.getConnection(target.url(), target.username(), target.password())) {
                return true;
            }
        } catch (ClassNotFoundException | SQLException exception) {
            logger.warn("No fue posible conectar con {}.", target.url());
            return false;
        }
    }

    private String resolveDriverClassName(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        }
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            return "org.h2.Driver";
        }
        if (jdbcUrl.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        }
        throw new IllegalArgumentException("No se reconoce el driver para la URL: " + jdbcUrl);
    }

    private record ConnectionTarget(String url, String username, String password, boolean fallback) {
    }
}
