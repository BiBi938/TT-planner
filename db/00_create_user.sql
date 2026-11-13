-- =====================================================================
-- TT플래너 DB 계정 만들기 (각자 PC에서 한 번만 실행)
-- SQL Developer에서 system 계정으로 접속한 뒤 실행한다.
-- =====================================================================
alter session set "_ORACLE_SCRIPT"=true;

create user tt_planner identified by tt_planner
  default tablespace users
  temporary tablespace temp;

grant connect, resource to tt_planner;
-- schema.sql의 v_trip_period 뷰를 만들기 위한 권한 (resource 역할에는 들어 있지 않음)
-- 이미 계정을 만든 사람은 system 접속에서 이 한 줄만 실행한다.
grant create view to tt_planner;
alter user tt_planner quota unlimited on users;

-- 이후 SQL Developer에서 tt_planner / tt_planner 로 새 접속을 만들어
-- 01_sample_board.sql (환경 확인용) 과 schema.sql (TT플래너 테이블) 을 실행한다.
