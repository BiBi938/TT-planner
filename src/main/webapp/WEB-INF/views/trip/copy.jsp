<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%--
  S-09-1 여행 복사: 복사할 DAY 선택 (담당: 송유림, UC-020)
  GET  /trips/{id}/copy → CopyController.copyForm() → 이 화면
  POST /trips/{id}/copy → CopyController.copy()     → 성공: /trips (내 여행), 오류: 이 화면 + copyError
  model: trip(TripVO), days(List<CopyDayVO>), token(공유 링크로 연 경우), copyError(오류 문구)
--%>
<%@include file="../includes/header.jsp"%>

<style>
	.tt-copy-day { display: block; padding: 10px 12px; margin: 0; border-bottom: 1px solid #eee; font-weight: normal; cursor: pointer; }
	.tt-copy-day:hover { background: #f5f8fc; }
	.tt-copy-day input { margin-right: 8px; }
	.tt-copy-day .tt-day-no { font-weight: bold; margin-right: 8px; }
	.tt-copy-day .tt-day-meta { color: #777; margin-left: 6px; }
	.tt-copy-day .tt-day-places { display: block; color: #999; font-size: 13px; margin: 2px 0 0 24px; }
</style>

<div class="row">
	<div class="col-lg-12">
		<h2 class="page-header">내 여행으로 복사 <small><c:out value="${trip.title}"/></small></h2>
	</div>
</div>

<form method="post" action="/trips/<c:out value='${trip.tripId}'/>/copy" id="copyForm">
	<c:if test="${not empty token}">
		<input type="hidden" name="token" value="<c:out value='${token}'/>">
	</c:if>

	<div class="panel panel-default">
		<div class="panel-heading">
			복사할 DAY를 고르세요
			<label class="pull-right" style="font-weight: normal; margin: 0;">
				<input type="checkbox" id="copyAll"> 전체 선택
			</label>
		</div>

		<%-- 오류 문구 (서버 검사 결과, 또는 아무것도 고르지 않고 [복사]를 눌렀을 때 JS가 표시) --%>
		<div id="copyError" class="alert alert-danger" style="margin: 10px; ${empty copyError ? 'display: none;' : ''}">
			<c:out value="${copyError}"/>
		</div>

		<div class="panel-body" style="padding: 0;">
			<c:forEach items="${days}" var="day">
				<label class="tt-copy-day">
					<input type="checkbox" name="dayIds" value="<c:out value='${day.dayId}'/>" class="js-copy-day">
					<span class="tt-day-no">DAY <c:out value="${day.dayNo}"/></span>
					<fmt:formatDate value="${day.dayDate}" pattern="yyyy.MM.dd (E)"/>
					<span class="tt-day-meta">· 일정 <c:out value="${day.scheduleCount}"/>개</span>
					<c:if test="${not empty day.placeNames}">
						<span class="tt-day-places"><c:out value="${day.placeNames}"/><c:if test="${day.scheduleCount > 3}"> …</c:if></span>
					</c:if>
				</label>
			</c:forEach>
		</div>
	</div>

	<p class="help-block small">
		시간, 장소, 설명만 복사하고 멤버와 좋아요는 복사하지 않습니다. 복사본은 원본과 따로 저장됩니다.<br>
		[복사]를 누르면 내 여행으로 이동합니다. 넣을 여행을 열고 [여기에 DAY 넣기]를 누르거나, [+ 복사한 DAY로 새 여행 만들기]를 누르세요.
	</p>

	<div class="text-right">
		<a class="btn btn-default" href="/trips/<c:out value='${trip.tripId}'/>">취소</a>
		<button type="submit" class="btn btn-primary">복사</button>
	</div>
</form>

<script>
	$(function () {
		var $days = $('.js-copy-day');

		// [전체 선택]: 모든 DAY를 고르거나 모두 해제
		$('#copyAll').on('change', function () {
			$days.prop('checked', this.checked);
		});
		// DAY를 하나씩 고를 때 [전체 선택] 표시를 맞춘다
		$days.on('change', function () {
			$('#copyAll').prop('checked', $days.length === $days.filter(':checked').length);
		});

		// 아무것도 고르지 않으면 제출하지 않고 문구 표시 (서버에서도 같은 검사를 한다)
		$('#copyForm').on('submit', function (e) {
			if ($days.filter(':checked').length === 0) {
				e.preventDefault();
				$('#copyError').text('복사할 DAY를 1개 이상 고르세요.').show();
			}
		});
	});
</script>

<%@include file="../includes/footer.jsp"%>
