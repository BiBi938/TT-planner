<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  S-05 여행 정보 (UC-005 여행 열람) · 담당: 박준성 — 이 파일은 박준성만 수정한다.
  GET /trips/{번호}   → TripController.detail()
  GET /s/{공유코드}   → TripController.shared()   (공유 링크, 로그인 없이)
  함께 처리: UC-009 여행 정보 수정, UC-010 여행 삭제, UC-015 공개 범위 설정, UC-016 공유 링크 복사

  model: trip(TripVO), myRole, days(List<DayVO>), schedules, summary, members, likeCount, liked, token(공유 링크로 열었을 때), shareUrl(멤버만), regionOptions, infoForm, periodForm,
         tripMsg(처리 결과), tripErrors(입력 오류), openDialog, periodConfirm
  다른 팀원 조각 (파일이 아직 없으면 그 자리만 비워 두고 화면은 그대로 뜬다)
    _days.jsp    DAY 카드 · 일정 (양지호)      _map.jsp   지도 (양지호)
    _members.jsp 멤버 목록 · 초대 (양지호)      _like.jsp  좋아요 버튼 (김민정)
--%>
<%@include file="../includes/header.jsp"%>

<style>
	.tt-created { display: inline-block; margin: 20px 0 4px; padding: 2px 10px; background: #fcf8e3; color: #8a6d3b; }
	.tt-trip-info dt { color: #777; font-weight: normal; }
	.tt-trip-info dd { margin-bottom: 10px; }
	.tt-field-error { color: #a94442; font-size: 13px; margin-top: 4px; }
	.tt-region-list label { font-weight: normal; margin-right: 14px; }
	.tt-share-url { max-width: 420px; display: inline-block; vertical-align: middle; }
	.tt-member-summary { padding: 0; border: 0; background: none; border-bottom: 1px dashed #337ab7; }
	.tt-manage { margin: 10px 0 20px; }
</style>

<%-- 처리 결과 (여행 생성 · 정보 저장 · 기간 변경 · 공개 범위 변경) --%>
<c:if test="${not empty tripMsg}">
	<div class="alert alert-success" style="margin-top: 20px;"><c:out value="${tripMsg}"/></div>
</c:if>

<%-- 3. 작성일은 맨 위에 표시 --%>
<div class="tt-created">작성일 : <c:out value="${trip.createdDateText}"/></div>
<h1 style="margin-top: 6px;"><c:out value="${trip.title}"/></h1>

<dl class="dl-horizontal tt-trip-info">

	<%-- 여행 기간 (UC-009 1A: OWNER는 눌러서 그 자리에서 기간을 바꾼다) --%>
	<dt>여행 기간</dt>
	<dd>
		<div id="periodView" <c:if test="${openDialog == 'period'}">style="display: none;"</c:if>>
			<c:out value="${trip.periodText}"/>
			<span class="text-muted">(<c:out value="${trip.durationText}"/>)</span>
			<c:if test="${trip.owner}">
				<button type="button" class="btn btn-link btn-xs js-period-edit">기간 변경</button>
			</c:if>
		</div>
		<c:if test="${trip.owner}">
			<form id="periodEdit" class="form-inline" method="post" action="/trips/<c:out value='${trip.tripId}'/>/period"
				<c:if test="${openDialog != 'period'}">style="display: none;"</c:if>>
				<input type="date" class="form-control input-sm js-start-date" name="startDate" aria-label="시작일"
					value="<c:out value='${periodForm.startDate}'/>">
				~
				<input type="date" class="form-control input-sm js-end-date" name="endDate" aria-label="종료일"
					value="<c:out value='${periodForm.endDate}'/>">
				<button type="submit" class="btn btn-primary btn-sm">저장</button>
				<button type="button" class="btn btn-default btn-sm js-period-cancel">취소</button>
				<c:if test="${not empty tripErrors.period}">
					<div class="tt-field-error"><c:out value="${tripErrors.period}"/></div>
				</c:if>
			</form>
		</c:if>
	</dd>

	<dt>여행 지역</dt>
	<dd>
		<c:forEach items="${trip.regions}" var="region" varStatus="s"><c:out value="${region}"/><c:if test="${not s.last}">, </c:if></c:forEach>
	</dd>

	<%-- 공개 범위 (UC-015: OWNER만 바꾼다. 지금 값이 아닌 쪽을 누르면 바뀐다) --%>
	<dt>공개 범위</dt>
	<dd>
		<c:choose>
			<c:when test="${trip.owner}">
				<form method="post" action="/trips/<c:out value='${trip.tripId}'/>/scope" class="btn-group btn-group-sm" role="group" aria-label="공개 범위">
					<button type="submit" name="scope" value="PRIVATE"
						class="btn ${trip.publicScope ? 'btn-default' : 'btn-primary active'}"
						<c:if test="${not trip.publicScope}">disabled</c:if>>비공개</button>
					<button type="submit" name="scope" value="PUBLIC"
						class="btn ${trip.publicScope ? 'btn-primary active' : 'btn-default'}"
						<c:if test="${trip.publicScope}">disabled</c:if>>전체 공개</button>
				</form>
				<c:if test="${not empty tripErrors.scope}">
					<div class="tt-field-error"><c:out value="${tripErrors.scope}"/></div>
				</c:if>
				<div class="help-block small">전체 공개는 일정이 1개 이상 있고 모든 일정에 시간과 장소가 있어야 합니다.
					비공개로 바꿔도 공유 링크를 가진 사람은 계속 볼 수 있습니다.</div>
			</c:when>
			<c:otherwise>
				<c:out value="${trip.scopeLabel}"/>
			</c:otherwise>
		</c:choose>
	</dd>

	<%-- 공유 링크 (UC-016: 멤버에게만 보인다, 여행을 만들 때 자동 생성 · 재발급 없음) --%>
	<c:if test="${trip.member}">
		<dt>공유 링크</dt>
		<dd>
			<input type="text" id="shareUrl" class="form-control input-sm tt-share-url" readonly
				value="<c:out value='${shareUrl}'/>" aria-label="공유 링크 주소">
			<button type="button" class="btn btn-default btn-sm js-copy-link">링크 복사</button>
			<span id="copyResult" class="small" role="status"></span>
			<div class="help-block small">링크를 받은 사람은 공개 범위와 관계없이 로그인 없이 볼 수 있습니다.</div>
		</dd>
	</c:if>

	<%-- 멤버 요약 "OWNER 닉네임 외 N명" (4A. 멤버가 누르면 멤버 목록 _members.jsp, 4B. 멤버가 아니면 열리지 않음) --%>
	<dt>멤버</dt>
	<dd>
		<c:choose>
			<c:when test="${trip.member}">
				<button type="button" id="memberSummaryBtn" class="tt-member-summary">
					<c:out value="${trip.memberSummary}"/> &#9662;
				</button>
			</c:when>
			<c:otherwise><c:out value="${trip.memberSummary}"/></c:otherwise>
		</c:choose>
	</dd>
</dl>

<%-- 7. 권한에 맞는 버튼만 표시 --%>
<div class="tt-manage">
	<c:if test="${trip.owner}">
		<button type="button" class="btn btn-default" data-toggle="modal" data-target="#infoModal">제목·지역 수정</button>
		<button type="button" class="btn btn-danger" data-toggle="modal" data-target="#deleteModal">여행 삭제</button>
	</c:if>
	<%-- 7B. 멤버가 아니거나 로그인하지 않아도 [내 여행으로 복사]와 좋아요는 보인다
	     공유 링크(/s/{token})로 열었으면 ?token=…을 붙인다 → 복사 쪽(송유림)이 비공개 여행도 허용 --%>
	<c:url var="copyUrl" value="/trips/${trip.tripId}/copy">
		<c:if test="${not empty token}"><c:param name="token" value="${token}"/></c:if>
	</c:url>
	<a class="btn btn-default" href="<c:out value='${copyUrl}'/>">내 여행으로 복사</a>
	<c:catch var="likeError"><jsp:include page="_like.jsp"/></c:catch>
</div>

<hr>

<div class="row">
	<%-- 5. DAY 카드와 일정 (양지호 _days.jsp) --%>
	<div class="col-md-8">
		<jsp:include page="_days.jsp"/>
	</div>
	<%-- 6. 지도 (양지호, UC-006) --%>
	<div class="col-md-4">
		<c:catch var="mapError"><jsp:include page="_map.jsp"/></c:catch>
	</div>
</div>

<%-- 멤버 목록 (양지호, 멤버에게만) --%>
<c:if test="${trip.member}">
	<c:catch var="membersError"><jsp:include page="_members.jsp"/></c:catch>
</c:if>

<c:if test="${trip.owner}">
	<%-- ===== UC-009 [제목·지역 수정] 창 ===== --%>
	<div class="modal fade" id="infoModal" tabindex="-1" role="dialog" aria-labelledby="infoModalTitle">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<form method="post" action="/trips/<c:out value='${trip.tripId}'/>/info">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-label="닫기"><span aria-hidden="true">&times;</span></button>
						<h4 class="modal-title" id="infoModalTitle">제목·지역 수정</h4>
					</div>
					<div class="modal-body">
						<div class="form-group ${empty tripErrors.title ? '' : 'has-error'}">
							<label for="infoTitle">여행 제목</label>
							<input type="text" class="form-control" id="infoTitle" name="title" maxlength="50"
								value="<c:out value='${infoForm.title}'/>">
							<c:if test="${not empty tripErrors.title}">
								<div class="tt-field-error"><c:out value="${tripErrors.title}"/></div>
							</c:if>
						</div>
						<div class="form-group ${empty tripErrors.regions ? '' : 'has-error'}">
							<label>여행 지역 (1개 이상)</label>
							<div class="tt-region-list">
								<c:forEach items="${regionOptions}" var="region">
									<label class="checkbox-inline">
										<input type="checkbox" name="regions" value="<c:out value='${region}'/>"
											<c:if test="${infoForm.regions.contains(region)}">checked</c:if>>
										<c:out value="${region}"/>
									</label>
								</c:forEach>
							</div>
							<c:if test="${not empty tripErrors.regions}">
								<div class="tt-field-error"><c:out value="${tripErrors.regions}"/></div>
							</c:if>
						</div>
					</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
						<button type="submit" class="btn btn-primary">저장</button>
					</div>
				</form>
			</div>
		</div>
	</div>

	<%-- ===== UC-010 [여행 삭제] 확인 창 ===== --%>
	<div class="modal fade" id="deleteModal" tabindex="-1" role="dialog" aria-labelledby="deleteModalTitle">
		<div class="modal-dialog modal-sm" role="document">
			<div class="modal-content">
				<form method="post" action="/trips/<c:out value='${trip.tripId}'/>/delete">
					<div class="modal-header">
						<h4 class="modal-title" id="deleteModalTitle">여행 삭제</h4>
					</div>
					<div class="modal-body">
						'<c:out value="${trip.title}"/>' 여행과 모든 일정을 삭제합니다.
					</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
						<button type="submit" class="btn btn-danger">삭제</button>
					</div>
				</form>
			</div>
		</div>
	</div>

	<%-- ===== UC-009 1B 기간을 줄여 일정이 지워질 때 확인 창 ===== --%>
	<c:if test="${not empty periodConfirm}">
		<div class="modal fade" id="periodConfirmModal" tabindex="-1" role="dialog" aria-labelledby="periodConfirmTitle">
			<div class="modal-dialog modal-sm" role="document">
				<div class="modal-content">
					<form method="post" action="/trips/<c:out value='${trip.tripId}'/>/period">
						<input type="hidden" name="startDate" value="<c:out value='${periodForm.startDate}'/>">
						<input type="hidden" name="endDate" value="<c:out value='${periodForm.endDate}'/>">
						<input type="hidden" name="confirmDelete" value="Y">
						<div class="modal-header">
							<h4 class="modal-title" id="periodConfirmTitle">여행 기간 변경</h4>
						</div>
						<div class="modal-body"><c:out value="${periodConfirm}"/></div>
						<div class="modal-footer">
							<button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
							<button type="submit" class="btn btn-danger">삭제하고 저장</button>
						</div>
					</form>
				</div>
			</div>
		</div>
	</c:if>
</c:if>

<script src="/resources/js/tt-trip.js"></script>
<script>
	// 입력 오류나 확인이 필요해서 돌아온 경우 해당 창을 다시 연다
	$(function () {
		var open = '<c:out value="${openDialog}"/>';
		if (open === 'info') {
			$('#infoModal').modal('show');
		}
		if ($('#periodConfirmModal').length) {
			$('#periodConfirmModal').modal('show');
		}
	});
</script>

<%@include file="../includes/footer.jsp"%>
