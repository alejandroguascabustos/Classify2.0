package com.classify20.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "classify.db")
public class ClassifyDatabaseProperties {

    private String url = "jdbc:postgresql://localhost:5432/classify";
    private String username = "postgres";
    private String password = "postgres";
    private String fallbackUrl = "jdbc:h2:file:./.classify-data/classify;MODE=PostgreSQL;AUTO_SERVER=TRUE;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
    private String fallbackUsername = "sa";
    private String fallbackPassword = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFallbackUrl() {
        return fallbackUrl;
    }

    public void setFallbackUrl(String fallbackUrl) {
        this.fallbackUrl = fallbackUrl;
    }

    public String getFallbackUsername() {
        return fallbackUsername;
    }

    public void setFallbackUsername(String fallbackUsername) {
        this.fallbackUsername = fallbackUsername;
    }

    public String getFallbackPassword() {
        return fallbackPassword;
    }

    public void setFallbackPassword(String fallbackPassword) {
        this.fallbackPassword = fallbackPassword;
    }
}
