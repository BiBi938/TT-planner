# TT-planner (트립 투게더 플래너)

B팀 정보시스템구축및관리 프로젝트. 버전과 규칙은 **TT플래너 개발환경 구성정의서 v1.2**를 따른다.

## 1. 설치할 것 (팀원 모두 같은 버전)

| 항목 | 버전 | 주의 |
|---|---|---|
| JDK | 1.8 | 프로젝트 JRE는 반드시 JavaSE-1.8 |
| STS | 3.9.x | Spring Legacy Project를 쓰기 위해 STS 3 사용 |
| Tomcat | **9.0.x** | **Tomcat 10 이상 사용 금지** (javax → jakarta 변경으로 Spring 5가 안 뜸) |
| Oracle | 21c XE | SQL Developer로 관리 |
| Lombok | 1.18.30 | STS에도 설치 필요 (아래 3번) |

## 2. 처음 받았을 때

1. `git clone` 후 STS에서 **File → Import → Existing Maven Projects**로 `TT-planner` 폴더를 연다.
2. SQL Developer에서 system 계정으로 `db/00_create_user.sql`을 실행해 `tt_planner` 계정을 만든다.
3. `tt_planner` 계정으로 접속해 `db/01_sample_board.sql`을 실행한다 (환경 확인용 게시판).
4. `src/main/resources/db.properties.example`을 복사해 `db.properties`로 저장하고, 비밀번호를 내 PC에 맞게 고친다.
   `db.properties`는 `.gitignore`에 있어서 GitHub에 올라가지 않는다.
5. 프로젝트 우클릭 → **Maven → Update Project** (Force Update 체크).
6. Servers 탭에서 Tomcat 9에 프로젝트를 추가하고 실행 → `http://localhost:8080/board/list`가 열리면 환경 설정 끝.
   (Tomcat 서버의 Modules 탭에서 Path가 `/`인지 확인. JSP가 `/resources/...` 절대 경로를 쓴다.)

## 3. Lombok을 STS에 설치하기

pom.xml만으로는 STS 편집기가 `@Data`, `@Setter`를 인식하지 못한다.

1. `~/.m2/repository/org/projectlombok/lombok/1.18.30/lombok-1.18.30.jar`를 찾는다
   (Update Project 후 자동으로 받아져 있다).
2. 명령 프롬프트에서 `java -jar lombok-1.18.30.jar` 실행 → STS 설치 위치 선택 → Install/Update.
3. STS를 완전히 껐다 켠다. 예전에 1.18.0을 설치했다면 `STS.ini`에 남은 옛 `-javaagent` 줄을 지운다.

## 4. 동작 확인 테스트 (STS에서 JUnit으로 실행)

| 테스트 | 확인하는 것 |
|---|---|
| `persistence/JDBCTests` | Oracle 접속 (db.properties의 계정) |
| `persistence/DataSourceTests` | HikariCP + MyBatis 연결 |
| `mapper/BoardMapperTests` | 매퍼 XML (`resources/mapper/`) 동작 |

Maven 빌드(`mvn package`)에서는 DB가 없어도 되도록 테스트를 건너뛴다. 테스트는 STS에서 **Run As → JUnit Test**로 돌린다.

## 5. 폴더 구조

```
src/main/java/com/ttplanner
 ├─ controller   요청 처리 (Ajax는 @ResponseBody로 JSON 반환)
 ├─ service      인터페이스 + ServiceImpl, 여러 행 변경은 @Transactional
 ├─ mapper       MyBatis 매퍼 인터페이스
 └─ vo           TripVO, DayVO ... (Lombok @Data)
src/main/resources
 ├─ mapper/      매퍼 XML (TripMapper.xml ...)
 ├─ db.properties.example
 ├─ log4j.xml, log4jdbc.log4j2.properties
src/main/webapp/WEB-INF/views   JSP (includes/header.jsp, footer.jsp 공통)
db/              계정 생성, 샘플, schema.sql
```

`Board*` 파일과 `db/01_sample_board.sql`은 환경 확인용이다. TT플래너 기능 구현을 시작하면 지운다.

## 6. 자주 나는 오류

| 증상 | 원인 · 해결 |
|---|---|
| `Could not resolve placeholder 'db.url'` | `db.properties`가 없음 → 2-4번 |
| `ORA-01017 invalid username/password` | `db.properties` 계정이 내 Oracle과 다름 |
| `@Data`를 붙였는데 getter가 없다는 빨간 줄 | STS에 Lombok 미설치 → 3번 |
| `NoClassDefFoundError: org/apache/logging/log4j` | `log4jdbc.log4j2.properties` 파일이 빠짐 |
| SQL 실행 시 `AbstractMethodError` | pom.xml에서 `log4jdbc-log4j2-jdbc4` → `log4jdbc-log4j2-jdbc4.1`로 변경 |
| `jakarta.servlet` 관련 오류, DispatcherServlet이 안 뜸 | Tomcat 10 이상 사용 중 → Tomcat 9로 교체 |
| jar를 못 받거나 깨짐 | `~/.m2/repository`의 해당 폴더 삭제 후 Update Project (Force Update) |
