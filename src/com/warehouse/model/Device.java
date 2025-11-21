package com.warehouse.model;

import com.warehouse.enums.RequestStatus;
import com.warehouse.Simulation;
import com.warehouse.enums.EventType;
import java.util.*;

public class Device {
    private final int id;
    private final int priority;
    private final int capacity;
    private final List<Request> currentRequests = new ArrayList<>();
    private final double minServiceTime;
    private final double maxServiceTime;
    private int processedCount;
    private double currentJobEndTime; // ДОБАВЛЕНО

    public Device(int id, int priority, int capacity, double minServiceTime, double maxServiceTime) {
        this.id = id;
        this.priority = priority;
        this.capacity = capacity;
        this.minServiceTime = minServiceTime;
        this.maxServiceTime = maxServiceTime;
        this.currentJobEndTime = Double.POSITIVE_INFINITY; // ДОБАВЛЕНО
    }

    public boolean startService(Request request, double currentTime) {
        if (!isFree()) {
            return false;
        }

        double serviceTime = generateServiceTime();
        currentRequests.add(request);

        // Устанавливаем время окончания работы
        this.currentJobEndTime = currentTime + serviceTime;

        // Создаем событие завершения обслуживания
        Simulation.getInstance().getEventCalendar().scheduleEvent(
                new Event(currentTime + serviceTime, EventType.SERVICE_COMPLETE, this,
                        String.format("Завершение обслуживания заявки %d на приборе %d", request.getId(), id))
        );

        request.setStatus(RequestStatus.IN_SERVICE);
        request.setServiceStartTime(currentTime);

        return true;
    }

    public Integer getCurrentRequestId() {
        return currentRequests.isEmpty() ? null : currentRequests.get(0).getId();
    }

    public Request finishService() {
        return finishService(Simulation.getInstance().getCurrentTime());
    }

    public Request finishService(double currentTime) {
        if (currentRequests.isEmpty()) return null;

        Request finishedRequest = currentRequests.remove(0);
        finishedRequest.setStatus(RequestStatus.COMPLETED);
        finishedRequest.setServiceEndTime(currentTime);
        processedCount++;

        // ДОБАВЛЕНО: сбрасываем время окончания работы
        this.currentJobEndTime = Double.POSITIVE_INFINITY;

        System.out.printf(">>> Заявка %d завершила обслуживание на приборе %d%n",
                finishedRequest.getId(), id);

        return finishedRequest;
    }

    public boolean isFree() {
        return currentRequests.size() < capacity;
    }

    private double generateServiceTime() {
        return minServiceTime + Math.random() * (maxServiceTime - minServiceTime);
    }

    // ДОБАВЛЕННЫЙ МЕТОД
    public double getCurrentJobEndTime() {
        return currentJobEndTime;
    }

    public void displayState(double currentTime) {
        String status = isFree() ? "ЕСТЬ СВОБОДНЫЕ МЕСТА" : "ЗАПОЛНЕН";
        System.out.printf("    Прибор %d (приоритет %d): %s (%d/%d)%n",
                id, priority, status, currentRequests.size(), capacity);
        System.out.printf("      Обработано всего: %d%n", processedCount);
    }

    // Getters
    public int getId() { return id; }
    public int getPriority() { return priority; }
    public int getCapacity() { return capacity; }
    public int getProcessedCount() { return processedCount; }
    public List<Request> getCurrentRequests() { return currentRequests; }
}