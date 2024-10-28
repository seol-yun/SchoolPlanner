package schoolplan.schoolplanner.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LectureEnrollmentIdRequestDto {
    private Long enrollmentId; // 삭제할 enrollment의 ID
}
