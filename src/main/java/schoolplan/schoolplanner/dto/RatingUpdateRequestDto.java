
package schoolplan.schoolplanner.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingUpdateRequestDto {
    private String lectureId;
    private double rating;
}