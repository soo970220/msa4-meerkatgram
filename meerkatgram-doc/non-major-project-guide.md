# Meerkatgram 비전공자용 프로젝트 설명서

이 문서는 비전공자도 현재 프로젝트가 어떤 역할을 하고, 코드가 어떻게 나뉘어 있고, 왜 Spring/JPA를 쓰는지 이해할 수 있도록 풀어서 설명한 문서입니다.

기준은 현재 작업 브랜치인 `feature/v2/migration-jpa`의 코드입니다. 추가로, 사용자가 언급한 `dev` 브랜치의 MyBatis 구조도 읽어서 현재 JPA 구조와 비교했습니다.

---

## 1. 이 프로젝트는 무엇인가

Meerkatgram은 이미지 게시글을 중심으로 하는 커뮤니티 서비스의 백엔드 API 서버입니다.

쉽게 말하면 프론트엔드 화면에서 사용자가 버튼을 누르거나 글 목록을 열 때, 실제 데이터를 처리해 주는 서버입니다.

예를 들어 사용자가 다음 행동을 한다고 가정합니다.

- 회원가입한다.
- 로그인한다.
- 게시글 목록을 본다.
- 게시글 상세를 본다.
- 프로필 이미지를 업로드한다.
- 게시글 이미지를 업로드한다.

이때 브라우저 화면은 서버에게 HTTP 요청을 보내고, 이 프로젝트는 요청을 받아 DB를 조회하거나 저장한 뒤 JSON 응답을 돌려줍니다.

전체 흐름은 다음과 같습니다.

```text
사용자
-> 프론트엔드 화면
-> HTTP 요청
-> Meerkatgram 백엔드 서버
-> DB 또는 파일 저장소
-> JSON 응답
-> 프론트엔드 화면에 결과 표시
```

---

## 2. 현재 사용 기술

현재 코드 기준 주요 기술은 다음과 같습니다.

| 영역 | 기술 | 쉽게 말하면 |
|---|---|---|
| 언어 | Java 17 | 서버 코드를 작성하는 프로그래밍 언어 |
| 서버 프레임워크 | Spring Boot | Java 서버를 빠르게 만들게 해주는 도구 묶음 |
| 웹 API | Spring Web MVC | `/api/login` 같은 주소를 Java 메서드와 연결해 주는 기능 |
| 보안 | Spring Security | 로그인 여부와 권한을 검사하는 기능 |
| 인증 토큰 | JWT | 로그인한 사용자를 구분하기 위한 디지털 출입증 |
| DB 접근 | Spring Data JPA | Java 객체를 DB 테이블과 연결해 주는 기술 |
| 복잡한 DB 조회 | QueryDSL | Java 코드로 SQL 비슷한 조회를 안전하게 작성하는 도구 |
| DB | MySQL | 사용자, 게시글 데이터를 저장하는 데이터베이스 |
| 입력 검증 | Jakarta Validation | 이메일, 비밀번호 형식 등을 자동 검사 |
| API 문서 | SpringDoc OpenAPI | Swagger 문서를 만들어 주는 도구 |
| 코드 보조 | Lombok | getter, 생성자 같은 반복 코드를 줄여 주는 도구 |
| 빌드 | Gradle | 프로젝트 실행, 테스트, 의존성 관리를 담당 |

중요한 점은 현재 브랜치는 MyBatis가 아니라 JPA 중심이라는 것입니다.

`application-prod.yaml`에는 `mybatis:` 설정이 남아 있지만, 현재 `build.gradle`에는 MyBatis 의존성이 없고, 실제 코드도 `Mapper.xml`이 아니라 `JpaRepository`, `@Entity`, QueryDSL을 사용합니다. 즉, 현재 브랜치에서는 MyBatis 설정이 이전 버전 흔적으로 남아 있을 가능성이 높습니다.

---

## 3. 프로젝트 폴더 구조

핵심 소스 코드는 `src/main/java/com/msa4meerkatgram` 아래에 있습니다.

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

크게 보면 `domain`과 `global`로 나뉩니다.

`domain`은 서비스의 실제 기능입니다.

| 폴더 | 역할 |
|---|---|
| `auth` | 회원가입, 로그인, 토큰 재발급, 로그아웃 |
| `post` | 게시글 목록 조회, 게시글 상세 조회 |
| `file` | 프로필 이미지, 게시글 이미지 업로드 |
| `user` | 사용자 정보, 사용자 DB 테이블, 사용자 응답 데이터 |

`global`은 여러 기능에서 공통으로 쓰는 기반 코드입니다.

| 폴더 | 역할 |
|---|---|
| `config` | CORS, Swagger, 파일 서빙, QueryDSL 설정 |
| `errors` | 에러를 한곳에서 처리 |
| `responses` | 모든 API 응답 형식을 통일 |
| `security` | JWT, 로그인 검사, 쿠키, Spring Security 설정 |
| `util.file` | 파일명 생성, 파일 저장, 확장자 검사 |
| `annotations` | Swagger 응답 문서용 커스텀 어노테이션 |

---

## 4. 요청 하나가 처리되는 전체 흐름

API 요청 하나는 보통 다음 순서로 처리됩니다.

```text
1. 프론트엔드가 HTTP 요청을 보냄
2. Spring Security가 먼저 요청을 검사함
3. 인증이 필요한 API라면 JWT 토큰을 확인함
4. Controller가 요청을 받음
5. Request DTO가 입력값 형식을 검사함
6. Service가 실제 업무 로직을 처리함
7. Repository 또는 QueryRepository가 DB에 접근함
8. Entity를 Response DTO로 바꿈
9. GlobalRes로 감싸서 응답함
10. 중간에 에러가 나면 GlobalExceptionHandler가 에러 응답을 만듦
```

그림으로 보면 다음과 같습니다.

```text
프론트엔드
  |
  v
Spring Security / JWT 검사
  |
  v
Controller
  |
  v
Service
  |
  v
Repository / QueryDSL
  |
  v
MySQL DB
  |
  v
Response DTO
  |
  v
GlobalRes
  |
  v
프론트엔드
```

각 레이어를 쉽게 말하면 다음과 같습니다.

| 레이어 | 쉽게 말하면 | 현재 프로젝트 예시 |
|---|---|---|
| Controller | 요청을 받는 창구 | `AuthController`, `PostController`, `FileController` |
| Request DTO | 요청서 양식 | `LoginReq`, `RegistrationReq`, `PostIndexReq` |
| Service | 실제 업무 담당자 | `AuthService`, `PostService`, `FileService` |
| Repository | DB 담당자 | `AuthRepository`, `PostRepository`, `UserRepository` |
| QueryRepository | 복잡한 DB 조회 담당자 | `PostQueryRepository` |
| Entity | DB 테이블과 연결된 Java 객체 | `User`, `Post` |
| Response DTO | 응답서 양식 | `AuthRes`, `PostIndexRes`, `PostWithUserRes`, `FileRes` |
| GlobalRes | 공통 응답 봉투 | `GlobalRes<T>` |
| ExceptionHandler | 에러 처리 담당자 | `GlobalExceptionHandler` |

---

## 5. 현재 구현된 API

현재 컨트롤러 코드 기준으로 실제 구현된 API는 다음과 같습니다.

| 기능 | HTTP | 주소 | 설명 |
|---|---:|---|---|
| 로그인 | POST | `/api/login` | 이메일, 비밀번호로 로그인 |
| 토큰 재발급 | POST | `/api/reissue-token` | Refresh Token으로 새 토큰 발급 |
| 로그아웃 | POST | `/api/logout` | Refresh Token 제거, 쿠키 만료 |
| 회원가입 | POST | `/api/registration` | 새 사용자 생성 |
| 게시글 목록 | GET | `/api/posts` | 페이지 단위 게시글 목록 조회 |
| 게시글 상세 | GET | `/api/posts/{id}` | 특정 게시글 조회 |
| 프로필 이미지 업로드 | POST | `/api/files/profiles` | 프로필 파일 저장 후 URL 반환 |
| 게시글 이미지 업로드 | POST | `/api/files/posts` | 게시글 파일 저장 후 URL 반환 |

보안 설정에는 게시글 작성 `POST /api/posts`, 게시글 삭제 `DELETE /api/posts/{id}`도 인증 필요 목록에 들어 있습니다. 하지만 현재 컨트롤러에는 해당 메서드가 아직 구현되어 있지 않습니다.

---

## 6. 인증 기능 설명

인증은 사용자가 누구인지 확인하는 기능입니다.

이 프로젝트는 JWT를 사용합니다. JWT는 쉽게 말해 서버가 발급하는 로그인 출입증입니다.

로그인 성공 시 서버는 두 종류의 토큰을 만듭니다.

| 토큰 | 역할 | 저장 위치 |
|---|---|---|
| Access Token | API 요청 때 사용자를 증명 | 응답 body로 내려줌 |
| Refresh Token | Access Token을 새로 받을 때 사용 | DB와 쿠키에 저장 |

### 6.1 로그인 흐름

```text
POST /api/login
-> AuthController.login()
-> LoginReq 입력값 검사
-> AuthService.login()
-> 이메일로 사용자 조회
-> 비밀번호 비교
-> Access Token 생성
-> Refresh Token 생성
-> Refresh Token을 DB에 저장
-> Refresh Token을 쿠키에 저장
-> AuthRes 응답
```

관련 주요 파일은 다음과 같습니다.

| 파일 | 역할 |
|---|---|
| `AuthController` | 로그인 요청을 받음 |
| `LoginReq` | 이메일, 비밀번호 형식 검사 |
| `AuthService` | 로그인 실제 처리 |
| `AuthRepository` | 사용자 DB 조회 |
| `JwtProvider` | JWT 생성과 검증 |
| `CookieManager` | Refresh Token 쿠키 저장 |
| `AuthRes` | 로그인 성공 응답 |

### 6.2 토큰 재발급 흐름

```text
POST /api/reissue-token
-> 쿠키에서 Refresh Token 추출
-> JWT 서명과 만료 여부 검증
-> 토큰 안의 사용자 id 확인
-> DB에서 사용자 조회
-> DB에 저장된 Refresh Token과 쿠키의 Refresh Token 비교
-> 새 Access Token과 Refresh Token 발급
-> DB와 쿠키 갱신
```

Refresh Token을 쿠키에만 두지 않고 DB에도 저장하는 이유는 서버가 토큰을 무효화할 수 있게 하기 위해서입니다.

예를 들어 로그아웃하면 DB의 `refresh_token`을 `null`로 바꿉니다. 그러면 예전 Refresh Token을 누군가 가지고 있어도 재발급이 실패합니다.

### 6.3 로그아웃 흐름

```text
POST /api/logout
-> Access Token 검사
-> 토큰에서 사용자 id 추출
-> DB의 refreshToken을 null로 변경
-> 쿠키의 Refresh Token 만료 처리
-> 성공 응답
```

---

## 7. 게시글 기능 설명

게시글 기능은 현재 목록 조회와 상세 조회가 구현되어 있습니다.

### 7.1 게시글 목록 조회

요청 예시는 다음과 같습니다.

```text
GET /api/posts?page=1&limit=6
```

흐름은 다음과 같습니다.

```text
PostController.index()
-> PostIndexReq가 page, limit 값을 받음
-> PostService.index()
-> offset 계산
-> PostQueryRepository.pagination()
-> QueryDSL로 게시글과 작성자 정보를 함께 조회
-> PostRepository.count()로 전체 게시글 수 조회
-> PostIndexRes로 변환
-> GlobalRes로 감싸서 응답
```

`page`와 `limit`이 없으면 기본값이 적용됩니다.

| 값 | 기본값 |
|---|---:|
| `page` | 1 |
| `limit` | 6 |

### 7.2 게시글 상세 조회

요청 예시는 다음과 같습니다.

```text
GET /api/posts/3
```

흐름은 다음과 같습니다.

```text
PostController.show()
-> PostService.show()
-> PostRepository.findById(id)
-> 게시글이 있으면 PostWithUserRes로 변환
-> 게시글이 없으면 DeletedRecordException 발생
```

게시글 상세 조회는 보안 설정상 인증이 필요합니다. 즉, Access Token이 있어야 통과합니다.

---

## 8. 파일 업로드 기능 설명

파일 업로드는 `FileController`, `FileService`, `LocalFileManager`가 담당합니다.

프로필 업로드 흐름은 다음과 같습니다.

```text
POST /api/files/profiles
-> FileController.storeProfile()
-> FileService.storeProfile()
-> LocalFileManager.generateProfilePath()
-> 파일 확장자 검사
-> 랜덤 파일명 생성
-> 실제 파일 저장
-> FileRes로 파일 URL 반환
```

파일명은 대략 다음 형태로 만들어집니다.

```text
20260710_UUID.png
```

허용 파일 타입은 설정 파일의 `file.allow-extension-list`에 들어 있습니다.

```yaml
image/jpg
image/jpeg
image/png
image/gif
image/svg
image/webp
```

주의할 점이 하나 있습니다.

현재 `FileController.storePosts()`는 게시글 이미지 업로드 API인데, 내부에서 `fileService.storeProfile(file)`을 호출하고 있습니다. 의도상으로는 `fileService.storePosts(file)`를 호출해야 게시글 이미지 경로에 저장됩니다.

---

## 9. DB 구조와 JPA Entity

현재 핵심 테이블은 `users`, `posts`입니다.

### 9.1 users

`User` 엔티티는 사용자 테이블과 연결됩니다.

주요 필드는 다음과 같습니다.

| 필드 | 의미 |
|---|---|
| `id` | 사용자 번호 |
| `email` | 이메일 |
| `password` | 암호화된 비밀번호 |
| `nick` | 닉네임 |
| `provider` | 가입 방식 |
| `role` | 권한 |
| `profile` | 프로필 이미지 URL |
| `refreshToken` | Refresh Token |
| `createdAt` | 생성일 |
| `updatedAt` | 수정일 |
| `deletedAt` | 삭제일 |

### 9.2 posts

`Post` 엔티티는 게시글 테이블과 연결됩니다.

주요 필드는 다음과 같습니다.

| 필드 | 의미 |
|---|---|
| `id` | 게시글 번호 |
| `content` | 게시글 내용 |
| `image` | 게시글 이미지 URL |
| `createdAt` | 생성일 |
| `updatedAt` | 수정일 |
| `deletedAt` | 삭제일 |
| `user` | 작성자 |

`Post`에는 다음 관계가 있습니다.

```java
@ManyToOne(fetch = FetchType.LAZY)
private User user;
```

이 말은 게시글 여러 개가 사용자 한 명에게 속한다는 뜻입니다.

예를 들어 사용자 1명이 게시글 10개를 쓸 수 있습니다.

```text
User 1명
  ├── Post 1
  ├── Post 2
  └── Post 3
```

### 9.3 Soft Delete

현재 `User`, `Post`는 실제 삭제 대신 soft delete 방식을 사용합니다.

soft delete는 데이터를 DB에서 완전히 지우지 않고 `deleted_at`에 삭제 시간을 넣는 방식입니다.

```java
@SQLDelete(sql = "UPDATE posts SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
```

쉽게 말하면 다음과 같습니다.

| 일반 삭제 | Soft Delete |
|---|---|
| DB row를 실제로 지움 | DB row는 남겨 둠 |
| 복구가 어려움 | 필요하면 복구 가능 |
| 삭제 기록 추적이 어려움 | 언제 삭제됐는지 알 수 있음 |

조회할 때는 `deleted_at IS NULL` 조건이 자동으로 붙습니다. 그래서 삭제된 데이터는 일반 조회에서 보이지 않습니다.

### 9.4 JPA Auditing

메인 클래스에는 다음 설정이 있습니다.

```java
@EnableJpaAuditing
```

엔티티에는 다음 필드가 있습니다.

```java
@CreatedDate
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
```

이 기능 덕분에 생성일과 수정일을 개발자가 매번 직접 넣지 않아도 JPA가 자동으로 채워 줍니다.

---

## 10. 공통 응답 구조

이 프로젝트는 응답을 `GlobalRes<T>`로 통일합니다.

성공 응답은 대략 다음 형태입니다.

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "example": "result"
  }
}
```

실패 응답도 비슷한 형태입니다.

```json
{
  "code": "E04",
  "message": "INVALID_TOKEN_ERROR",
  "data": null
}
```

이렇게 통일하면 프론트엔드는 모든 API 응답을 같은 방식으로 처리할 수 있습니다.

```text
code가 "00"이면 성공
code가 "E.."이면 실패
data에 실제 결과가 있음
```

에러 코드는 `CustomResponseCode`에서 관리합니다.

| 코드 | 의미 |
|---|---|
| `00` | 성공 |
| `E01` | 가입되지 않은 사용자 또는 로그인 실패 |
| `E02` | 인증되지 않음 |
| `E03` | 권한 없음 |
| `E04` | 토큰 오류 |
| `E10` | 데이터 없음 |
| `E20` | 중복 데이터 |
| `E21` | 입력값 오류 |
| `E40` | 파일 처리 오류 |
| `E80` | DB 오류 |
| `E99` | 시스템 오류 |

---

## 11. 에러 처리 구조

일반적으로 코드 중간에서 문제가 생기면 예외가 발생합니다.

예를 들어 로그인할 때 이메일이 DB에 없으면 `NotRegisteredException`이 발생합니다.

하지만 에러마다 컨트롤러에서 직접 응답을 만들면 코드가 지저분해집니다. 그래서 이 프로젝트는 `GlobalExceptionHandler` 한곳에서 에러를 모아서 처리합니다.

흐름은 다음과 같습니다.

```text
Service에서 예외 발생
-> GlobalExceptionHandler가 예외 종류 확인
-> CustomResponseCode 선택
-> GlobalRes 형태로 에러 응답 생성
```

예시는 다음과 같습니다.

```text
NotRegisteredException
-> NOT_REGISTERED_ERROR
-> HTTP 401
-> code "E01"
```

```text
DuplicatedRecordException
-> DUPLICATED_DATA_ERROR
-> HTTP 409
-> code "E20"
```

```text
InvalidTokenException
-> INVALID_TOKEN_ERROR
-> HTTP 401
-> code "E04"
```

이 구조의 장점은 에러 응답이 항상 일정하다는 것입니다.

---

## 12. JPA와 dev 브랜치의 MyBatis 차이

이 부분이 가장 중요합니다.

현재 브랜치는 JPA를 사용하고, `dev` 브랜치는 MyBatis를 사용합니다.

둘 다 Java 서버에서 DB를 사용하는 기술입니다. 하지만 생각하는 방식이 다릅니다.

### 12.1 한 문장 차이

| 기술 | 한 문장 설명 |
|---|---|
| MyBatis | SQL을 개발자가 직접 쓰고, 결과를 Java 객체에 담는다 |
| JPA | Java 객체를 중심으로 만들면, JPA가 SQL을 대신 만들어 실행한다 |

### 12.2 비유

MyBatis는 식당에서 주문서를 직접 자세히 쓰는 방식입니다.

```text
볶음밥 1개, 짜장면 1개, 단무지 제외, 3번 테이블로 주세요
```

JPA는 직원에게 원하는 결과를 객체 중심으로 말하는 방식에 가깝습니다.

```text
id가 1번인 사용자를 찾아줘
이 사용자의 refreshToken을 바꿔줘
이 게시글을 저장해줘
```

MyBatis는 SQL을 직접 제어하기 좋습니다. JPA는 반복 CRUD를 줄이고 객체 중심으로 개발하기 좋습니다.

### 12.3 dev 브랜치의 MyBatis 구조

`dev` 브랜치에는 다음 구조가 있습니다.

```text
domain/auth/mapper/AuthMapper.java
domain/post/mapper/PostMapper.java
domain/user/mapper/UserMapper.java

resources/mapper/auth/AuthMapper.xml
resources/mapper/posts/PostsMapper.xml
resources/mapper/users/UesrMapper.xml
```

MyBatis에서는 Java 인터페이스와 XML SQL이 짝을 이룹니다.

예를 들어 `dev` 브랜치의 `UserMapper`는 이런 식입니다.

```java
@Mapper
public interface UserMapper {
    User findByPk(long id);
    User findByEmail(String email);
}
```

그리고 XML에는 실제 SQL이 있습니다.

```xml
<select id="findByEmail" resultMap="UserResultMap">
    SELECT
        *
    FROM users
    WHERE
        deleted_at IS NULL
      AND email = #{email}
</select>
```

즉, `userMapper.findByEmail(email)`을 호출하면 MyBatis가 XML의 `findByEmail` SQL을 실행합니다.

### 12.4 현재 브랜치의 JPA 구조

현재 브랜치에서는 Mapper XML이 아니라 Repository를 사용합니다.

```java
public interface AuthRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

여기에는 SQL이 직접 보이지 않습니다.

Spring Data JPA가 메서드 이름을 보고 의미를 해석합니다.

```text
findByEmail
-> email 컬럼으로 User를 찾는 쿼리를 자동 생성
```

```text
existsByEmail
-> email이 존재하는지 확인하는 쿼리를 자동 생성
```

### 12.5 같은 기능 비교: 이메일로 사용자 찾기

MyBatis 방식은 다음과 같습니다.

```text
UserMapper.findByEmail(email)
-> UserMapper.xml의 SELECT SQL 실행
-> users 테이블에서 email 조건 조회
-> 결과를 User 객체에 매핑
```

JPA 방식은 다음과 같습니다.

```text
AuthRepository.findByEmail(email)
-> Spring Data JPA가 메서드 이름 분석
-> email 조건 조회 SQL 자동 생성
-> 결과를 User 엔티티로 반환
```

비교하면 다음과 같습니다.

| 항목 | MyBatis | JPA |
|---|---|---|
| SQL 작성 | 개발자가 직접 XML에 작성 | 기본 CRUD는 자동 생성 |
| DB 중심/객체 중심 | DB와 SQL 중심 | Java 객체와 관계 중심 |
| 단순 CRUD | SQL을 계속 작성해야 함 | Repository로 간단히 처리 |
| 복잡한 SQL | 직접 쓰기 쉬움 | QueryDSL 또는 JPQL이 필요 |
| 테이블 관계 | SQL join을 직접 작성 | `@ManyToOne` 같은 관계로 표현 |
| 변경 저장 | `UPDATE` SQL 직접 작성 | 객체 값을 바꾸고 `save()` 또는 변경 감지 |
| 학습 난이도 | SQL을 알면 흐름이 직관적 | 객체 관계, 영속성 개념을 알아야 함 |
| 유지보수 | SQL이 명확히 보임 | 반복 코드가 줄지만 JPA 동작 이해 필요 |

### 12.6 같은 기능 비교: Refresh Token 저장

`dev` 브랜치의 MyBatis 방식은 다음처럼 직접 UPDATE SQL을 실행합니다.

```text
authMapper.updateRefreshToken(userId, refreshToken)
-> AuthMapper.xml의 UPDATE users SET refresh_token = ... 실행
```

현재 JPA 방식은 다음과 같습니다.

```java
user.setRefreshToken(newRefreshToken);
authRepository.save(user);
```

즉, MyBatis는 SQL을 직접 부르는 방식이고, JPA는 Java 객체의 값을 바꾸고 저장하는 방식입니다.

### 12.7 같은 기능 비교: 게시글 목록 조회

`dev` 브랜치의 MyBatis 방식은 XML에 직접 SQL이 있습니다.

```sql
SELECT *
FROM posts
WHERE deleted_at IS NULL
ORDER BY created_at DESC, id ASC
LIMIT #{offset}, #{limit}
```

현재 브랜치의 JPA + QueryDSL 방식은 Java 코드로 조회를 만듭니다.

```java
return jPAQueryFactory
    .selectFrom(post)
    .join(post.user, user).fetchJoin()
    .orderBy(post.createdAt.desc(), post.id.desc())
    .limit(limit)
    .offset(offset)
    .fetch();
```

둘 다 게시글 목록을 가져오지만, 작성 방식이 다릅니다.

| 항목 | MyBatis dev | 현재 JPA + QueryDSL |
|---|---|---|
| 위치 | XML 파일 | Java 클래스 |
| SQL 형태 | 실제 SQL 그대로 | Java 메서드 체인 |
| join | SQL에 직접 작성 | `.join(...).fetchJoin()` |
| 오타 확인 | 실행 전까지 놓치기 쉬움 | Java 컴파일 단계에서 일부 잡힘 |
| SQL 가독성 | SQL을 아는 사람에게 좋음 | Java/JPA를 아는 사람에게 좋음 |

### 12.8 Entity 차이

`dev` 브랜치의 MyBatis `User`는 단순 Java 클래스입니다.

```java
public class User {
    private long id;
    private String email;
    private String password;
}
```

현재 JPA `User`는 DB 테이블과 직접 연결되는 엔티티입니다.

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;
}
```

MyBatis에서는 XML의 `resultMap`이 DB 컬럼과 Java 필드를 연결합니다.

JPA에서는 `@Entity`, `@Table`, `@Column` 같은 어노테이션이 DB 테이블과 Java 필드를 연결합니다.

### 12.9 이 프로젝트에서는 왜 JPA로 바꾼 것으로 보이는가

현재 브랜치 이름이 `feature/v2/migration-jpa`이고, 코드도 JPA 구조로 되어 있습니다.

즉, dev의 MyBatis 구조에서 JPA 구조로 마이그레이션 중인 브랜치로 보입니다.

JPA로 바꾸면 다음 장점이 있습니다.

- 반복적인 SELECT, INSERT, UPDATE, DELETE 코드가 줄어듭니다.
- `User`, `Post` 관계를 Java 객체 관계로 표현할 수 있습니다.
- 생성일, 수정일 자동 입력 같은 기능을 쉽게 붙일 수 있습니다.
- soft delete 조건을 엔티티에 붙여 반복 SQL 조건을 줄일 수 있습니다.
- Repository 메서드 이름만으로 간단한 조회를 만들 수 있습니다.

대신 주의할 점도 있습니다.

- JPA가 실제로 어떤 SQL을 실행하는지 이해해야 합니다.
- 연관관계, 지연 로딩, fetch join 같은 개념을 알아야 합니다.
- 아주 복잡한 SQL은 QueryDSL이나 직접 쿼리를 써야 합니다.

---

## 13. 이 프로젝트를 순수 Java로만 만들면 어떻게 해야 하는가

현재 프로젝트는 Java로 작성되어 있습니다. 다만 Spring Boot가 많은 일을 대신해 줍니다.

만약 Spring 없이 순수 Java로만 만든다면 다음 작업을 직접 구현해야 합니다.

### 13.1 서버 실행

현재는 다음 한 줄이 서버 시작의 핵심입니다.

```java
SpringApplication.run(Msa4MeerkatgramApplication.class, args);
```

Spring Boot가 내부 Tomcat 서버를 실행하고, API 주소들을 자동으로 등록합니다.

순수 Java라면 직접 HTTP 서버를 열어야 합니다.

```text
포트 8080 열기
-> HTTP 요청 받기
-> 요청 method 확인
-> 요청 path 확인
-> 알맞은 Java 메서드 호출
-> HTTP 응답 직접 작성
```

Java 기본 라이브러리의 `HttpServer`를 쓰거나, 더 낮은 수준에서는 `ServerSocket`부터 다뤄야 합니다.

### 13.2 라우팅

현재 Spring에서는 이렇게 씁니다.

```java
@PostMapping("/login")
public ResponseEntity<GlobalRes<AuthRes>> login(...) {
    ...
}
```

이 코드의 의미는 다음과 같습니다.

```text
POST /api/login 요청이 오면 이 메서드를 실행해라
```

순수 Java라면 직접 분기문을 작성해야 합니다.

```text
if method == POST and path == /api/login:
    login 처리
else if method == GET and path == /api/posts:
    게시글 목록 처리
else:
    404 응답
```

### 13.3 JSON 처리

현재 Spring은 JSON 요청을 Java 객체로 자동 변환합니다.

```java
public ResponseEntity<?> login(@RequestBody LoginReq loginReq)
```

프론트엔드가 보낸 JSON:

```json
{
  "email": "test@test.com",
  "password": "qwer1234"
}
```

Spring이 자동으로 `LoginReq`로 바꿔 줍니다.

순수 Java라면 다음을 직접 해야 합니다.

```text
요청 body 문자열 읽기
-> JSON 파싱
-> email 값 꺼내기
-> password 값 꺼내기
-> LoginReq 같은 객체 직접 생성
```

응답도 마찬가지입니다. Java 객체를 JSON 문자열로 직접 바꿔야 합니다.

### 13.4 객체 생성과 연결

현재는 Spring이 필요한 객체를 자동으로 만들어 연결합니다.

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthRepository authRepository;
    private final JwtProvider jwtProvider;
}
```

Spring이 `AuthRepository`, `JwtProvider`를 찾아서 `AuthService`에 넣어 줍니다.

순수 Java라면 직접 만들어야 합니다.

```java
JwtProvider jwtProvider = new JwtProvider(...);
AuthRepository authRepository = new AuthRepository(...);
AuthService authService = new AuthService(authRepository, jwtProvider, ...);
AuthController authController = new AuthController(authService);
```

프로젝트가 커질수록 이런 연결 코드가 많아집니다.

### 13.5 DB 연결

현재 JPA Repository는 이런 메서드만으로 DB 조회를 합니다.

```java
authRepository.findByEmail(email)
authRepository.save(user)
postRepository.findById(id)
postRepository.count()
```

순수 Java라면 JDBC를 직접 써야 합니다.

```text
DB 연결 열기
-> SQL 문자열 작성
-> PreparedStatement 생성
-> 파라미터 넣기
-> SQL 실행
-> ResultSet 읽기
-> User 객체 생성
-> DB 연결 닫기
```

예를 들어 이메일로 사용자를 찾으려면 이런 일을 직접 해야 합니다.

```sql
SELECT *
FROM users
WHERE deleted_at IS NULL
  AND email = ?
```

그리고 결과의 각 컬럼을 직접 꺼내 Java 객체에 넣어야 합니다.

```text
resultSet.getLong("id")
resultSet.getString("email")
resultSet.getString("password")
...
```

### 13.6 트랜잭션

현재는 다음 어노테이션으로 트랜잭션을 처리합니다.

```java
@Transactional(rollbackFor = Exception.class)
```

트랜잭션은 여러 DB 작업을 하나의 묶음으로 처리하는 기능입니다.

예를 들어 로그인 중 Refresh Token을 DB에 저장하다가 문제가 생기면 중간 상태로 남으면 안 됩니다. 실패하면 되돌리는 것이 안전합니다.

순수 Java라면 직접 작성해야 합니다.

```text
connection.setAutoCommit(false)
try:
    DB 작업 1
    DB 작업 2
    connection.commit()
catch:
    connection.rollback()
finally:
    connection.close()
```

### 13.7 입력값 검증

현재는 다음 어노테이션으로 이메일과 비밀번호 형식을 검사합니다.

```java
@NotBlank
@Pattern(...)
```

그리고 컨트롤러에서 `@Valid`를 붙이면 자동 검사됩니다.

순수 Java라면 직접 검사해야 합니다.

```text
email이 비었는지 확인
email 정규식 검사
password가 비었는지 확인
password 정규식 검사
틀리면 400 응답 만들기
```

### 13.8 보안과 JWT

현재는 `TokenAuthenticationFilter`가 모든 요청 앞에서 Access Token을 검사합니다.

순수 Java라면 모든 보호 API 앞에 다음 로직을 직접 넣어야 합니다.

```text
Authorization 헤더 읽기
Bearer 로 시작하는지 확인
JWT 서명 검증
JWT 만료 여부 확인
사용자 id 추출
실패하면 401 응답
성공하면 다음 로직 실행
```

이 작업을 API마다 반복하면 코드가 중복됩니다.

### 13.9 파일 업로드

현재 Spring은 `MultipartFile`로 업로드 파일을 받을 수 있게 해 줍니다.

```java
public ResponseEntity<GlobalRes<FileRes>> storeProfile(@ModelAttribute MultipartFile file)
```

순수 Java라면 multipart 요청 형식을 직접 파싱해야 합니다.

```text
요청 body에서 boundary 찾기
-> 파일명 찾기
-> 파일 content-type 찾기
-> 실제 파일 byte 추출
-> 확장자 검사
-> 디렉토리 생성
-> 파일 저장
```

직접 구현하면 실수하기 쉬운 부분입니다.

### 13.10 에러 응답

현재는 `GlobalExceptionHandler`가 에러를 자동으로 공통 응답으로 바꿉니다.

순수 Java라면 모든 API마다 try-catch를 직접 작성해야 합니다.

```text
try:
    처리
catch NotRegisteredException:
    401 + E01 JSON 응답
catch InvalidTokenException:
    401 + E04 JSON 응답
catch Exception:
    500 + E99 JSON 응답
```

---

## 14. 자바의 어느 부분을 Spring으로 한 것인가

정확히 말하면 Spring은 Java를 대체한 것이 아닙니다.

이 프로젝트는 여전히 Java 프로젝트입니다. 다만 Java로 직접 작성해야 할 반복적인 서버 작업을 Spring이 대신 관리해 줍니다.

### 14.1 Java로 직접 작성한 부분

개발자가 직접 작성한 Java 로직은 다음과 같습니다.

| Java 코드 | 예시 |
|---|---|
| 업무 규칙 | 로그인, 회원가입, 게시글 조회, 파일 저장 |
| 조건문 | 비밀번호가 틀리면 예외 발생 |
| 객체 생성 | `new User()` |
| 값 변경 | `user.setRefreshToken(...)` |
| DTO 변환 | `AuthRes.from(...)`, `PostWithUserRes.from(...)` |
| 예외 정의 | `InvalidTokenException`, `DuplicatedRecordException` |
| enum | `RolePolicy`, `ProviderPolicy`, `CustomResponseCode` |
| record | `LoginReq`, `RegistrationReq`, `GlobalRes` |
| 컬렉션 처리 | `List<Post>`, `posts.stream().map(...)` |

즉, 서비스가 무엇을 해야 하는지는 Java 코드로 작성했습니다.

### 14.2 Spring이 대신해 주는 부분

Spring은 다음 작업을 대신합니다.

| 원래 Java로 직접 해야 할 일 | Spring에서 쓰는 방식 | 현재 프로젝트 예시 |
|---|---|---|
| HTTP 서버 실행 | Spring Boot 내장 서버 | `SpringApplication.run(...)` |
| URL과 메서드 연결 | `@GetMapping`, `@PostMapping` | `/api/login`, `/api/posts` |
| JSON을 Java 객체로 변환 | `@RequestBody` | `LoginReq` |
| Java 객체를 JSON 응답으로 변환 | `ResponseEntity`, Jackson | `GlobalRes.success(...)` |
| 객체 생성과 의존성 연결 | `@Service`, `@Component`, `@RequiredArgsConstructor` | `AuthService`, `JwtProvider` |
| 설정값 주입 | `@ConfigurationProperties` | `JwtConfig`, `FileConfig`, `CorsConfig` |
| 입력값 검증 | `@Valid`, `@NotBlank`, `@Pattern` | 로그인/회원가입 검증 |
| DB CRUD 구현 | `JpaRepository` | `AuthRepository`, `PostRepository` |
| DB 트랜잭션 처리 | `@Transactional` | 로그인, 재발급, 로그아웃, 회원가입 |
| 생성일/수정일 자동 입력 | `@EnableJpaAuditing` | `createdAt`, `updatedAt` |
| 인증 필터 연결 | Spring Security Filter Chain | `TokenAuthenticationFilter` |
| 에러 공통 처리 | `@RestControllerAdvice`, `@ExceptionHandler` | `GlobalExceptionHandler` |
| CORS 처리 | `CorsConfigurationSource` | `SecurityConfiguration` |
| 파일 업로드 파싱 | `MultipartFile` | `FileController` |

핵심은 다음입니다.

```text
Java:
  무엇을 할지 작성한다.

Spring:
  언제 실행할지, 어떻게 연결할지, 반복 인프라 작업을 관리한다.
```

### 14.3 예시: 로그인에서 Java와 Spring 역할 나누기

로그인 API를 보면 다음과 같이 나뉩니다.

```java
@PostMapping("/login")
public ResponseEntity<GlobalRes<AuthRes>> login(
    @Valid @RequestBody LoginReq loginReq,
    HttpServletResponse response
) {
    return ResponseEntity.ok(GlobalRes.success(authService.login(response, loginReq)));
}
```

Spring이 하는 일:

```text
POST /api/login 요청을 이 메서드에 연결
JSON body를 LoginReq로 변환
@Valid로 입력값 검사
HttpServletResponse 객체 제공
반환값을 JSON으로 변환
```

Java 코드가 하는 일:

```text
authService.login(...) 호출
성공 결과를 GlobalRes.success(...)로 감싸기
```

`AuthService.login()` 내부에서는 Java 업무 로직이 실행됩니다.

```java
User user = authRepository.findByEmail(loginReq.email())
    .orElseThrow(() -> new NotRegisteredException("아이디와 비밀번호를 확인해주세요."));

if (!passwordEncoder.matches(loginReq.password(), user.getPassword())) {
    throw new NotRegisteredException("아이디와 비밀번호를 확인해주세요.");
}
```

여기서 Java가 하는 일:

```text
사용자 조회 결과 확인
사용자가 없으면 예외 발생
비밀번호 비교
틀리면 예외 발생
```

Spring/JPA가 도와주는 일:

```text
authRepository.findByEmail(...)가 DB 조회로 실행되도록 연결
passwordEncoder 객체를 미리 만들어 주입
예외가 발생하면 GlobalExceptionHandler로 보내기
```

---

## 15. 새 기능을 만들 때 보통 필요한 파일

예를 들어 게시글 작성 기능을 추가한다고 가정하면 보통 다음 순서로 만듭니다.

```text
1. 요청 DTO 생성
   - PostStoreReq
   - content, image 같은 입력값 검증

2. Controller 메서드 추가
   - POST /api/posts
   - @AuthenticationPrincipal로 로그인 사용자 확인

3. Service 메서드 추가
   - 사용자 조회
   - Post 엔티티 생성
   - Repository save

4. Repository 사용
   - PostRepository.save(post)

5. Response DTO 생성 또는 재사용
   - PostWithUserRes

6. 에러 상황 정의
   - 사용자 없음
   - 입력값 오류
   - 권한 없음

7. 보안 설정 확인
   - SecurityUrlRegistry에 이미 POST /api/posts가 인증 필요로 등록되어 있음
```

Spring/JPA 방식으로는 직접 SQL `INSERT INTO posts ...`를 쓰기보다 `Post` 엔티티를 만들고 `postRepository.save(post)`를 호출하는 방식이 됩니다.

---

## 16. 현재 코드에서 알아둘 주의점

설명 문서이므로 코드 수정은 하지 않았지만, 현재 코드 기준으로 확인되는 주의점은 다음과 같습니다.

| 항목 | 현재 상태 | 영향 |
|---|---|---|
| MyBatis 설정 흔적 | `application-prod.yaml`에 `mybatis:` 설정이 남아 있음 | 현재 JPA 코드와 혼동될 수 있음 |
| 기존 문서 일부 | README와 일부 문서에 MyBatis 설명이 남아 있음 | 현재 브랜치와 설명이 다를 수 있음 |
| 게시글 파일 업로드 | `FileController.storePosts()`가 `fileService.storeProfile(file)` 호출 | 게시글 이미지가 프로필 경로로 저장될 수 있음 |
| QueryRepository 어노테이션 | `PostQueryRepository`에 `@RestController`가 붙어 있음 | 조회용 클래스이므로 `@Repository` 또는 `@Component`가 더 적절함 |
| 권한 예외 import | `GlobalExceptionHandler`가 `java.nio.file.AccessDeniedException`을 import | Spring Security 권한 예외 처리 의도와 다를 수 있음 |
| 보안 URL과 컨트롤러 | 작성/삭제 API는 보안 목록에 있지만 컨트롤러 메서드는 없음 | 아직 구현 예정 상태로 보임 |

---

## 17. 핵심 요약

이 프로젝트는 Java와 Spring Boot로 만든 Meerkatgram 백엔드 API 서버입니다.

가장 큰 흐름은 다음과 같습니다.

```text
Controller가 요청을 받는다.
Service가 실제 일을 처리한다.
Repository가 DB에 다녀온다.
Entity가 DB 테이블과 연결된다.
Response DTO가 응답 모양을 정리한다.
GlobalRes가 모든 응답을 같은 봉투에 담는다.
GlobalExceptionHandler가 에러 응답을 통일한다.
Spring Security와 JWT가 로그인 여부를 검사한다.
```

JPA와 MyBatis의 핵심 차이는 다음입니다.

```text
MyBatis:
  SQL을 직접 작성한다.
  SQL이 눈에 잘 보인다.
  반복 SQL도 직접 써야 한다.

JPA:
  Java 객체를 중심으로 DB를 다룬다.
  기본 CRUD SQL은 Spring Data JPA가 만들어 준다.
  복잡한 조회는 QueryDSL 같은 도구를 함께 쓴다.
```

순수 Java로도 만들 수는 있습니다. 하지만 그러면 HTTP 서버, 라우팅, JSON 변환, DB 연결, 트랜잭션, 검증, 보안, 에러 응답, 파일 업로드를 대부분 직접 구현해야 합니다.

Spring은 Java를 없앤 것이 아니라, Java 서버 개발에서 반복되는 기반 작업을 대신 관리해 주는 도구입니다. 개발자는 그 위에서 로그인, 게시글 조회, 파일 저장 같은 실제 서비스 규칙에 집중합니다.
