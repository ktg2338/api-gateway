# API Gateway

마이크로서비스 아키텍처를 위한 API Gateway입니다. Spring Cloud Gateway 기반으로 라우팅, JWT 인증, CSRF 보호, Circuit Breaker, Rate Limiting 등을 제공합니다.

## 기술 스택

| 구성요소 | 기술 |
|----------|------|
| 언어 | Kotlin 2.2.0 |
| JDK | Java 21 |
| 프레임워크 | Spring Boot 3.5.6 + Spring Cloud Gateway (WebFlux) |
| 보안 | Spring Security (WebFlux) + JWT (JJWT 0.12.5) |
| 복원력 | Resilience4j Circuit Breaker |
| Rate Limiting | Redis (Reactive Lettuce) |
| API 문서 | SpringDoc OpenAPI |
| 모니터링 | Azure Application Insights |
| 배포 | Azure Container Apps |
| CI/CD | GitHub Actions |

## 프로젝트 구조

```
src/main/kotlin/com/aifactory/apigateway/
├── ApiGatewayApplication.kt              # 메인 애플리케이션
├── config/
│   ├── GatewayConfig.kt                  # 라우팅 설정
│   ├── SecurityConfig.kt                 # Spring Security 설정
│   ├── CustomCircuitBreakerConfig.kt     # Circuit Breaker 설정
│   ├── RateLimitConfig.kt                # Rate Limit 설정
│   ├── JwtProperties.kt                  # JWT 속성
│   └── SwaggerConfig.kt                  # Swagger 설정
├── filter/
│   ├── JwtAuthenticationFilter.kt        # JWT 인증 필터
│   ├── GlobalRateLimitFilter.kt          # Rate Limit 필터
│   └── StatelessCsrfFilter.kt           # CSRF 필터 (Double Submit Cookie)
├── controller/
│   ├── JwtTestController.kt             # JWT 테스트 엔드포인트
│   ├── CsrfController.kt                # CSRF 토큰 발급
│   └── FallbackController.kt            # Circuit Breaker 폴백
├── util/
│   └── JwtUtil.kt                        # JWT 유틸리티
└── exception/
    └── GlobalExceptionHandler.kt         # 전역 예외 처리
```

## 주요 기능

### 라우팅

| 경로 | 대상 서비스 | 기본 URI |
|------|-------------|----------|
| `/auth/**`, `/oauth2/**` | 인증 서비스 | `http://localhost:8081` |
| `/competition/**` | 경진대회 서비스 | `http://localhost:8082` |

### JWT 인증

- HTTP-only 쿠키(`accessToken`)에서 JWT 토큰 추출 및 검증
- 검증된 사용자 정보를 다운스트림 서비스에 헤더로 전달:
  - `X-User-Id`: 사용자 ID
  - `X-User-Roles`: 사용자 역할 (쉼표 구분)
- 공개 경로(`/auth/login`, `/auth/signup` 등)는 인증 불필요

### CSRF 보호

- **Double Submit Cookie 패턴** 적용
- 쿠키 `XSRF-TOKEN`과 헤더 `X-XSRF-TOKEN` 값 일치 여부 검증
- GET/HEAD/OPTIONS 요청은 검증 스킵
- 토큰 미존재 시 UUID 기반 자동 생성

### Circuit Breaker

- **Sliding Window**: 최근 10개 호출 기준
- **실패율 임계값**: 50% 이상 시 OPEN
- **OPEN 대기**: 10초 후 HALF_OPEN 전환
- **타임아웃**: 3초
- 실패 시 `/fallback/*` 엔드포인트에서 503 응답 반환

### Rate Limiting

- **Redis 기반** 토큰 버킷 알고리즘
- **기본 설정**: 50 req/sec (permitted), 100 req/sec (capacity)
- **Key**: 클라이언트 IP 주소
- **초과 시**: 429 Too Many Requests 반환
- **응답 헤더**: `X-RateLimit-Remaining`, `X-RateLimit-Retry-After-Seconds`
- **Fail-open**: Rate Limiter 오류 시 요청 허용 (가용성 우선)

## API 엔드포인트

### 공개 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/csrf-token` | CSRF 토큰 발급 |
| POST | `/test/generate-token` | JWT 테스트 토큰 생성 |
| GET | `/test/protected` | 보호된 엔드포인트 테스트 |
| GET | `/test/info` | 사용자 정보 조회 |
| GET | `/fallback/auth` | 인증 서비스 폴백 |
| GET | `/fallback/competition` | 경진대회 서비스 폴백 |
| GET | `/actuator/health` | 헬스체크 |
| GET | `/swagger-ui.html` | API 문서 |

## 시작하기

### 사전 요구사항

- Java 21+
- Redis

### 환경변수

| 변수명 | 설명 | 필수 |
|--------|------|------|
| `SPRING_PROFILES_ACTIVE` | 활성 프로필 (`prod`) | X |
| `AUTH_SERVICE_URI` | 인증 서비스 URL | X (기본: `http://localhost:8081`) |
| `COMPETITION_SERVICE_URI` | 경진대회 서비스 URL | X (기본: `http://localhost:8082`) |
| `REDIS_HOST` | Redis 호스트 | X (기본: `localhost`) |
| `REDIS_PORT` | Redis 포트 | X (기본: `6379`) |
| `AZURE_REDIS_PASSWORD` | Azure Redis 비밀번호 | O (prod) |
| `JWT_SECRET` | JWT 서명 키 | O |
| `AZURE_MONITOR_CONNECTION_STRING` | Application Insights 연결 문자열 | X |

### 로컬 실행

```bash
./gradlew bootRun
# 포트 8080에서 실행
```

### 빌드

```bash
./gradlew bootJar    # JAR 빌드
./gradlew build      # 전체 빌드
./gradlew test       # 테스트 실행
```

### Docker

```bash
docker build -t api-gateway:latest .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e AUTH_SERVICE_URI="https://auth-service" \
  -e COMPETITION_SERVICE_URI="https://competition-service" \
  -e REDIS_HOST="redis-host" \
  -e JWT_SECRET="your-secret-key" \
  api-gateway:latest
```

## 프로필별 설정

| 프로필 | 용도 |
|--------|------|
| 기본 | 로컬 개발 (Redis localhost, DEBUG 로깅) |
| `prod` | 운영 환경 (Redis SSL, Azure 내부 도메인, 메트릭 내보내기) |

## 테스트

```bash
# 단위 테스트
./gradlew test

# Circuit Breaker 통합 테스트
./test-circuit-breaker.sh
```

Circuit Breaker 테스트 항목:
- 타임아웃 시나리오 (5초 지연, 3초 타임아웃)
- 실패 모드 및 자동 복구
- 50% 실패율 시나리오

## CI/CD

GitHub Actions 워크플로우 (`.github/workflows/deploy.yml`)가 `main` 브랜치 푸시 시 실행:

1. Docker 이미지 빌드 후 Azure Container Registry에 푸시
2. Azure Container Apps 이미지 업데이트
3. 레지스트리 정리 (최신 10개 이미지만 유지)
