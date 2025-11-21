package com.warehouse.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SimulationConfig {
    private final Properties properties;

    public SimulationConfig() {
        this.properties = new Properties();
        loadProperties();
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                setDefaultProperties();
            }
        } catch (IOException e) {
            setDefaultProperties();
        }
    }

    private void setDefaultProperties() {
        properties.setProperty("simulation.duration", "1440");
        properties.setProperty("source.lambda", "0.3");
        properties.setProperty("buffer.perishable.capacity", "5");
        properties.setProperty("buffer.regular.capacity", "8");
        properties.setProperty("device1.minServiceTime", "15");
        properties.setProperty("device1.maxServiceTime", "45");
        properties.setProperty("device2.minServiceTime", "20");
        properties.setProperty("device2.maxServiceTime", "60");
    }
}