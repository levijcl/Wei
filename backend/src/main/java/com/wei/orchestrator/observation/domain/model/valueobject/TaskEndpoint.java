package com.wei.orchestrator.observation.domain.model.valueobject;

import java.util.Objects;

public class TaskEndpoint {
    private final String url;
    private final String authToken;

    public TaskEndpoint(String url, String authToken) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (authToken == null || authToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Auth token cannot be null or empty");
        }
        this.url = url;
        this.authToken = authToken;
    }

    public String getUrl() {
        return url;
    }

    public String getAuthToken() {
        return authToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskEndpoint that = (TaskEndpoint) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "TaskEndpoint{" + "url='" + url + '\'' + '}';
    }
}
