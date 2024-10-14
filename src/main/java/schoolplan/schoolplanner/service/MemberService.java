package schoolplan.schoolplanner.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schoolplan.schoolplanner.domain.Member;
import schoolplan.schoolplanner.repository.MemberRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor //final만 사용해서 생성자 만듦(lombok)
public class MemberService {
    private final MemberRepository memberRepository;

    /**
     * 회원 가입
     */
    public String join(Member member) {
        validateDuplicateMember(member); //중복 회원 검증
        memberRepository.save(member);
        return member.getId();
    }

    /**
     * id가 중복인지 검사
     * @param member
     * @return
     */
    public int validateDuplicateMember(Member member) {
        Member existingMember = memberRepository.findOne(member.getId());
        if (existingMember != null) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * 회원 전체 조회
     */
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public Member findOne(String memberId) {
        return memberRepository.findOne(memberId);
    }

    /**
     * 로그인
     */
    public String login(String memberId, String password) {
        // 아이디로 회원을 찾기
        Member member = memberRepository.findOne(memberId);
        if (member != null) {
            // 비밀번호가 일치하면 로그인 성공 메시지 반환
            if (member.getPw().equals(password)) {
                return member.getId();
            } else {
                // 비밀번호가 일치하지 않으면 실패 메시지 반환
                return "0";
            }
        } else {
            // 회원이 존재하지 않으면 실패 메시지 반환
            return "0";
        }
    }



    /**
     * 사용자 정보 수정
     */
    public Member update(Member member) {
        Member existingMember = memberRepository.findOne(member.getId());
        if (existingMember != null) {
            // 필요한 필드 업데이트
            existingMember.setPw(member.getPw());
            existingMember.setName(member.getName());
            existingMember.setEmail(member.getEmail());
            existingMember.setAddress(member.getAddress());
            existingMember.setGender(member.getGender());

            memberRepository.save(existingMember);
            // 저장 후 반환
            return existingMember;
        } else {
            // 회원이 존재하지 않는 경우 예외 처리
            throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
        }
    }

}
