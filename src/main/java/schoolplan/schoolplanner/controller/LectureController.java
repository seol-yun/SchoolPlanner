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
import schoolplan.schoolplanner.service.LectureTimeParser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    @Operation(summary = "강의 수강신청", description = "현재 로그인된 회원이 강의를 수강신청합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "강의 수강신청이 성공적으로 완료되었습니다."),
            @ApiResponse(responseCode = "401", description = "인증 실패: 유효하지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "회원 정보 또는 강의 정보를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "수강신청 처리 중 오류 발생")
    })
    public ResponseEntity<String> enrollLecture(
            @RequestBody LectureIdRequestDto lectureIdDto, // Only lectureId is received in the request
            HttpServletRequest request) {
        try {
            String memberId = getMemberIdFromJwt(request); // Get the memberId from JWT
            if (memberId == null) {
                return ResponseEntity.status(401).body("인증 실패: 유효하지 않은 토큰");
            }

            Optional<Member> memberOpt = memberRepository.findOne(memberId); // Fetch Member by memberId
            if (!memberOpt.isPresent()) {
                return ResponseEntity.status(404).body("회원 정보를 찾을 수 없습니다.");
            }

            Optional<Lecture> lectureOpt = lectureRepository.findById(lectureIdDto.getLectureId()); // Fetch Lecture by lectureId
            if (!lectureOpt.isPresent()) {
                return ResponseEntity.status(404).body("강의 정보를 찾을 수 없습니다.");
            }

            LectureEnrollment enrollment = new LectureEnrollment();
            enrollment.setMember(memberOpt.get()); // Set the current member
            enrollment.setLecture(lectureOpt.get()); // Set the lecture

            lectureEnrollmentRepository.save(enrollment); // Save the enrollment

            return ResponseEntity.ok("강의 수강신청이 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("수강신청 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }


    @PostMapping("/findEnrollments")
    @Operation(summary = "수강신청 내역 조회", description = "회원 ID와 특정 연도, 학기를 기준으로 수강신청 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수강신청 내역 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LectureEnrollmentDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패: 유효하지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "해당 연도와 학기에 대한 수강신청 내역이 없습니다."),
            @ApiResponse(responseCode = "500", description = "수강신청 조회 중 오류 발생")
    })
    @Transactional
    public ResponseEntity<?> getEnrollmentsByYearAndSemester(
            @RequestBody YearSemesterRequestDto requestDto, // 요청 본문에서 EnrollmentRequestDto 객체 수신
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

    @PostMapping("/getLectureInfo")
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

    @PostMapping("/findAll")
    @Operation(summary = "모든 강의 조회", description = "모든 강의의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "모든 강의 조회 성공"),
            @ApiResponse(responseCode = "500", description = "강의 조회 중 오류 발생")
    })
    public ResponseEntity<?> getAllLectures(@RequestBody YearSemesterRequestDto filterDTO) {
        try {
            List<Lecture> lectures = lectureRepository.findAll()
                    .stream()
                    .filter(lecture ->
                            (filterDTO.getYear() == null || filterDTO.getYear().equals(lecture.getOpenYear())) &&
                                    (filterDTO.getSemester() == null || filterDTO.getSemester().equals(lecture.getSemester()))
                    )
                    .collect(Collectors.toList());
            return ResponseEntity.ok(lectures); // 필터된 강의 리스트 반환
        } catch (Exception e) {
            return ResponseEntity.status(500).body("강의 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/averageDifficulty")
    @Operation(summary = "시간표 평균 난이도 조회", description = "특정 회원의 특정 연도와 학기에 해당하는 시간표의 평균 난이도를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "평균 난이도 조회 성공"),
            @ApiResponse(responseCode = "500", description = "평균 난이도 조회 중 오류 발생")
    })
    @Transactional
    public ResponseEntity<?> getAverageDifficulty(@RequestBody YearSemesterRequestDto requestDto, HttpServletRequest request) {
        try {
            // JWT에서 회원 ID 추출
            String memberId = getMemberIdFromJwt(request);

            // 특정 회원의 연도와 학기에 해당하는 수강 신청 내역 조회
            List<LectureEnrollment> enrollments = lectureEnrollmentRepository
                    .findByMember_IdAndLecture_OpenYearAndLecture_Semester(memberId, requestDto.getYear(), requestDto.getSemester());

            // 강의가 없을 경우 처리
            if (enrollments.isEmpty()) {
                return ResponseEntity.ok("해당 연도와 학기에 수강 신청된 강의가 없습니다.");
            }

            // 평균 난이도 계산
            double averageDifficulty = enrollments.stream()
                    .mapToDouble(enrollment -> enrollment.getLecture().getDifficultyTotal() / enrollment.getLecture().getDifficultyCount())
                    .average()
                    .orElse(0.0);

            return ResponseEntity.ok("평균 난이도: " + averageDifficulty);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("평균 난이도 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/averageLearningAmount")
    @Operation(summary = "시간표 평균 학습 유용도 조회", description = "특정 회원의 특정 연도와 학기에 해당하는 시간표의 평균 학습 유용도를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "평균 학습 유용도 조회 성공"),
            @ApiResponse(responseCode = "500", description = "평균 학습 유용도 조회 중 오류 발생")
    })
    @Transactional
    public ResponseEntity<?> getAverageLearningAmount(@RequestBody YearSemesterRequestDto requestDto, HttpServletRequest request) {
        try {
            // JWT에서 회원 ID 추출
            String memberId = getMemberIdFromJwt(request);

            // 특정 회원의 연도와 학기에 해당하는 수강 신청 내역 조회
            List<LectureEnrollment> enrollments = lectureEnrollmentRepository
                    .findByMember_IdAndLecture_OpenYearAndLecture_Semester(memberId, requestDto.getYear(), requestDto.getSemester());

            // 강의가 없을 경우 처리
            if (enrollments.isEmpty()) {
                return ResponseEntity.ok("해당 연도와 학기에 수강 신청된 강의가 없습니다.");
            }

            // 평균 학습 유용도 계산
            double averageLearningAmount = enrollments.stream()
                    .mapToDouble(enrollment -> enrollment.getLecture().getLearningAmountTotal() / enrollment.getLecture().getLearningAmountCount())
                    .average()
                    .orElse(0.0);

            return ResponseEntity.ok("평균 학습 유용도: " + averageLearningAmount);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("평균 학습 유용도 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }


    @PostMapping("/recommendOtherLectures")
    @Operation(summary = "대체 강의 추천", description = "시간표와 시간이 겹치지 않으면서 ISSUE_DIVISION, openYear, semester이 같은 강의를 추천합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추천 강의 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Lecture.class))),
            @ApiResponse(responseCode = "404", description = "해당 강의를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "추천 강의 조회 중 오류 발생")
    })
    @Transactional
    public ResponseEntity<?> recommendLectures(@RequestBody LectureIdRequestDto lectureIdRequestDto, HttpServletRequest request) {
        String lectureId = lectureIdRequestDto.getLectureId();
        Optional<Lecture> lectureOpt = lectureRepository.findById(lectureId);

        if (!lectureOpt.isPresent()) {
            return ResponseEntity.status(404).body("강의를 찾을 수 없습니다.");
        }

        Lecture selectedLecture = lectureOpt.get();
        String selectedIssueDivision = selectedLecture.getIssueDivision();
        String selectedOpenYear = selectedLecture.getOpenYear();
        String selectedSemester = selectedLecture.getSemester();

        // 현재 사용자의 모든 수강 신청 내역 조회
        String memberId = getMemberIdFromJwt(request);
        List<LectureEnrollment> userEnrollments = lectureEnrollmentRepository.findByMember_IdAndLecture_OpenYearAndLecture_Semester(memberId, selectedOpenYear, selectedSemester);
        List<LectureTime> enrolledLectureTimes = userEnrollments.stream()
                .flatMap(enrollment -> LectureTimeParser.parseLectureTimes(enrollment.getLecture().getScheduleInformation()).stream())
                .collect(Collectors.toList());

        List<Lecture> allLectures = lectureRepository.findAll();
        List<Lecture> recommendedLectures = allLectures.stream()
                .filter(lecture -> !lecture.getId().equals(lectureId))
                .filter(lecture -> lecture.getIssueDivision().equals(selectedIssueDivision))
                .filter(lecture -> lecture.getOpenYear().equals(selectedOpenYear)) // openYear 필터 추가
                .filter(lecture -> lecture.getSemester().equals(selectedSemester)) // semester 필터 추가
                .filter(lecture -> {
                    List<LectureTime> lectureTimes = LectureTimeParser.parseLectureTimes(lecture.getScheduleInformation());
                    return isNonConflicting(enrolledLectureTimes, lectureTimes);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(recommendedLectures);
    }

    private boolean isNonConflicting(List<LectureTime> enrolledTimes, List<LectureTime> otherTimes) {
        for (LectureTime enrolled : enrolledTimes) {
            for (LectureTime other : otherTimes) {
                if (enrolled.getDay().equals(other.getDay())) {
                    if (enrolled.getEndTime().isAfter(other.getStartTime()) && other.getEndTime().isAfter(enrolled.getStartTime())) {
                        return false; // 시간이 겹치는 경우
                    }
                }
            }
        }
        return true;
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