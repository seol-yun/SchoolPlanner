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
import schoolplan.schoolplanner.dto.*;
import schoolplan.schoolplanner.repository.LectureEnrollmentRepository;
import schoolplan.schoolplanner.repository.LectureRepository;
import schoolplan.schoolplanner.repository.MemberRepository;
import schoolplan.schoolplanner.service.LectureService;

import java.util.List;
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


    @DeleteMapping("/deleteEnrollment")
    @Operation(summary = "수강신청 삭제", description = "수강신청을 취소합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수강신청이 성공적으로 취소되었습니다."),
            @ApiResponse(responseCode = "401", description = "인증 실패: 유효하지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "해당 수강신청을 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "수강신청 삭제 중 오류 발생")
    })
    public ResponseEntity<String> deleteEnrollment(
            @RequestBody LectureEnrollmentIdRequestDto requestDto, // DTO에서 enrollmentId 받기
            HttpServletRequest request) {
        try {
            String memberId = getMemberIdFromJwt(request);
            if (memberId == null) {
                return ResponseEntity.status(401).body("인증 실패: 유효하지 않은 토큰");
            }

            Long enrollmentId = requestDto.getEnrollmentId(); // DTO에서 enrollmentId 추출
            Optional<LectureEnrollment> enrollmentOpt = lectureEnrollmentRepository.findById(enrollmentId);
            if (!enrollmentOpt.isPresent()) {
                return ResponseEntity.status(404).body("해당 수강신청을 찾을 수 없습니다.");
            }

            LectureEnrollment enrollment = enrollmentOpt.get();
            if (!enrollment.getMember().getId().equals(memberId)) {
                return ResponseEntity.status(401).body("본인의 수강신청만 취소할 수 있습니다.");
            }

            lectureEnrollmentRepository.delete(enrollment);
            return ResponseEntity.ok("수강신청이 성공적으로 취소되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("수강신청 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/getLectureInfo")
    @Operation(summary = "강의 정보 조회", description = "강의 ID를 기반으로 해당 강의의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "강의 정보 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Lecture.class))),
            @ApiResponse(responseCode = "404", description = "해당 강의를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "강의 정보 조회 중 오류 발생")
    })
    public ResponseEntity<?> getLectureInfo(
            @RequestBody LectureIdRequestDto lectureIdRequestDto) { // JSON 요청에서 lectureId 받기
        try {
            String lectureId = lectureIdRequestDto.getLectureId(); // JSON에서 lectureId 추출
            Optional<Lecture> lectureOpt = lectureRepository.findById(lectureId);
            if (!lectureOpt.isPresent()) {
                return ResponseEntity.status(404).body("해당 강의를 찾을 수 없습니다.");
            }

            Lecture lecture = lectureOpt.get();
            return ResponseEntity.ok(lecture); // 조회된 강의 정보 반환
        } catch (Exception e) {
            return ResponseEntity.status(500).body("강의 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }


    // 평점 업데이트 API
    @PostMapping("/rating")
    @Operation(summary = "과목 평점 등록", description = "새로운 평점을 추가하여 평균 평점을 계산합니다.")
    public ResponseEntity<String> updateRating(@RequestBody RatingUpdateRequestDto ratingRequest) {
        Optional<Lecture> lectureOpt = lectureRepository.findById(ratingRequest.getLectureId());

        if (!lectureOpt.isPresent()) {
            return ResponseEntity.status(404).body("해당 강의를 찾을 수 없습니다.");
        }

        Lecture lecture = lectureOpt.get();
        lecture.setRatingTotal(lecture.getRatingTotal() + ratingRequest.getRating());
        lecture.setRatingCount(lecture.getRatingCount() + 1);

        lectureRepository.save(lecture);
        return ResponseEntity.ok("평점이 성공적으로 업데이트되었습니다.");
    }

    // 난이도 업데이트 API
    @PostMapping("/difficulty")
    @Operation(summary = "과목 난이도 등록", description = "새로운 난이도 점수를 추가하여 평균 난이도를 계산합니다.")
    public ResponseEntity<String> updateDifficulty(@RequestBody DifficultyUpdateRequestDto difficultyRequest) {
        Optional<Lecture> lectureOpt = lectureRepository.findById(difficultyRequest.getLectureId());

        if (!lectureOpt.isPresent()) {
            return ResponseEntity.status(404).body("해당 강의를 찾을 수 없습니다.");
        }

        Lecture lecture = lectureOpt.get();
        lecture.setDifficultyTotal(lecture.getDifficultyTotal() + difficultyRequest.getDifficulty());
        lecture.setDifficultyCount(lecture.getDifficultyCount() + 1);

        lectureRepository.save(lecture);
        return ResponseEntity.ok("난이도가 성공적으로 업데이트되었습니다.");
    }

    // 학습 유용도 업데이트 API
    @PostMapping("/learningAmount")
    @Operation(summary = "과목 학습 유용도 등록", description = "새로운 학습 유용도 점수를 추가하여 평균 학습 유용도를 계산합니다.")
    public ResponseEntity<String> updateLearningAmount(@RequestBody LearningAmountUpdateRequestDto learningAmountRequest) {
        Optional<Lecture> lectureOpt = lectureRepository.findById(learningAmountRequest.getLectureId());

        if (!lectureOpt.isPresent()) {
            return ResponseEntity.status(404).body("해당 강의를 찾을 수 없습니다.");
        }

        Lecture lecture = lectureOpt.get();
        lecture.setLearningAmountTotal(lecture.getLearningAmountTotal() + learningAmountRequest.getLearningAmount());
        lecture.setLearningAmountCount(lecture.getLearningAmountCount() + 1);

        lectureRepository.save(lecture);
        return ResponseEntity.ok("학습 유용도가 성공적으로 업데이트되었습니다.");
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
}