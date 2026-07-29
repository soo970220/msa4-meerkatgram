# 코드 컨벤션 분석

이 문서는 현재 프로젝트에서 사용 중인 코드 스타일과 일관성 문제, 개선 권고를 정리합니다.

## 1. 현재 사용 중인 컨벤션

현재 코드에서 반복적으로 보이는 패턴은 다음과 같습니다.

| 항목 | 현재 패턴 |
|---|---|
| 패키지 구조 | `domain/{기능}/controllers`, `services`, `repositories`, `requests`, `responses`, `entities` |
| 공통 코드 | `global` 아래에 config, errors, responses, security, util 배치 |
| DTO | Java `record` 사용 |
| Entity | JPA Entity에 Lombok `@Getter`, `@Setter` 사용 |
| 의존성 주입 | `@RequiredArgsConstructor`와 `final` 필드 사용 |
| API 응답 | `GlobalRes.success(...)` 또는 `GlobalRes.from(...)` 사용 |
| 에러 코드 | `CustomResponseCode` enum 사용 |
| 트랜잭션 | 인증 Service 일부 메서드에 `@Transactional(rollbackFor = Exception.class)` 사용 |
| 문서화 | Controller에 Swagger/OpenAPI 어노테이션 일부 사용 |

이 방향 자체는 좋습니다. 기능별 코드와 공통 코드가 나뉘어 있고, Controller가 Service를 호출하는 기본 흐름도 명확합니다.

## 2. 패키지와 클래스 네이밍

현재 패키지는 복수형을 주로 사용합니다.

```text
controllers
services
repositories
requests
responses
entities
```

한 가지 방식으로 정하면 유지하는 것이 좋습니다. 현재는 대부분 복수형이므로 새 파일도 복수형 패키지에 맞추는 편이 자연스럽습니다.

권장 규칙:

- Controller 클래스는 `AuthController`, `PostController`처럼 도메인명 + `Controller`
- Service 클래스는 `AuthService`, `PostService`처럼 도메인명 + `Service`
- Repository 클래스는 `AuthRepository`, `PostRepository`처럼 도메인명 + `Repository`
- Request DTO는 `LoginReq`, `RegistrationReq`처럼 요청 목적 + `Req`
- Response DTO는 `AuthRes`, `PostIndexRes`처럼 응답 목적 + `Res`

## 3. 현재 일관성이 깨진 부분

### 3-1. `PostQueryRepository`의 어노테이션

현재 코드:

```java
@RestController
public class PostQueryRepository {
```

이 클래스는 HTTP 요청을 받는 Controller가 아니라 QueryDSL 조회를 담당하는 클래스입니다. 따라서 다음 중 하나가 더 적절합니다.

```java
@Repository
```

또는

```java
@Component
```

권장:

- DB 조회 역할이라는 의미를 살리려면 `@Repository`
- 단순 QueryDSL helper 성격이면 `@Component`

### 3-2. `FileController.storePosts()` 호출 대상

현재 코드:

```java
@PostMapping("/files/posts")
public ResponseEntity<GlobalRes<FileRes>> storePosts(
    @ModelAttribute MultipartFile file
) {
   return ResponseEntity.ok(GlobalRes.success(fileService.storeProfile(file)));
}
```

게시글 파일 업로드 API인데 `storeProfile(file)`을 호출하고 있습니다. 의도상 다음이 맞습니다.

```java
return ResponseEntity.ok(GlobalRes.success(fileService.storePosts(file)));
```

이 문제는 기능 동작에 직접 영향을 줍니다.

### 3-3. `GlobalExceptionHandler`의 권한 예외 import

현재 코드:

```java
import java.nio.file.AccessDeniedException;
```

Spring Security에서 사용하는 권한 예외는 보통 다음 타입입니다.

```java
import org.springframework.security.access.AccessDeniedException;
```

현재 `SecurityExceptionHandler`는 Spring Security의 `AccessDeniedException`을 넘기고 있습니다. 그런데 `GlobalExceptionHandler`는 파일 접근 예외 타입을 받고 있으므로, 403 권한 예외가 `UNAUTHORIZED_ERROR`로 처리되지 않고 마지막 `Exception` 핸들러로 떨어질 수 있습니다.

### 3-4. 검증 에러 상세를 만들지만 응답에 넣지 않음

현재 `methodArgumentNotValidHandle()`은 필드별 에러를 `Map<String, String>`으로 만듭니다.

```java
Map<String, String> errors = ...
```

하지만 반환은 다음처럼 상세 없는 응답입니다.

```java
return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
```

즉, 서버 로그에는 남기려는 의도가 있지만 클라이언트는 어떤 필드가 잘못됐는지 알 수 없습니다.

선택지는 두 가지입니다.

| 선택 | 설명 |
|---|---|
| 상세를 응답에 포함 | 프론트엔드가 사용자에게 정확한 필드 에러를 보여줄 수 있음 |
| 상세를 응답하지 않음 | 보안상 노출을 줄일 수 있으나, 현재 만든 `errors` Map이 의미가 약해짐 |

일반적인 회원가입/로그인 폼에서는 필드별 검증 메시지를 응답에 포함하는 편이 사용성이 좋습니다.

### 3-5. 사용하지 않는 코드

현재 확인된 불필요하거나 사용이 약한 코드:

| 파일 | 내용 |
|---|---|
| `DuplicatedUserException.java` | 현재 검색 기준 사용처가 없음 |
| `PostService` | `JPAQueryFactory queryFactory` 주입 필드가 사용되지 않음 |
| `UserController`, `UserService` | 구조만 있고 실제 API/로직은 거의 없음 |

사용하지 않는 코드는 유지할 이유가 명확하지 않으면 제거하거나 TODO를 남기는 것이 좋습니다.

### 3-6. 필드명 오타와 표현 통일

현재 `PostWithUserRes`에는 `createAt` 필드가 있습니다.

```java
LocalDateTime createAt
```

엔티티 필드는 `createdAt`입니다. 응답 DTO도 `createdAt`으로 맞추는 것이 좋습니다.

또한 Swagger 태그에 `인증 APT`라고 되어 있는데, `인증 API` 오타로 보입니다.

## 4. 포맷팅 스타일

현재 코드에는 다음처럼 쉼표를 줄 앞에 배치하는 스타일이 자주 보입니다.

```java
public record GlobalRes<T> (
    String code
    ,String message
    ,T data
)
```

이 스타일도 팀이 합의하면 사용할 수는 있습니다. 다만 Java 프로젝트에서는 보통 다음 스타일이 더 흔합니다.

```java
public record GlobalRes<T>(
    String code,
    String message,
    T data
) {
}
```

권장:

- 팀 컨벤션을 하나 정합니다.
- IDE formatter 설정을 공유합니다.
- `package` 앞 불필요한 공백, 들여쓰기, 빈 줄 수를 formatter로 정리합니다.

## 5. 주석 컨벤션

현재 코드는 학습용 설명 주석이 많은 편입니다. 학습 단계에서는 도움이 됩니다.

다만 운영 코드 기준으로는 다음 원칙이 좋습니다.

- 코드만 봐도 알 수 있는 설명은 줄입니다.
- 왜 이렇게 했는지 설명하는 주석은 남깁니다.
- 오타가 있는 주석은 수정합니다.
- 예전 구조와 맞지 않는 주석은 제거합니다.

예시:

```java
// 해당 클래스가 JPA 엔티티임을 선언
@Entity
```

이 정도는 Java/Spring에 익숙한 개발자에게는 반복 설명입니다. 반면 아래 같은 주석은 의미가 있습니다.

```java
// 물리적 FK 생성은 하지 않고 JPA 관계만 사용한다.
@ForeignKey(ConstraintMode.NO_CONSTRAINT)
```

## 6. 예외와 응답 컨벤션

현재 에러 코드는 `CustomResponseCode` 하나에서 관리하므로 방향은 좋습니다.

개선하면 좋은 점:

- enum name을 그대로 message로 쓰는 대신 사용자용 메시지 필드를 추가합니다.
- 커스텀 예외의 `e.getMessage()`를 응답에 포함할지 정책을 정합니다.
- 인증/인가/검증/DB/시스템 예외를 명확히 분류합니다.

현재 응답:

```json
{
  "code": "E01",
  "message": "NOT_REGISTERED_ERROR",
  "data": null
}
```

사용자 친화적인 응답 예시:

```json
{
  "code": "E01",
  "message": "아이디와 비밀번호를 확인해주세요.",
  "data": null
}
```

## 7. 설정 파일 컨벤션

현재 `application.yaml`은 JPA 설정을 가지고 있습니다. 그런데 `application-prod.yaml`에는 MyBatis 설정이 남아 있습니다.

```yaml
mybatis:
  mapper-locations: classpath:mapper/**/*Mapper.xml
  type-aliases-package: com.msa4meerkatgram
```

현재 코드가 JPA 기준이라면 prod 설정에서도 MyBatis 설정은 제거하거나, 실제로 MyBatis를 사용할 계획인지 명확히 해야 합니다.

권장:

- local, prod 모두 현재 사용하는 기술 기준으로 정리
- 불필요한 설정 제거
- 민감한 값은 환경 변수로만 주입
- `server.error.include-stacktrace`는 prod에서 `never` 유지

## 8. 테스트 컨벤션

현재 테스트는 기본 컨텍스트 로딩 테스트만 있습니다.

```java
@SpringBootTest
class Msa4MeerkatgramApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

추가하면 좋은 테스트:

| 대상 | 테스트 예시 |
|---|---|
| `GlobalExceptionHandler` | 예외별 HTTP status와 code 검증 |
| `AuthService` | 로그인 성공, 비밀번호 실패, 중복 회원가입 |
| `JwtProvider` | 토큰 생성, 만료/위조/형식 오류 처리 |
| `FileService` | 프로필/게시글 파일 저장 경로 분리 |
| `PostService` | 게시글 목록 페이징, 없는 게시글 조회 |

## 9. 우선순위별 개선 권고

### 높은 우선순위

| 항목 | 이유 |
|---|---|
| `AccessDeniedException` import 수정 | 인증/인가 에러 응답이 잘못 나갈 수 있음 |
| `FileController.storePosts()` 호출 수정 | 실제 기능 동작 오류 |
| `PostQueryRepository` 어노테이션 수정 | 레이어 의미가 잘못 표현됨 |

### 중간 우선순위

| 항목 | 이유 |
|---|---|
| 검증 에러 상세 응답 정책 정리 | 프론트엔드 폼 처리에 영향 |
| 기존 문서의 MyBatis 설명 정리 | 신규 개발자 혼란 방지 |
| 사용하지 않는 필드/클래스 정리 | 코드 이해 비용 감소 |
| `createdAt` 필드명 통일 | API 응답 일관성 |

### 낮은 우선순위

| 항목 | 이유 |
|---|---|
| 줄바꿈과 쉼표 스타일 통일 | 가독성 개선 |
| 주석 오타 수정 | 유지보수성 개선 |
| Swagger 어노테이션 정리 | API 문서 품질 개선 |
