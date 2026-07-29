# GlobalExceptionHandler 분석

분석 대상:

```text
src/main/java/com/msa4meerkatgram/global/errors/GlobalExceptionHandler.java
```

## 1. 이 클래스의 역할

`GlobalExceptionHandler`는 프로젝트 전체에서 발생한 예외를 한 곳에서 처리하는 클래스입니다.

핵심 어노테이션:

```java
@RestControllerAdvice
```

이 어노테이션은 여러 Controller에서 발생한 예외를 모아서 처리하게 해줍니다. 즉, 각 Controller마다 `try-catch`를 반복해서 쓰지 않아도 됩니다.

쉽게 말하면 다음 역할입니다.

```text
서비스 처리 중 문제가 생김
-> 예외 발생
-> GlobalExceptionHandler가 예외를 받음
-> 정해진 에러 코드와 HTTP 상태로 응답 생성
```

## 2. 공통 에러 응답 생성 방식

현재 모든 에러 응답은 이 메서드를 통해 만들어집니다.

```java
private ResponseEntity<GlobalRes<Void>> generateErrorResponse(CustomResponseCode customResponseCode) {
    return ResponseEntity.status(customResponseCode.getHttpStatus())
            .body(GlobalRes.<Void>from(customResponseCode));
}
```

이 메서드가 하는 일:

1. `CustomResponseCode`에서 HTTP status를 꺼냅니다.
2. `GlobalRes.from(customResponseCode)`로 응답 body를 만듭니다.
3. `ResponseEntity`로 HTTP status와 body를 함께 반환합니다.

예를 들어 `INVALID_TOKEN_ERROR`라면 `CustomResponseCode`에 따라 다음 형태가 됩니다.

```json
{
  "code": "E04",
  "message": "INVALID_TOKEN_ERROR",
  "data": null
}
```

HTTP status는 `401 Unauthorized`입니다.

## 3. 현재 예외 매핑표

현재 코드 기준 예외 처리표입니다.

| 예외 타입 | 응답 코드 | HTTP status | 의미 |
|---|---:|---:|---|
| `NotRegisteredException` | `E01` | 401 | 로그인 이메일 없음 또는 비밀번호 불일치 |
| `AuthenticationException` | `E02` | 401 | 인증되지 않은 요청 |
| `AccessDeniedException` | `E03` | 403 | 권한 부족 의도 |
| `InvalidTokenException` | `E04` | 401 | 토큰 없음, 만료, 위조, 형식 오류 등 |
| `DeletedRecordException` | `E10` | 404 | 조회 대상 없음 또는 삭제된 데이터 |
| `DuplicatedRecordException` | `E20` | 409 | 중복 데이터 |
| `MethodArgumentTypeMismatchException` | `E21` | 400 | URL/path/query parameter 타입 변환 실패 |
| `MethodArgumentNotValidException` | `E21` | 400 | `@Valid` 요청 body 검증 실패 |
| `FileManagedException` | `E40` | 500 | 파일 저장/검증 실패 |
| `SQLException` | `E80` | 500 | DB SQL 예외 |
| `Exception` | `E99` | 500 | 위에서 잡히지 않은 모든 예외 |

## 4. 실제 동작 예시

### 로그인 실패

`AuthService.login()`에서 사용자를 찾지 못하거나 비밀번호가 틀리면 다음 예외가 발생합니다.

```java
throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
```

그러면 `GlobalExceptionHandler`의 이 메서드가 실행됩니다.

```java
@ExceptionHandler(NotRegisteredException.class)
public ResponseEntity<GlobalRes<Void>> notRegisteredException(NotRegisteredException e) {
    log.debug(CustomResponseCode.NOT_REGISTERED_ERROR.name(), e);
    return this.generateErrorResponse(CustomResponseCode.NOT_REGISTERED_ERROR);
}
```

응답:

```json
{
  "code": "E01",
  "message": "NOT_REGISTERED_ERROR",
  "data": null
}
```

### 토큰 오류

`JwtProvider.extractClaims()`에서 토큰이 만료되었거나 잘못된 경우 `InvalidTokenException`이 발생합니다.

```java
throw new InvalidTokenException("토큰이 만료됐습니다.");
```

이 예외는 일반 Service에서 발생할 수도 있고, `TokenAuthenticationFilter`에서 발생할 수도 있습니다.

필터에서 발생한 경우:

```text
TokenAuthenticationFilter
-> InvalidTokenException 발생
-> HandlerExceptionResolver로 예외 위임
-> GlobalExceptionHandler.invalidTokenHandle()
```

응답:

```json
{
  "code": "E04",
  "message": "INVALID_TOKEN_ERROR",
  "data": null
}
```

### 입력값 검증 실패

`AuthController.login()`은 `@Valid @RequestBody LoginReq`를 사용합니다.

```java
public ResponseEntity<GlobalRes<AuthRes>> login(
    @Valid @RequestBody LoginReq loginReq,
    HttpServletResponse response
)
```

이메일이나 비밀번호가 비어 있거나 정규식에 맞지 않으면 `MethodArgumentNotValidException`이 발생합니다.

현재 핸들러는 필드별 에러를 Map으로 만듭니다.

```java
Map<String, String> errors = e.getBindingResult()
    .getFieldErrors()
    .stream()
    .collect(Collectors.toMap(...));
```

하지만 응답에는 이 `errors`를 넣지 않고 `E21`만 내려줍니다.

```json
{
  "code": "E21",
  "message": "INVALID_PARAMETER_ERROR",
  "data": null
}
```

## 5. 현재 구조의 장점

- 예외 응답 형식이 `GlobalRes`로 통일됩니다.
- Controller마다 `try-catch`를 쓰지 않아도 됩니다.
- 커스텀 예외와 Spring 기본 예외를 한 곳에서 관리할 수 있습니다.
- `CustomResponseCode` enum으로 HTTP status와 내부 code를 함께 관리합니다.
- 마지막에 `@ExceptionHandler(Exception.class)`가 있어 예상하지 못한 예외도 응답 형식이 깨지지 않습니다.

## 6. 중요한 문제점

### 6-1. `AccessDeniedException` import가 잘못되어 있습니다

현재 코드:

```java
import java.nio.file.AccessDeniedException;
```

하지만 Spring Security의 권한 부족 예외는 보통 다음 타입입니다.

```java
import org.springframework.security.access.AccessDeniedException;
```

`SecurityExceptionHandler`는 실제로 Spring Security의 `AccessDeniedException`을 사용합니다.

```java
import org.springframework.security.access.AccessDeniedException;
```

따라서 현재 상태에서는 권한 부족 상황이 `UNAUTHORIZED_ERROR(E03)`로 가지 않고, 마지막 `Exception` 핸들러로 떨어져 `SYSTEM_ERROR(E99)`가 될 수 있습니다.

권장 수정:

```java
import org.springframework.security.access.AccessDeniedException;
```

### 6-2. 검증 에러 상세가 응답에 없습니다

현재 코드는 검증 실패 상세를 만들지만 응답에는 넣지 않습니다.

문제:

- 프론트엔드는 어떤 필드가 잘못됐는지 알 수 없습니다.
- `errors` Map을 만든 코드가 실제 API 응답에는 영향을 주지 않습니다.

권장 방향:

```java
private <T> ResponseEntity<GlobalRes<T>> generateErrorResponse(
    CustomResponseCode customResponseCode,
    T data
) {
    return ResponseEntity.status(customResponseCode.getHttpStatus())
        .body(GlobalRes.from(customResponseCode, data));
}
```

그 후 검증 에러에서:

```java
return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR, errors);
```

### 6-3. 커스텀 예외 메시지가 응답에 반영되지 않습니다

예를 들어 Service에서는 다음처럼 구체적인 메시지를 던집니다.

```java
throw new InvalidTokenException("토큰이 일치하지 않습니다.");
```

하지만 응답 body에는 이 메시지가 들어가지 않습니다.

현재 응답:

```json
{
  "code": "E04",
  "message": "INVALID_TOKEN_ERROR",
  "data": null
}
```

정책상 일부러 숨기는 것일 수도 있습니다. 다만 로그인/회원가입/검증 오류처럼 사용자에게 안내가 필요한 상황에서는 메시지를 내려주는 편이 좋습니다.

### 6-4. JPA 환경에서는 `SQLException`만으로 DB 예외를 잡기 어렵습니다

현재 DB 에러 핸들러:

```java
@ExceptionHandler(SQLException.class)
public ResponseEntity<GlobalRes<Void>> SQLHandle(SQLException e) {
    log.error("DB 에러", e);
    return this.generateErrorResponse(CustomResponseCode.DB_ERROR);
}
```

현재 프로젝트는 Spring Data JPA를 사용합니다. JPA나 Spring Data에서 발생하는 DB 예외는 `SQLException` 그대로 올라오기보다 `DataAccessException`, `JpaSystemException` 같은 Spring 예외로 감싸지는 경우가 많습니다.

따라서 실제 DB 에러가 `DB_ERROR(E80)`이 아니라 `SYSTEM_ERROR(E99)`로 처리될 수 있습니다.

권장 검토 대상:

```java
org.springframework.dao.DataAccessException
org.springframework.orm.jpa.JpaSystemException
```

### 6-5. 로그 메시지 포맷이 일부 효과적이지 않습니다

현재 코드:

```java
log.debug(CustomResponseCode.INVALID_PARAMETER_ERROR.name(), errors);
```

SLF4J 로그에서 두 번째 인자를 출력하려면 보통 `{}` placeholder를 사용합니다.

권장:

```java
log.debug("{} {}", CustomResponseCode.INVALID_PARAMETER_ERROR.name(), errors);
```

또는:

```java
log.debug("Validation errors: {}", errors);
```

## 7. 개선 후 기대되는 에러 응답 예시

검증 실패 시 상세를 포함한다면 다음처럼 응답할 수 있습니다.

```json
{
  "code": "E21",
  "message": "INVALID_PARAMETER_ERROR",
  "data": {
    "email": "이메일은 필수 항목입니다.",
    "password": "허용하지 않는 비밀번호 양식입니다."
  }
}
```

토큰 오류에서 메시지를 포함한다면 다음처럼 응답할 수 있습니다.

```json
{
  "code": "E04",
  "message": "토큰이 만료됐습니다.",
  "data": null
}
```

단, 보안상 상세 메시지를 너무 많이 노출하면 공격자가 힌트를 얻을 수 있습니다. 인증 관련 메시지는 서비스 정책에 맞춰 조절해야 합니다.

## 8. 권장 리팩터링 방향

우선순위가 높은 순서입니다.

1. `AccessDeniedException` import를 Spring Security 타입으로 수정합니다.
2. 검증 에러 상세를 응답에 포함할지 정책을 정합니다.
3. `GlobalRes`의 `message`가 enum name이어도 되는지, 사용자용 메시지가 필요한지 정합니다.
4. JPA DB 예외를 처리할 핸들러를 추가할지 검토합니다.
5. 로그 포맷을 `{}` placeholder 방식으로 정리합니다.
6. 예외 핸들러 메서드 이름을 일관되게 맞춥니다.

메서드 이름 예시:

```java
handleNotRegisteredException
handleAuthenticationException
handleAccessDeniedException
handleInvalidTokenException
handleDeletedRecordException
handleDuplicatedRecordException
handleMethodArgumentTypeMismatchException
handleMethodArgumentNotValidException
handleFileManagedException
handleSQLException
handleException
```

## 9. 비전공자용 한 줄 설명

`GlobalExceptionHandler`는 서버에서 문제가 생겼을 때, 그 문제를 사용자와 프론트엔드가 이해할 수 있는 정해진 에러 응답으로 바꿔주는 공통 처리기입니다.
