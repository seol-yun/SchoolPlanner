package schoolplan.schoolplanner.dto;

import lombok.*;

@Getter
@Setter
public class YearSemesterRequestDto {
    private String year;        // 연도
    private String semester;    // 학기
    private int page;           // 페이지 번호
    private int size;           // 페이지 크기
}
