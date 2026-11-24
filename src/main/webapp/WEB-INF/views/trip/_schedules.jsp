<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  DAY 하나의 일정 목록 조각 (UC-013 일정 관리 · 순서 변경) - 담당: 송유림
  사용법: trip/_days.jsp(양지호)의 DAY 카드 안에서
    <jsp:include page="_schedules.jsp"><jsp:param name="dayId" value="${day.dayId}"/></jsp:include>
    (c:forEach의 day 변수는 include한 파일로 넘어오지 않으므로 dayId를 param으로 받는다)

  필요한 model 값 (TripController.detail(박준성)이 넣어 줌)
    - schedules : List<ScheduleVO>  여행 전체 일정 (ScheduleService.findByTrip) → 여기서 이 DAY 것만 고른다
    - trip      : TripVO  (trip.tripId, trip.myRole 편집 버튼 표시, trip.publicScope 전체 공개면 시간 · 장소 필수)

  화면설계서 S-05
    9  일정: 시간, 장소, 설명과 [수정][삭제]. ⠿를 끌어 순서 변경(시간은 자리에 남음), 키보드 ↑↓로도 이동
    10 [일정 추가]: 시간, 장소, 설명. 장소는 이름만 있어도 저장. 전체 공개 여행은 시간 · 장소 필수
    11 마지막 일정 보호: 서버(ScheduleServiceImpl.delete)에서 막고 문구를 보여 준다
    14 OWNER · EDITOR만 편집 버튼이 보인다. VIEWER와 멤버가 아닌 사용자는 열람만

  저장은 폼 제출 → ScheduleController → redirect:/trips/{tripId} (결과 문구는 _days.jsp의 msg)
  순서 변경만 Ajax: /resources/js/tt-schedule.js → POST /api/days/{dayId}/order
--%>

<c:set var="schDayId" value="${param.dayId}" />
<c:set var="schTripId" value="${trip.tripId}" />
<c:set var="canEditSchedules" value="${trip.myRole == 'OWNER' or trip.myRole == 'EDITOR'}" />

<div class="tt-sch" data-day-id="<c:out value='${schDayId}'/>" data-editable="${canEditSchedules}">

	<%-- 9. 이 DAY의 일정 (schedules는 DAY 번호 → SLOT_NO 순이므로 고른 순서가 곧 표시 순서) --%>
	<c:set var="schCount" value="0" />
	<ul class="list-group tt-sch-list">
	<c:forEach items="${schedules}" var="s">
		<c:if test="${s.dayId == schDayId}">
		<c:set var="schCount" value="${schCount + 1}" />
		<li class="list-group-item tt-sch-item" data-sch-id="<c:out value='${s.schId}'/>"
			<c:if test="${canEditSchedules}">tabindex="0" title="⠿를 끌거나 ↑↓ 키로 순서를 바꿀 수 있습니다"</c:if>>

			<%-- 보기 모양 --%>
			<div class="tt-sch-view">
				<c:if test="${canEditSchedules}">
					<span class="tt-sch-handle" aria-hidden="true">⠿</span>
				</c:if>
				<span class="tt-sch-time"><c:out value="${empty s.time ? '--:--' : s.time}" /></span>
				<span class="tt-sch-place">
					<c:choose>
						<c:when test="${empty s.placeName}"><span class="text-muted">(장소 없음)</span></c:when>
						<c:otherwise>
							<c:out value="${s.placeName}" />
							<c:if test="${not empty s.lat}"><i class="fa fa-map-marker text-success" title="지도에 표시되는 장소"></i></c:if>
						</c:otherwise>
					</c:choose>
				</span>
				<span class="tt-sch-desc text-muted"><c:out value="${s.description}" /></span>

				<c:if test="${canEditSchedules}">
				<span class="pull-right">
					<button type="button" class="btn btn-default btn-xs tt-sch-edit-btn">수정</button>
					<form class="tt-sch-inline" method="post"
						action="/trips/<c:out value='${schTripId}'/>/schedules/<c:out value='${s.schId}'/>/delete"
						onsubmit="return confirm('이 일정을 삭제할까요?');">
						<button type="submit" class="btn btn-default btn-xs">삭제</button>
					</form>
				</span>
				</c:if>
			</div>

			<%-- [수정]을 누르면 보기 모양 대신 이 폼이 열린다 (값은 지금 값으로 채워 둠) --%>
			<c:if test="${canEditSchedules}">
			<form class="tt-sch-form" style="display: none;" method="post" data-require-place="${trip.publicScope}"
				action="/trips/<c:out value='${schTripId}'/>/schedules/<c:out value='${s.schId}'/>/edit">
				<div class="form-group form-group-sm">
					<label>시간</label>
					<input type="time" name="time" class="form-control tt-sch-time-input"
						value="<c:out value='${s.time}'/>" <c:if test="${trip.publicScope}">required</c:if>>
				</div>
				<div class="form-group form-group-sm">
					<label>장소</label>
					<%-- 장소 검색 조각 (윤아연, UC-014): placeName · lat · lng 입력칸을 만들고 카카오 검색으로 채운다 --%>
					<jsp:include page="_placeSearch.jsp">
						<jsp:param name="placeName" value="${s.placeName}"/>
						<jsp:param name="lat" value="${s.lat}"/>
						<jsp:param name="lng" value="${s.lng}"/>
					</jsp:include>
				</div>
				<div class="form-group form-group-sm">
					<label>설명</label>
					<input type="text" name="description" class="form-control" maxlength="500"
						value="<c:out value='${s.description}'/>">
				</div>
				<button type="submit" class="btn btn-primary btn-xs">저장</button>
				<button type="button" class="btn btn-default btn-xs tt-sch-cancel">취소</button>
			</form>
			</c:if>
		</li>
		</c:if>
	</c:forEach>
	</ul>

	<%-- 일정이 없을 때 --%>
	<c:if test="${schCount == 0}">
		<p class="text-muted small">등록된 일정이 없습니다.</p>
	</c:if>

	<%-- 10. [+ 일정 추가] → 같은 자리에 폼이 열린다 (OWNER, EDITOR에게만) --%>
	<c:if test="${canEditSchedules}">
	<button type="button" class="btn btn-default btn-sm tt-sch-add-btn">+ 일정 추가</button>
	<form class="tt-sch-form tt-sch-add-form well well-sm" style="display: none;" method="post"
		data-require-place="${trip.publicScope}" action="/trips/<c:out value='${schTripId}'/>/schedules">
		<input type="hidden" name="dayId" value="<c:out value='${schDayId}'/>">
		<div class="form-group form-group-sm">
			<label>시간</label>
			<input type="time" name="time" class="form-control" <c:if test="${trip.publicScope}">required</c:if>>
		</div>
		<div class="form-group form-group-sm">
			<label>장소</label>
			<%-- 장소 검색 조각 (윤아연, UC-014) --%>
			<jsp:include page="_placeSearch.jsp"/>
		</div>
		<div class="form-group form-group-sm">
			<label>설명</label>
			<input type="text" name="description" class="form-control" maxlength="500">
		</div>
		<c:if test="${trip.publicScope}">
			<p class="help-block small">전체 공개 여행은 시간과 장소를 모두 입력해야 저장됩니다.</p>
		</c:if>
		<button type="submit" class="btn btn-primary btn-xs">저장</button>
		<button type="button" class="btn btn-default btn-xs tt-sch-cancel">취소</button>
	</form>
	</c:if>

	<%-- 순서 변경 결과 문구 (Ajax 실패 시) --%>
	<div class="text-danger small tt-sch-msg"></div>
</div>

<%-- DAY 카드마다 이 조각이 들어가도 스타일 · 스크립트는 한 번만 넣는다 --%>
<c:if test="${empty requestScope.ttScheduleLoaded}">
	<c:set var="ttScheduleLoaded" value="true" scope="request" />
	<style>
		.tt-sch { margin-top: 10px; }
		.tt-sch-list { margin-bottom: 6px; }
		.tt-sch-item:focus { outline: 2px solid #337ab7; }
		.tt-sch-handle { cursor: move; color: #999; margin-right: 6px; }
		.tt-sch-time { display: inline-block; width: 48px; font-weight: bold; }
		.tt-sch-place { margin-right: 6px; }
		.tt-sch-inline { display: inline; }
		.tt-sch-form { margin-top: 6px; }
		.tt-sch-ghost { opacity: .4; }
	</style>
	<script src="/resources/vendor/sortablejs/Sortable.min.js"></script>
	<script src="/resources/js/tt-schedule.js"></script>
</c:if>
