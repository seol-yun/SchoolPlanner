
package schoolplan.schoolplanner.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DifficultyUpdateRequestDto {
    private String lectureId;
    private double difficulty;
}