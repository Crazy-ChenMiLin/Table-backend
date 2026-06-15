package org.example.demo1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String baseUrl;
    private String apiKey;
    private String model;
    private double temperature = 0.0;
    private int timeoutSeconds = 180;
    private int maxRetries = 1;
    private int maxImageSizeMb = 5;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model == null ? null : model.trim();
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getMaxImageSizeMb() {
        return maxImageSizeMb;
    }

    public void setMaxImageSizeMb(int maxImageSizeMb) {
        this.maxImageSizeMb = maxImageSizeMb;
    }
}
