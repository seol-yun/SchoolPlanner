package schoolplan.schoolplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import schoolplan.schoolplanner.domain.LectureEnrollment;

import java.util.List;
import java.util.Optional;

public interface LectureEnrollmentRepository extends JpaRepository<LectureEnrollment, Long> {
    //배포4
    // 특정 회원의 연도와 학기에 해당하는 수강신청 내역 조회
    List<LectureEnrollment> findByMember_IdAndLecture_OpenYearAndLecture_Semester(String memberId, String openYear, String semester);
}
