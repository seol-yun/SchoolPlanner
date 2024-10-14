package schoolplan.schoolplanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import schoolplan.schoolplanner.config.JwtUtil;
import schoolplan.schoolplanner.domain.Member;
import schoolplan.schoolplanner.repository.MemberRepository;

@Service
public class LoginService {
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    public String login(String id, String pw) {
        Member member = memberRepository.findOne(id);
        if (member != null && passwordEncoder.matches(pw, member.getPw())) {
            return jwtUtil.generateToken(id);
        }
        return "0";
    }

    @Transactional
    public String signUp(Member member) {
        // ID 중복 확인
        if (memberRepository.findOne(member.getId()) != null) {
            return "중복된 회원";
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(member.getPw());
        member.setPw(encodedPassword);

        // 멤버 저장
        memberRepository.save(member);
        return "회원가입 성공!";
    }
}
