package schoolplan.schoolplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import schoolplan.schoolplanner.config.JwtUtil;
import schoolplan.schoolplanner.domain.Member;
import schoolplan.schoolplanner.service.LoginService;
import schoolplan.schoolplanner.service.MemberService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth Controller", description = "로그인, 로그아웃 및 회원가입")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private LoginService loginService;
    @Autowired
    private MemberService memberService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "회원 정보를 JSON 형식으로 입력받아 회원가입을 처리합니다.(성향 0:안정형, 1:밸런스, 2:도전형 )")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "중복된 회원", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @Transactional
    public ResponseEntity<String> signup(@RequestBody SignupRequest signupRequest) {
        Member newMember = new Member(signupRequest.getId(), signupRequest.getPw(), signupRequest.getName(), signupRequest.getTendency());
        String result = loginService.signUp(newMember);
        if ("회원가입 성공!".equals(result)) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    // 회원가입 요청 데이터를 담을 클래스
    @Getter
    @Setter
    public static class SignupRequest {
        private String id;
        private String pw;
        private String name;
        private int tendency;
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "회원 ID와 비밀번호를 JSON 형식으로 입력받아 로그인을 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "로그인 실패", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        logger.info("로그인 시도: ID={}, PW=*****", loginRequest.getId());

        String token = loginService.login(loginRequest.getId(), loginRequest.getPw());

        if (!token.equals("0")) {
            return ResponseEntity.ok().body(Map.of("token", token));
        } else {
            logger.warn("로그인 실패: ID={}", loginRequest.getId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패");
        }
    }

    // 로그인 요청 데이터를 담을 클래스
    @Getter
    @Setter
    public static class LoginRequest {
        private String id;
        private String pw;
    }


    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 세션을 무효화하여 로그아웃을 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public void logout() {
        // No need to handle logout on the server side with JWT
        // 클라이언트에서 토큰을 삭제함
    }
}
