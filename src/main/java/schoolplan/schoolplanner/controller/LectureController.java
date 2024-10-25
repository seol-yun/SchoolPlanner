package schoolplan.schoolplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import schoolplan.schoolplanner.config.JwtUtil;
import schoolplan.schoolplanner.domain.Lecture;
import schoolplan.schoolplanner.domain.LectureEnrollment;
import schoolplan.schoolplanner.domain.Member;
import schoolplan.schoolplanner.dto.EnrollmentRequestDto;
import schoolplan.schoolplanner.dto.LectureEnrollmentDto;
import schoolplan.schoolplanner.repository.LectureEnrollmentRepository;
import schoolplan.schoolplanner.repository.LectureRepository;
import schoolplan.schoolplanner.repository.MemberRepository;
import schoolplan.schoolplanner.service.LectureService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
@RestController
@RequestMapping("/api/lectures")
@Tag(name = "Lecture Controller", description = "강의 관련")
public class LectureController {

    @Autowired
    private LectureRepository lectureRepository;
    @Autowired
    private LectureService lectureService;
    @Autowired
    private LectureEnrollmentRepository lectureEnrollmentRepository;
    @Autowired
    private MemberRepository memberRepository; // MemberRepository를 사용하여 member 정보를 가져옴
    @Autowired
    private JwtUtil jwtUtil; // JWT 유틸리티 추가

    // 외부 API로부터 모든 강의 불러오기
    @Operation(summary = "Open API로부터 강의 정보 가져오기", description = "외부 API를 호출하여 모든 강의를 가져오고 DB에 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "강의 가져오기 성공"),
            @ApiResponse(responseCode = "500", description = "강의 가져오기 실패")
    })
    @PostMapping("/fetch")
    public ResponseEntity<String> fetchLecturesFromApi(
            @RequestParam String year,
            @RequestParam String semester,
            @RequestParam String page) {
        try {
            lectureService.createLecturesFromApi(year, semester, page);
            return ResponseEntity.ok("강의 정보가 성공적으로 업데이트되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("강의 정보를 가져오는 데 실패했습니다.");
        }
    }

    @PostMapping("/enrollment")
    @Operation(summary = "강의 수강신청", description = "현재 로그인된 회원이 강의를 수강신청합니다.(lecture객체만 보내주면 됩니다.)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "강의 수강신청이 성공적으로 완료되었습니다."),
            @ApiResponse(responseCode = "401", description = "인증 실패: 유효하지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "회원 정보를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "수강신청 처리 중 오류 발생")
    })
    public ResponseEntity<String> enrollLecture(
            @RequestBody LectureEnrollment enrollment, // 요청 본문에서 LectureEnrollment 객체 수신
            HttpServletRequest request) {
        try {
            String memberId = getMemberIdFromJwt(request); // 현재 로그인한 사용자의 memberId 가져오기
            if (memberId == null) {
                return ResponseEntity.status(401).body("인증 실패: 유효하지 않은 토큰"); // 인증 실패 처리
            }

            Optional<Member> memberOpt = memberRepository.findOne(memberId); // memberId로 Member 객체 조회
            if (!memberOpt.isPresent()) {
                return ResponseEntity.status(404).body("회원 정보를 찾을 수 없습니다."); // 회원이 존재하지 않으면 에러 반환
            }

            // LectureEnrollment 객체에 회원 정보 설정
            enrollment.setMember(memberOpt.get()); // 현재 로그인한 회원으로 설정

            // DB에 LectureEnrollment 저장
            lectureEnrollmentRepository.save(enrollment);

            return ResponseEntity.ok("강의 수강신청이 성공적으로 완료되었습니다."); // 성공 메시지 반환
        } catch (Exception e) {
            return ResponseEntity.status(500).body("수강신청 처리 중 오류가 발생했습니다: " + e.getMessage()); // 오류 메시지 반환
        }
    }

    @GetMapping("/findEnrollments")
    @Operation(summary = "수강신청 내역 조회", description = "회원 ID와 특정 연도, 학기를 기준으로 수강신청 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수강신청 내역 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LectureEnrollmentDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패: 유효하지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "해당 연도와 학기에 대한 수강신청 내역이 없습니다."),
            @ApiResponse(responseCode = "500", description = "수강신청 조회 중 오류 발생")
    })
    @Transactional
    public ResponseEntity<?> getEnrollmentsByYearAndSemester(
            @RequestBody EnrollmentRequestDto requestDto, // 요청 본문에서 EnrollmentRequestDto 객체 수신
            HttpServletRequest request) {
        try {
            String memberId = getMemberIdFromJwt(request);
            if (memberId == null) {
                return ResponseEntity.status(401).body("인증 실패: 유효하지 않은 토큰");
            }

            List<LectureEnrollment> enrollments = lectureEnrollmentRepository
                    .findByMember_IdAndLecture_OpenYearAndLecture_Semester(memberId, requestDto.getYear(), requestDto.getSemester());

            if (enrollments.isEmpty()) {
                return ResponseEntity.status(404).body("해당 연도와 학기에 대한 수강신청 내역이 없습니다.");
            }

            // LectureEnrollment를 DTO로 변환
            List<LectureEnrollmentDto> enrollmentDtos = enrollments.stream()
                    .map(enrollment -> new LectureEnrollmentDto(
                            enrollment.getEnrollmentId(),
                            enrollment.getLecture().getId(),
                            enrollment.getMember().getId(),
                            enrollment.getLecture().getSubjectName(),
                            enrollment.getLecture().getOpenYear(),
                            enrollment.getLecture().getSemester()
                    ))
                    .toList();

            return ResponseEntity.ok(enrollmentDtos);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("수강신청 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }




    /**
     * JWT에서 memberId를 가져오는 메서드
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * HTTP 요청에서 JWT에서 memberId 추출, 검증까지
     */
    private String getMemberIdFromJwt(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null && jwtUtil.validateToken(token, jwtUtil.extractUsername(token))) {
            return jwtUtil.extractUsername(token); // JWT의 subject에서 memberId 추출
        }
        return null;
    }


//
//    @Operation(summary = "모든 강의 조회", description = "모든 Lecture의 목록을 반환합니다.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "강의 목록 반환", content = @Content(schema = @Schema(implementation = Lecture.class))),
//            @ApiResponse(responseCode = "500", description = "서버 오류")
//    })
//    @GetMapping
//    public List<Lecture> getAllLectures() {
//        return lectureRepository.findAll();
//    }
//
//    @Operation(summary = "특정 강의 조회", description = "특정 Lecture의 정보를 ID로 조회합니다.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "강의 정보 반환", content = @Content(schema = @Schema(implementation = Lecture.class))),
//            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
//    })
//    @GetMapping("/{id}")
//    public ResponseEntity<Lecture> getLectureById(@PathVariable String id) {
//        Optional<Lecture> lecture = lectureRepository.findById(id);
//        if (lecture.isPresent()) {
//            return ResponseEntity.ok(lecture.get());
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @Operation(summary = "새 강의 생성", description = "새로운 Lecture를 생성합니다.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "201", description = "강의 생성 성공", content = @Content(schema = @Schema(implementation = Lecture.class))),
//            @ApiResponse(responseCode = "400", description = "잘못된 요청")
//    })
//    @PostMapping
//    public String createLecture(@RequestBody Lecture lecture) {
//        return lectureRepository.save(lecture);
//    }
//
//    @Operation(summary = "강의 업데이트", description = "특정 Lecture의 정보를 업데이트합니다.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "강의 정보 업데이트 성공", content = @Content(schema = @Schema(implementation = Lecture.class))),
//            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
//    })
//    @PutMapping("/{id}")
//    public ResponseEntity<String> updateLecture(@PathVariable String id, @RequestBody Lecture lectureDetails) {
//        Optional<Lecture> lecture = lectureRepository.findById(id);
//        if (lecture.isPresent()) {
//            Lecture updatedLecture = lecture.get();
//            updatedLecture.setName(lectureDetails.getName());
//            updatedLecture.setOpenYear(lectureDetails.getOpenYear());
//            updatedLecture.setProfessor(lectureDetails.getProfessor());
//            updatedLecture.setCredits(lectureDetails.getCredits());
//
//            return ResponseEntity.ok(lectureRepository.save(updatedLecture));
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @Operation(summary = "강의 삭제", description = "특정 Lecture를 삭제합니다.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "강의 삭제 성공"),
//            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
//    })
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteLecture(@PathVariable String id) {
//        if (lectureRepository.existsById(id)) {
//            lectureRepository.deleteById(id);
//            return ResponseEntity.ok().build();
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
}