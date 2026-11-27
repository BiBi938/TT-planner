<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%--
  로그인 필요 안내 (담당: 윤아연, UC-002 / SFR-001)
  GET /user/login?required → UserController.loginForm() → 이 화면
  브라우저 팝업 "로그인이 필요합니다." → [확인] → GET /user/login (로그인 화면)

  예) 비로그인으로 GET / → /trips → LoginInterceptor → /user/login?required → 팝업 → /user/login
  로그인 후 돌아갈 주소(세션 backUrl)는 LoginInterceptor · loginForm()이 이미 저장해 두었다.
  replace()로 이동해서 [뒤로] 버튼을 눌러도 팝업이 다시 뜨지 않는다.
--%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>TT플래너</title>
</head>
<body>
<script>
	alert('로그인이 필요합니다.');
	location.replace('${pageContext.request.contextPath}/user/login');
</script>
<noscript><a href="${pageContext.request.contextPath}/user/login">로그인이 필요합니다. 로그인 화면으로 이동</a></noscript>
</body>
