package schoolplan.schoolplanner.service;

import schoolplan.schoolplanner.dto.LectureTime;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LectureTimeParser {
    private static final Pattern TIME_PATTERN = Pattern.compile("(월|화|수|목|금|토|일)(\\d{2}):(\\d{2})~(\\d{2}):(\\d{2})");

    public static List<LectureTime> parseLectureTimes(String schedule) {
        List<LectureTime> times = new ArrayList<>();
        Matcher matcher = TIME_PATTERN.matcher(schedule);

        while (matcher.find()) {
            String day = matcher.group(1);
            LocalTime startTime = LocalTime.of(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
            LocalTime endTime = LocalTime.of(Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)));
            times.add(new LectureTime(day, startTime, endTime));
        }

        return times;
    }
}