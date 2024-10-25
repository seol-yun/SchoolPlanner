package schoolplan.schoolplanner.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schoolplan.schoolplanner.domain.Member;
import schoolplan.schoolplanner.repository.MemberRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor // final 필드를 위한 생성자 자동 생성
public class MemberService {
    private final MemberRepository memberRepository;

    /**
     * 회원 전체 조회
     */
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    /**
     * 특정 회원 조회
     */
    public Optional<Member> findOne(String memberId) {
        return memberRepository.findOne(memberId);
    }

    /**
     * 사용자 정보 수정
     */
    public Member update(Member member) {
        // Optional을 사용하여 기존 회원 조회
        Optional<Member> existingMemberOpt = memberRepository.findOne(member.getId());

        // 회원이 존재할 경우
        if (existingMemberOpt.isPresent()) {
            Member existingMember = existingMemberOpt.get(); // 회원 객체 가져오기

            // 필요한 필드 업데이트
            existingMember.setPw(member.getPw());
            existingMember.setName(member.getName());
            existingMember.setEmail(member.getEmail());
            existingMember.setAddress(member.getAddress());
            existingMember.setGender(member.getGender());

            memberRepository.save(existingMember); // 변경된 회원 정보 저장
            return existingMember; // 업데이트된 회원 반환
        } else {
            // 회원이 존재하지 않는 경우 예외 처리
            throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
        }
    }
}
