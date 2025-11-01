package com.wei.orchestrator.observation.domain.model.valueobject;

import java.util.Objects;

public class SourceEndpoint {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public SourceEndpoint(String jdbcUrl, String username, String password) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("JDBC URL cannot be null or empty");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceEndpoint that = (SourceEndpoint) o;
        return Objects.equals(jdbcUrl, that.jdbcUrl) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jdbcUrl, username);
    }

    @Override
    public String toString() {
        return "SourceEndpoint{"
                + "jdbcUrl='"
                + jdbcUrl
                + '\''
                + ", username='"
                + username
                + '\''
                + ", databaseType=ORACLE"
                + '}';
    }
}
