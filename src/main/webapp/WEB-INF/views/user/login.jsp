<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  S-01 로그인 (담당: 윤아연, UC-002 / SFR-001)
  GET  /user/login → UserController.loginForm()
  POST /user/login → UserController.login() → 성공: 내 여행(또는 보던 여행)으로 이동, 실패: 이 화면에 오류 표시
--%>
<%@include file="../includes/header.jsp"%>

<div class="tt-narrow">
	<h2 class="page-header">로그인</h2>

	<%-- 로그아웃 · 가입 완료 · 탈퇴 완료 안내 (로그인 필요 안내는 loginRequired.jsp의 브라우저 팝업) --%>
	<c:if test="${not empty message}">
		<div class="alert alert-info"><c:out value="${message}"/></div>
	</c:if>
	<c:if test="${not empty error}">
		<div class="alert alert-danger"><c:out value="${error}"/></div>
	</c:if>

	<div class="panel panel-default">
		<div class="panel-body">
			<form method="post" action="/user/login">
				<div class="form-group">
					<label for="email">이메일 주소</label>
					<input type="email" class="form-control" id="email" name="email"
						value="<c:out value='${email}'/>" placeholder="example@email.com" autofocus>
				</div>
				<div class="form-group">
					<label for="password">비밀번호</label>
					<input type="password" class="form-control" id="password" name="password">
				</div>
				<button type="submit" class="btn btn-primary btn-block">로그인</button>
			</form>

			<hr>
			<a href="/user/join" class="btn btn-default btn-block">회원가입</a>
			<a href="/public/trips" class="btn btn-link btn-block">로그인 없이 공개 여행 보기</a>
		</div>
	</div>

	<%-- 공유 링크 열기 (S-01 5번): 코드를 넣고 [열기] → GET /s/{token} (박준성, UC-005 · UC-016, 구성정의서 v1.4 8장 나.)
	     없는 코드일 때의 안내는 /s/{token}(TripController) 쪽에서 보여 준다 --%>
	<div class="panel panel-default">
		<div class="panel-heading">공유 링크로 여행 열기</div>
		<div class="panel-body">
			<form id="shareForm" class="form-inline">
				<div class="form-group">
					<input type="text" class="form-control" id="shareCode" placeholder="받은 공유 링크 코드" size="30">
				</div>
				<button type="submit" class="btn btn-default">열기</button>
			</form>
			<div id="shareError" class="tt-field-error"></div>
		</div>
	</div>
</div>

<script>
	$(function () {
		$('#shareForm').on('submit', function (e) {
			e.preventDefault();
			var code = $.trim($('#shareCode').val());
			// 링크 전체(http://.../s/코드)를 붙여 넣어도 마지막 코드만 쓴다
			code = code.substring(code.lastIndexOf('/') + 1);
			if (!code) {
				$('#shareError').text('공유 링크 코드를 입력하세요.');
				return;
			}
			location.href = '/s/' + encodeURIComponent(code);
		});
	});
</script>

<%@include file="../includes/footer.jsp"%>
