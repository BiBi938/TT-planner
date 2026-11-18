<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  S-11 마이페이지 (담당: 윤아연, UC-023 / SFR-011)
  GET  /settings/mypage  → UserController.mypage()          현재 닉네임 · 이메일 표시
  POST /settings/mypage  → UserController.updateInfo()      닉네임 · 이메일 저장 (오류는 infoErrors)
  POST /settings/password→ UserController.changePassword()  비밀번호 변경 (오류는 pwErrors)
--%>
<%@include file="../includes/header.jsp"%>

<div class="row">
	<div class="col-lg-12">
		<h2 class="page-header">마이페이지</h2>
	</div>
</div>

<c:if test="${not empty message}">
	<div class="alert alert-success"><c:out value="${message}"/></div>
</c:if>

<div class="row">
	<div class="col-md-6">
		<div class="panel panel-default">
			<div class="panel-heading">회원 정보</div>
			<div class="panel-body">
				<form method="post" action="/settings/mypage" novalidate>
					<div class="form-group">
						<label for="nickname">닉네임</label>
						<input type="text" class="form-control" id="nickname" name="nickname" maxlength="20"
							value="<c:out value='${user.nickname}'/>">
						<div id="nicknameMsg" class="tt-field-error"><c:out value="${infoErrors.nickname}"/></div>
					</div>
					<div class="form-group">
						<label for="email">이메일 주소</label>
						<input type="email" class="form-control" id="email" name="email" maxlength="100"
							value="<c:out value='${user.email}'/>">
						<div id="emailMsg" class="tt-field-error"><c:out value="${infoErrors.email}"/></div>
					</div>
					<button type="submit" class="btn btn-primary">저장</button>
				</form>
			</div>
		</div>
	</div>

	<div class="col-md-6">
		<div class="panel panel-default">
			<div class="panel-heading">비밀번호 변경</div>
			<div class="panel-body">
				<form method="post" action="/settings/password" novalidate>
					<div class="form-group">
						<label for="currentPassword">현재 비밀번호</label>
						<input type="password" class="form-control" id="currentPassword" name="currentPassword">
						<div class="tt-field-error"><c:out value="${pwErrors.currentPassword}"/></div>
					</div>
					<div class="form-group">
						<label for="newPassword">새 비밀번호</label>
						<input type="password" class="form-control" id="newPassword" name="newPassword" maxlength="64">
						<p class="help-block">8자리 이상, 영문·숫자·특수문자를 각각 1자 이상</p>
						<div id="newPasswordMsg" class="tt-field-error"><c:out value="${pwErrors.newPassword}"/></div>
					</div>
					<div class="form-group">
						<label for="newPasswordConfirm">새 비밀번호 확인</label>
						<input type="password" class="form-control" id="newPasswordConfirm" name="newPasswordConfirm" maxlength="64">
						<div id="newPasswordConfirmMsg" class="tt-field-error"><c:out value="${pwErrors.newPasswordConfirm}"/></div>
					</div>
					<button type="submit" class="btn btn-warning">비밀번호 변경</button>
				</form>
			</div>
		</div>
	</div>
</div>

<%-- 현재 저장된 값: 이 값으로 되돌리면 중복 확인을 하지 않는다 --%>
<input type="hidden" id="savedNickname" value="<c:out value='${sessionScope.loginUser.nickname}'/>">
<input type="hidden" id="savedEmail" value="<c:out value='${sessionScope.loginUser.email}'/>">

<script src="/resources/js/tt-user-check.js"></script>
<script>
	$(function () {
		ttUserCheck.watch('#nickname', 'nickname', '#nicknameMsg', $('#savedNickname').val());
		ttUserCheck.watch('#email', 'email', '#emailMsg', $('#savedEmail').val());
		ttUserCheck.watchPassword('#newPassword', '#newPasswordMsg', '#newPasswordConfirm', '#newPasswordConfirmMsg',
			'새 비밀번호와 일치하지 않습니다.');
	});
</script>

<%@include file="../includes/footer.jsp"%>
