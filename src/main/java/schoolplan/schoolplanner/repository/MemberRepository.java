package schoolplan.schoolplanner.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import schoolplan.schoolplanner.domain.Member;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepository {
    private final EntityManager em;

    /**
     * 멤버 추가
     *
     * @param member
     * @return
     */
    public String save(Member member) {
        em.persist(member);
        return member.getId();
    }

    /**
     * id로 멤버 찾아서 반환
     * Optional을 사용하는 이유
     * 명확한 의도: 메서드가 값을 반환할 때 값이 없을 가능성을 명확하게 나타냅니다.
     * 코드 안전성: NullPointerException을 예방하여 코드의 안정성을 높입니다.
     * 가독성: Optional 메서드를 사용하여 처리할 수 있는 방법을 제공하여 코드의 가독성을 향상시킵니다.
     * @param id
     * @return
     */
    public Optional<Member> findOne(String id) {
        Member member = em.find(Member.class, id); // ID로 Member 조회
        return Optional.ofNullable(member); // member가 null일 경우 Optional.empty() 반환
    }

    /**
     * 모든 멤버 반환
     *
     * @return
     */
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }

}
