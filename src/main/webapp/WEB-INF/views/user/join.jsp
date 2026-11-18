<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  S-02 회원가입 (담당: 윤아연, UC-001 / SFR-002)
  GET  /user/join → UserController.joinForm()
  POST /user/join → UserController.join() → UserService.join()
       → 성공: 이메일이 채워진 로그인 화면, 실패: 이 화면에 항목별 오류(errors.nickname 등) 표시
  입력하는 동안 GET /api/users/check (Ajax)로 닉네임 · 이메일 중복을 바로 보여 준다.
--%>
<%@include file="../includes/header.jsp"%>

<div class="tt-narrow">
	<h2 class="page-header">회원가입</h2>

	<div class="panel panel-default">
		<div class="panel-body">
			<form id="joinForm" method="post" action="/user/join" novalidate>
				<div class="form-group">
					<label for="nickname">닉네임</label>
					<input type="text" class="form-control" id="nickname" name="nickname" maxlength="20"
						value="<c:out value='${user.nickname}'/>">
					<div id="nicknameMsg" class="${empty errors.nickname ? '' : 'tt-field-error'}"><c:out value="${errors.nickname}"/></div>
				</div>
				<div class="form-group">
					<label for="email">이메일 주소</label>
					<input type="email" class="form-control" id="email" name="email" maxlength="100"
						value="<c:out value='${user.email}'/>">
					<div id="emailMsg" class="${empty errors.email ? '' : 'tt-field-error'}"><c:out value="${errors.email}"/></div>
				</div>
				<div class="form-group">
					<label for="password">비밀번호</label>
					<input type="password" class="form-control" id="password" name="password" maxlength="64">
					<p class="help-block">8자리 이상, 영문·숫자·특수문자를 각각 1자 이상</p>
					<div id="passwordMsg" class="tt-field-error"><c:out value="${errors.password}"/></div>
				</div>
				<div class="form-group">
					<label for="passwordConfirm">비밀번호 확인</label>
					<input type="password" class="form-control" id="passwordConfirm" name="passwordConfirm" maxlength="64">
					<div id="passwordConfirmMsg" class="tt-field-error"><c:out value="${errors.passwordConfirm}"/></div>
				</div>
				<button type="submit" class="btn btn-primary btn-block">가입하기</button>
			</form>
			<hr>
			<a href="/user/login" class="btn btn-link btn-block">이미 계정이 있으면 로그인</a>
		</div>
	</div>
</div>

<script src="/resources/js/tt-user-check.js"></script>
<script>
	$(function () {
		// 닉네임 · 이메일 중복을 입력하는 동안 확인 (tt-user-check.js)
		ttUserCheck.watch('#nickname', 'nickname', '#nicknameMsg');
		ttUserCheck.watch('#email', 'email', '#emailMsg');
		// 비밀번호 규칙 · 확인 일치를 바로 보여 준다 (서버에서도 다시 검사함)
		ttUserCheck.watchPassword('#password', '#passwordMsg', '#passwordConfirm', '#passwordConfirmMsg',
			'비밀번호와 일치하지 않습니다.');
	});
</script>

<%@include file="../includes/footer.jsp"%>
