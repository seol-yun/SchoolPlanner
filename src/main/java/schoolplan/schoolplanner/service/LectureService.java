package schoolplan.schoolplanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import schoolplan.schoolplanner.domain.Lecture;
import schoolplan.schoolplanner.repository.LectureRepository;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Service
public class LectureService {

    @Autowired
    private LectureRepository lectureRepository;

    private final String AUTH_KEY = "23A727D0E1FE4D7E8433C67F96B8A2981C4EDA51";  // 인증키
    private final String API_URL = "https://api.cnu.ac.kr/svc/offcam/pub/lsnSmry";
    private final WebClient webClient;

    public LectureService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(API_URL)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs()
                                .maxInMemorySize(1024 * 1024 * 10)) // Set to 10 MB (or adjust as needed)
                        .build())
                .build();
    }
    public void createLecturesFromApi(String year, String semester, String page) {
        try {
            // 1. API 호출
            byte[] responseBytes = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("AUTH_KEY", AUTH_KEY)
                            .queryParam("P_YR", year)
                            .queryParam("P_OPEN_SHTM_CD", semester)
                            .queryParam("page", page)
                            .build())
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept-Charset", "UTF-8")  // UTF-8로 응답을 요청
                    .retrieve()
                    .bodyToMono(byte[].class)  // 바이트 배열로 받기
                    .block();

            // 응답 바이트 배열을 UTF-8로 변환
            String response = new String(responseBytes, StandardCharsets.UTF_8);
            System.out.println(response);

            // JSON 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode resultNode = rootNode.path("RESULT");

            // 데이터 처리 및 DB 저장
            for (JsonNode lectureNode : resultNode) {
                // Lecture 객체 생성
                Lecture lecture = new Lecture();

                // 데이터 매핑
                lecture.setId(lectureNode.path("OPEN_SBJT_NO").asText());  // 과목번호
                // lecture.setPageCount(lectureNode.path("PAGECNT").asText());  // 전체 페이지 수
                lecture.setOpenYear(lectureNode.path("OPEN_YR").asText());  // 개설 년도
                lecture.setSemester(lectureNode.path("SHTM").asText());  // 학기
                lecture.setTargetSchoolYear(lectureNode.path("TRGT_SHYR").asInt());  // 대상 학년
                lecture.setOrganizationClassificationCode(lectureNode.path("ORGN_CLSF_CD").asText());  // 조직명
                lecture.setCollege(lectureNode.path("COLG").asText());  // 단과대학명
                lecture.setDepartment(lectureNode.path("DEGR_NM_SUST").asText());  // 학과명
                lecture.setSubjectName(lectureNode.path("OPEN_SBJT_NM").asText());  // 과목명
                lecture.setClassNumber(lectureNode.path("OPEN_DCLSS").asText());  // 분반번호
                lecture.setIssueDivision(lectureNode.path("CPTN_DIV_NM").asText());  // 이수구분
                lecture.setCredits(lectureNode.path("PNT").asInt(0));  // 학점, 기본값 0
                // lecture.setTheoryHours(lectureNode.path("THEO_TMCNT").asInt(0));  // 이론 시수
                // lecture.setPracticeHours(lectureNode.path("PRAC_TMCNT").asInt(0));  // 실습 시수
                // lecture.setCourseSummary(lectureNode.path("LSN_SMRY").asText(null));  // 수업 개요
                // lecture.setSubjectGoal(lectureNode.path("SBJT_SHT").asText(null));  // 교과 목표
                // lecture.setReferenceLiterature(lectureNode.path("TEMT_REF_LITRT").asText(null));  // 참고 문헌
                // lecture.setReferenceBook(lectureNode.path("REF_BOOK").asText(null));  // 참고 도서
                // lecture.setPreLearningContent(lectureNode.path("PRE_LRN_CN").asText(null));  // 선수 학습 내용
                lecture.setProfessorInformation(lectureNode.path("PROF_INFO").asText(null));  // 교수 정보
                lecture.setScheduleInformation(lectureNode.path("TMTBL_INFO").asText(null));  // 시간표 및 강의실
                lecture.setClassType(lectureNode.path("LSN_TYPE_NM").asText(null));  // 수업 유형
                lecture.setEvaluationMethod(lectureNode.path("MRKS_EVL_MTHD_NM").asText(null));  // 평가 방법

                // DB에 저장
                lectureRepository.save(lecture);
            }

        } catch (Exception e) {
            System.err.println("강의 정보를 가져오는 데 실패했습니다: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
