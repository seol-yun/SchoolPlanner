package schoolplan.schoolplanner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Lecture {

    @Id
    @Column(name = "lecture_id")
    private String id;  // 과목번호 (OPEN_SBJT_NO)

//    @Column(name = "page_count")
//    private String pageCount;  // 전체 페이지 수 (PAGECNT)

    @Column(name = "open_year")
    private String openYear;  // 개설 년도 (OPEN_YR)

    @Column(name = "semester")
    private String semester;  // 학기 (SHTM)

    @Column(name = "target_school_year")
    private int targetSchoolYear;  // 대상 학년 (TRGT_SHYR)

    @Column(name = "organization_classification_code")
    private String organizationClassificationCode;  // 조직명(대) (ORGN_CLSF_CD)

    @Column(name = "college")
    private String college;  // 단과대학명 (COLG)

    @Column(name = "department")
    private String department;  // 학과명 (DEGR_NM_SUST)

    @Column(name = "subject_name")
    private String subjectName;  // 과목명 (OPEN_SBJT_NM)

    @Column(name = "class_number")
    private String classNumber;  // 분반번호 (OPEN_DCLSS)

    @Column(name = "issue_division")
    private String issueDivision;  // 이수구분 (CPTN_DIV_NM)

    @Column(name = "credits")
    private int credits;  // 학점 (PNT)

//    @Column(name = "theory_hours")
//    private int theoryHours;  // 이론 시수 (THEO_TMCNT)
//
//    @Column(name = "practice_hours")
//    private int practiceHours;  // 실습 시수 (PRAC_TMCNT)
//
//    @Column(name = "course_summary")
//    private String courseSummary;  // 수업 개요 (LSN_SMRY)
//
//    @Column(name = "subject_goal")
//    private String subjectGoal;  // 교과 목표 (SBJT_SHT)
//
//    @Column(name = "reference_literature")
//    private String referenceLiterature;  // 참고 문헌 (TEMT_REF_LITRT)
//
//    @Column(name = "reference_book")
//    private String referenceBook;  // 참고 도서 (REF_BOOK)
//
//    @Column(name = "pre_learning_content")
//    private String preLearningContent;  // 선수 학습 내용 (PRE_LRN_CN)

    @Column(name = "professor_information")
    private String professorInformation;  // 교수 정보 (PROF_INFO)

    @Column(name = "schedule_information")
    private String scheduleInformation;  // 시간표, 강의실 정보 (TMTBL_INFO)

    @Column(name = "class_type")
    private String classType;  // 수업 유형 (LSN_TYPE_NM)

    @Column(name = "evaluation_method")
    private String evaluationMethod;  // 평가 방법 (MRKS_EVL_MTHD_NM)
}
