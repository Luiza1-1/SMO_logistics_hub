package com.warehouse.enums;

public enum RequestStatus {
    ARRIVED("Прибыла"),
    IN_QUEUE("В очереди"),
    IN_SERVICE("На обслуживании"),
    COMPLETED("Обслужена"),
    REJECTED("Отказано"),
    EVICTED("Вытеснена");

    private final String description;

    RequestStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}