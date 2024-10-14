package schoolplan.schoolplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
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
@Tag(name = "Auth Controller", description = "로그인 및 회원가입")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private LoginService loginService;
    @Autowired
    private MemberService memberService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "회원 정보를 입력받아 회원가입을 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "중복된 회원", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @Transactional
    public ResponseEntity<String> signup(
            @Parameter(description = "회원 ID", required = true) @RequestParam("id") String id,
            @Parameter(description = "비밀번호", required = true) @RequestParam("pw") String pw,
            @Parameter(description = "이름", required = true) @RequestParam("name") String name,
            @Parameter(description = "이메일", required = true) @RequestParam("email") String email,
            @Parameter(description = "주소", required = true) @RequestParam("address") String address,
            @Parameter(description = "성별", required = true) @RequestParam("gender") String gender){
        Member newMember = new Member(id, pw, name, email, address, gender);
        String result = loginService.signUp(newMember);
        if ("회원가입 성공!".equals(result)) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "회원 ID와 비밀번호를 입력받아 로그인을 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "로그인 실패", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<?> login(
            @Parameter(description = "회원 ID", required = true) @RequestParam("id") String id,
            @Parameter(description = "비밀번호", required = true) @RequestParam("pw") String pw) {

        logger.info("로그인 시도: ID={}, PW=*****", id);  // 비밀번호는 로그에 남기지 않음

        String token = loginService.login(id, pw);

        if (!token.equals("0")) {
            return ResponseEntity.ok().body(Map.of("token", token));
        } else {
            logger.warn("로그인 실패: ID={}", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패");
        }
    }


}
