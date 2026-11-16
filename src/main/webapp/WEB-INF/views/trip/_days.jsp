<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%--
  DAY 카드 조각 (UC-011 DAY 추가·삭제) - 담당: 양지호
  사용법: trip/detail.jsp(박준성)에서 <jsp:include page="_days.jsp"/>
  필요한 model 값
    - days : List<DayVO>  (TripController.detail에서 dayService.findDays(tripId)로 넣어 줌)
    - msg  : 처리 결과 문구 (DayController가 flash로 전달, 있을 때만)
--%>

<style>
	.day-card       { border-left: 6px solid #ccc; }
	.day-color-0    { border-left-color: #e74c3c; }  /* 빨강 */
	.day-color-1    { border-left-color: #3498db; }  /* 파랑 */
	.day-color-2    { border-left-color: #2ecc71; }  /* 초록 */
	.day-color-3    { border-left-color: #f39c12; }  /* 주황 */
	.day-color-4    { border-left-color: #9b59b6; }  /* 보라 */
	.day-card .day-title { font-weight: bold; margin-right: 10px; }
</style>

<%-- 여행 번호와 DAY 개수 (DAY는 항상 1개 이상이므로 첫 DAY에서 여행 번호를 꺼낸다) --%>
<c:set var="tripId" value="${days[0].tripId}" />
<c:set var="dayCount" value="${fn:length(days)}" />

<%-- 처리 결과 문구 (DAY 추가·삭제 후 한 번만 보임) --%>
<c:if test="${not empty msg}">
	<div class="alert alert-info">
		<c:out value="${msg}" />
	</div>
</c:if>

<%-- DAY 카드 목록 --%>
<c:forEach items="${days}" var="day">
	<div class="panel panel-default day-card day-color-${(day.dayNo - 1) % 5}">
		<div class="panel-body">

			<span class="day-title">DAY <c:out value="${day.dayNo}" /></span>
			<span class="text-muted"><fmt:formatDate value="${day.dayDate}" pattern="M/d" /></span>
			&nbsp;·&nbsp;
			<span>일정 <c:out value="${day.scheduleCount}" />개</span>

			<%-- [DAY 삭제]
			     1B: DAY가 1개면 버튼 비활성화
			     1C: 일정이 있는 DAY만 확인 창 → [취소]를 누르면 제출하지 않음 --%>
			<form class="pull-right" method="post"
				action="/trips/<c:out value='${tripId}'/>/days/<c:out value='${day.dayNo}'/>/delete"
				<c:if test="${day.scheduleCount > 0}">
					onsubmit="return confirm('DAY <c:out value='${day.dayNo}'/>에 일정 <c:out value='${day.scheduleCount}'/>개가 있습니다. 함께 삭제할까요?');"
				</c:if>>
				<button type="submit" class="btn btn-default btn-xs"
					<c:if test="${dayCount == 1}">disabled</c:if>>DAY 삭제</button>
			</form>

			<%-- TODO(양지호): UC-013 일정 목록, UC-006 [지도] 버튼 자리 --%>
		</div>
	</div>
</c:forEach>

<%-- [+ DAY 추가]: 마지막 DAY 뒤에 빈 DAY 추가 --%>
<form method="post" action="/trips/<c:out value='${tripId}'/>/days" class="text-center">
	<button type="submit" class="btn btn-primary">+ DAY 추가</button>
</form>

<%-- TODO(윤아연): 로그인 정보 확정 후 OWNER·EDITOR에게만 [DAY 삭제], [+ DAY 추가] 버튼 보이기 --%>
