# 사용할 자바 버전
FROM openjdk:17-jdk-slim

# 애플리케이션 JAR 파일 경로 설정
ADD /build/libs/*.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]