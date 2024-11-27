package schoolplan.schoolplanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import schoolplan.schoolplanner.domain.Lecture;
import schoolplan.schoolplanner.repository.LectureRepository;

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
                                .maxInMemorySize(1024 * 1024 * 10)) // 10 MB 메모리 제한 설정
                        .build())
                .build();
    }

    public void createLecturesFromApi(String year, String semester) {
        fetchLecturesFromApi(year, semester, 1); // 시작 페이지를 1로 설정
    }

    private void fetchLecturesFromApi(String year, String semester, final int currentPage) {
        try {
            // API 호출
            byte[] responseBytes = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("AUTH_KEY", AUTH_KEY)
                            .queryParam("P_YR", year)
                            .queryParam("P_OPEN_SHTM_CD", semester)
                            .queryParam("page", currentPage)
                            .build())
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept-Charset", "UTF-8")
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            // 응답 바이트 배열을 UTF-8로 변환
            String response = new String(responseBytes, StandardCharsets.UTF_8);

            // JSON 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            String sanitizedResponse = response.replaceAll("[\\x00-\\x1F\\x7F]", ""); // Remove control characters
            JsonNode rootNode = objectMapper.readTree(sanitizedResponse);


            // 결과 데이터 가져오기
            JsonNode resultNode = rootNode.path("RESULT");
            if (!resultNode.isArray() || resultNode.isEmpty()) {
                System.out.println("더 이상 데이터를 찾을 수 없습니다. 루프를 종료합니다.");
                return; // RESULT가 비어 있으면 종료
            }

            // 결과 데이터 처리
            for (JsonNode lectureNode : resultNode) {
                // Lecture 객체 생성 및 데이터 매핑
                Lecture lecture = new Lecture();
                lecture.setId(lectureNode.path("OPEN_SBJT_NO").asText());
                lecture.setOpenYear(lectureNode.path("OPEN_YR").asText());
                lecture.setSemester(lectureNode.path("SHTM").asText());
                lecture.setTargetSchoolYear(lectureNode.path("TRGT_SHYR").asInt());
                lecture.setOrganizationClassificationCode(lectureNode.path("ORGN_CLSF_CD").asText());
                lecture.setCollege(lectureNode.path("COLG").asText());
                lecture.setDepartment(lectureNode.path("DEGR_NM_SUST").asText());
                lecture.setSubjectName(lectureNode.path("OPEN_SBJT_NM").asText());
                lecture.setClassNumber(lectureNode.path("OPEN_DCLSS").asText());
                lecture.setIssueDivision(lectureNode.path("CPTN_DIV_NM").asText());
                lecture.setCredits(lectureNode.path("PNT").asInt(0));
                lecture.setProfessorInformation(lectureNode.path("PROF_INFO").asText(null));
                lecture.setScheduleInformation(lectureNode.path("TMTBL_INFO").asText(null));
                lecture.setClassType(lectureNode.path("LSN_TYPE_NM").asText(null));
                lecture.setEvaluationMethod(lectureNode.path("MRKS_EVL_MTHD_NM").asText(null));

                // DB에 저장
                lectureRepository.save(lecture);
            }

            System.out.println("Page " + currentPage + " processed successfully.");

            // 다음 페이지 처리 (재귀 호출)
            fetchLecturesFromApi(year, semester, currentPage + 1);

        } catch (Exception e) {
            System.err.println("강의 정보를 가져오는 데 실패했습니다: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
