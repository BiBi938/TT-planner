<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%--
  복사 안내 조각 (담당: 송유림, UC-020 · 화면설계서 S-09 ②)
  넣는 곳: trip/list.jsp (내 여행, 박준성) — <jsp:include page="_copyBar.jsp"/>
  복사 중(세션 copyDays가 있을 때)에만 보인다. model 값은 쓰지 않고 세션만 본다.

  [복사 취소]                       POST /trips/copy/cancel → CopyController.cancel()
  [+ 복사한 DAY로 새 여행 만들기]   POST /trips/copy/new    → CopyController.createTrip()
--%>
<c:if test="${not empty sessionScope.copyDays}">
	<div class="alert alert-info clearfix">
		<strong>복사한 DAY <c:out value="${fn:length(sessionScope.copyDays)}"/>개</strong>
		· 넣을 여행(진한 테두리)을 열고 [여기에 DAY 넣기]를 누르세요.

		<div class="pull-right">
			<form method="post" action="/trips/copy/new" style="display: inline;">
				<button type="submit" class="btn btn-primary btn-sm">+ 복사한 DAY로 새 여행 만들기</button>
			</form>
			<form method="post" action="/trips/copy/cancel" style="display: inline;">
				<button type="submit" class="btn btn-default btn-sm">복사 취소</button>
			</form>
		</div>
	</div>
</c:if>
