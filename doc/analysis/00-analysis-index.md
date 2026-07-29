# Meerkatgram 프로젝트 분석 인덱스

이 문서는 현재 소스 코드 기준으로 프로젝트 구조, 레이어 흐름, 코드 컨벤션, 공통 예외 처리 구조를 분석한 자료입니다.

분석 기준 파일과 범위:

- `src/main/java/com/msa4meerkatgram`
- `src/main/resources/application.yaml`
- `build.gradle`
- 요청에서 지정한 `src/main/java/com/msa4meerkatgram/global/errors/GlobalExceptionHandler.java`

## 문서 구성

| 문서 | 내용 |
|---|---|
| `01-project-architecture.md` | 전체 아키텍처, 주요 기술, 패키지 구조, 설정 구조 |
| `02-layer-and-request-flow.md` | 비전공자도 이해할 수 있는 레이어별 역할과 요청 흐름 |
| `03-code-convention-review.md` | 현재 코드 컨벤션, 일관성, 개선 권고 |
| `04-global-exception-handler.md` | `GlobalExceptionHandler` 중심의 예외 처리 흐름과 주의점 |

## 핵심 요약

현재 프로젝트는 Spring Boot 기반의 백엔드 API 서버입니다. 기능별 코드는 `domain` 아래에 있고, 보안, 응답, 예외, 설정, 파일 유틸처럼 여러 기능에서 같이 쓰는 코드는 `global` 아래에 있습니다.

요청 처리의 큰 흐름은 다음과 같습니다.

```text
클라이언트
-> Spring Security Filter
-> Controller
-> Service
-> Repository 또는 QueryDSL 조회 클래스
-> DB
-> Response DTO
-> GlobalRes
-> 클라이언트
```

예외가 발생하면 대부분 `GlobalExceptionHandler`가 받아서 `GlobalRes` 형식으로 통일된 에러 응답을 만듭니다.

## 현재 코드 기준 주요 특징

- Java 17, Spring Boot, Spring Web, Spring Security, Validation, Spring Data JPA, QueryDSL, MySQL을 사용합니다.
- 기존 문서 일부에는 MyBatis 기반 설명이 남아 있지만, 현재 소스 코드는 JPA Repository와 QueryDSL 중심입니다.
- `User`, `Post` 엔티티는 JPA Auditing과 soft delete를 사용합니다.
- 인증은 Access Token을 응답으로 내려주고, Refresh Token은 DB와 쿠키에 저장하는 구조입니다.
- API 응답은 `GlobalRes<T>`로 감쌉니다.
- 에러 응답 코드는 `CustomResponseCode` enum에서 관리합니다.

## 우선 확인해야 할 개선 포인트

| 중요도 | 항목 | 요약 |
|---|---|---|
| 높음 | 권한 예외 import 불일치 | `GlobalExceptionHandler`가 `java.nio.file.AccessDeniedException`을 import하고 있어 Spring Security 권한 예외를 의도대로 처리하지 못할 수 있습니다. |
| 높음 | 파일 게시글 업로드 메서드 호출 오류 | `FileController.storePosts()`가 `fileService.storeProfile(file)`을 호출합니다. 게시글 파일은 `storePosts(file)`을 호출해야 합니다. |
| 중간 | Query Repository 어노테이션 | `PostQueryRepository`가 `@RestController`로 등록되어 있습니다. 조회용 컴포넌트이므로 `@Repository` 또는 `@Component`가 적절합니다. |
| 중간 | 검증 에러 상세 응답 누락 | `MethodArgumentNotValidException`에서 필드별 에러를 만들지만 실제 응답 `data`에는 넣지 않습니다. |
| 중간 | 문서와 코드 불일치 | README와 기존 아키텍처 문서에 MyBatis 설명이 남아 있어 현재 JPA 코드와 맞지 않습니다. |

## 읽는 순서 추천

1. 전체 구조를 보고 싶으면 `01-project-architecture.md`
2. 요청이 코드 안에서 어떻게 흘러가는지 알고 싶으면 `02-layer-and-request-flow.md`
3. 코드 스타일과 리팩터링 포인트를 보고 싶으면 `03-code-convention-review.md`
4. 요청 파일인 `GlobalExceptionHandler.java`를 깊게 보고 싶으면 `04-global-exception-handler.md`
