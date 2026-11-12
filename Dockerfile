# Azure Container App용 Dockerfile
# Multi-stage build를 사용하여 이미지 크기 최적화

# 1단계: 빌드 스테이지
FROM gradle:8.5-jdk21 AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 관련 파일들 먼저 복사 (캐시 활용을 위해)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/

# 의존성 다운로드 (별도 레이어로 캐시)
RUN gradle dependencies --no-daemon

# 소스 코드 복사
COPY src/ src/

# 애플리케이션 빌드
RUN gradle bootJar --no-daemon

# 2단계: 런타임 스테이지
FROM eclipse-temurin:21-jre-alpine

# 보안을 위한 non-root 사용자 생성
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 작업 디렉토리 설정
WORKDIR /app

#curl -L -o applicationinsights-agent.jar https://github.com/microsoft/ApplicationInsights-Java/releases/latest/download/applicationinsights-agent-3.7.4.jar
COPY applicationinsights-agent.jar /app/
#COPY applicationinsights.json /app/

# 빌드된 JAR 파일 복사 (이름을 명시적으로 지정)
COPY --from=builder /app/build/libs/*.jar /app/app.jar

# 소유권 변경
RUN chown -R appuser:appgroup /app

# non-root 사용자로 전환
USER appuser

# Azure Container App에서 사용하는 포트 노출
EXPOSE 8080

# Java 21에 맞는 최적화된 JVM 옵션
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -Xlog:gc \
               -Djava.security.egd=file:/dev/./urandom"

# 헬스체크 추가 (선택사항)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Application Insights와 함께 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:/app/applicationinsights-agent.jar -Dapplicationinsights.connection.string=$AZURE_MONITOR_CONNECTION_STRING -jar /app/app.jar"]
#ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:/app/applicationinsights-agent.jar -Dapplicationinsights.configuration.file=/app/applicationinsights.json -Dapplicationinsights.connection.string=$AZURE_MONITOR_CONNECTION_STRING -jar /app/app.jar"]