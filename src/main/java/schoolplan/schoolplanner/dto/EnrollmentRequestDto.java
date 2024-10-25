package schoolplan.schoolplanner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnrollmentRequestDto {
    private String year;       // 연도
    private String semester;   // 학기
}
