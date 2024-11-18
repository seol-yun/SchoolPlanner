package schoolplan.schoolplanner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Member {

    @Id
    @Column(name = "member_id")
    private String id;
    private String pw;
    private String name;
    private String department;
    private long tendency;
    private double difficulty;
    private double learningAmount;

    public Member() {
    }

    public Member(String id, String pw, String name, String department, long tendency, double difficulty, double learningAmount) {
        this.id = id;
        this.pw = pw;
        this.name = name;
        this.department = department;
        this.tendency = tendency; // 0: 안정형, 1: 밸런스형, 2: 도전형
        this.difficulty = difficulty;
        this.learningAmount = learningAmount;
    }
}

