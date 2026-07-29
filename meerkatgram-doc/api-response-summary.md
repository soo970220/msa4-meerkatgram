# API Response Summary

현재 코드 기준으로 각 API에서 반환 가능한 Response를 정리한다.

- 분석 기준: `Controller`, `Service`, `GlobalExceptionHandler`, `SecurityConfiguration`, `TokenAuthenticationFilter`
- 공통 응답 형태: `GlobalRes<T>`
- 대분류: API
- 중분류: HTTP Status
- 소분류: 응답 코드 (`00`, `E01`, `E02` ...)

## 1. 공통 Response Body

### 성공

```json
{
  "code": "00",
  "message": "성공 메시지",
  "data": {}
}
```

### 실패

```json
{
  "code": "E01",
  "message": "에러 메시지",
  "data": "상세 메시지"
}
```

## 2. 공통 에러 코드

| HTTP Status | Code | Handler | 발생 조건 |
|---|---|---|---|
| 400 | `E21` | `MethodArgumentTypeMismatchException` | 요청 파라미터 타입 변환 실패 |
| 400 | `E21` | `MethodArgumentNotValidException` | `@Valid` 요청 바디 검증 실패 |
| 401 | `E01` | `NotRegisteredException` | 로그인 이메일 미존재 또는 비밀번호 불일치 |
| 401 | `E02` | `AuthenticationException` | 인증 필수 API에 인증 정보가 없거나 인증 실패 |
| 401 | `E04` | `InvalidTokenException` | 토큰 없음, 만료, 위조, 형식 오류, DB 토큰 불일치 |
| 403 | `E03` | `AccessDeniedException` | 접근 권한 부족 |
| 404 | `E10` | `DeletedRecordException` | 조회 대상 레코드 없음 또는 삭제된 레코드 |
| 409 | `E11` | `DuplicatedRecordException` | 중복 레코드 |
| 500 | `E40` | `FileManagedException` | 파일 저장/검증/디렉토리 생성 실패 |
| 500 | `E80` | `SQLException` | DB 예외 |
| 500 | `E99` | `Exception` | 처리되지 않은 시스템 예외 |

> 주의: 기존 문서 일부와 달리 현재 `GlobalExceptionHandler` 코드 기준 `E04`는 `401`, `E11`은 `409`, `E40`은 `500`이다.

## 3. Auth API

### 3-1. `POST /api/login`

로그인 처리. `LoginReq`는 `@Valid` 검증 대상이다.

| HTTP Status | Code | Response 종류 | 발생 조건 | Data |
|---|---|---|---|---|
| 200 | `00` | 로그인 성공 | 이메일/비밀번호 인증 성공 | `AuthRes` |
| 400 | `E21` | 요청 바디 검증 실패 | `email`, `password` 누락 또는 패턴 불일치 | 필드별 검증 메시지 `Map<String, String>` |
| 401 | `E01` | 로그인 실패 | 이메일 미존재 또는 비밀번호 불일치 | 예외 메시지 |
| 500 | `E80` | DB 에러 | SQL 예외 발생 | 고정 안내 메시지 |
| 500 | `E99` | 시스템 에러 | 처리되지 않은 예외 | 고정 안내 메시지 |

### 3-2. `POST /api/reissue-token`

Refresh Token으로 Access Token을 재발급한다.

| HTTP Status | Code | Response 종류 | 발생 조건 | Data |
|---|---|---|---|---|
| 200 | `00` | 토큰 재발급 성공 | Refresh Token 검증 및 사용자 조회 성공 | `AuthRes` |
| 401 | `E04` | 토큰 오류 | Refresh Token 없음, 만료, 위조, 형식 오류 | 예외 메시지 |
| 401 | `E04` | 토큰 사용자 오류 | 토큰의 사용자 ID로 회원을 찾을 수 없음 | 예외 메시지 |
| 401 | `E04` | 토큰 상태 오류 | 사용자 `refreshToken`이 `null` | 예외 메시지 |
| 401 | `E04` | 토큰 불일치 | DB Refresh Token과 요청 Refresh Token 불일치 | 예외 메시지 |
| 500 | `E80` | DB 에러 | SQL 예외 발생 | 고정 안내 메시지 |
| 500 | `E99` | 시스템 에러 | 처리되지 않은 예외 | 고정 안내 메시지 |

### 3-3. `POST /api/logout`

로그아웃 처리. `SecurityUrlRegistry.AUTH_REQUIRED_POST_URLS`에 포함되어 인증이 필요하다.

| HTTP Status | Code | Response 종류 | 발생 조건 | Data |
|---|---|---|---|---|
| 200 | `00` | 로그아웃 성공 | 인증 사용자 조회 후 Refresh Token 제거 성공 | `null` |
| 401 | `E02` | 미인증 | Access Token 없이 요청 | 고정 안내 메시지 |
| 401 | `E04` | 토큰 오류 | Access Token 만료, 위조, 형식 오류 | 예외 메시지 |
| 401 | `E04` | 토큰 사용자 오류 | 토큰의 사용자 ID로 회원을 찾을 수 없음 | 예외 메시지 |
| 403 | `E03` | 권한 부족 | 인증은 되었으나 접근 거부 | 고정 안내 메시지 |
| 500 | `E80` | DB 에러 | SQL 예외 발생 | 고정 안내 메시지 |
| 500 | `E99` | 시스템 에러 | 처리되지 않은 예외 | 고정 안내 메시지 |

### 3-4. `POST /api/registration`

회원가입 처리. `RegistrationReq`는 `@Valid` 검증 대상이다.

| HTTP Status | Code | Response 종류 | 발생 조건 | Data |
|---|---|---|---|---|
| 200 | `00` | 회원가입 성공 | 회원 저장 성공 | `null` |
| 400 | `E21` | 요청 바디 검증 실패 | `email`, `password`, `passwordChk`, `nick`, `profile` 누락/패턴 불일치 또는 비밀번호 확인 불일치 | 필드별 검증 메시지 `Map<String, String>` |
| 409 | `E11` | 중복 회원 | 이미 가입된 이메일 | 예외 메시지 |
| 500 | `E80` | DB 에러 | SQL 예외 발생 | 고정 안내 메시지 |
| 500 | `E99` | 시스템 에러 | 처리되지 않은 예외 | 고정 안내 메시지 |

## 4. Post API

### 4-1. `GET /api/posts`

게시글 목록 조회. `page`, `limit`이 없거나 0 이하이면 `PostIndexReq` 생성자에서 기본값으로 보정된다.

| HTTP Status | Code | Response 종류 | 발생 조건 | Data |
|---|---|---|---|---|
| 200 | `00` | 목록 조회 성공 | 게시글 페이지 조회 성공 | `PostIndexRes` |
| 400 | `E21` | 요청 파라미터 타입 오류 | `page`, `limit`에 숫자로 변환할 수 없는 값 전달 | 예외 메시지 |
| 500 | `E80` | DB 에러 | SQL 예외 발생 | 고정 안내 메시지 |
| 500 | `E99` | 시스템 에러 | 처리되지 않은 예외 | 고정 안내 메시지 |

> `PostIndexReq` 필드에 `@Min(1)`이 있으나 컨트롤러 파라미터에 `@Valid` 또는 `@Validated`가 적용되어 있지 않다. 현재 코드 흐름상 0 이하 값은 검증 에러가 아니라 기본값으로 보정된다.

### 4-2. `GET /api/posts/{id}`

게시글 상세 조회. `SecurityUrlRegistry.AUTH_REQUIRED_GET_URLS`에 포함되어 인증이 필요하다.

| HTTP Status | Code | Response 종류 | 발생 조건 | Data |
|---|---|---|---|---|
| 200 | `00` | 상세 조회 성공 | 게시글 조회 성공 | `PostWithUserRes` |
| 400 | `E21` | Path Variable 타입 오류 | `id`에 `long`으로 변환할 수 없는 값 전달 | 예외 메시지 |
| 401 | `E02` | 미인증 | Access Token 없이 요청 | 고정 안내 메시지 |
| 401 | `E04` | 토큰 오류 | Access Token 만료, 위조, 형식 오류 | 예외 메시지 |
| 403 | `E03` | 권한 부족 | 인증은 되었으나 접근 거부 | 고정 안내 메시지 |
| 404 | `E10` | 게시글 없음 | `postRepository.findById(id)` 결과 없음 | 예외 메시지 |
| 500 | `E80` | DB 에러 | SQL 예외 발생 | 고정 안내 메시지 |
| 500 | `E99` | 시스템 에러 | 처리되지 않은 예외 | 고정 안내 메시지 |

> `id`에 `@Min(1)`이 있으나 컨트롤러 클래스에 `@Validated`가 없다. 현재 코드 기준 음수/0은 Bean Validation으로 잡히기보다 조회 후 `E10`으로 처리될 가능성이 높다.

## 5. File API

### 5-1. `POST /api/files/profiles`

프로필 이미지 파일 업로드. 인증 필수 URL 목록에는 포함되어 있지 않다.

| HTTP Status | Code | Response 종류 | 발생 조건 | Data |
|---|---|---|---|---|
| 200 | `00` | 파일 저장 성공 | 파일 확장자 검증 및 저장 성공 | `FileRes` |
| 500 | `E40` | 파일 없음 | `file == null` 또는 `file.isEmpty()` | 예외 메시지 |
| 500 | `E40` | 파일명 오류 | 원본 파일명이 없거나 확장자가 없음 | 예외 메시지 |
| 500 | `E40` | 확장자 오류 | `FileConfig.allowExtensionList()`에 없는 이미지 확장자 | 예외 메시지 |
| 500 | `E40` | 디렉토리 생성 실패 | 저장 디렉토리 생성 실패 | 예외 메시지 |
| 500 | `E40` | 파일 쓰기 실패 | `MultipartFile.transferTo()` 실패 | 예외 메시지 |
| 500 | `E99` | 시스템 에러 | 처리되지 않은 예외 | 고정 안내 메시지 |

### 5-2. `POST /api/files/posts`

게시글 이미지 파일 업로드. 인증 필수 URL 목록에는 포함되어 있지 않다.

| HTTP Status | Code | Response 종류 | 발생 조건 | Data |
|---|---|---|---|---|
| 200 | `00` | 파일 저장 성공 | 파일 확장자 검증 및 저장 성공 | `FileRes` |
| 500 | `E40` | 파일 없음 | `file == null` 또는 `file.isEmpty()` | 예외 메시지 |
| 500 | `E40` | 파일명 오류 | 원본 파일명이 없거나 확장자가 없음 | 예외 메시지 |
| 500 | `E40` | 확장자 오류 | `FileConfig.allowExtensionList()`에 없는 이미지 확장자 | 예외 메시지 |
| 500 | `E40` | 디렉토리 생성 실패 | 저장 디렉토리 생성 실패 | 예외 메시지 |
| 500 | `E40` | 파일 쓰기 실패 | `MultipartFile.transferTo()` 실패 | 예외 메시지 |
| 500 | `E99` | 시스템 에러 | 처리되지 않은 예외 | 고정 안내 메시지 |

## 6. User API

`UserController`는 `/api` 매핑과 `UserService` 주입만 있으며, 현재 구현된 엔드포인트가 없다.

| API | HTTP Status | Code | Response 종류 |
|---|---|---|---|
| 구현 없음 | - | - | 현재 코드 기준 정리할 API Response 없음 |

## 7. 구현 코드 기준 특이사항

| 항목 | 현재 코드 기준 |
|---|---|
| 성공 코드 | 모든 성공 응답은 `code = "00"` |
| 공통 실패 포맷 | 모든 `GlobalExceptionHandler` 응답은 `GlobalRes` 사용 |
| `InvalidTokenException` | `401/E04` |
| `DuplicatedRecordException` | `409/E11` |
| `FileManagedException` | `500/E40` |
| 실제 파일 업로드 경로 | `/api/files/profiles`, `/api/files/posts` |
| 구현된 Post API | `GET /api/posts`, `GET /api/posts/{id}` |
| 인증 필수로 설정된 미구현 API | `POST /api/posts`, `DELETE /api/posts/{id}` |
