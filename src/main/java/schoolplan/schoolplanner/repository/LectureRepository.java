package schoolplan.schoolplanner.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import schoolplan.schoolplanner.domain.Lecture;

import java.util.List;
import java.util.Optional;


import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, String> {
    @Query("SELECT l FROM Lecture l WHERE l.openYear = :openYear AND l.semester = :semester")
    List<Lecture> findByOpenYearAndSemester(@Param("openYear") String openYear, @Param("semester") String semester);

    List<Lecture> findBySubjectNameContaining(String subjectName);
}