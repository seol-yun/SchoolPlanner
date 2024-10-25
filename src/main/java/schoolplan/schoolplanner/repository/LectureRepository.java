package schoolplan.schoolplanner.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import schoolplan.schoolplanner.domain.Lecture;

import java.util.List;
import java.util.Optional;


import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, String> {
    // 필요한 추가 메서드를 정의할 수 있습니다.
}


//@Repository
//@RequiredArgsConstructor
//public class LectureRepository a{
//    private final EntityManager em;
//
//    /**
//     * 강의 저장
//     * @param lecture
//     * @return
//     */
//    public String save(Lecture lecture) {
//        em.persist(lecture);
//        return lecture.getId();
//    }
//
//    /**
//     * 모든 강의 조회
//     * @return List<Lecture>
//     */
//    public List<Lecture> findAll() {
//        TypedQuery<Lecture> query = em.createQuery("SELECT l FROM Lecture l", Lecture.class);
//        return query.getResultList();
//    }
//
//    /**
//     * 특정 강의 조회
//     * @param id
//     * @return Optional<Lecture>
//     */
//    public Optional<Lecture> findById(String id) {
//        Lecture lecture = em.find(Lecture.class, id);
//        return Optional.ofNullable(lecture);
//    }
//
//    /**
//     * 특정 강의 삭제
//     * @param id
//     */
//    public void deleteById(String id) {
//        Lecture lecture = em.find(Lecture.class, id);
//        if (lecture != null) {
//            em.remove(lecture);
//        }
//    }
//
//    /**
//     * 특정 강의 존재 여부 확인
//     * @param id
//     * @return boolean
//     */
//    public boolean existsById(String id) {
//        Lecture lecture = em.find(Lecture.class, id);
//        return lecture != null;
//    }
//}