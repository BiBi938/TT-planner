<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%--
  DAY 카드 조각 (UC-011 DAY 추가·삭제, UC-012 DAY 날짜 변경) - 담당: 양지호
  사용법: trip/detail.jsp(박준성)에서 <jsp:include page="_days.jsp"/>
  필요한 model 값
    - days : List<DayVO>  (TripController.detail에서 dayService.findDays(tripId)로 넣어 줌)
    - msg  : 처리 결과 문구 (DayController가 flash로 전달, 있을 때만)
    - trip : TripVO (trip.myRole로 버튼 표시 여부를 정함)
    - 세션 copyDays : 여행 복사 중(송유림 UC-020)이면 맨 앞 · DAY 카드 사이 · 맨 끝에 [여기에 DAY 넣기] (화면설계서 S-09 ③)
--%>

<style>
	.day-card       { border-left: 6px solid #ccc; }
	.day-color-0    { border-left-color: #e74c3c; }  /* 빨강 */
	.day-color-1    { border-left-color: #3498db; }  /* 파랑 */
	.day-color-2    { border-left-color: #2ecc71; }  /* 초록 */
	.day-color-3    { border-left-color: #f39c12; }  /* 주황 */
	.day-color-4    { border-left-color: #9b59b6; }  /* 보라 */
	.day-card .day-title { font-weight: bold; margin-right: 10px; }
	.tt-insert-day  { margin: -10px 0 10px; text-align: center; }  /* [여기에 DAY 넣기] (UC-020) */
</style>

<%-- 여행 번호와 DAY 개수 (DAY는 항상 1개 이상이므로 첫 DAY에서 여행 번호를 꺼낸다) --%>
<c:set var="tripId" value="${days[0].tripId}" />
<c:set var="dayCount" value="${fn:length(days)}" />

<%-- DAY를 추가·삭제할 수 있는지: OWNER, EDITOR만 (trip = TripController(박준성)가 넣는 여행, myRole = 내 권한) --%>
<c:set var="canEditDays" value="${trip.myRole == 'OWNER' or trip.myRole == 'EDITOR'}" />

<%-- [여기에 DAY 넣기]를 보일지: 여행 복사 중(세션 copyDays, 송유림 UC-020)이고 내가 OWNER · EDITOR일 때만
     position = "이 DAY 뒤" (0 = 맨 앞) → POST /trips/{tripId}/copy/insert → CopyController.insert() --%>
<c:set var="canInsertDays" value="${not empty sessionScope.copyDays and canEditDays}" />

<%-- 처리 결과 문구 (DAY 추가·삭제 후 한 번만 보임) --%>
<c:if test="${not empty msg}">
	<div class="alert alert-info">
		<c:out value="${msg}" />
	</div>
</c:if>

<%-- [여기에 DAY 넣기] 맨 앞 (position 0 → 복사한 DAY가 DAY 1부터 들어가고 기존 DAY는 뒤로 밀림) --%>
<c:if test="${canInsertDays}">
<form method="post" action="/trips/<c:out value='${tripId}'/>/copy/insert" class="tt-insert-day" style="margin-top: 0;">
	<input type="hidden" name="position" value="0">
	<button type="submit" class="btn btn-success btn-xs">여기에 DAY 넣기 (맨 앞)</button>
</form>
</c:if>

<%-- DAY 카드 목록 --%>
<c:forEach items="${days}" var="day">
	<div class="panel panel-default day-card day-color-${(day.dayNo - 1) % 5}">
		<div class="panel-body">

			<span class="day-title">DAY <c:out value="${day.dayNo}" /></span>
			<%-- DAY 날짜 + ✎ (UC-012): ✎를 누르면 같은 자리에 날짜 선택칸이 열린다
			     날짜(M/d)는 모두에게 보이고, ✎와 날짜 선택칸은 OWNER에게만 보인다 --%>
			<span id="dateView-<c:out value='${day.dayId}'/>">
				<span class="text-muted"><fmt:formatDate value="${day.dayDate}" pattern="M/d" /></span>
				<c:if test="${trip.myRole == 'OWNER'}">
				<button type="button" class="btn btn-link btn-xs" title="날짜 변경"
					onclick="openDateForm('<c:out value='${day.dayId}'/>')">✎</button>
				</c:if>
			</span>
			<c:if test="${trip.myRole == 'OWNER'}">
			<form id="dateForm-<c:out value='${day.dayId}'/>" class="form-inline" style="display: none;" method="post"
				action="/trips/<c:out value='${tripId}'/>/days/<c:out value='${day.dayId}'/>/date">
				<input type="date" name="dayDate" class="form-control input-sm" required
					value="<fmt:formatDate value='${day.dayDate}' pattern='yyyy-MM-dd' />">
				<button type="submit" class="btn btn-primary btn-xs">변경</button>
				<button type="button" class="btn btn-default btn-xs"
					onclick="closeDateForm('<c:out value='${day.dayId}'/>')">취소</button>
			</form>
			</c:if>
			&nbsp;·&nbsp;
			<span>일정 <c:out value="${day.scheduleCount}" />개</span>

			<%-- [DAY 삭제]
			     1B: DAY가 1개면 버튼 비활성화
			     1C: 일정이 있는 DAY만 확인 창 → [취소]를 누르면 제출하지 않음
			     주소에는 DAY 번호가 아니라 바뀌지 않는 DAY_ID를 넣는다
			     OWNER, EDITOR에게만 보인다 --%>
			<c:if test="${canEditDays}">
			<form class="pull-right" method="post"
				action="/trips/<c:out value='${tripId}'/>/days/<c:out value='${day.dayId}'/>/delete"
				<c:if test="${day.scheduleCount > 0}">
					onsubmit="return confirm('DAY <c:out value='${day.dayNo}'/>에 일정 <c:out value='${day.scheduleCount}'/>개가 있습니다. 함께 삭제할까요?');"
				</c:if>>
				<button type="submit" class="btn btn-default btn-xs"
					<c:if test="${dayCount == 1}">disabled</c:if>>DAY 삭제</button>
			</form>
			</c:if>

			<%-- 일정 목록 (송유림, UC-013). c:forEach의 day는 조각으로 넘어가지 않으므로 dayId를 param으로 넘긴다 --%>
			<jsp:include page="_schedules.jsp"><jsp:param name="dayId" value="${day.dayId}"/></jsp:include>
			<%-- [지도] 이 DAY만 지도에 보기 (UC-006, 윤아연 tt-map.js가 .tt-map-day-btn 클릭을 받아 ttMap.showDay(DAY 번호)) --%>
			<button type="button" class="btn btn-default btn-xs tt-map-day-btn" data-day="<c:out value='${day.dayNo}'/>"><i class="fa fa-map-marker"></i> 지도</button>
		</div>
	</div>

	<%-- [여기에 DAY 넣기] 이 DAY 뒤 (카드 사이, 마지막 DAY 뒤면 맨 끝) --%>
	<c:if test="${canInsertDays}">
	<form method="post" action="/trips/<c:out value='${tripId}'/>/copy/insert" class="tt-insert-day">
		<input type="hidden" name="position" value="<c:out value='${day.dayNo}'/>">
		<button type="submit" class="btn btn-success btn-xs">여기에 DAY 넣기 (DAY <c:out value="${day.dayNo}"/> 뒤)</button>
	</form>
	</c:if>
</c:forEach>

<%-- [+ DAY 추가]: 마지막 DAY 뒤에 빈 DAY 추가 (OWNER, EDITOR에게만 보인다) --%>
<c:if test="${canEditDays}">
<form method="post" action="/trips/<c:out value='${tripId}'/>/days" class="text-center">
	<button type="submit" class="btn btn-primary">+ DAY 추가</button>
</form>
</c:if>

<script>
	// UC-012 2. ✎ → 날짜 표시를 숨기고 날짜 선택칸을 보인다
	function openDateForm(dayId) {
		document.getElementById('dateView-' + dayId).style.display = 'none';
		document.getElementById('dateForm-' + dayId).style.display = 'inline-block';
	}
	// UC-012 3A. [취소] → 고른 날짜를 원래대로 되돌리고 선택칸을 닫는다
	function closeDateForm(dayId) {
		var form = document.getElementById('dateForm-' + dayId);
		form.reset();
		form.style.display = 'none';
		document.getElementById('dateView-' + dayId).style.display = 'inline';
	}
</script>
