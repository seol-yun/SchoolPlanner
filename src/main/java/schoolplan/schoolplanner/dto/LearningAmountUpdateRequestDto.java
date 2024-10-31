package schoolplan.schoolplanner.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LearningAmountUpdateRequestDto {
    private String lectureId;
    private double learningAmount;
}