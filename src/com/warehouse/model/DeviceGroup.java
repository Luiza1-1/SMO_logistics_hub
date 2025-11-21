package com.warehouse.model;

import com.warehouse.enums.RequestStatus;
import com.warehouse.Simulation;
import com.warehouse.enums.EventType;
import java.util.*;

public class DeviceGroup {
    private final int priority;
    private final List<Device> devices = new ArrayList<>();
    private final Map<Integer, Device> deviceMap = new HashMap<>();

    public DeviceGroup(int priority, int deviceCount, int capacityPerDevice,
                       double minServiceTime, double maxServiceTime) {
        this.priority = priority;

        for (int i = 1; i <= deviceCount; i++) {
            Device device = new Device(i, priority, capacityPerDevice, minServiceTime, maxServiceTime);
            devices.add(device);
            deviceMap.put(i, device);
        }
    }

    public boolean assignRequest(Request request, double currentTime) {
        for (Device device : devices) {
            if (device.isFree()) {
                return device.startService(request, currentTime);
            }
        }
        return false;
    }

    public Device getFreeDevice() {
        return devices.stream()
                .filter(Device::isFree)
                .findFirst()
                .orElse(null);
    }

    public void handleDeviceReleased(Device device) {
        Buffer targetBuffer = getTargetBuffer();
        if (targetBuffer != null) {
            while (device.isFree() && !targetBuffer.isEmpty()) {
                Request nextRequest = targetBuffer.getNextRequestForDevice();
                if (nextRequest != null) {
                    targetBuffer.removeRequest(nextRequest);

                    Simulation.getInstance().getEventCalendar().recordEvent(
                            Simulation.getInstance().getCurrentTime(),
                            EventType.SENT_TO_DEVICE,  // ИЛИ EventType.SENT_FOR_PROCESSING
                            nextRequest,
                            String.format("Заявка %d начинает обслуживание из буфера", nextRequest.getId())

                    );

                    device.startService(nextRequest, Simulation.getInstance().getCurrentTime());
                }
            }
        }
    }

    private Buffer getTargetBuffer() {
        WarehouseDispatcher dispatcher = Simulation.getInstance().getDispatcher();
        return (priority == 1) ? dispatcher.getBufferPerishable() : dispatcher.getBufferRegular();
    }

    public List<Device> getDevices() { return devices; }
    public int getPriority() { return priority; }
    public int getTotalDevices() { return devices.size(); }

    public void displayState(double currentTime) {
        System.out.printf("  Группа приборов (приоритет %d):%n", priority);
        for (Device device : devices) {
            device.displayState(currentTime);
        }
    }
}