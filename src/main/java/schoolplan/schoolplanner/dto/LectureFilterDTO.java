package schoolplan.schoolplanner.dto;

public class LectureFilterDTO {
    private String openYear;
    private String semester;

    // Getters and Setters
    public String getOpenYear() {
        return openYear;
    }

    public void setOpenYear(String openYear) {
        this.openYear = openYear;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }
}
