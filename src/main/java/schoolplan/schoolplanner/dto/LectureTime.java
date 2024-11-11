package schoolplan.schoolplanner.dto;

import java.time.LocalTime;

public class LectureTime {
    private String day;
    private LocalTime startTime;
    private LocalTime endTime;

    public LectureTime(String day, LocalTime startTime, LocalTime endTime) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getDay() { return day; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}
