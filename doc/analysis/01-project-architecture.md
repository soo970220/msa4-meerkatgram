# 프로젝트 아키텍처 분석

## 1. 프로젝트 성격

Meerkatgram은 이미지 게시글을 중심으로 하는 커뮤니티형 서비스의 백엔드 API 서버입니다. 현재 백엔드 코드는 Spring Boot로 구성되어 있고, 프론트엔드와는 HTTP API로 통신하는 구조입니다.

주요 기능은 다음과 같습니다.

- 인증: 회원가입, 로그인, 토큰 재발급, 로그아웃
- 게시글: 목록 조회, 상세 조회
- 파일: 프로필 이미지 업로드, 게시글 이미지 업로드
- 공통 처리: JWT 인증, CORS, 파일 경로 설정, 공통 응답, 공통 예외 처리

## 2. 기술 스택

`build.gradle`과 실제 코드 기준으로 보면 현재 주요 기술은 다음과 같습니다.

| 영역 | 현재 사용 기술 |
|---|---|
| 언어 | Java 17 |
| 서버 | Spring Boot |
| API | Spring Web MVC |
| 인증/인가 | Spring Security, JWT |
| 요청 검증 | Jakarta Validation |
| DB 접근 | Spring Data JPA, QueryDSL |
| DB | MySQL |
| 문서화 | SpringDoc OpenAPI |
| 보조 라이브러리 | Lombok |
| 빌드 | Gradle |

주의할 점은 기존 `README.md`와 `meerkatgram-doc/1st-doc/03-backend-architecture.md`에는 MyBatis 설명이 남아 있다는 것입니다. 현재 소스 코드에는 Mapper XML 기반 구조가 없고, JPA Repository와 QueryDSL을 사용합니다.

## 3. 전체 패키지 구조

현재 핵심 패키지는 크게 `domain`과 `global`로 나뉩니다.

```text
com.msa4meerkatgram
├── Msa4MeerkatgramApplication.java
├── domain
│   ├── auth
│   ├── file
│   ├── post
│   └── user
└── global
    ├── annotations
    ├── config
    ├── errors
    ├── responses
    ├── security
    └── util
```

`domain`은 서비스 기능별 코드입니다.

| 도메인 | 역할 |
|---|---|
| `auth` | 로그인, 회원가입, 토큰 재발급, 로그아웃 |
| `post` | 게시글 목록 조회, 상세 조회 |
| `file` | 이미지 파일 저장 |
| `user` | 유저 엔티티, 유저 응답 DTO, 현재 컨트롤러/서비스는 거의 비어 있음 |

`global`은 여러 도메인이 공통으로 사용하는 코드입니다.

| 패키지 | 역할 |
|---|---|
| `global.config` | CORS, OpenAPI, 정적 파일 서빙, QueryDSL 설정 |
| `global.errors` | 전역 예외 처리와 커스텀 예외 |
| `global.responses` | 공통 API 응답 형식 |
| `global.security` | Spring Security, JWT, 쿠키, 인증 필터 |
| `global.util.file` | 파일 저장 경로 생성과 실제 저장 로직 |
| `global.annotations.openapi` | Swagger 응답 문서용 커스텀 어노테이션 |

## 4. 레이어드 아키텍처

현재 프로젝트는 전형적인 레이어드 아키텍처에 가깝습니다.

```text
Controller
-> Service
-> Repository 또는 QueryRepository
-> Entity
-> Database
```

보안 처리는 Controller 앞단에서 먼저 실행됩니다.

```text
HTTP 요청
-> SecurityConfiguration
-> TokenAuthenticationFilter
-> Controller
```

예외 처리는 흐름 중간 어디에서든 발생할 수 있고, 대부분 `GlobalExceptionHandler`로 모입니다.

```text
Service에서 예외 발생
-> GlobalExceptionHandler
-> CustomResponseCode
-> GlobalRes
-> HTTP 에러 응답
```

## 5. 인증 아키텍처

인증 관련 클래스는 다음 역할을 가집니다.

| 클래스 | 역할 |
|---|---|
| `SecurityConfiguration` | 인증이 필요한 URL과 필터 체인을 설정 |
| `TokenAuthenticationFilter` | 요청 헤더에서 Access Token을 꺼내 검증 |
| `SecurityAuthenticationProvider` | JWT Claims를 Spring Security의 Authentication으로 변환 |
| `JwtProvider` | Access Token, Refresh Token 생성과 검증 |
| `CookieManager` | Refresh Token 쿠키 저장과 조회 |
| `SecurityExceptionHandler` | Security에서 발생한 인증/권한 예외를 전역 예외 처리로 위임 |
| `SecurityUrlRegistry` | 인증이 필요한 URL 목록 관리 |

로그인 성공 시 흐름은 다음과 같습니다.

```text
AuthController.login()
-> AuthService.login()
-> 이메일로 User 조회
-> 비밀번호 검증
-> Access Token 생성
-> Refresh Token 생성
-> Refresh Token DB 저장
-> Refresh Token 쿠키 저장
-> AuthRes 반환
```

## 6. DB 접근 아키텍처

현재 DB 접근은 JPA 중심입니다.

| 구성 | 설명 |
|---|---|
| `User`, `Post` | JPA Entity |
| `AuthRepository`, `UserRepository`, `PostRepository` | Spring Data JPA Repository |
| `PostQueryRepository` | QueryDSL로 게시글 목록을 fetch join 조회 |
| `QueryDSLConfig` | `JPAQueryFactory` Bean 등록 |

`User`와 `Post`는 soft delete 구조입니다.

```java
@SQLDelete(sql = "UPDATE posts SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
```

즉, 삭제 시 실제 row를 지우는 것이 아니라 `deleted_at`을 채우고, 조회할 때는 `deleted_at IS NULL` 조건이 자동으로 붙습니다.

## 7. 공통 응답 아키텍처

모든 성공 응답은 대체로 `GlobalRes.success(...)`를 통해 다음 형태로 내려갑니다.

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {}
}
```

실패 응답도 `GlobalExceptionHandler`에서 같은 구조를 사용합니다.

```json
{
  "code": "E04",
  "message": "INVALID_TOKEN_ERROR",
  "data": null
}
```

이 구조의 장점은 프론트엔드가 항상 `code`, `message`, `data`를 같은 방식으로 읽을 수 있다는 점입니다.

## 8. 설정 구조

설정 파일은 `src/main/resources/application.yaml`과 `application-prod.yaml`이 있습니다.

`application.yaml`의 주요 설정:

- DB 연결
- JPA 설정
- SQL 초기화 여부
- Swagger 경로
- 로그 레벨
- JWT 설정
- 파일 저장 경로
- CORS 허용 origin

메인 애플리케이션에는 다음 어노테이션이 있습니다.

```java
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing
```

의미는 다음과 같습니다.

| 어노테이션 | 의미 |
|---|---|
| `@SpringBootApplication` | Spring Boot 애플리케이션 시작점 |
| `@ConfigurationPropertiesScan` | `JwtConfig`, `FileConfig`, `CorsConfig` 같은 설정 record를 자동 등록 |
| `@EnableJpaAuditing` | `@CreatedDate`, `@LastModifiedDate` 자동 입력 활성화 |

## 9. 아키텍처 장점

- 기능 코드와 공통 코드가 `domain`, `global`로 분리되어 있습니다.
- Controller, Service, Repository 책임이 대체로 분리되어 있습니다.
- 공통 응답 형식이 있어 API 응답 구조가 통일됩니다.
- JWT 인증 로직을 필터로 분리해 Controller가 인증 검증 세부 구현을 몰라도 됩니다.
- JPA Auditing과 soft delete를 사용해 생성/수정/삭제 시점을 일관되게 관리하려는 방향이 있습니다.
- QueryDSL을 사용해 게시글 목록 조회에서 fetch join을 적용하고 있습니다.

## 10. 아키텍처상 주의점

| 항목 | 현재 상태 | 영향 |
|---|---|---|
| 기존 문서와 코드 불일치 | 기존 문서에는 MyBatis 설명이 남아 있음 | 신규 개발자가 잘못된 구조로 이해할 수 있음 |
| `PostQueryRepository` 역할 표시 | `@RestController`가 붙어 있음 | 실제 역할과 Spring Bean 의미가 어긋남 |
| `GlobalExceptionHandler` 권한 예외 import | `java.nio.file.AccessDeniedException` 사용 | Spring Security 403 예외가 `E03`이 아니라 `E99`로 처리될 수 있음 |
| 파일 업로드 게시글 경로 | 게시글 업로드 메서드가 프로필 저장 로직 호출 | 게시글 이미지가 프로필 경로로 저장될 수 있음 |
| 검증 에러 상세 | 에러 Map을 만들지만 응답에는 넣지 않음 | 프론트엔드가 어떤 필드가 잘못됐는지 알기 어려움 |
