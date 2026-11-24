<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  S-03 내 여행 (UC-007) + S-04 새 여행 만들기 창 (UC-008) · 담당: 박준성
  GET  /trips → TripController.list()   → 이 화면
  POST /trips → TripController.create() → 성공: /trips/{번호}, 입력 오류: 이 화면에서 창을 다시 연다
  model: trips(List<TripVO>), regionOptions(지역 17개), newForm(TripFormVO), tripErrors(입력 오류), tripMsg(처리 결과)
--%>
<%@include file="../includes/header.jsp"%>

<style>
	.tt-trip-card { display: block; min-height: 150px; margin-bottom: 20px; padding: 16px 18px;
		border: 1px solid #ddd; border-radius: 4px; background: #fff; color: #333; text-decoration: none; }
	.tt-trip-card:hover, .tt-trip-card:focus { border-color: #337ab7; color: #333; text-decoration: none; }
	.tt-trip-card .tt-card-title { display: block; font-size: 17px; font-weight: bold; margin-bottom: 6px; }
	.tt-trip-card .tt-card-line { display: block; color: #777; }
	.tt-trip-card .tt-card-meta { display: block; margin-top: 12px; color: #337ab7; }
	.tt-new-card { width: 100%; border-style: dashed; color: #337ab7; font-size: 16px; text-align: center; padding-top: 60px; }
	.tt-region-list label { font-weight: normal; margin-right: 14px; }
	.tt-field-error { color: #a94442; font-size: 13px; margin-top: 4px; }
	/* UC-007 2B. 여행 복사 중: DAY를 넣을 수 있는 여행(OWNER · EDITOR)은 진한 테두리, 나머지는 흐리게 */
	.tt-copy-target { border: 3px solid #337ab7; }
	.tt-copy-dim { opacity: .45; }
</style>

<div class="row">
	<div class="col-lg-12">
		<h1 class="page-header">내 여행</h1>
	</div>
</div>

<c:if test="${not empty tripMsg}">
	<div class="alert alert-success"><c:out value="${tripMsg}"/></div>
</c:if>

<%-- UC-007 2B. 여행 복사 중(세션 copyDays, 송유림 UC-020)이면 복사 안내 조각을 보여 준다 --%>
<c:set var="copying" value="${not empty sessionScope.copyDays}"/>
<c:if test="${copying}">
	<c:catch var="copyBarError"><jsp:include page="_copyBar.jsp"/></c:catch>
</c:if>

<div class="row">
	<%-- 2. 내가 만들었거나 멤버로 참여한 여행 카드 (2A. 여행이 없으면 [+ 새로운 여행] 카드만 보인다) --%>
	<c:forEach items="${trips}" var="trip">
		<div class="col-sm-6 col-md-4">
			<c:set var="copyClass" value=""/>
			<c:if test="${copying}">
				<c:set var="copyClass" value="${trip.myRole == 'OWNER' or trip.myRole == 'EDITOR' ? 'tt-copy-target' : 'tt-copy-dim'}"/>
			</c:if>
			<a class="tt-trip-card ${copyClass}" href="/trips/<c:out value='${trip.tripId}'/>">
				<span class="tt-card-title"><c:out value="${trip.title}"/></span>
				<span class="tt-card-line"><c:out value="${trip.periodText}"/></span>
				<span class="tt-card-line"><c:out value="${trip.durationText}"/> · <c:out value="${trip.regionNames}"/></span>
				<span class="tt-card-meta"><c:out value="${trip.myRole}"/> · <c:out value="${trip.scopeLabel}"/></span>
			</a>
		</div>
	</c:forEach>

	<div class="col-sm-6 col-md-4">
		<c:choose>
			<%-- 2B2. 복사 중이면 [+ 새로운 여행] 대신 [+ 복사한 DAY로 새 여행 만들기] (POST /trips/copy/new, 송유림) --%>
			<c:when test="${copying}">
				<form method="post" action="/trips/copy/new">
					<button type="submit" class="tt-trip-card tt-new-card">+ 복사한 DAY로 새 여행 만들기</button>
				</form>
			</c:when>
			<c:otherwise>
				<button type="button" class="tt-trip-card tt-new-card" data-toggle="modal" data-target="#newTripModal">
					+ 새로운 여행
				</button>
			</c:otherwise>
		</c:choose>
	</div>
</div>

<%-- ===== S-04 새 여행 만들기 (UC-008) ===== --%>
<div class="modal fade" id="newTripModal" tabindex="-1" role="dialog" aria-labelledby="newTripTitle">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<form method="post" action="/trips">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-label="닫기"><span aria-hidden="true">&times;</span></button>
					<h4 class="modal-title" id="newTripTitle">새로운 여행</h4>
				</div>
				<div class="modal-body">
					<div class="form-group ${empty tripErrors.title ? '' : 'has-error'}">
						<label for="newTitle">여행 제목</label>
						<input type="text" class="form-control" id="newTitle" name="title" maxlength="50"
							value="<c:out value='${newForm.title}'/>" placeholder="예: 제주도 3박 4일">
						<c:if test="${not empty tripErrors.title}">
							<div class="tt-field-error"><c:out value="${tripErrors.title}"/></div>
						</c:if>
					</div>

					<div class="form-group ${empty tripErrors.period ? '' : 'has-error'}">
						<div class="row">
							<div class="col-xs-6">
								<label for="newStart">시작일</label>
								<input type="date" class="form-control js-start-date" id="newStart" name="startDate"
									value="<c:out value='${newForm.startDate}'/>">
							</div>
							<div class="col-xs-6">
								<label for="newEnd">종료일</label>
								<input type="date" class="form-control js-end-date" id="newEnd" name="endDate"
									value="<c:out value='${newForm.endDate}'/>">
							</div>
						</div>
						<c:if test="${not empty tripErrors.period}">
							<div class="tt-field-error"><c:out value="${tripErrors.period}"/></div>
						</c:if>
					</div>

					<div class="form-group ${empty tripErrors.regions ? '' : 'has-error'}">
						<label>여행 지역 (1개 이상)</label>
						<div class="tt-region-list">
							<c:forEach items="${regionOptions}" var="region">
								<label class="checkbox-inline">
									<input type="checkbox" name="regions" value="<c:out value='${region}'/>"
										<c:if test="${newForm.regions.contains(region)}">checked</c:if>>
									<c:out value="${region}"/>
								</label>
							</c:forEach>
						</div>
						<c:if test="${not empty tripErrors.regions}">
							<div class="tt-field-error"><c:out value="${tripErrors.regions}"/></div>
						</c:if>
					</div>

					<p class="text-muted small">새 여행은 비공개로 만들어지고 만든 사람이 OWNER가 됩니다.
						기간의 날짜마다 빈 DAY가 생기고 공유 링크도 자동으로 만들어집니다.</p>
				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
					<button type="submit" class="btn btn-primary">저장</button>
				</div>
			</form>
		</div>
	</div>
</div>

<script src="/resources/js/tt-trip.js"></script>
<script>
	// 입력 오류로 돌아온 경우 새 여행 창을 다시 연다 (UC-008 5A~5E → 3단계)
	$(function () {
		if ('<c:out value="${openDialog}"/>' === 'new') {
			$('#newTripModal').modal('show');
		}
	});
</script>

<%@include file="../includes/footer.jsp"%>
