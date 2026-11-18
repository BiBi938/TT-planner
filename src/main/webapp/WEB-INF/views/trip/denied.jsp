<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  여행을 볼 수 없거나 권한이 없을 때 (담당: 박준성)
  TripController.accessDenied()가 TripAccessException의 문구(message)를 넣어 보여 준다.
  예) UC-005 2A "이 여행은 비공개입니다. 여행 멤버이거나 공유 링크를 가진 사람만 볼 수 있습니다."
--%>
<%@include file="../includes/header.jsp"%>

<div class="row">
	<div class="col-lg-8" style="padding-top: 40px;">
		<div class="alert alert-warning"><c:out value="${message}"/></div>
		<a class="btn btn-default" href="/public/trips">공개 여행 보기</a>
		<c:if test="${not empty sessionScope.loginUser}">
			<a class="btn btn-primary" href="/trips">내 여행으로</a>
		</c:if>
	</div>
</div>

<%@include file="../includes/footer.jsp"%>
