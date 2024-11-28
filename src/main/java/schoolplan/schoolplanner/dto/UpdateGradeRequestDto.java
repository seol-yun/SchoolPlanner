package schoolplan.schoolplanner.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGradeRequestDto {
    private Long enrollmentId; // 수강신청 ID
    private double grade; // 수정할 성적
}
