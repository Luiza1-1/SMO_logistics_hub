package com.warehouse.model;

import com.warehouse.enums.CargoType;
import com.warehouse.enums.RequestStatus;
import java.util.*;

public class Buffer {
    private final int capacity;
    private final List<Request> requests = new ArrayList<>();
    private final CargoType bufferType;
    private final Set<Integer> freePositions = new HashSet<>();

    public Buffer(int capacity, CargoType bufferType) {
        this.capacity = capacity;
        this.bufferType = bufferType;

        for (int i = 1; i <= capacity; i++) {
            freePositions.add(i);
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean addRequest(Request request) {
        if (requests.size() >= capacity) {
            return false;
        }

        int position = freePositions.stream().findFirst().orElse(-1);
        if (position != -1) {
            request.setBufferPosition(position);
            request.setBufferDeadline();
            requests.add(request);
            freePositions.remove(position);
            return true;
        }
        return false;
    }

    public List<Request> getRequestsSnapshot() {
        return new ArrayList<>(requests);
    }

    // ДЛЯ ОСВОБОЖДЕНИЯ ПРИБОРА - берем ПОСЛЕДНЮЮ заявку (LIFO)
    public Request getLastRequestForDevice() {
        if (requests.isEmpty()) return null;

        Request lastRequest = requests.get(requests.size() - 1); // Последняя заявка в списке
        System.out.printf(">>> Взята ПОСЛЕДНЯЯ заявка %d (LIFO) из буфера%n", lastRequest.getId());
        return lastRequest;
    }

    // ДЛЯ ВЫТЕСНЕНИЯ - ищем ПРОСРОЧЕННУЮ заявку
    public Request findExpiredRequest(double currentTime) {
        Request expiredRequest = requests.stream()
                .filter(request -> request.isDeadlineExceeded(currentTime))
                .findFirst()
                .orElse(null);

        if (expiredRequest != null) {
            System.out.printf(">>> Найдена ПРОСРОЧЕННАЯ заявка %d (дедлайн: %.2f, текущее время: %.2f)%n",
                    expiredRequest.getId(), expiredRequest.getDeadline(), currentTime);
        } else {
            System.out.println(">>> Просроченных заявок не найдено");
        }
        return expiredRequest;
    }

    // СТАРЫЙ МЕТОД для совместимости
    public Request getNextRequestForDevice() {
        return getLastRequestForDevice(); // По умолчанию используем LIFO
    }

    public boolean removeRequest(Request request) {
        boolean removed = requests.remove(request);
        if (removed) {
            request.clearDeadline(); // Сбрасываем дедлайн при извлечении
            freePositions.add(request.getBufferPosition());
            System.out.printf(">>> Заявка %d удалена из буфера%n", request.getId());
        }
        return removed;
    }

    public Request getLastRequest() {
        if (requests.isEmpty()) return null;

        Request lastRequest = requests.get(requests.size() - 1);
        System.out.printf(">>> Взята последняя заявка %d (LIFO)%n", lastRequest.getId());
        return lastRequest;
    }

    public List<Request> getRequests() {
        return new ArrayList<>(requests);
    }

    public Request getOldestRequest() {
        if (requests.isEmpty()) return null;

        Request oldestRequest = requests.get(0);
        System.out.printf(">>> Найдена самая старая заявка %d (время прибытия: %.2f)%n",
                oldestRequest.getId(), oldestRequest.getArrivalTime());
        return oldestRequest;
    }

    public void displayState() {
        System.out.printf("\n СОСТОЯНИЕ БУФЕРА (%s):%n", bufferType.getDescription());
        System.out.printf("  Загруженность: %d/%d (%.1f%%)%n",
                requests.size(), capacity, getLoadFactor() * 100);

        if (requests.isEmpty()) {
            System.out.println("  [Буфер пуст]");
        } else {
            requests.forEach(request -> {
                String deadlineInfo = request.hasDeadline() ?
                        String.format("Дедлайн: %.2f", request.getDeadline()) : "Нет дедлайна";
                System.out.printf("  Заявка %d | Поз.%d | %s | Статус: %s%n",
                        request.getId(), request.getBufferPosition(),
                        deadlineInfo, request.getStatus().getDescription());
            });
        }
    }

    public boolean hasFreeSpace() { return requests.size() < capacity; }
    public boolean isEmpty() { return requests.isEmpty(); }
    public int getRequestCount() { return requests.size(); }
    public double getLoadFactor() { return (double) requests.size() / capacity; }
    public CargoType getBufferType() { return bufferType; }
}