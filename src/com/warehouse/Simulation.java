package com.warehouse;

import com.warehouse.model.*;
import com.warehouse.utils.EventCalendar;
import com.warehouse.utils.Statistics;
import com.warehouse.enums.EventType;
import java.util.*;

public class Simulation {
    private double currentTime;
    private final WarehouseDispatcher dispatcher;
    private final EventCalendar eventCalendar;
    private final Statistics statistics;
    private static Simulation instance;

    public Simulation() {
        this.currentTime = 0;
        this.dispatcher = new WarehouseDispatcher();
        this.eventCalendar = new EventCalendar();
        this.statistics = new Statistics();
        instance = this;
    }

    public void runStepByStep(Scanner scanner) {
        System.out.println("🏃 ЗАПУСК ПОШАГОВОГО РЕЖИМА");

        // ШАГ 1: Показываем пустую систему
        System.out.println("\n>>> НАЧАЛЬНОЕ СОСТОЯНИЕ: СИСТЕМА ПУСТА");
        eventCalendar.printStepAndWait(this, null);

        // ШАГ 2: Генерируем заявки
        System.out.println("\n>>>  ГЕНЕРАЦИЯ ЗАЯВОК НА ВСЕХ ИСТОЧНИКАХ");
        generateRequestsOnAllSources();

        // ШАГ 3: Обрабатываем события
        while (currentTime < 24 * 60 && !eventCalendar.isEmpty()) {
            Event nextEvent = eventCalendar.getNextEvent();
            if (nextEvent == null) break;

            currentTime = nextEvent.getTime();

            // Обрабатываем ТОЛЬКО одно событие
            processEvent(nextEvent);

            // Показываем состояние системы
            boolean continueSimulation = eventCalendar.printStepAndWait(this, nextEvent);
            if (!continueSimulation) break;
        }

        System.out.println("\n СИМУЛЯЦИЯ ЗАВЕРШЕНА");
        eventCalendar.generateDetailedReport();
        generateReport();
    }

    // МЕТОД: генерация на всех источниках
    private void generateRequestsOnAllSources() {
        for (Source source : dispatcher.getSources()) {
            double firstArrivalTime = currentTime + source.generateInterArrivalTime();
            eventCalendar.scheduleEvent(new Event(firstArrivalTime, EventType.ARRIVAL,
                    source, String.format("Первая заявка от источника %d", source.getId())));

            source.setNextGenerationTime(firstArrivalTime);

            System.out.printf(">>> Источник %d: заявка запланирована на время %.2f%n",
                    source.getId(), firstArrivalTime);
        }
    }

    private String getEventDescription(Event event) {
        switch (event.getType()) {
            case ARRIVAL:
                return "Прибытие заявки";
            case SERVICE_COMPLETE:
                return "Завершение обслуживания";
            case BUFFER_ADD:
                return "Добавление в буфер";
            case BUFFER_REMOVE:
                return "Удаление из буфера";
            case REJECTION:
                return "Отказ заявке";
            case BUFFER_EVICTION:
                return "Выбивание из буфера";
            default:
                return event.getType().getDescription();
        }
    }

    private void processEvent(Event event) {
        System.out.printf("\n>>> СОБЫТИЕ: %s | %s%n",
                event.getType().getDescription(), event.getDescription());

        switch (event.getType()) {
            case ARRIVAL:
                processArrival(event);
                break;
            case SERVICE_COMPLETE:
                processServiceComplete(event);
                break;
        }
    }

    private void processArrival(Event event) {
        Source source = (Source) event.getSource();

        Request request = source.generateRequest(currentTime);
        statistics.recordArrival(request);

        dispatcher.processArrival(request, currentTime);

        scheduleNextArrival(source);
    }

    private void processServiceComplete(Event event) {
        Device device = (Device) event.getSource();
        Request completedRequest = device.finishService();
        statistics.recordServiceCompletion(completedRequest, currentTime);
        dispatcher.handleDeviceReleased(device);
    }

    private void scheduleNextArrival(Source source) {
        double nextArrivalTime = currentTime + source.generateInterArrivalTime();
        eventCalendar.scheduleEvent(new Event(nextArrivalTime, EventType.ARRIVAL,
                source, String.format("Прибытие от источника %d", source.getId())));

        source.setNextGenerationTime(nextArrivalTime);
    }

    public void run(double simulationTime) {
        // Для автоматического режима генерируем заявки на всех источниках
        generateRequestsOnAllSources();

        while (currentTime < simulationTime && !eventCalendar.isEmpty()) {
            Event nextEvent = eventCalendar.getNextEvent();
            currentTime = nextEvent.getTime();
            processEventSilent(nextEvent);
        }
    }

    private void processEventSilent(Event event) {
        switch (event.getType()) {
            case ARRIVAL: {
                Source source = (Source) event.getSource();
                Request request = source.generateRequest(currentTime);
                statistics.recordArrival(request);
                dispatcher.processArrival(request, currentTime);
                scheduleNextArrival(source);
                break;
            }
            case SERVICE_COMPLETE: {
                Device device = (Device) event.getSource();
                Request completedRequest = device.finishService();
                statistics.recordServiceCompletion(completedRequest, currentTime);
                dispatcher.handleDeviceReleased(device);
                break;
            }
        }
    }

    public void generateReport() {
        statistics.generateReport();
    }

    // Getters
    public static Simulation getInstance() {
        return instance;
    }

    public EventCalendar getEventCalendar() {
        return eventCalendar;
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public WarehouseDispatcher getDispatcher() {
        return dispatcher;
    }
}