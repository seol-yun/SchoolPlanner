package schoolplan.schoolplanner.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class LectureEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long enrollmentId; // 각 수강신청의 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", referencedColumnName = "member_id", nullable = false)
    private Member member; // Member 엔티티에 대한 외래키

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", referencedColumnName = "lecture_id", nullable = false)
    private Lecture lecture; // Lecture 엔티티에 대한 외래키
    public LectureEnrollment() {
    }

    public LectureEnrollment(Member member, Lecture lecture) {
        this.member = member; // Member 객체 설정
        this.lecture = lecture; // Lecture 객체 설정
    }
}
