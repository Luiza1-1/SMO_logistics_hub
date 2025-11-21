package com.warehouse.utils;

import com.warehouse.model.*;
import com.warehouse.enums.EventType;
import com.warehouse.Simulation;
import com.warehouse.enums.RequestStatus;

import java.util.*;
import java.util.stream.Collectors;

public class EventCalendar {
    private final PriorityQueue<Event> futureEvents = new PriorityQueue<>(Comparator.comparingDouble(Event::getTime).thenComparing(e -> e.getType().ordinal()));
    private final List<Event> occurredEvents = new ArrayList<>();
    private int stepCounter;

    public void scheduleEvent(Event event) {
        futureEvents.offer(event);
    }

    public Event getNextEvent() {
        stepCounter++;
        Event nextEvent = futureEvents.poll();
        if (nextEvent != null) {
            occurredEvents.add(nextEvent);
        }
        return nextEvent;
    }

    public boolean isEmpty() {
        return futureEvents.isEmpty();
    }

    public int getStepCounter() {
        return stepCounter;
    }

    public List<Event> getFutureEvents() {
        List<Event> list = new ArrayList<>(futureEvents);
        list.sort(Comparator.comparingDouble(Event::getTime).thenComparing(e -> e.getType().ordinal()));
        return list;
    }

    /**
     * Главная точка: печать полного состояния за шаг в требуемом формате
     */
    public boolean printStepAndWait(Simulation sim, Event currentEvent) {
        double now = sim.getCurrentTime();

        // Шапка
        System.out.println();
        System.out.println("=".repeat(70));
        if (currentEvent != null) {
            System.out.printf("                  КАЛЕНДАРЬ СОБЫТИЙ — ШАГ %d%n", getStepCounter());
        } else {
            System.out.printf("                  НАЧАЛЬНОЕ СОСТОЯНИЕ СИСТЕМЫ%n");
        }
        System.out.println("=".repeat(70));
        System.out.println();

        // Текущее событие
        if (currentEvent != null) {
            String eventType = getEventAbbreviation(currentEvent);
            System.out.printf("ТЕКУЩЕЕ СОБЫТИЕ:  %-5s   Время: %.2f%n",
                    getEventDescription(currentEvent), currentEvent.getTime());
        } else {
            System.out.println("ТЕКУЩЕЕ СОБЫТИЕ:  НЕТ СОБЫТИЙ");
        }
        System.out.println("-".repeat(70));
        System.out.println();

        // 1) Таблица источников
        printSourcesTable(sim);

        // 2) Буферы раздельно
        printBuffersSeparately(sim);

        // 3) Приборы раздельно
        printDevicesSeparately(sim);

        // 4) Статистика
        printStatistics(sim);

        // Запрос ввода
        System.out.println();
        if (currentEvent != null) {
            System.out.println("Нажмите ENTER для следующего шага, или 'q' — чтобы остановить процесс:");
        } else {
            System.out.println("Нажмите ENTER для генерации заявок:");
        }
        Scanner sc = new Scanner(System.in);
        String line = sc.nextLine().trim();
        return !line.equalsIgnoreCase("q");
    }

    private String getEventAbbreviation(Event event) {
        switch (event.getType()) {
            case ARRIVAL:
                if (event.getSource() instanceof Source) {
                    Source src = (Source) event.getSource();
                    return "И" + src.getId();
                }
                return "И";
            case SERVICE_COMPLETE:
                return "З";
            case BUFFER_ADD:
                return "Б+";
            case BUFFER_REMOVE:
                return "Б-";
            case REJECTION:
                return "ОТК";
            case BUFFER_EVICTION:
                return "ВЫТ";
            default:
                return event.getType().name();
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

    private void printSourcesTable(Simulation sim) {
        WarehouseDispatcher dispatcher = sim.getDispatcher();
        List<Source> sources = dispatcher.getSources();

        System.out.println("ТАБЛИЦА ИСТОЧНИКОВ");
        System.out.println("-".repeat(70));
        System.out.printf("| %-10s | %-15s | %-13s | %-8s |%n",
                "Источник", "След. генерация", "Сгенерировано", "Отказов");
        System.out.println("-".repeat(70));

        for (Source s : sources) {
            double nextGen = s.getNextGenerationTime();
            String nextStr = Double.isFinite(nextGen) && nextGen > 0 ?
                    String.format("%.2f", nextGen) : "—";

            System.out.printf("|   %-6s  | %15s | %13d | %8d |%n",
                    "И" + s.getId(),
                    nextStr,
                    s.getGeneratedCount(),
                    s.getRejectedCount()
            );
        }
        System.out.println("-".repeat(70));
        System.out.println();
    }

    private void printBuffersSeparately(Simulation sim) {
        WarehouseDispatcher dispatcher = sim.getDispatcher();
        Buffer b1 = dispatcher.getBufferPerishable();
        Buffer b2 = dispatcher.getBufferRegular();

        System.out.println("БУФЕР P1 (СКОРОПОРТЯЩИЕСЯ)");
        printSingleBuffer(b1);

        System.out.println("\nБУФЕР P2 (ОБЫЧНЫЕ)");
        printSingleBuffer(b2);
        System.out.println();
    }

    private void printSingleBuffer(Buffer b) {
        System.out.println("-".repeat(75));
        System.out.printf("| %-2s | %-12s | %-4s | %-6s | %-12s | %-9s |%n",
                "№", "Время пост.", "Ист.", "Приор.", "Дедлайн", "Состояние");
        System.out.println("-".repeat(75));

        List<Request> reqs = b.getRequests();
        if (reqs.isEmpty()) {
            System.out.printf("| %-2s | %-12s | %-4s | %-6s | %-12s | %-9s |%n",
                    "-", "-", "-", "-", "-", "-");
        } else {
            for (int i = 0; i < reqs.size(); i++) {
                Request r = reqs.get(i);
                String priority = b.getBufferType() == com.warehouse.enums.CargoType.PERISHABLE ? "P1" : "P2";
                String status = getRequestStatus(r);

                String deadlineStr = "—";
                if (r.hasDeadline()) {
                    deadlineStr = String.format("%.2f", r.getDeadline());
                }

                System.out.printf("| %-2d | %-12.3f | %-4d | %-6s | %-12s | %-9s |%n",
                        i, r.getArrivalTime(), r.getSourceId(), priority, deadlineStr, status);
            }
        }
        System.out.println("-".repeat(75));
    }

    private String getRequestStatus(Request request) {
        switch (request.getStatus()) {
            case IN_QUEUE: return "Ожидает";
            case IN_SERVICE: return "Обслужив.";
            default: return request.getStatus().getDescription();
        }
    }

    private void printDevicesSeparately(Simulation sim) {
        WarehouseDispatcher dispatcher = sim.getDispatcher();
        List<Device> devicesP1 = dispatcher.getDevicesP1();
        List<Device> devicesP2 = dispatcher.getDevicesP2();

        System.out.println("ПРИБОРЫ P1 (СКОРОПОРТЯЩИЕСЯ)");
        printSingleDeviceGroup(devicesP1);

        System.out.println("\nПРИБОРЫ P2 (ОБЫЧНЫЕ)");
        printSingleDeviceGroup(devicesP2);
        System.out.println();
    }

    private void printSingleDeviceGroup(List<Device> devices) {
        System.out.println("-".repeat(74));
        System.out.printf("| %-8s | %-7s | %-11s | %-15s |%n",
                "Прибор", "Сост.", "Конец раб.", "Текущая заявка");
        System.out.println("-".repeat(74));

        for (Device d : devices) {
            String status = d.isFree() ? "свобод" : "занят";
            String endTime = d.isFree() ? "—" : String.format("%.2f", d.getCurrentJobEndTime());
            String currentReq = d.isFree() ? "—" : ("R" + d.getCurrentRequestId());

            System.out.printf("|   %-4s  | %-7s | %-11s | %-15s |%n",
                    "П" + d.getId(), status, endTime, currentReq);
        }
        System.out.println("-".repeat(74));
    }

    private void printStatistics(Simulation sim) {
        System.out.println("СТАТИСТИКА");
        System.out.println("-".repeat(70));
        Statistics stats = sim.getStatistics();
        int total = stats.getTotalArrivals();
        int rejected = stats.getTotalRejected();
        double pct = total > 0 ? (100.0 * rejected / total) : 0.0;

        System.out.printf("Общее число заявок:  %2d%n", total);
        System.out.printf("Число отказов:        %2d%n", rejected);
        System.out.printf("Процент отказов:     %5.2f %% %n", pct);
        System.out.println("-".repeat(70));
    }

    private String padRight(String s, int width) {
        if (s == null) {
            return " ".repeat(width);
        }
        if (s.length() >= width) {
            return s;
        }
        return s + " ".repeat(width - s.length());
    }

    public void displayEventCalendarTable(double currentTime) {
        System.out.println("\n КАЛЕНДАРЬ СОБЫТИЙ:");
        System.out.println("-".repeat(70));

        List<Event> futureEvents = getFutureEvents();
        if (futureEvents.isEmpty()) {
            System.out.println("  Нет запланированных событий");
        } else {
            for (Event event : futureEvents) {
                System.out.printf("  %.2f мин: %s - %s%n",
                        event.getTime(),
                        event.getType().getDescription(),
                        event.getDescription()
                );
            }
        }
    }

    public void generateDetailedReport() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("ДЕТАЛИЗИРОВАННЫЙ ОТЧЕТ");
        System.out.println("=".repeat(70));

        System.out.printf("Всего шагов симуляции: %d%n", stepCounter);
        System.out.printf("Всего произошло событий: %d%n", occurredEvents.size());
    }

    public void recordEvent(double time, EventType type, Object source, String description) {
        Event event = new Event(time, type, source, description);
        occurredEvents.add(event);
    }
}