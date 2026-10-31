# TT플래너 (트립 투게더 플래너) — Claude Code 작업 규칙

B팀 5명이 함께 쓰는 규칙이다. 각자의 이름과 담당은 `CLAUDE.local.md`(개인 파일, GitHub에 올리지 않음)에 적는다.
작업을 시작하기 전에 `CLAUDE.local.md`를 읽고, 이 사람이 누구이며 어떤 유스케이스(UC)를 맡았는지 확인한다.
`CLAUDE.local.md`가 없으면 작업하기 전에 이름과 담당 UC를 먼저 물어본다.

## 1. 작업 방식 (반드시 지킬 것)

1. **수정 전에 계획을 먼저 보여 준다.** 어떤 파일을 새로 만들거나 고칠지, 무엇을 바꿀지 목록으로 보여 주고 승인을 받은 뒤에 작업한다.
2. **담당 UC의 파일만 수정한다.** 다른 팀원 소유 파일(아래 4장)을 고쳐야 하면 고치지 말고, 누구에게 무엇을 요청해야 하는지만 알려 준다.
3. **설정 파일은 묻고 바꾼다.** `pom.xml`, `web.xml`, `root-context.xml`, `servlet-context.xml`, `log4j.xml`, `db/schema.sql`은 팀 전체에 영향을 주므로 바꾸기 전에 이유를 설명하고 확인을 받는다. 라이브러리 버전은 바꾸지 않는다.
4. **git 명령은 실행하지 않는다.** `git add`, `commit`, `push`, `merge`, `checkout`, `reset` 등은 사용자가 직접 한다. 작업이 끝나면 커밋할 파일 목록과 커밋 메시지 예시만 제안한다.
5. **설명은 초보자 수준으로 한다.** 바꾼 코드가 어디와 어떻게 연결되는지 실제 흐름으로 설명한다.
   예: `GET /trips/1 → TripController.detail() → TripService.find() → TripMapper.xml의 select → detail.jsp`
   비유("택배 기사가…")보다 실제 클래스 이름, 메서드 이름, 값으로 설명한다.
6. 답변과 코드 주석은 한국어로 쓴다.
7. 작업이 끝나면 **STS에서 확인할 방법**을 알려 준다 (어떤 JUnit 테스트를 실행할지, 어느 주소를 열어 무엇을 확인할지).
8. 모르는 것(팀원이 만들 메서드 이름, DB 컬럼 등)은 추측해서 만들지 말고 물어본다. 다른 팀원의 메서드가 아직 없으면 빈 값을 돌려주는 임시 구현을 쓰고 `// TODO(담당자): ...` 주석을 남긴다.

## 2. 기술 스택 (바꾸지 않음)

기준 문서: `TT플래너_개발환경_구성정의서_v1.2`

- Java **1.8** — Java 9 이상 문법(`var`, `List.of`, `record`, 텍스트 블록 등) 사용 금지
- Spring MVC **5.0.7** (Spring Boot 아님, XML 설정), Maven, Tomcat **9.0.x** (`javax.*` 패키지, `jakarta.*` 사용 금지)
- JSP + JSTL 1.2, SB Admin 2 (Bootstrap 3.4.1, jQuery 3.1.0)
- Oracle 21c XE, MyBatis 3.5.10 + mybatis-spring 2.0.7, HikariCP
- Lombok 1.18.30, log4j 1.2 (`@Log4j`), JUnit 4.13.2 (`@RunWith(SpringJUnit4ClassRunner.class)`)
- Ajax는 아래 4곳만: 일정 순서 변경(UC-013), 멤버 권한 변경(UC-018), 좋아요(UC-021), 닉네임·이메일 중복 확인(UC-001, 023). 이 외에는 폼 제출 + 화면 이동으로 처리한다.
- 드래그: SortableJS, 지도: 카카오맵 JavaScript API v2

## 3. 코드 규칙

### 패키지와 파일 위치
```
src/main/java/com/ttplanner
 ├─ controller   XxxController        화면 요청은 JSP 이름 반환, Ajax는 @ResponseBody로 JSON 반환
 ├─ service      XxxService(인터페이스) + XxxServiceImpl(@Service)
 ├─ mapper       XxxMapper(인터페이스)
 ├─ vo           XxxVO (Lombok @Data)
 └─ interceptor  LoginInterceptor
src/main/resources/mapper/XxxMapper.xml   namespace = com.ttplanner.mapper.XxxMapper
src/main/webapp/WEB-INF/views/            trip/, user/, settings/ ... (공통: includes/header.jsp, footer.jsp)
```

### 작성 규칙
- 의존성 주입: `@AllArgsConstructor` 또는 `@Setter(onMethod_ = @Autowired)` (강의 방식과 동일)
- Controller는 Service만 호출한다. Mapper를 Controller에서 직접 부르지 않는다.
- 여러 행을 함께 바꾸는 작업(DAY 넣기·삭제, 기간 변경, OWNER 넘기기, 여행 복사)은 ServiceImpl 메서드에 `@Transactional`을 붙인다.
- SQL은 Mapper XML에만 쓴다. 값은 `#{}`로 넣고 `${}`는 쓰지 않는다 (SQL 인젝션 방지).
- DB 컬럼은 대문자 언더스코어(`START_DATE`), VO 필드는 카멜 표기(`startDate`). 자동 매핑이 켜져 있다.
- 처리 후 화면 이동은 `redirect:` + `RedirectAttributes.addFlashAttribute()`로 결과를 전달한다.
- JSP에서 값 출력은 `<c:out value="${...}"/>`를 사용한다 (XSS 방지).
- 로그는 `log.info()`를 쓰고 `System.out.println()`은 쓰지 않는다.

### DB 설계 규칙 (구성정의서 6장)
- DAY 날짜는 저장하지 않는다. `START_DATE + DAY_NO - 1`로 계산한다.
- 여행 삭제는 `DEL_YN = 'Y'`로 표시만 한다. 조회 SQL에는 `DEL_YN = 'N'` 조건을 넣는다. 좋아요는 지우지 않는다.
- 일정 순서 변경은 `SLOT_NO`를 바꾸고, `TIME`은 원래 슬롯 순서대로 다시 배정한다.
- 전체 공개 여행의 일정이 0개가 되는 작업(일정 삭제, DAY 삭제, 기간 줄이기)은 Service에서 막는다.
- 공개 여행 목록 페이지 나누기는 `OFFSET ... ROWS FETCH NEXT 10 ROWS ONLY`.
- OWNER는 여행당 항상 1명이다.

### 비밀 정보
- `src/main/resources/db.properties`, 카카오맵 API 키는 코드나 커밋에 넣지 않는다.
- 비밀번호, API 키를 코드에 직접 적는 코드를 만들지 않는다.

## 4. 담당과 파일 소유

| 담당 | UC | 소유 파일 (예시) |
|---|---|---|
| 박준성 | 005, 007, 008, 009, 010, 015, 016 | `trip/detail.jsp`(단독 수정), TripController, TripService, TripMapper, `db/schema.sql` |
| 양지호 | 006, 011~014, 017~019, 025~027 | DayService, ScheduleService, MemberService와 각 Mapper, `trip/_days.jsp`, `trip/_members.jsp`, 지도 조각 |
| 윤아연 | 001~004, 023, 024 | UserController, UserService, LoginInterceptor, `user/*.jsp`, 공개 여행 검색 화면, `includes/` 레이아웃 |
| 송유림 (PM) | 020, 022 | CopyService, StatsService, 복사·통계 화면 |
| 김민정 (QA) | 021 | LikeService, 좋아요 버튼 조각, `src/test/` 테스트 코드 |

- `trip/detail.jsp`는 박준성만 수정한다. 다른 사람은 자기 `_xxx.jsp` 조각만 만들고 `<jsp:include>`로 넣어 달라고 요청한다.
- 팀원 간 연결 메서드(구성정의서 8장 나.)는 이름과 매개변수를 임의로 바꾸지 않는다.

## 5. Git 규칙 (사용자가 직접 실행)

- `main`에 직접 커밋하지 않는다. 작업은 `feature/이름-기능` 브랜치에서 하고 Pull Request로 합친다.
  예: `feature/yurim-copy`, `feature/jiho-day-order`
- 현재 브랜치가 `main`이면 코드 작성 전에 사용자에게 알려 준다.
- 커밋 메시지 형식: `[UC-020] 여행 복사 DAY 선택 화면 추가`
  설정이나 공통 파일이면 `[공통] ...`, 버그 수정이면 `[UC-xxx] fix: ...`
- 커밋 제안 시 `db.properties`, `target/`, `.metadata/`가 포함되지 않았는지 확인한다.

## 6. 자주 나는 오류 (README.md 6장 참고)

- `Could not resolve placeholder 'db.url'` → `db.properties` 없음
- `@Data` getter 빨간 줄 → STS에 Lombok 미설치
- `jakarta.servlet` 오류 → Tomcat 10 사용 중, Tomcat 9로 교체
- 매퍼 XML 오류 → namespace가 인터페이스 전체 이름과 같은지, id가 메서드 이름과 같은지 확인
