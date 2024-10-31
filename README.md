# 수강신청 도우미 어플리케이션(서버)

- JDK 17버전 다운로드 -> 에디터에서 설정
- H2 데이터베이스 설치(https://www.h2database.com) 2.1.214 버전 이상
- 데이터베이스 파일 생성 방법
  ```
  jdbc:h2:~/schoolplanner (최소 한번)
  ~/jpashop.mv.db 파일 생성 확인
  이후 부터는 jdbc:h2:tcp://localhost/~/schoolplanner 이렇게 접속
  ```
- cmd창에서 H2다운받은 폴더로 이동 -> bin에서 h2.bat명령어 실행
- 처음 한번 spring.jpa.hibernate.ddl-auto: create 로 설정 후에는 none으로 설정
  
