package com.warehouse.enums;

public enum CargoType {
    PERISHABLE("Скоропортящийся", 15),    // 1 час дедлайн
    REGULAR("Обычный", 20);             // 4 часа дедлайн

    private final String description;
    private final int deadlineMinutes;

    CargoType(String description, int deadlineMinutes) {
        this.description = description;
        this.deadlineMinutes = deadlineMinutes;
    }

    public String getDescription() { return description; }
    public int getDeadlineMinutes() { return deadlineMinutes; }
}