package com.warehouse.model;

import com.warehouse.enums.EventType;

public class Event implements Comparable<Event> {
    private final double time;
    private final EventType type;
    private final Object source;
    private final String description;

    public Event(double time, EventType type, Object source, String description) {
        this.time = time;
        this.type = type;
        this.source = source;
        this.description = description;
    }

    @Override
    public int compareTo(Event other) {
        return Double.compare(this.time, other.time);
    }

    // Getters
    public double getTime() { return time; }
    public EventType getType() { return type; }
    public Object getSource() { return source; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("Время: %.2f | %s | %s", time, type.getDescription(), description);
    }
}