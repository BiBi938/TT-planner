-- =====================================================================
-- TT플래너 테이블 · 시퀀스 생성 스크립트
-- 담당: 박준성 (초안 작성: 송유림, 2026-11-11, 박준성 검토 후 확정)
-- 기준: 구성정의서 v1.3 6장, 화면설계서 v1.2 (문서가 다르면 화면설계서 우선)
-- 실행: SQL Developer에서 tt_planner 접속으로 전체 실행(F5)
-- 구조를 바꾸면 이 파일을 고쳐 커밋하고 팀에 알린다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 다시 만들 때만 아래 주석을 풀고 먼저 실행한다 (데이터가 모두 지워짐)
-- ---------------------------------------------------------------------
-- drop view v_trip_period;
-- drop table trip_like purge;
-- drop table trip_member purge;
-- drop table schedule purge;
-- drop table trip_day purge;
-- drop table trip_region purge;
-- drop table trip purge;
-- drop table users purge;
-- drop sequence seq_users;
-- drop sequence seq_trip;
-- drop sequence seq_day;
-- drop sequence seq_schedule;


-- ---------------------------------------------------------------------
-- 시퀀스: 새 행의 번호를 만든다. MyBatis <selectKey>로 seq_xxx.nextval을 받아 쓴다.
-- ---------------------------------------------------------------------
create sequence seq_users;
create sequence seq_trip;
create sequence seq_day;
create sequence seq_schedule;


-- ---------------------------------------------------------------------
-- USERS: 회원
-- 탈퇴해도 행을 지우지 않고 DEL_YN = 'Y'로 표시한다.
--   → 이 회원이 누른 좋아요(TRIP_LIKE)가 남아서 다른 사람의 '좋아요 받은 수'가 줄지 않는다.
--   → 탈퇴할 때 NICKNAME, EMAIL을 '탈퇴회원' + USER_ID 같은 값으로 바꿔서
--     같은 닉네임 · 이메일로 다시 가입할 수 있게 한다 (UserService 탈퇴 처리).
-- 로그인 · 중복 확인 · 회원 검색 SQL에는 DEL_YN = 'N' 조건을 넣는다.
-- ---------------------------------------------------------------------
create table users (
    user_id     number(10)          not null,
    nickname    varchar2(20 char)   not null,
    email       varchar2(100)       not null,
    password    varchar2(100)       not null,   -- 암호화(해시)해서 저장해도 들어가는 길이
    del_yn      char(1)             default 'N' not null,
    constraint pk_users primary key (user_id),
    constraint uk_users_nickname unique (nickname),
    constraint uk_users_email unique (email),
    constraint ck_users_del_yn check (del_yn in ('Y', 'N'))
);


-- ---------------------------------------------------------------------
-- TRIP: 여행
-- 종료일은 저장하지 않는다: START_DATE + (DAY 개수) - 1 로 계산한다.
-- 삭제는 DEL_YN = 'Y'로 표시만 한다 (좋아요 · 통계 유지). 조회 SQL에는 DEL_YN = 'N' 조건.
-- ---------------------------------------------------------------------
create table trip (
    trip_id     number(10)          not null,
    title       varchar2(60 char)   not null,   -- 입력은 50자까지(Service 검사), 복사본 ' (복사)' 붙일 여유 포함
    start_date  date                not null,
    scope       varchar2(10)        default 'PRIVATE' not null,
    share_token varchar2(36)        not null,   -- 여행을 만들 때 Java UUID로 생성, 재발급 없음
    del_yn      char(1)             default 'N' not null,
    created_at  date                default sysdate not null,
    constraint pk_trip primary key (trip_id),
    constraint uk_trip_share_token unique (share_token),
    constraint ck_trip_scope check (scope in ('PRIVATE', 'PUBLIC')),
    constraint ck_trip_del_yn check (del_yn in ('Y', 'N'))
);


-- ---------------------------------------------------------------------
-- TRIP_REGION: 여행 지역 (여행당 1개 이상, Service에서 검사)
-- 지역 목록은 17개 시 · 도로 고정한다 (구성정의서 v1.3 6장).
-- ---------------------------------------------------------------------
create table trip_region (
    trip_id     number(10)          not null,
    region      varchar2(10 char)   not null,
    constraint pk_trip_region primary key (trip_id, region),
    constraint fk_trip_region_trip foreign key (trip_id) references trip (trip_id),
    constraint ck_trip_region_region check (region in (
        '서울', '부산', '대구', '인천', '광주', '대전', '울산', '세종', '경기',
        '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주'))
);


-- ---------------------------------------------------------------------
-- TRIP_DAY: 여행의 DAY
-- DAY 날짜는 저장하지 않는다: START_DATE + DAY_NO - 1 로 계산한다.
-- DAY 넣기 · 삭제로 뒤 DAY 번호를 밀거나 당길 때는 UPDATE 한 문장으로 바꾼다
--   (예: update trip_day set day_no = day_no + 2 where trip_id = #{tripId} and day_no > #{afterDayNo})
--   Oracle은 문장이 끝난 뒤 중복을 검사하므로 한 문장이면 uk_trip_day 오류가 나지 않는다.
-- 30일 제한: Service에서 먼저 검사해 안내 문구를 보여 주고,
--   검사를 빠뜨려도 ck_trip_day_no가 31번째 DAY 저장을 막는다 (DAY 번호는 1부터 빈틈없이 이어짐).
-- ---------------------------------------------------------------------
create table trip_day (
    day_id      number(10)          not null,
    trip_id     number(10)          not null,
    day_no      number(3)           not null,
    constraint pk_trip_day primary key (day_id),
    constraint fk_trip_day_trip foreign key (trip_id) references trip (trip_id),
    constraint uk_trip_day unique (trip_id, day_no),
    constraint ck_trip_day_no check (day_no between 1 and 30)
);


-- ---------------------------------------------------------------------
-- SCHEDULE: DAY별 일정
-- DAY를 지우면 그 DAY의 일정도 함께 지워진다 (on delete cascade).
-- SLOT_NO에는 중복 금지를 걸지 않는다: 순서 변경 중간에 번호가 잠깐 겹칠 수 있다.
-- 비공개 여행은 시간 · 장소 없이도 저장 가능 → TIME, PLACE_NAME은 NULL 허용.
--   전체 공개 여행의 '시간 · 장소 필수' 규칙은 Service에서 검사한다.
-- 장소 이름만 저장할 수 있으므로 LAT, LNG는 NULL 허용.
-- ---------------------------------------------------------------------
create table schedule (
    sch_id      number(10)          not null,
    day_id      number(10)          not null,
    slot_no     number(3)           not null,
    time        varchar2(5),                    -- 'HH24:MI' 형식 (예: '09:30')
    place_name  varchar2(100 char),
    lat         number(10, 7),
    lng         number(10, 7),
    description varchar2(500 char),
    constraint pk_schedule primary key (sch_id),
    constraint fk_schedule_day foreign key (day_id) references trip_day (day_id) on delete cascade,
    constraint ck_schedule_time check (time is null or regexp_like(time, '^([01][0-9]|2[0-3]):[0-5][0-9]$'))
);


-- ---------------------------------------------------------------------
-- TRIP_MEMBER: 여행 멤버 · 초대 · 참여 요청
--   STATUS = 'INVITED'   : OWNER가 초대함, 수락 대기 (ROLE에 초대한 권한)
--   STATUS = 'REQUESTED' : 회원이 참여 요청함, OWNER 처리 대기 (ROLE은 NULL)
--   STATUS = 'JOINED'    : 멤버
-- 거절하면 행을 지운다. 수락하면 ROLE을 채우고 STATUS를 'JOINED'로 바꾼다.
-- 여행을 삭제(DEL_YN = 'Y')해도 OWNER 행은 남긴다 → 통계의 '좋아요 받은 수'에 필요.
-- ---------------------------------------------------------------------
create table trip_member (
    trip_id     number(10)          not null,
    user_id     number(10)          not null,
    role        varchar2(10),
    status      varchar2(10)        not null,
    constraint pk_trip_member primary key (trip_id, user_id),
    constraint fk_trip_member_trip foreign key (trip_id) references trip (trip_id),
    constraint fk_trip_member_user foreign key (user_id) references users (user_id),
    constraint ck_trip_member_role check (role in ('OWNER', 'EDITOR', 'VIEWER')),
    constraint ck_trip_member_status check (status in ('INVITED', 'REQUESTED', 'JOINED')),
    -- 참여 요청일 때만 ROLE이 비어 있고, OWNER는 항상 JOINED
    constraint ck_trip_member_role_status check (
        (status = 'REQUESTED' and role is null)
        or (status = 'INVITED' and role in ('EDITOR', 'VIEWER'))
        or (status = 'JOINED' and role is not null))
);

-- OWNER는 여행당 1명: ROLE이 'OWNER'인 행만 TRIP_ID가 겹치지 않게 막는다.
-- OWNER 넘기기는 한 UPDATE 문장으로 처리하거나 '기존 OWNER 강등 → 새 OWNER 지정' 순서로 한다.
create unique index uk_trip_member_owner
    on trip_member (case when role = 'OWNER' then trip_id end);


-- ---------------------------------------------------------------------
-- TRIP_LIKE: 좋아요 (한 사람이 한 여행에 1회)
-- 여행을 삭제하거나 회원이 탈퇴해도 지우지 않는다.
-- ---------------------------------------------------------------------
create table trip_like (
    trip_id     number(10)          not null,
    user_id     number(10)          not null,
    constraint pk_trip_like primary key (trip_id, user_id),
    constraint fk_trip_like_trip foreign key (trip_id) references trip (trip_id),
    constraint fk_trip_like_user foreign key (user_id) references users (user_id)
);


-- ---------------------------------------------------------------------
-- V_TRIP_PERIOD: 여행 기간 뷰 (종료일 · 일수를 저장하지 않고 매번 계산)
-- 통계 · 공개 여행 검색 · 내 여행 목록에서 같은 계산식을 쓰도록 한 곳에 모은다.
--   END_DATE  = START_DATE + DAY 개수 - 1
--   DAY_COUNT = DAY 개수 (당일치기 = 1, 1박 2일 = 2 ...)
-- 삭제한 여행도 들어 있으므로, 쓰는 쪽 SQL에서 TRIP과 조인해 DEL_YN 조건을 건다.
--   예) select t.title, p.end_date from trip t
--       join v_trip_period p on p.trip_id = t.trip_id
--       where t.del_yn = 'N' and p.end_date < trunc(sysdate)
-- 뷰를 만들려면 tt_planner에 CREATE VIEW 권한이 필요하다 (00_create_user.sql).
-- ---------------------------------------------------------------------
create or replace view v_trip_period as
select t.trip_id,
       t.start_date,
       t.start_date + count(d.day_id) - 1 as end_date,
       count(d.day_id)                    as day_count
  from trip t
  left join trip_day d on d.trip_id = t.trip_id
 group by t.trip_id, t.start_date;
