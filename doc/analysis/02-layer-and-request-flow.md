# 레이어와 요청 흐름 분석

이 문서는 비전공자도 프로젝트 흐름을 따라갈 수 있도록 각 레이어의 역할과 실제 요청 처리 순서를 설명합니다.

## 1. 레이어란 무엇인가

레이어는 코드의 역할을 나눠 놓은 구역입니다. 한 클래스가 모든 일을 다 하면 복잡해지기 때문에, 프로젝트는 역할별로 일을 나눕니다.

예를 들어 API 요청 하나를 처리할 때도 다음처럼 담당자가 나뉩니다.

```text
Controller: 요청을 받는 입구
Service: 실제 업무 규칙 처리
Repository: DB와 대화
Entity: DB 테이블과 연결되는 객체
Response DTO: 클라이언트에게 보낼 결과 모양
GlobalExceptionHandler: 문제 발생 시 에러 응답 생성
```

## 2. 전체 요청 흐름

일반적인 API 요청 흐름은 다음과 같습니다.

```text
1. 클라이언트가 HTTP 요청을 보냄
2. Spring Security가 인증이 필요한 요청인지 확인
3. JWT가 있으면 TokenAuthenticationFilter가 토큰을 검증
4. Controller가 요청을 받음
5. Request DTO의 입력값을 검증
6. Service가 실제 비즈니스 로직 처리
7. Repository 또는 QueryRepository가 DB 조회/저장
8. Entity를 Response DTO로 변환
9. GlobalRes로 감싸서 응답
10. 예외가 발생하면 GlobalExceptionHandler가 에러 응답 생성
```

## 3. 각 레이어의 현재 역할

| 레이어 | 현재 프로젝트 예시 | 쉽게 말하면 |
|---|---|---|
| Security Filter | `TokenAuthenticationFilter` | 문 앞에서 출입증인 JWT를 확인 |
| Controller | `AuthController`, `PostController`, `FileController` | 요청을 받는 창구 |
| Request DTO | `LoginReq`, `RegistrationReq`, `PostIndexReq` | 클라이언트가 보낸 데이터의 모양 |
| Service | `AuthService`, `PostService`, `FileService` | 실제 업무 규칙 처리 |
| Repository | `AuthRepository`, `PostRepository`, `UserRepository` | DB에 물어보거나 저장 |
| QueryRepository | `PostQueryRepository` | 복잡한 조회를 QueryDSL로 처리 |
| Entity | `User`, `Post` | DB 테이블과 연결되는 Java 객체 |
| Response DTO | `AuthRes`, `PostIndexRes`, `FileRes` | 클라이언트에게 보여줄 데이터 모양 |
| Global Response | `GlobalRes` | 모든 응답을 같은 봉투에 담는 역할 |
| Exception Handler | `GlobalExceptionHandler` | 에러를 같은 형식으로 바꿔주는 역할 |

## 4. 로그인 요청 흐름

요청:

```text
POST /api/login
```

흐름:

```text
AuthController.login()
-> @Valid로 LoginReq 검사
-> AuthService.login()
-> AuthRepository.findByEmail()
-> PasswordEncoder로 비밀번호 비교
-> JwtProvider로 Access Token 생성
-> JwtProvider로 Refresh Token 생성
-> User.refreshToken에 Refresh Token 저장
-> CookieManager로 Refresh Token 쿠키 저장
-> AuthRes 생성
-> GlobalRes.success(AuthRes)
```

성공 응답은 대략 다음 형태입니다.

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "user": {
      "user": {
        "id": 1,
        "email": "test@test.com",
        "nick": "tester",
        "role": "NORMAL",
        "profile": "/files/profiles/sample.png",
        "createdAt": "2026-07-10T10:00:00"
      },
      "countPosts": 3
    },
    "accessToken": "..."
  }
}
```

로그인 실패 예시:

```text
이메일이 없음
-> AuthService에서 NotRegisteredException 발생
-> GlobalExceptionHandler.notRegisteredException()
-> E01, 401 응답
```

## 5. 회원가입 요청 흐름

요청:

```text
POST /api/registration
```

흐름:

```text
AuthController.registration()
-> @Valid로 RegistrationReq 검사
-> AuthService.registration()
-> AuthRepository.existsByEmail()로 이메일 중복 확인
-> PasswordEncoder로 비밀번호 암호화
-> User Entity 생성
-> AuthRepository.save()
-> GlobalRes.success()
```

입력값 검증은 `RegistrationReq`에서 처리합니다.

- 이메일 필수
- 이메일 형식 검사
- 비밀번호 필수
- 비밀번호 형식 검사
- 비밀번호 확인 필수
- 닉네임 필수
- 프로필 필수
- `@AssertTrue`로 비밀번호와 비밀번호 확인 일치 여부 검사

검증에 실패하면 `MethodArgumentNotValidException`이 발생하고, `GlobalExceptionHandler`가 `E21` 응답을 만듭니다.

## 6. 토큰 재발급 요청 흐름

요청:

```text
POST /api/reissue-token
```

흐름:

```text
AuthController.reissue()
-> AuthService.reissue()
-> JwtProvider.extractRefreshToken()으로 쿠키에서 Refresh Token 추출
-> JwtProvider.extractClaims()로 토큰 검증
-> AuthRepository.findById()로 사용자 조회
-> DB에 저장된 Refresh Token과 요청 쿠키의 Refresh Token 비교
-> 새 Access Token과 Refresh Token 생성
-> DB와 쿠키 갱신
-> AuthRes 반환
```

이 구조는 Refresh Token 탈취나 불일치를 어느 정도 막기 위한 구조입니다. 단순히 쿠키에 토큰이 있다고 통과시키지 않고, DB에 저장된 토큰과 같은지 한 번 더 비교합니다.

## 7. 로그아웃 요청 흐름

요청:

```text
POST /api/logout
```

`SecurityUrlRegistry.AUTH_REQUIRED_POST_URLS`에 포함되어 있어 인증이 필요합니다.

흐름:

```text
TokenAuthenticationFilter가 Access Token 검증
-> SecurityContext에 Claims 저장
-> AuthController.logout()에서 @AuthenticationPrincipal Claims 사용
-> AuthService.logout()
-> DB의 refreshToken을 null로 변경
-> 쿠키의 Refresh Token 만료 처리
-> GlobalRes.success()
```

## 8. 게시글 목록 조회 흐름

요청:

```text
GET /api/posts?page=1&limit=6
```

흐름:

```text
PostController.index()
-> PostIndexReq 생성
-> PostService.index()
-> offset 계산
-> PostQueryRepository.pagination()
-> QueryDSL로 Post와 User fetch join 조회
-> PostRepository.count()로 전체 개수 조회
-> PostIndexRes 생성
-> GlobalRes.success(PostIndexRes)
```

`PostIndexReq`는 `page`, `limit`이 없거나 0 이하이면 기본값을 사용합니다.

```text
page 기본값: 1
limit 기본값: 6
```

주의할 점:

- `PostIndexReq` 필드에 `@Min`이 있지만, 현재 Controller에 `@Validated`가 없습니다.
- 현재 생성자에서 0 이하 값을 기본값으로 바꾸므로, 0 이하 값은 에러가 아니라 보정될 가능성이 높습니다.

## 9. 게시글 상세 조회 흐름

요청:

```text
GET /api/posts/{id}
```

`SecurityUrlRegistry.AUTH_REQUIRED_GET_URLS`에 포함되어 있어 인증이 필요합니다.

흐름:

```text
TokenAuthenticationFilter가 Access Token 검증
-> PostController.show()
-> PostService.show()
-> PostRepository.findById(id)
-> PostWithUserRes.from(Post)
-> GlobalRes.success(PostWithUserRes)
```

게시글이 없으면:

```text
PostRepository.findById(id) 결과 없음
-> DeletedRecordException 발생
-> GlobalExceptionHandler.deletedRecordHandler()
-> E10, 404 응답
```

## 10. 파일 업로드 흐름

프로필 업로드 요청:

```text
POST /api/files/profiles
```

흐름:

```text
FileController.storeProfile()
-> FileService.storeProfile()
-> LocalFileManager.generateProfilePath()
-> LocalFileManager.extractExtension()
-> LocalFileManager.saveFile()
-> FileRes 반환
-> GlobalRes.success(FileRes)
```

게시글 이미지 업로드 요청:

```text
POST /api/files/posts
```

의도상 흐름은 다음이어야 합니다.

```text
FileController.storePosts()
-> FileService.storePosts()
-> LocalFileManager.generatePostPath()
-> LocalFileManager.saveFile()
```

하지만 현재 코드는 `FileController.storePosts()`가 `fileService.storeProfile(file)`을 호출합니다. 그래서 게시글 이미지가 프로필 이미지 경로로 저장될 수 있습니다.

## 11. 에러 흐름

에러는 크게 두 곳에서 발생합니다.

첫 번째는 Controller 이후의 일반 처리 흐름입니다.

```text
Controller 또는 Service에서 예외 발생
-> GlobalExceptionHandler가 예외 타입별로 처리
-> CustomResponseCode 선택
-> GlobalRes 에러 응답 반환
```

두 번째는 Security Filter에서 발생하는 JWT 예외입니다.

```text
TokenAuthenticationFilter에서 토큰 검증 실패
-> InvalidTokenException 발생
-> HandlerExceptionResolver로 예외 위임
-> GlobalExceptionHandler가 처리
-> E04 응답 반환
```

이 구조 덕분에 JWT 검증 실패도 일반 API 에러처럼 `GlobalRes` 형식으로 통일됩니다.

## 12. 비전공자용 핵심 이해

이 프로젝트를 가장 단순하게 보면 다음 구조입니다.

```text
Controller는 요청을 받는다.
Service는 일을 처리한다.
Repository는 DB에 다녀온다.
Response DTO는 결과를 보기 좋게 정리한다.
GlobalRes는 모든 결과를 같은 봉투에 담는다.
GlobalExceptionHandler는 문제가 생겼을 때 에러 봉투를 만든다.
Security Filter는 요청이 들어오기 전에 로그인 여부를 확인한다.
```

따라서 새 기능을 만들 때는 보통 다음 순서로 파일이 추가됩니다.

```text
Request DTO
-> Controller 메서드
-> Service 메서드
-> Repository 메서드
-> Response DTO
-> 필요한 예외와 응답 코드
```
