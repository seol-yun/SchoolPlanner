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
    private String email;
    private String address;
    private String gender;
    private int tendency;
    public Member() {
    }

    public Member(String id, String pw, String name, String email, String address, String gender, int tendency) {
        this.id = id;
        this.pw = pw;
        this.name = name;
        this.email = email;
        this.address = address;
        this.gender = gender;
        this.tendency = tendency; //0:안정형, 1:밸런스형, 2:도전형
    }

}
