package com.warehouse.model;

import com.warehouse.enums.CargoType;
import com.warehouse.enums.RequestStatus;
import com.warehouse.Simulation;
import com.warehouse.utils.Statistics;
import com.warehouse.enums.EventType;
import java.util.*;

public class WarehouseDispatcher {
    private final Buffer bufferPerishable;
    private final Buffer bufferRegular;
    private final List<Device> devicesP1 = new ArrayList<>(); // Приборы для P1
    private final List<Device> devicesP2 = new ArrayList<>(); // Приборы для P2
    private final List<Source> sources = new ArrayList<>();

    public WarehouseDispatcher() {
        this.bufferPerishable = new Buffer(8, CargoType.PERISHABLE);
        this.bufferRegular = new Buffer(10, CargoType.REGULAR);

        // Создаем приборы для P1 (скоропортящиеся)
        devicesP1.add(new Device(1, 1, 1, 5, 10));
        devicesP1.add(new Device(2, 1, 1, 5, 10));

        // Создаем приборы для P2 (обычные)
        devicesP2.add(new Device(3, 2, 1, 8, 15));
        devicesP2.add(new Device(4, 2, 1, 8, 15));

        this.sources.add(new Source(1, 0.5));
        this.sources.add(new Source(2, 0.4));
        this.sources.add(new Source(3, 0.5));
    }

    // В WarehouseDispatcher добавим метод для обработки одной заявки из буфера
    public void processNextRequestFromBuffer(double currentTime) {
        // Сначала пробуем обработать заявки из буфера P1 (высший приоритет)
        if (!bufferPerishable.isEmpty()) {
            // ИЗМЕНЕНИЕ: Берем САМУЮ СТАРУЮ заявку
            Request request = bufferPerishable.getLastRequestForDevice();
            if (request != null) {
                Device freeDevice = getFreeDevice(devicesP1);
                if (freeDevice != null) {
                    bufferPerishable.removeRequest(request);
                    freeDevice.startService(request, currentTime);
                    System.out.printf(">>> Заявка %d из буфера P1 назначена на прибор %d (FIFO)%n",
                            request.getId(), freeDevice.getId());
                    return;
                }
            }
        }

        // Затем пробуем обработать заявки из буфера P2
        if (!bufferRegular.isEmpty()) {
            // ИЗМЕНЕНИЕ: Берем САМУЮ СТАРУЮ заявку
            Request request = bufferRegular.getLastRequestForDevice();

            if (request != null) {
                Device freeDevice = getFreeDevice(devicesP2);
                if (freeDevice != null) {
                    bufferRegular.removeRequest(request);
                    freeDevice.startService(request, currentTime);
                    System.out.printf(">>> Заявка %d из буфера P2 назначена на прибор %d (FIFO)%n",
                            request.getId(), freeDevice.getId());
                    return;
                }
            }
        }
    }

    public void processArrival(Request request, double currentTime) {
        boolean isPerishable = request.getCargoType() == CargoType.PERISHABLE;
        List<Device> targetDevices = isPerishable ? devicesP1 : devicesP2;
        Buffer targetBuffer = isPerishable ? bufferPerishable : bufferRegular;

        System.out.printf(">>> ОБРАБОТКА: Заявка %d (%s) от источника %d%n",
                request.getId(), request.getCargoType().getDescription(), request.getSourceId());

        // 1. Пробуем поставить на свободный прибор
        Device freeDevice = getFreeDevice(targetDevices);
        if (freeDevice != null) {
            boolean assigned = freeDevice.startService(request, currentTime);
            if (assigned) {
                System.out.printf(">>> Заявка %d назначена на прибор %d%n",
                        request.getId(), freeDevice.getId());

                // Записываем событие начала обслуживания
                Simulation.getInstance().getEventCalendar().recordEvent(
                        currentTime, EventType.SERVICE_START, request,
                        String.format("Заявка %d начинает обслуживание на приборе %d",
                                request.getId(), freeDevice.getId())
                );
                return;
            }
        }

        // 2. Если приборы заняты - пробуем добавить в буфер
        System.out.printf(">>> Приборы заняты, пробуем добавить в буфер %s%n",
                targetBuffer.getBufferType().getDescription());

        if (targetBuffer.hasFreeSpace()) {
            boolean added = targetBuffer.addRequest(request);
            if (added) {
                request.setStatus(RequestStatus.IN_QUEUE);
                System.out.printf(">>> Заявка %d добавлена в буфер %s%n",
                        request.getId(), targetBuffer.getBufferType().getDescription());

                // Записываем событие добавления в буфер
                Simulation.getInstance().getEventCalendar().recordEvent(
                        currentTime, EventType.BUFFER_ADD, request,
                        String.format("Заявка %d добавлена в буфер %s",
                                request.getId(), targetBuffer.getBufferType().getDescription())
                );
            }
        } else {
            // 3. Если буфер полон - применяем правила вытеснения
            System.out.printf(">>> Буфер %s полон, применяем правила вытеснения%n",
                    targetBuffer.getBufferType().getDescription());
            applyD1002Rule(request, targetBuffer, currentTime, isPerishable ? 1 : 2);
        }
    }

    private Device getFreeDevice(List<Device> devices) {
        // Вместо поиска первого свободного, ищем прибор с наименьшей загрузкой
        return devices.stream()
                .filter(Device::isFree)
                .min(Comparator.comparingInt(device -> device.getCurrentRequests().size()))
                .orElse(null);
    }

    private void handleBusyDevice(Request request, Buffer buffer, double currentTime, int priority) {
        if (applyD1023Rule(request, buffer, currentTime)) {
            request.setStatus(RequestStatus.IN_QUEUE);
        } else {
            applyD1002Rule(request, buffer, currentTime, priority);
        }
    }

    public boolean applyD1023Rule(Request request, Buffer buffer, double currentTime) {
        System.out.println(">>> Применяем Д10З3: Буферизация на свободное место");
        if (buffer.hasFreeSpace()) {
            boolean added = buffer.addRequest(request);
            if (added) {
                request.setStatus(RequestStatus.IN_QUEUE);
                System.out.printf(">>> Заявка %d размещена в буфере%n", request.getId());
            }
            return added;
        } else {
            System.out.println(">>> Буфер полен - переходим к проверке дедлайнов");
            return false;
        }
    }

    public void applyD1002Rule(Request request, Buffer buffer, double currentTime, int priority) {
        System.out.println(">>> Применяем Д10ОЗ: Поиск просроченной заявки");

        // ИЗМЕНЕНИЕ: Ищем именно ПРОСРОЧЕННУЮ заявку
        Request expiredRequest = buffer.findExpiredRequest(currentTime);
        if (expiredRequest != null) {
            // СОБЫТИЕ: Выбивание заявки из буфера
            Simulation.getInstance().getEventCalendar().recordEvent(
                    currentTime, EventType.BUFFER_EVICTION, expiredRequest,
                    String.format("Заявка %d выбита из буфера (просрочена)", expiredRequest.getId())
            );

            buffer.removeRequest(expiredRequest);
            expiredRequest.setStatus(RequestStatus.EVICTED);
            Simulation.getInstance().getStatistics().recordEviction(expiredRequest, currentTime);

            // СОБЫТИЕ: Удаление из буфера
            Simulation.getInstance().getEventCalendar().recordEvent(
                    currentTime, EventType.BUFFER_REMOVE, expiredRequest,
                    String.format("Заявка %d удалена из буфера", expiredRequest.getId())
            );

            // Добавляем новую заявку в буфер
            buffer.addRequest(request);
            request.setStatus(RequestStatus.IN_QUEUE);

            // СОБЫТИЕ: Заявка отправлена в буфер (после выбивания)
            Simulation.getInstance().getEventCalendar().recordEvent(
                    currentTime, EventType.SENT_TO_BUFFER, request,
                    String.format("Заявка %d добавлена в буфер вместо выбитой", request.getId())
            );
        } else {
            Simulation.getInstance().getEventCalendar().recordEvent(
                    currentTime, EventType.REJECTION, request,
                    String.format("Заявка %d отклонена - нет свободных мест и просроченных заявок", request.getId())
            );
            request.setStatus(RequestStatus.REJECTED);
            Simulation.getInstance().getStatistics().recordRejection(request);
        }
    }

    public void handleDeviceReleased(Device device) {
        System.out.printf("\n>>> Прибор %d освободил одно место%n", device.getId());

        Buffer targetBuffer = (device.getPriority() == 1) ? bufferPerishable : bufferRegular;
        if (targetBuffer != null) {
            while (device.isFree() && !targetBuffer.isEmpty()) {
                // ИЗМЕНЕНИЕ: Берем САМУЮ СТАРУЮ заявку для прибора
                Request nextRequest = targetBuffer.getLastRequestForDevice();
                if (nextRequest != null) {
                    targetBuffer.removeRequest(nextRequest);

                    Simulation.getInstance().getEventCalendar().recordEvent(
                            Simulation.getInstance().getCurrentTime(),
                            EventType.SERVICE_START,
                            nextRequest,
                            String.format("Заявка %d начинает обслуживание из буфера (FIFO)", nextRequest.getId())
                    );

                    device.startService(nextRequest, Simulation.getInstance().getCurrentTime());
                }
            }
        }
    }

    public void displayState(double currentTime) {
        System.out.println("\nСОСТОЯНИЕ СИСТЕМЫ:");
        System.out.println("-".repeat(60));

        System.out.println("ПРИБОРЫ P1 (скоропортящиеся):");
        for (Device device : devicesP1) {
            device.displayState(currentTime);
        }

        System.out.println("ПРИБОРЫ P2 (обычные):");
        for (Device device : devicesP2) {
            device.displayState(currentTime);
        }

        System.out.println("\nБУФЕРЫ:");
        bufferPerishable.displayState();
        bufferRegular.displayState();

        System.out.println("\nИСТОЧНИКИ:");
        for (Source source : sources) {
            System.out.printf("  Источник %d: сгенерировано %d заявок%n",
                    source.getId(), source.getRequestCounter());
        }
    }

    public int getTotalDevicesInGroup(int priority) {
        if (priority == 1) {
            return devicesP1.size();
        } else if (priority == 2) {
            return devicesP2.size();
        }
        return 0;
    }

    // Getters
    public List<Source> getSources() { return sources; }
    public Buffer getBufferPerishable() { return bufferPerishable; }
    public Buffer getBufferRegular() { return bufferRegular; }
    public List<Device> getDevicesP1() { return devicesP1; }
    public List<Device> getDevicesP2() { return devicesP2; }
}