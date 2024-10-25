package schoolplan.schoolplanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import schoolplan.schoolplanner.config.JwtUtil;
import schoolplan.schoolplanner.domain.Member;
import schoolplan.schoolplanner.repository.MemberRepository;

import java.util.Optional;

@Service
public class LoginService {
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 로그인 메서드
     * @param id 사용자 ID
     * @param pw 사용자 비밀번호
     * @return JWT 토큰 또는 "0" (로그인 실패)
     */
    public String login(String id, String pw) {
        // ID로 회원 조회
        Optional<Member> memberOpt = memberRepository.findOne(id);

        // 회원이 존재하고 비밀번호가 일치하는지 확인
        if (memberOpt.isPresent() && passwordEncoder.matches(pw, memberOpt.get().getPw())) {
            return jwtUtil.generateToken(id); // JWT 토큰 생성
        }

        return "0"; // 로그인 실패
    }

    /**
     * 회원가입 메서드
     * @param member 회원 정보
     * @return 회원가입 결과 메시지
     */
    @Transactional
    public String signUp(Member member) {
        // ID 중복 확인
        if (memberRepository.findOne(member.getId()).isPresent()) {
            return "중복된 회원"; // 중복 ID
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(member.getPw());
        member.setPw(encodedPassword);

        // 멤버 저장
        memberRepository.save(member);
        return "회원가입 성공!"; // 회원가입 성공 메시지
    }
}
