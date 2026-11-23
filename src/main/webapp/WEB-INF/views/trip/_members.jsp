<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  S-06 멤버 목록 · 멤버 초대 조각 (UC-017) · 담당: 양지호
  trip/detail.jsp(박준성)가 멤버에게만 <jsp:include page="_members.jsp"/>로 넣는다.

  model:
    trip(TripVO)            tripId, owner(OWNER인지)
    members(List<MemberVO>) MemberService.findByTrip(tripId) — OWNER → EDITOR → VIEWER → 참여 요청 순
    memberMsg(flash)        초대 결과 문구 (MemberController.invite) → 있으면 멤버 목록 창을 다시 열고 표시

  열기: detail.jsp의 멤버 요약 버튼(#memberSummaryBtn)을 누르면 멤버 목록 창(#membersModal)이 열린다.
  초대: [+ 멤버 초대](OWNER만) → 초대 창(#inviteModal)
        [검색] Ajax GET /api/trips/{tripId}/members/search?nickname=
           200 {"users": [...]} → 목록 (0명이면 5A 문구) / 400 {"message"} → 문구 / 401 → 로그인 화면
        [초대] 폼 제출 POST /trips/{tripId}/members (targetUserId, role)

  이번 범위(UC-017): S-06의 1 · 2 · 3 · 7 · 9
  나중(UC-018 · 019 등): 이름 메뉴, 드래그로 권한 변경, OWNER 넘기기, 참여 요청 처리, 여행 나가기
--%>

<style>
	.tt-member-owner { padding-bottom: 10px; margin-bottom: 10px; border-bottom: 1px solid #ddd; }
	.tt-member-col-viewer { border-left: 1px solid #ddd; }
	.tt-member-list { list-style: none; padding-left: 0; margin-bottom: 0; }
	.tt-member-list li { padding: 3px 0; }
	.tt-member-invited { margin-top: 12px; padding-top: 10px; border-top: 1px dashed #ddd; }
	.tt-search-result { max-height: 220px; overflow-y: auto; margin: 10px 0; }
	.tt-search-result label { display: block; font-weight: normal; padding: 3px 0; cursor: pointer; }
</style>

<%-- 칸마다 인원수를 세어 둔다 (참여 중인 멤버만) --%>
<c:set var="editorCount" value="0" />
<c:set var="viewerCount" value="0" />
<c:set var="invitedCount" value="0" />
<c:forEach var="m" items="${members}">
	<c:if test="${m.status == 'JOINED' && m.role == 'EDITOR'}"><c:set var="editorCount" value="${editorCount + 1}" /></c:if>
	<c:if test="${m.status == 'JOINED' && m.role == 'VIEWER'}"><c:set var="viewerCount" value="${viewerCount + 1}" /></c:if>
	<c:if test="${m.status == 'INVITED'}"><c:set var="invitedCount" value="${invitedCount + 1}" /></c:if>
</c:forEach>

<%-- ===== 멤버 목록 창 (S-06 1 · 2 · 3 · 9) ===== --%>
<div class="modal fade" id="membersModal" tabindex="-1" role="dialog" aria-labelledby="membersModalTitle">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="닫기"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title" id="membersModalTitle">멤버</h4>
			</div>
			<div class="modal-body">

				<%-- 초대 결과 문구 (성공 · 7A · 7B · 7C 등) --%>
				<c:if test="${not empty memberMsg}">
					<div class="alert alert-info" role="status"><c:out value="${memberMsg}" /></div>
				</c:if>

				<%-- 2. OWNER 칸 (항상 1명) + 아래 가로선 --%>
				<div class="tt-member-owner">
					<strong>OWNER</strong>
					<ul class="tt-member-list">
						<c:forEach var="m" items="${members}">
							<c:if test="${m.status == 'JOINED' && m.role == 'OWNER'}">
								<li><c:out value="${m.nickname}" /></li>
							</c:if>
						</c:forEach>
					</ul>
				</div>

				<%-- 3. 왼쪽 EDITOR | 오른쪽 VIEWER, 칸 제목에는 인원수 --%>
				<div class="row">
					<div class="col-xs-6">
						<strong>EDITOR <c:out value="${editorCount}" />명</strong>
						<ul class="tt-member-list">
							<c:forEach var="m" items="${members}">
								<c:if test="${m.status == 'JOINED' && m.role == 'EDITOR'}">
									<li><c:out value="${m.nickname}" /></li>
								</c:if>
							</c:forEach>
						</ul>
					</div>
					<div class="col-xs-6 tt-member-col-viewer">
						<strong>VIEWER <c:out value="${viewerCount}" />명</strong>
						<ul class="tt-member-list">
							<c:forEach var="m" items="${members}">
								<c:if test="${m.status == 'JOINED' && m.role == 'VIEWER'}">
									<li><c:out value="${m.nickname}" /></li>
								</c:if>
							</c:forEach>
						</ul>
					</div>
				</div>

				<%-- 9. 초대 중 (수락 대기): 이름과 권한. 수락 전이라 멤버 칸에는 넣지 않는다 --%>
				<c:if test="${invitedCount > 0}">
					<div class="tt-member-invited">
						<strong class="text-muted">초대 중 (수락 대기)</strong>
						<ul class="tt-member-list">
							<c:forEach var="m" items="${members}">
								<c:if test="${m.status == 'INVITED'}">
									<li class="text-muted"><c:out value="${m.nickname}" /> · <c:out value="${m.role}" /></li>
								</c:if>
							</c:forEach>
						</ul>
					</div>
				</c:if>
			</div>

			<%-- 7. [+ 멤버 초대] OWNER만 --%>
			<c:if test="${trip.owner}">
				<div class="modal-footer">
					<button type="button" class="btn btn-primary" id="openInviteBtn">+ 멤버 초대</button>
				</div>
			</c:if>
		</div>
	</div>
</div>

<%-- ===== 멤버 초대 창 (S-06 7, OWNER만) ===== --%>
<c:if test="${trip.owner}">
	<div class="modal fade" id="inviteModal" tabindex="-1" role="dialog" aria-labelledby="inviteModalTitle">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<form id="inviteForm" method="post" action="/trips/<c:out value='${trip.tripId}'/>/members">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-label="닫기"><span aria-hidden="true">&times;</span></button>
						<h4 class="modal-title" id="inviteModalTitle">멤버 초대</h4>
					</div>
					<div class="modal-body">
						<%-- 4. 닉네임 일부 입력 → [검색] (Enter도 검색, 폼 제출 아님) --%>
						<div class="input-group">
							<input type="text" id="memberKeyword" class="form-control" maxlength="20"
								placeholder="초대할 회원의 닉네임을 입력하세요" aria-label="초대할 회원 닉네임">
							<span class="input-group-btn">
								<button type="button" class="btn btn-default" id="memberSearchBtn">검색</button>
							</span>
						</div>

						<%-- 5. 검색 결과 (JS가 라디오 버튼으로 채운다) / 5A · 오류 문구 --%>
						<div id="memberSearchResult" class="tt-search-result"></div>
						<p id="memberSearchMsg" class="text-danger small" role="status"></p>

						<%-- 6. 권한 고르기 --%>
						<div>
							<strong>권한</strong>&nbsp;
							<label class="radio-inline"><input type="radio" name="role" value="EDITOR"> EDITOR</label>
							<label class="radio-inline"><input type="radio" name="role" value="VIEWER"> VIEWER</label>
						</div>
					</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
						<button type="submit" class="btn btn-primary" id="inviteSubmitBtn">초대</button>
					</div>
				</form>
			</div>
		</div>
	</div>
</c:if>

<script>
	$(function () {
		var tripId = '<c:out value="${trip.tripId}"/>';

		// 1. 멤버 요약(detail.jsp #memberSummaryBtn)을 누르면 멤버 목록 창 열기
		//    바깥 클릭 · Esc로 닫기는 Bootstrap 모달 기본 동작
		$('#memberSummaryBtn').on('click', function () {
			$('#membersModal').modal('show');
		});

		// 초대 결과 문구가 있으면(초대 후 돌아온 경우) 멤버 목록 창을 바로 연다
		var memberMsg = '<c:out value="${memberMsg}"/>';
		if (memberMsg !== '') {
			$('#membersModal').modal('show');
		}

		// 2. [+ 멤버 초대] → 목록 창을 닫고 초대 창 열기 (Bootstrap 3은 창 두 개를 겹쳐 열면 화면이 깨짐)
		$('#openInviteBtn').on('click', function () {
			$('#membersModal').one('hidden.bs.modal', function () {
				$('#inviteModal').modal('show');
			});
			$('#membersModal').modal('hide');
		});

		// 4 · 5. [검색] 또는 Enter → Ajax로 회원 검색
		$('#memberSearchBtn').on('click', searchMembers);
		$('#memberKeyword').on('keydown', function (e) {
			if (e.keyCode === 13) {   // Enter: 폼이 제출되지 않게 막고 검색만
				e.preventDefault();
				searchMembers();
			}
		});

		function searchMembers() {
			var keyword = $.trim($('#memberKeyword').val());
			$('#memberSearchResult').empty();
			$('#memberSearchMsg').text('');

			if (keyword === '') {
				$('#memberSearchMsg').text('닉네임을 입력하세요.');
				return;
			}

			$.ajax({
				type: 'GET',
				url: '/api/trips/' + tripId + '/members/search',
				data: { nickname: keyword },
				dataType: 'json',
				success: function (data) {
					// 5A. 일치하는 회원 없음
					if (data.users.length === 0) {
						$('#memberSearchMsg').text('일치하는 회원이 없습니다.');
						return;
					}
					// 결과마다 라디오 버튼 한 줄. 닉네임은 .text()로 넣는다 (HTML로 넣으면 XSS 위험)
					for (var i = 0; i < data.users.length; i++) {
						var user = data.users[i];
						var $radio = $('<input type="radio" name="targetUserId">').val(user.userId);
						var $label = $('<label>').append($radio).append($('<span>').text(' ' + user.nickname));
						$('#memberSearchResult').append($label);
					}
				},
				error: function (xhr) {
					// 로그인이 풀렸으면 로그인 화면으로 (돌아올 주소 포함)
					if (xhr.status === 401) {
						location.href = '/user/login?back=' + encodeURIComponent(location.pathname + location.search);
						return;
					}
					// 400: 권한 없음 · 빈 검색어 문구 / 그 밖의 오류
					var msg = '검색 중 오류가 발생했습니다.';
					if (xhr.responseJSON && xhr.responseJSON.message) {
						msg = xhr.responseJSON.message;
					}
					$('#memberSearchMsg').text(msg);
				}
			});
		}

		// 7. [초대] → 화면에서 먼저 확인 (서버도 한 번 더 검사한다)
		$('#inviteForm').on('submit', function (e) {
			if ($('input[name="targetUserId"]:checked').length === 0) {
				e.preventDefault();
				$('#memberSearchMsg').text('검색 결과에서 초대할 회원을 고르세요.');   // 7A
				return;
			}
			if ($('input[name="role"]:checked').length === 0) {
				e.preventDefault();
				$('#memberSearchMsg').text('초대할 권한(EDITOR 또는 VIEWER)을 고르세요.');
				return;
			}
			// 두 번 눌러 같은 초대가 두 번 가지 않게 버튼 잠금
			$('#inviteSubmitBtn').prop('disabled', true);
		});
	});
</script>
