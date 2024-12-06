package schoolplan.schoolplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.util.*;
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
            @RequestParam String semester) {
        try {
            lectureService.createLecturesFromApi(year, semester);
            return ResponseEntity.ok("강의 정보가 성공적으로 업데이트되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("강의 정보를 가져오는 데 실패했습니다.");
        }
    }

    @PostMapping("/enrollment")
    @Operation(summary = "강의 수강신청", description = "현재 로그인된 회원이 강의를 수강신청합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "강의 수강신청이 성공적으로 완료되었습니다."),
            @ApiResponse(responseCode = "400", description = "시간표 겹침 또는 이미 신청한 강의입니다."),
            @ApiResponse(responseCode = "401", description = "인증 실패: 유효하지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "회원 정보 또는 강의 정보를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "수강신청 처리 중 오류 발생")
    })
    @Transactional
    public ResponseEntity<String> enrollLecture(
            @RequestBody LectureIdRequestDto lectureIdDto, HttpServletRequest request) {
        try {
            String memberId = getMemberIdFromJwt(request); // JWT에서 memberId 추출
            if (memberId == null) {
                return ResponseEntity.status(401).body("인증 실패: 유효하지 않은 토큰");
            }

            Optional<Member> memberOpt = memberRepository.findOne(memberId); // 회원 정보 조회
            if (!memberOpt.isPresent()) {
                return ResponseEntity.status(404).body("회원 정보를 찾을 수 없습니다.");
            }

            Optional<Lecture> lectureOpt = lectureRepository.findById(lectureIdDto.getLectureId()); // 강의 정보 조회
            if (!lectureOpt.isPresent()) {
                return ResponseEntity.status(404).body("강의 정보를 찾을 수 없습니다.");
            }

            // 해당 강의 정보 가져오기
            Lecture newLecture = lectureOpt.get();
            String selectedOpenYear = newLecture.getOpenYear();
            String selectedSemester = newLecture.getSemester();

            // 이미 수강한 강의가 있는지 확인
            List<LectureEnrollment> existingEnrollments = lectureEnrollmentRepository
                    .findByMember_IdAndLecture_OpenYearAndLecture_Semester(memberId, selectedOpenYear, selectedSemester);

            // 중복 신청 여부 확인
            boolean alreadyEnrolled = existingEnrollments.stream()
                    .anyMatch(enrollment -> enrollment.getLecture().getId().equals(lectureIdDto.getLectureId()));

            if (alreadyEnrolled) {
                return ResponseEntity.status(400).body("이미 신청한 강의입니다.");
            }

            // 현재 사용자의 수강 신청 내역 조회 (openYear와 semester 기준)
            List<LectureEnrollment> userEnrollments = lectureEnrollmentRepository
                    .findByMember_IdAndLecture_OpenYearAndLecture_Semester(memberId, selectedOpenYear, selectedSemester);

            // 이미 수강한 강의들의 시간표 가져오기
            List<LectureTime> enrolledLectureTimes = userEnrollments.stream()
                    .flatMap(enrollment -> LectureTimeParser.parseLectureTimes(enrollment.getLecture().getScheduleInformation()).stream())
                    .collect(Collectors.toList());

            // 신규 강의 시간표 가져오기
            List<LectureTime> newLectureTimes = LectureTimeParser.parseLectureTimes(newLecture.getScheduleInformation());

            // 시간표 중복 체크
            if (!isNonConflicting(enrolledLectureTimes, newLectureTimes)) {
                return ResponseEntity.status(400).body("수강신청이 불가능합니다. 시간표가 겹칩니다.");
            }

            // 수강신청 진행
            LectureEnrollment enrollment = new LectureEnrollment();
            enrollment.setMember(memberOpt.get());
            enrollment.setLecture(newLecture);
            lectureEnrollmentRepository.save(enrollment);

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
                            enrollment.getLecture().getSemester(),
                            enrollment.getLecture().getScheduleInformation()
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

    @PostMapping("/getLectureInfoByNameContaining")
    @Operation(summary = "강의 정보 조회(포함된 이름으로)", description = "강의 이름에 특정 글자가 포함된 강의들을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "강의 정보 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Lecture.class))),
            @ApiResponse(responseCode = "404", description = "해당 이름의 강의를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "강의 정보 조회 중 오류 발생")
    })
    public ResponseEntity<?> getLectureInfoByNameContaining(
            @RequestBody LectureNameDto lectureNameRequestDto) {
        try {
            String subjectName = lectureNameRequestDto.getLectureName(); // JSON에서 subjectName 추출
            List<Lecture> lectures = lectureRepository.findBySubjectNameContaining(subjectName); // 포함된 이름으로 강의 검색

            if (lectures.isEmpty()) {
                return ResponseEntity.status(404).body("해당 이름의 강의를 찾을 수 없습니다.");
            }

            return ResponseEntity.ok(lectures); // 조회된 강의 정보 리스트 반환
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

//    @PostMapping("/findAll")
//    @Operation(summary = "모든 강의 조회", description = "모든 강의의 상세 정보를 조회합니다.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "모든 강의 조회 성공"),
//            @ApiResponse(responseCode = "500", description = "강의 조회 중 오류 발생")
//    })
//    public ResponseEntity<?> getAllLectures(@RequestBody YearSemesterRequestDto filterDTO) {
//        try {
//            // DB에서 연도와 학기에 맞는 모든 강의 조회 (페이징 없이)
//            List<Lecture> lectures = lectureRepository.findByOpenYearAndSemester(
//                    filterDTO.getYear(), filterDTO.getSemester());
//
//            // 페이지 번호와 페이지 크기에 맞게 리스트 자르기
//            int start = filterDTO.getPage() * filterDTO.getSize();
//            int end = Math.min(start + filterDTO.getSize(), lectures.size());
//            List<Lecture> pagedLectures = lectures.subList(start, end);
//
//            // 페이징된 강의 리스트 반환
//            Map<String, Object> response = new HashMap<>();
//            response.put("lectures", pagedLectures);
//            response.put("currentPage", filterDTO.getPage());
//            response.put("totalItems", lectures.size());
//            response.put("totalPages", (lectures.size() / filterDTO.getSize()) + 1);
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("강의 조회 중 오류가 발생했습니다: " + e.getMessage());
//        }
//    }


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
    @Operation(summary = "대체 강의 추천", description = "시간표와 시간이 겹치지 않으면서 ISSUE_DIVISION, openYear, semester, DEPARTMENT가 같은 강의를 추천합니다.")
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
        String selectedDepartment = selectedLecture.getDepartment(); // DEPARTMENT 값 가져오기

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
                .filter(lecture -> lecture.getOpenYear().equals(selectedOpenYear)) // openYear 필터
                .filter(lecture -> lecture.getSemester().equals(selectedSemester)) // semester 필터
                .filter(lecture -> lecture.getDepartment().equals(selectedDepartment)) // DEPARTMENT 필터 추가
                .filter(lecture -> {
                    List<LectureTime> lectureTimes = LectureTimeParser.parseLectureTimes(lecture.getScheduleInformation());
                    return isNonConflicting(enrolledLectureTimes, lectureTimes);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(recommendedLectures);
    }

    @PostMapping("/recommendLecture")
    @Operation(summary = "강의 추천(유사도 기반)", description = "주어진 연도와 학기의 강의를 추천합니다. 추천 강의는 해당 멤버의 difficulty와 learningAmount 기준으로 유사한 순으로 정렬됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추천 강의 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Lecture.class))),
            @ApiResponse(responseCode = "404", description = "추천 강의 조회 실패"),
            @ApiResponse(responseCode = "500", description = "추천 강의 조회 중 오류 발생")
    })
    @Transactional
    public ResponseEntity<?> recommendLectures(@RequestBody YearSemesterRequestDto yearSemesterRequestDto, HttpServletRequest request) {
        String year = yearSemesterRequestDto.getYear();
        String semester = yearSemesterRequestDto.getSemester();

        // 현재 사용자의 ID 조회
        String memberId = getMemberIdFromJwt(request);

        // 사용자의 선호도 정보 조회
        Optional<Member> memberOpt = memberRepository.findOne(memberId);
        if (memberOpt.isEmpty()) {
            return ResponseEntity.status(404).body("멤버를 찾을 수 없습니다.");
        }

        Member member = memberOpt.get();
        double memberDifficulty = member.getDifficulty();
        double memberLearningAmount = member.getLearningAmount();

        // 연도와 학기에 맞는 강의 조회 (전체 강의를 조회하고, 페이징은 직접 처리)
        List<Lecture> lectures = lectureRepository.findByOpenYearAndSemester(year, semester);

        // 추천 강의 정렬
        List<Lecture> recommendedLectures = lectures.stream()
                .sorted(Comparator.comparingDouble(lecture -> calculateSimilarity(memberDifficulty, memberLearningAmount, lecture)))
                .collect(Collectors.toList());

        if (recommendedLectures.isEmpty()) {
            return ResponseEntity.status(404).body("추천할 강의가 없습니다.");
        }

        // 페이지 번호와 크기값을 받아 페이징 처리
        int start = yearSemesterRequestDto.getPage() * yearSemesterRequestDto.getSize();
        int end = Math.min(start + yearSemesterRequestDto.getSize(), recommendedLectures.size());
        List<Lecture> pagedLectures = recommendedLectures.subList(start, end);

        // 페이징 정보와 함께 반환
        Map<String, Object> response = new HashMap<>();
        response.put("lectures", pagedLectures);
        response.put("currentPage", yearSemesterRequestDto.getPage());
        response.put("totalItems", recommendedLectures.size());
        response.put("totalPages", (recommendedLectures.size() / yearSemesterRequestDto.getSize()) + 1);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "강의 성적 수정",
            description = "특정 수강신청 ID에 대한 강의 성적을 수정합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성적 수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력 값"),
            @ApiResponse(responseCode = "404", description = "해당 ID에 대한 수강신청 정보 없음"),
            @ApiResponse(responseCode = "500", description = "내부 서버 오류")
    })
    @PostMapping("/update-grade")
    public ResponseEntity<String> updateGrade(@RequestBody UpdateGradeRequestDto requestDto) {
        try {
            // ID로 LectureEnrollment 조회
            LectureEnrollment enrollment = lectureEnrollmentRepository.findById(requestDto.getEnrollmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid enrollment ID: " + requestDto.getEnrollmentId()));

            // 성적 수정
            enrollment.setGrade(requestDto.getGrade());
            lectureEnrollmentRepository.save(enrollment);

            return ResponseEntity.ok("성적이 성공적으로 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("성적 수정에 실패하였습니다.");
        }
    }


    /**
     * 사용자가 등록한 강의들의 평균 학점을 계산
     */
    @Operation(
            summary = "사용자가 등록한 강의들의 평균 학점 계산",
            description = "JWT에서 memberId를 추출하고 해당 사용자가 등록한 강의들의 평균 학점을 계산하여 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "평균 학점 계산 성공"),
            @ApiResponse(responseCode = "404", description = "등록된 강의가 없음"),
            @ApiResponse(responseCode = "500", description = "내부 서버 오류")
    })
    @PostMapping("/average-grade")
    public ResponseEntity<String> getAverageGrade(HttpServletRequest request,
                                                  @RequestBody YearSemesterRequestDto requestDto) {
        try {
            // JWT에서 memberId 추출
            String memberId = getMemberIdFromJwt(request);
            if (memberId == null) {
                return ResponseEntity.status(401).body("JWT 토큰이 유효하지 않거나 memberId를 추출할 수 없습니다.");
            }

            // memberId에 해당하는 수강신청 목록 조회
            List<LectureEnrollment> enrollments = lectureEnrollmentRepository.findByMember_IdAndLecture_OpenYearAndLecture_Semester(
                    memberId, requestDto.getYear(), requestDto.getSemester());

            if (enrollments.isEmpty()) {
                return ResponseEntity.status(404).body("해당 학기 및 연도에 등록된 강의가 없습니다.");
            }

            // 평균 학점 계산
            double totalGrade = 0;
            for (LectureEnrollment enrollment : enrollments) {
                totalGrade += enrollment.getGrade();
            }
            double averageGrade = totalGrade / enrollments.size();

            // 결과 반환
            return ResponseEntity.ok("사용자가 등록한 강의들의 평균 학점: " + averageGrade);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("내부 서버 오류: " + e.getMessage());
        }
    }


    // 유사도 계산 메서드 (최적화)
    private double calculateSimilarity(double memberDifficulty, double memberLearningAmount, Lecture lecture) {
        double lectureDifficultyAvg = lecture.getDifficultyTotal() / lecture.getDifficultyCount();
        double lectureLearningAmountAvg = lecture.getLearningAmountTotal() / lecture.getLearningAmountCount();

        // 유사도 계산 방식 수정 (절대값의 합을 대신 평균 차이의 합을 사용)
        double difficultyDifference = Math.abs(memberDifficulty - lectureDifficultyAvg);
        double learningAmountDifference = Math.abs(memberLearningAmount - lectureLearningAmountAvg);

        // 유사도는 차이의 합으로 계산
        return difficultyDifference + learningAmountDifference;
    }


    // 시간 충돌 확인 메서드 (기존 코드 재사용)
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