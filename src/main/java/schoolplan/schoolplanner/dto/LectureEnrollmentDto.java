package schoolplan.schoolplanner.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LectureEnrollmentDto {
    private Long enrollmentId;
    private String lectureId;
    private String memberId;
    private String lectureName;
    private String openYear;
    private String semester;

    public LectureEnrollmentDto(Long enrollmentId, String lectureId, String memberId, String lectureName, String openYear, String semester) {
        this.enrollmentId = enrollmentId;
        this.lectureId = lectureId;
        this.memberId = memberId;
        this.lectureName = lectureName;
        this.openYear = openYear;
        this.semester = semester;
    }
}
