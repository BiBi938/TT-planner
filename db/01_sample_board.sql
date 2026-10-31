-- =====================================================================
-- 환경 확인용 샘플 (강의 ex02 게시판)
-- tt_planner 계정으로 실행 → http://localhost:8080/board/list 가 열리면
-- STS · Tomcat 9 · Oracle · MyBatis · JSP 설정이 모두 정상이다.
-- TT플래너 기능 구현이 시작되면 Board 관련 파일과 함께 삭제한다.
-- =====================================================================
create sequence seq_board;

create table tbl_board (
  bno        number(10, 0),
  title      varchar2(200)  not null,
  content    varchar2(2000) not null,
  writer     varchar2(50)   not null,
  regdate    date default sysdate,
  updatedate date default sysdate
);

alter table tbl_board add constraint pk_board primary key (bno);

insert into tbl_board (bno, title, content, writer)
values (seq_board.nextval, '환경 확인용 첫 글', 'TT플래너 개발 환경 확인', 'tt_planner');
commit;
