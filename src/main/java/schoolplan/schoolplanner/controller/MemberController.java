package schoolplan.schoolplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import schoolplan.schoolplanner.config.JwtUtil;
import schoolplan.schoolplanner.domain.Member;
import schoolplan.schoolplanner.service.MemberService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member Controller", description = "회원 관리 API")
public class MemberController {

    private final MemberService memberService;
    private final JwtUtil jwtUtil;


    @GetMapping("/profileImage/{id}")
    @Operation(summary = "프로필 이미지 가져오기", description = "회원 ID를 입력받아 해당 회원의 프로필 이미지를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 반환", content = @Content(mediaType = "image/jpeg")),
            @ApiResponse(responseCode = "404", description = "이미지 없음")
    })
    public ResponseEntity<Resource> getProfileImage(
            @Parameter(description = "회원 ID", required = true) @PathVariable String id) {
        String imagePath = "C://SchoolImage/" + id + ".jpg";
        Resource imageResource = new FileSystemResource(imagePath);

        if (imageResource.exists() && imageResource.isReadable()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageResource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/uploadProfileImage")
    @Operation(summary = "프로필 이미지 업로드", description = "회원 ID와 이미지를 입력받아 프로필 이미지를 업로드합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "이미지 업로드 실패", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<String> uploadProfileImage(
            @Parameter(description = "회원 ID", required = true) @RequestParam("id") String id,
            @Parameter(description = "프로필 이미지", required = true) @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("빈 파일입니다.");
            }

            String directoryPath = "C://SchoolImage";
            Path directory = Paths.get(directoryPath);

            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path filePath = directory.resolve(id + ".jpg");
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok("이미지 업로드 성공");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이미지 업로드 실패: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 세션을 무효화하여 로그아웃을 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public void logout() {
        // No need to handle logout on the server side with JWT
    }

    @GetMapping("/info")
    @Operation(summary = "회원 정보 조회", description = "현재 로그인된 회원의 정보를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 정보 반환", content = @Content(schema = @Schema(implementation = Member.class))),
            @ApiResponse(responseCode = "401", description = "로그인되지 않음"),
            @ApiResponse(responseCode = "404", description = "회원 정보 없음")
    })
    public ResponseEntity<Member> getMemberInfo(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        // Authorization 헤더가 없는 경우 예외 처리
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7); // "Bearer " 이후의 토큰 추출
        String loginId = jwtUtil.extractUsername(token);

        Member member = memberService.findOne(loginId);
        if (member != null) {
            return ResponseEntity.ok().body(member);
        } else {
            return ResponseEntity.notFound().build();
        }
    }



    @PostMapping("/update")
    @Operation(summary = "회원 정보 수정", description = "회원 정보를 수정하고 프로필 이미지를 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "수정 실패", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public String update(
            @Parameter(description = "비밀번호", required = true) @RequestParam("pw") String pw,
            @Parameter(description = "이름", required = true) @RequestParam("name") String name,
            @Parameter(description = "이메일", required = true) @RequestParam("email") String email,
            @Parameter(description = "주소", required = true) @RequestParam("address") String address,
            @Parameter(description = "성별", required = true) @RequestParam("gender") String gender,
            @Parameter(description = "운동 유형", required = true) @RequestParam("exerciseType") String exerciseType,
            @Parameter(description = "트레이너 여부", required = true) @RequestParam("isTrainer") boolean isTrainer,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String loginId = jwtUtil.extractUsername(token);
        memberService.update(new Member(loginId, pw, name, email, address, gender));
        return "수정 성공!";
    }

//    @Setter
//    @Getter
//    class MemberInfo {
//        private String id;
//        private String name;
//        private String exerciseType;
//        private String gender;
//
//        public MemberInfo(String id, String name, String exerciseType, String gender) {
//            this.id = id;
//            this.name = name;
//            this.exerciseType = exerciseType;
//            this.gender = gender;
//        }
//    }
}
