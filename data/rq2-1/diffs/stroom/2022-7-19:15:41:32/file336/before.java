package stroom.job.api;

public class Schedule {

    private final String schedule;
    private final ScheduleType scheduleType;

    public Schedule(final ScheduleType scheduleType, final String schedule) {
        this.schedule = schedule;
        this.scheduleType = scheduleType;
    }

    public String getSchedule() {
        return schedule;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public enum ScheduleType {
        CRON,
        PERIODIC
    }
}
