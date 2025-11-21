package com.warehouse.enums;

public enum EventType {
    ARRIVAL("Прибытие заявки"),
    GENERATED("Заявка сгенерирована"),
    SENT_TO_BUFFER("Заявка отправлена в буфер"),
    SENT_TO_DEVICE("Заявка отправлена на прибор"),
    BUFFER_EVICTION("Выбивание заявки из буфера"), // Используем только этот термин
    SELECTED_FOR_PROCESSING("Выбор заявки на обработку"),
    SENT_FOR_PROCESSING("Отправка заявки на обработку"),
    SERVICE_COMPLETE("Завершение обслуживания"),
    BUFFER_REMOVE("Удаление из буфера"), // и освобождение места
    REJECTION("Отказ заявке"),
    DEADLINE_CHECK("Проверка дедлайнов"),
    SERVICE_START("Начало обслуживания"),
    BUFFER_ADD("Добавление в буфер");


    private final String description;

    EventType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}