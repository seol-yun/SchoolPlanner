package schoolplan.schoolplanner.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import schoolplan.schoolplanner.domain.Member;

import java.util.List;

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
     *
     * @param id
     * @return
     */
    public Member findOne(String id) {
        return em.find(Member.class, id);
    }

    /**
     * 모든 멤버 반환
     *
     * @return
     */
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }

    /**
     * 일반사용자 반환
     *
     * @return
     */
    public List<Member> findGeneralUser() {
        return em.createQuery("select m from Member m where isTrainer = false", Member.class).getResultList();
    }

    /**
     * 운동전문가 반환
     *
     * @return
     */
    public List<Member> findTrainer() {
        return em.createQuery("select m from Member m where isTrainer = true", Member.class).getResultList();
    }
}
