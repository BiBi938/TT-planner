<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  S-06 멤버 목록 · 멤버 초대 (UC-017) · 멤버 권한 관리 (UC-018) 조각 · 담당: 양지호
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

  UC-018 (OWNER가 볼 때만): 참여 중인 EDITOR · VIEWER 이름 → 메뉴 (Bootstrap 드롭다운, 바깥 클릭 · Esc로 닫힘 = 4A)
        [권한 변경] Ajax POST /api/trips/{tripId}/members/{userId}/role (role) → 이름을 반대 칸으로 옮기고 인원수 다시 셈
        [OWNER 넘기기] · [내보내기] → 목록 창 안의 확인 상자(#memberConfirm) → 폼 제출 POST …/{userId}/owner · …/kick
        (두 번째 모달을 띄우지 않음: Bootstrap 3 모달 겹침 문제)

        드래그(3A, SortableJS): 이름을 반대 칸에 놓으면 [권한 변경]과 같은 Ajax, 같은 칸이면 아무 일 없음,
        OWNER 칸에 놓으면 원래 자리로 돌려놓고 5B 확인 상자. 실패하면 원래 칸으로 되돌림

  범위: S-06의 1 · 2 · 3 · 4 · 5 · 6 · 7 · 9
  나중(다른 UC): 참여 요청 처리, 여행 나가기(UC-019)
--%>

<style>
	.tt-member-owner { padding-bottom: 10px; margin-bottom: 10px; border-bottom: 1px solid #ddd; }
	.tt-member-col-viewer { border-left: 1px solid #ddd; }
	.tt-member-list { list-style: none; padding-left: 0; margin-bottom: 0; }
	.tt-member-list li { padding: 3px 0; }
	.tt-member-invited { margin-top: 12px; padding-top: 10px; border-top: 1px dashed #ddd; }
	.tt-search-result { max-height: 220px; overflow-y: auto; margin: 10px 0; }
	.tt-search-result label { display: block; font-weight: normal; padding: 3px 0; cursor: pointer; }
	.tt-member-menu > a { color: inherit; text-decoration: none; cursor: pointer; }
	.tt-member-menu > a:hover, .tt-member-menu > a:focus { text-decoration: underline; }
	.tt-member-confirm { margin-top: 12px; padding: 10px 12px; border: 1px solid #ebccd1; border-radius: 4px; background: #fdf7f7; }
	.tt-member-confirm p { margin-bottom: 8px; }
	/* 드래그: 빈 칸에도 놓을 수 있게 최소 높이, 끄는 중인 자리 표시 */
	.tt-member-draggable .tt-member-list { min-height: 30px; }
	.tt-member-draggable .tt-member-item { cursor: move; }
	.tt-member-ghost { opacity: 0.4; background: #e8f4fb; }
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
					<div class="alert alert-info tt-member-flash" role="status"><c:out value="${memberMsg}" /></div>
				</c:if>
				<%-- UC-018 권한 변경(Ajax) 결과 문구: JS가 채운다 --%>
				<div id="memberAjaxMsg" class="alert alert-info" role="status" style="display: none;"></div>

				<%-- 2. OWNER 칸 (항상 1명) + 아래 가로선 --%>
				<%-- OWNER가 볼 때만 tt-member-draggable: 드래그 모양(CSS)과 SortableJS 대상 --%>
				<div class="<c:if test="${trip.owner}">tt-member-draggable</c:if>">
				<div class="tt-member-owner">
					<strong>OWNER</strong>
					<%-- memberColOWNER: 드래그로 여기에 놓으면 OWNER 넘기기(5B) 확인 --%>
					<ul class="tt-member-list" id="memberColOWNER">
						<c:forEach var="m" items="${members}">
							<c:if test="${m.status == 'JOINED' && m.role == 'OWNER'}">
								<li class="tt-member-owner-name"><c:out value="${m.nickname}" /></li>
							</c:if>
						</c:forEach>
					</ul>
				</div>

				<%-- 3. 왼쪽 EDITOR | 오른쪽 VIEWER, 칸 제목에는 인원수
				     col을 "EDITOR", "VIEWER" 차례로 바꾸며 같은 모양의 칸을 두 번 그린다
				     ul의 id(memberColEDITOR · memberColVIEWER): 권한 변경 후 JS가 이름을 옮겨 넣는 칸 --%>
				<div class="row">
					<c:forEach var="col" items="EDITOR,VIEWER">
						<div class="col-xs-6 tt-member-col<c:if test="${col == 'VIEWER'}"> tt-member-col-viewer</c:if>">
							<strong><c:out value="${col}" /> <span class="tt-member-count"><c:out value="${col == 'EDITOR' ? editorCount : viewerCount}" /></span>명</strong>
							<ul class="tt-member-list" id="memberCol<c:out value='${col}'/>">
								<c:forEach var="m" items="${members}">
									<c:if test="${m.status == 'JOINED' && m.role == col}">
										<li class="tt-member-item" data-user-id="<c:out value='${m.userId}'/>"
											data-nickname="<c:out value='${m.nickname}'/>" data-role="<c:out value='${m.role}'/>">
											<c:choose>
												<%-- 4 · 5. OWNER가 볼 때: 이름을 누르면 메뉴 --%>
												<c:when test="${trip.owner}">
													<div class="dropdown tt-member-menu">
														<a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false"
															><c:out value="${m.nickname}" /> <span class="caret"></span></a>
														<ul class="dropdown-menu">
															<li><a href="#" class="tt-act-role">권한 변경 (→ <span class="tt-other-role"><c:out value="${col == 'EDITOR' ? 'VIEWER' : 'EDITOR'}" /></span>)</a></li>
															<li><a href="#" class="tt-act-owner">OWNER 넘기기</a></li>
															<li role="separator" class="divider"></li>
															<li><a href="#" class="tt-act-kick text-danger">내보내기</a></li>
														</ul>
													</div>
												</c:when>
												<%-- EDITOR · VIEWER가 볼 때: 이름 글자만 (메뉴 HTML이 아예 안 써짐) --%>
												<c:otherwise><c:out value="${m.nickname}" /></c:otherwise>
											</c:choose>
										</li>
									</c:if>
								</c:forEach>
							</ul>
						</div>
					</c:forEach>
				</div>
				</div><%-- /tt-member-draggable --%>

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

				<%-- 5A · 5B 확인 상자 (OWNER만): 메뉴의 [내보내기] · [OWNER 넘기기]를 누르면 JS가 문구와 action을 채워 보여 준다
				     [확인] → 폼 제출 POST /trips/{tripId}/members/{userId}/kick 또는 /owner, [취소] → 상자만 닫음 --%>
				<c:if test="${trip.owner}">
					<div id="memberConfirm" class="tt-member-confirm" style="display: none;">
						<p id="memberConfirmText"></p>
						<form id="memberConfirmForm" method="post" action="" class="text-right">
							<button type="button" class="btn btn-default btn-sm" id="memberConfirmCancel">취소</button>
							<button type="submit" class="btn btn-danger btn-sm" id="memberConfirmOk">확인</button>
						</form>
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

<%-- 드래그(3A)용 SortableJS: OWNER 화면에서만 필요. 파일은 송유림 님 UC-013이 넣어 둔 것 (_schedules.jsp도 같은 파일을 씀) --%>
<c:if test="${trip.owner}">
	<script src="/resources/vendor/sortablejs/Sortable.min.js"></script>
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

		// ===================== UC-018 멤버 권한 관리 (OWNER 화면에만 메뉴 · 확인 상자가 있음) =====================

		var memberUrl = '/trips/' + tripId + '/members/';

		// 메뉴 링크가 속한 멤버 줄 (<li data-user-id data-nickname data-role>)
		function memberItem(link) {
			return $(link).closest('.tt-member-item');
		}

		// 목록 창 위에 문구 표시 (초대 결과 같은 예전 flash 문구는 숨김)
		function showMemberMsg(text) {
			$('.tt-member-flash').hide();
			$('#memberAjaxMsg').text(text).show();
		}

		// 칸 제목의 인원수를 칸 안의 이름 개수로 다시 센다 (서버가 처음 센 숫자는 이미 글자라 JS가 고쳐야 함)
		function refreshMemberCounts() {
			$('.tt-member-col').each(function () {
				$(this).find('.tt-member-count').text($(this).find('.tt-member-item').length);
			});
		}

		// 멤버 줄을 role 칸으로 옮기고, 메뉴 글자 "권한 변경 (→ 반대 권한)"을 고친다
		function moveMember($item, role) {
			$item.attr('data-role', role);
			$item.find('.tt-other-role').text(role === 'EDITOR' ? 'VIEWER' : 'EDITOR');
			$('#memberCol' + role).append($item);
			refreshMemberCounts();
		}

		// 권한 변경 Ajax (메뉴와 드래그가 함께 쓴다). 실패하면 onFail(), 끝나면(성공 · 실패 모두) onDone()을 부른다
		function changeMemberRole($item, newRole, onFail, onDone) {
			$.ajax({
				type: 'POST',
				url: '/api/trips/' + tripId + '/members/' + $item.attr('data-user-id') + '/role',
				data: { role: newRole },
				dataType: 'json',
				complete: function () {
					if (onDone) {
						onDone();
					}
				},
				success: function (data) {
					moveMember($item, data.role);
					showMemberMsg(data.message);
				},
				error: function (xhr) {
					if (onFail) {
						onFail();
					}
					if (xhr.status === 401) {
						location.href = '/user/login?back=' + encodeURIComponent(location.pathname + location.search);
						return;
					}
					var msg = '권한을 바꾸지 못했습니다.';
					if (xhr.responseJSON && xhr.responseJSON.message) {
						msg = xhr.responseJSON.message;
					}
					showMemberMsg(msg);
				}
			});
		}

		// 4 · 5. [권한 변경]: 지금 권한의 반대로
		$('#membersModal').on('click', '.tt-act-role', function (e) {
			e.preventDefault();
			var $item = memberItem(this);
			var newRole = ($item.attr('data-role') === 'EDITOR') ? 'VIEWER' : 'EDITOR';
			changeMemberRole($item, newRole, null, null);
		});

		// 확인 상자 열기 · 닫기
		function openMemberConfirm(text, action, okLabel) {
			$('#memberConfirmText').text(text);
			$('#memberConfirmForm').attr('action', action);
			$('#memberConfirmOk').text(okLabel).prop('disabled', false);
			$('#memberConfirm').show();
		}
		function closeMemberConfirm() {
			$('#memberConfirm').hide();
			$('#memberConfirmForm').attr('action', '');
		}

		// 5A. [내보내기] → "○○ 님을 이 여행에서 내보냅니다."
		$('#membersModal').on('click', '.tt-act-kick', function (e) {
			e.preventDefault();
			var $item = memberItem(this);
			openMemberConfirm($item.attr('data-nickname') + ' 님을 이 여행에서 내보냅니다.',
				memberUrl + $item.attr('data-user-id') + '/kick', '내보내기');
		});

		// 5B. [OWNER 넘기기] → "○○ 님에게 OWNER를 넘깁니다. △△ 님은 EDITOR가 됩니다."
		$('#membersModal').on('click', '.tt-act-owner', function (e) {
			e.preventDefault();
			var $item = memberItem(this);
			var ownerName = $.trim($('.tt-member-owner-name').first().text());
			openMemberConfirm($item.attr('data-nickname') + ' 님에게 OWNER를 넘깁니다. ' + ownerName + ' 님은 EDITOR가 됩니다.',
				memberUrl + $item.attr('data-user-id') + '/owner', '넘기기');
		});

		$('#memberConfirmCancel').on('click', closeMemberConfirm);
		$('#membersModal').on('hidden.bs.modal', closeMemberConfirm); // 창을 닫으면 상자도 초기화

		// ===== 3A. 드래그로 권한 변경 (OWNER 화면 + SortableJS가 있을 때만) =====
		// EDITOR · VIEWER 칸: 이름을 꺼내고 놓을 수 있음 / OWNER 칸: 놓기만 (OWNER 이름은 tt-member-item이 아니라 안 끌림)
		var memberSortables = [];

		// 드래그 중 Ajax를 보내는 동안 다른 드래그를 막는다
		function lockMemberDrag(locked) {
			for (var i = 0; i < memberSortables.length; i++) {
				memberSortables[i].option('disabled', locked);
			}
		}

		// 끌어 온 줄을 원래 칸 · 원래 순서 자리로 되돌린다
		function putBack(item, fromList, oldIndex) {
			fromList.insertBefore(item, fromList.children[oldIndex] || null);
			refreshMemberCounts();
		}

		// 이름을 놓았을 때 (evt.from = 꺼낸 칸, evt.to = 놓은 칸)
		function onMemberDrop(evt) {
			var $item = $(evt.item);
			var toId = evt.to.id;

			// 같은 칸 안에서 순서만 바뀜 → 아무 일 없음 (임의결정 55)
			if (evt.from === evt.to) {
				return;
			}

			// OWNER 칸 → 원래 자리로 돌려놓고 5B 확인 상자 ([넘기기]를 눌러야 실제로 넘어감)
			if (toId === 'memberColOWNER') {
				putBack(evt.item, evt.from, evt.oldIndex);
				$item.find('.tt-act-owner').trigger('click');
				return;
			}

			// 반대 칸 (memberColEDITOR / memberColVIEWER) → 권한 변경 Ajax
			var newRole = toId.replace('memberCol', '');
			refreshMemberCounts();
			lockMemberDrag(true);
			changeMemberRole($item, newRole, function () {
				putBack(evt.item, evt.from, evt.oldIndex); // 실패 → 원래 칸으로
			}, function () {
				lockMemberDrag(false); // 성공 · 실패 모두 끝나면 다시 끌 수 있게
			});
		}

		if (window.Sortable && $('.tt-member-draggable').length) {
			var dragLists = ['memberColEDITOR', 'memberColVIEWER'];
			for (var i = 0; i < dragLists.length; i++) {
				var list = document.getElementById(dragLists[i]);
				if (list) {
					memberSortables.push(Sortable.create(list, {
						group: { name: 'tt-members', pull: true, put: true },
						draggable: '.tt-member-item',
						animation: 150,
						ghostClass: 'tt-member-ghost',
						onEnd: onMemberDrop
					}));
				}
			}
			var ownerList = document.getElementById('memberColOWNER');
			if (ownerList) {
				memberSortables.push(Sortable.create(ownerList, {
					group: { name: 'tt-members', pull: false, put: true }, // 놓기만, OWNER 이름은 못 꺼냄
					draggable: '.tt-member-item',
					sort: false
				}));
			}
		}

		// [확인] → 폼 제출. 두 번 눌러 두 번 가지 않게 잠금
		$('#memberConfirmForm').on('submit', function (e) {
			if (!$(this).attr('action')) {
				e.preventDefault();
				return;
			}
			$('#memberConfirmOk').prop('disabled', true);
		});
	});
</script>
