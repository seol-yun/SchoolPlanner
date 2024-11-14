package schoolplan.schoolplanner.dto;

import lombok.*;

@Getter
@Setter
public class YearSemesterRequestDto {
    private String year;       // 연도
    private String semester;   // 학기
}
