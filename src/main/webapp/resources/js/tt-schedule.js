/*
 * 일정 목록 동작 (담당: 송유림, UC-013)
 * 화면 조각: WEB-INF/views/trip/_schedules.jsp (DAY 카드마다 .tt-sch 영역이 하나씩 있다)
 * 필요한 것: jQuery(header.jsp), SortableJS 1.15.7(/resources/vendor/sortablejs/Sortable.min.js)
 *
 * 1. [+ 일정 추가] · [수정] → 같은 자리에 폼 열기, [취소] → 원래대로 (저장은 폼 제출, Ajax 아님)
 * 2. 순서 변경 (구성정의서 4장 Ajax): ⠿를 끌거나, 일정 줄을 눌러 고른 뒤 ↑↓ 키
 *    → POST /api/days/{dayId}/order  본문 [35, 33, 34]
 *    → 서버가 돌려준 시간으로 화면의 시간 글자를 다시 쓴다 (시간은 자리에 남고 장소 · 설명만 움직임)
 *    → 실패하면 원래 순서로 되돌리고 문구를 보여 준다
 * 폼이 하나라도 열려 있으면 그 DAY의 순서 변경을 잠근다.
 */
(function ($) {
	'use strict';

	var MSG_ERROR = '순서를 바꾸지 못했습니다. 화면을 새로 고친 뒤 다시 시도하세요.';

	function init(root) {
		var $root = $(root);
		if ($root.attr('data-editable') !== 'true') return; // VIEWER, 멤버 아님 → 보기만

		var dayId = $root.attr('data-day-id');
		var $list = $root.find('.tt-sch-list');
		var $msg = $root.find('.tt-sch-msg');
		var saving = false;
		var savedOrder = currentOrder(); // 마지막으로 서버에 저장된 순서 (실패 시 되돌릴 때 사용)

		// 위치(숨은 칸)의 처음 값을 기억해 둔다 → [취소] 때 되돌림
		$root.find('.tt-sch-lat, .tt-sch-lng').each(function () {
			$(this).data('orig', this.value);
		});

		var sortable = (window.Sortable && $list.length) ? Sortable.create($list[0], {
			handle: '.tt-sch-handle',     // ⠿를 잡아야만 끌린다 (버튼 · 글자 선택과 겹치지 않게)
			draggable: '.tt-sch-item',
			animation: 150,
			ghostClass: 'tt-sch-ghost',
			onEnd: function (evt) {
				if (evt.oldIndex !== evt.newIndex) save();
			}
		}) : null;

		// ----- 순서 -----

		function currentOrder() {
			return $list.children('.tt-sch-item').map(function () {
				return parseInt($(this).attr('data-sch-id'), 10);
			}).get();
		}

		function isLocked() {
			return saving || $root.find('.tt-sch-form:visible').length > 0;
		}

		function refreshLock() {
			if (sortable) sortable.option('disabled', isLocked());
		}

		// 실패하면 화면 순서를 savedOrder로 되돌린다
		function restore() {
			$.each(savedOrder, function (i, schId) {
				$list.append($list.children('.tt-sch-item[data-sch-id="' + schId + '"]'));
			});
		}

		// 서버가 돌려준 순서 · 시간으로 화면의 시간 글자와 수정 폼의 시간 값을 고친다
		function applyTimes(schedules) {
			$.each(schedules, function (i, s) {
				var $item = $list.children('.tt-sch-item[data-sch-id="' + s.schId + '"]');
				$item.find('.tt-sch-time').text(s.time || '--:--');
				var input = $item.find('.tt-sch-time-input')[0];
				if (input) {
					input.value = s.time || '';
					input.defaultValue = s.time || ''; // [취소](reset) 때 돌아갈 값도 새 시간으로
				}
			});
		}

		function save() {
			var order = currentOrder();
			saving = true;
			refreshLock();
			$msg.text('');

			$.ajax({
				type: 'POST',
				url: '/api/days/' + encodeURIComponent(dayId) + '/order',
				contentType: 'application/json; charset=UTF-8',
				data: JSON.stringify(order),
				dataType: 'json',
				complete: function () {
					saving = false;
					refreshLock();
				},
				success: function (data) {
					savedOrder = order;
					applyTimes(data.schedules || []);
				},
				error: function (xhr) {
					restore();
					// 로그인이 풀렸으면 로그인 화면으로 (LoginInterceptor · Controller가 401을 돌려줌)
					if (xhr.status === 401) {
						window.location.href = '/user/login?back='
							+ encodeURIComponent(location.pathname + location.search);
						return;
					}
					var text = MSG_ERROR;
					try {
						var d = JSON.parse(xhr.responseText);
						if (d.message) text = d.message;
					} catch (ignore) {}
					$msg.text(text);
				}
			});
		}

		// 키보드 ↑↓: 일정 줄에 초점이 있을 때 한 칸씩 옮긴다 (화면설계서 S-05 9)
		$list.on('keydown', '.tt-sch-item', function (e) {
			if (e.target !== this) return;           // 폼 입력칸 안에서 누른 키는 무시
			if (e.which !== 38 && e.which !== 40) return;
			e.preventDefault();                       // 화면이 스크롤되지 않게
			if (isLocked()) return;

			var $item = $(this);
			if (e.which === 38) {                     // ↑
				var $prev = $item.prev('.tt-sch-item');
				if (!$prev.length) return;
				$item.insertBefore($prev);
			} else {                                  // ↓
				var $next = $item.next('.tt-sch-item');
				if (!$next.length) return;
				$item.insertAfter($next);
			}
			$item.focus();
			save();
		});

		// ----- 폼 열기 · 닫기 -----

		// [+ 일정 추가]
		$root.on('click', '.tt-sch-add-btn', function () {
			$(this).hide();
			$root.find('.tt-sch-add-form').show().find('input[name="time"]').focus();
			refreshLock();
		});

		// [수정]: 그 줄의 보기 모양을 숨기고 폼을 연다
		$list.on('click', '.tt-sch-edit-btn', function () {
			var $item = $(this).closest('.tt-sch-item');
			$item.find('.tt-sch-view').hide();
			$item.find('.tt-sch-form').show().find('input[name="time"]').focus();
			refreshLock();
		});

		// [취소]: 입력한 값을 원래대로 되돌리고 닫는다
		$root.on('click', '.tt-sch-cancel', function () {
			var $form = $(this).closest('.tt-sch-form');
			$form[0].reset();
			// hidden 칸은 reset()으로 돌아오지 않으므로 처음 위치를 직접 되돌린다
			$form.find('.tt-sch-lat, .tt-sch-lng').each(function () {
				this.value = $(this).data('orig');
			});
			$form.hide();
			if ($form.hasClass('tt-sch-add-form')) {
				$root.find('.tt-sch-add-btn').show();
			} else {
				$form.closest('.tt-sch-item').find('.tt-sch-view').show();
			}
			refreshLock();
		});

		// 장소 이름을 처음 값과 다르게 고치면 위치(lat · lng)를 지운다 → 이름만 있는 장소로 저장
		// (다른 장소 이름에 예전 위치가 붙어 지도에 엉뚱하게 찍히지 않게)
		// TODO(송유림): UC-014 _placeSearch.jsp로 바꾸면 그 조각이 같은 일을 하므로 이 부분은 지운다
		$root.on('input', '.tt-sch-place-input', function () {
			if (this.value !== this.defaultValue) {
				var $form = $(this).closest('.tt-sch-form');
				$form.find('.tt-sch-lat, .tt-sch-lng').val('');
			}
		});
	}

	$(function () {
		$('.tt-sch').each(function () { init(this); });
	});
})(jQuery);
