/*
 * 장소 검색 (담당: 윤아연, UC-014)
 * 화면 조각: WEB-INF/views/trip/_placeSearch.jsp
 *
 * 흐름 (UC-014 시나리오)
 *   1. 장소 입력칸에 이름을 입력하고 [검색] 또는 Enter
 *   2. 카카오맵 Places.keywordSearch()로 검색 요청 (우리 서버가 아니라 카카오로 가는 요청)
 *   3. 결과 목록 표시 (최대 10곳: 장소 이름, 주소, 분류)
 *   4. 결과 하나를 누르면
 *   5. placeName 입력칸에 장소 이름, 숨은 칸 lat · lng에 위도 · 경도를 채운다
 *      → 이 값들은 일정 등록 · 수정 폼(UC-013, 송유림)과 함께 제출되어 SCHEDULE.PLACE_NAME · LAT · LNG에 저장된다
 *
 * 확장
 *   2A. 지도 API를 쓸 수 없으면 "지도를 표시할 수 없습니다." → 장소 이름만 입력해 저장할 수 있다
 *   3A. 결과가 없으면 "검색 결과가 없습니다." → 다른 이름으로 다시 검색하거나 이름만 입력한다
 *   고른 뒤에 이름을 고치면 위치(lat · lng)를 지운다 (다른 장소 이름에 엉뚱한 위치가 붙지 않게)
 *
 * 한 화면에 입력 폼이 여러 개 있어도 각 .tt-place-search 영역이 따로 동작한다.
 * 폼을 나중에 화면에 넣었다면 ttPlaceSearch.init(그 영역)을 부르면 된다.
 */
window.ttPlaceSearch = window.ttPlaceSearch || (function ($) {
	'use strict';

	var MSG_MAP_UNAVAILABLE = '지도를 표시할 수 없습니다. 장소 이름만 입력해 저장할 수 있습니다.';
	var MSG_NO_RESULT = '검색 결과가 없습니다. 다른 이름으로 검색하거나 장소 이름만 입력해 저장하세요.';
	var MSG_EMPTY = '검색할 장소 이름을 입력하세요.';
	var MSG_ERROR = '검색 중 문제가 생겼습니다. 잠시 뒤 다시 검색하거나 장소 이름만 입력하세요.';
	var MSG_NAME_ONLY = '이름만 저장됩니다 (지도에는 표시되지 않음)';
	var MAX_RESULTS = 10;

	/** DB 컬럼 LAT · LNG가 NUMBER(10, 7)이라 소수점 7자리로 맞춘다 */
	function round7(v) {
		var n = parseFloat(v);
		return isNaN(n) ? '' : n.toFixed(7);
	}

	function init(root) {
		var $root = $(root);
		if ($root.data('ttPlaceReady')) return; // 이미 준비한 영역
		$root.data('ttPlaceReady', true);

		var appKey = $root.attr('data-kakao-key') || '';
		var $name = $root.find('.tt-place-name');
		var $lat = $root.find('.tt-place-lat');
		var $lng = $root.find('.tt-place-lng');
		var $btn = $root.find('.tt-place-btn');
		var $list = $root.find('.tt-place-results');
		var $msg = $root.find('.tt-place-msg');
		var $status = $root.find('.tt-place-status');

		// 고른 장소의 이름 (이름을 고치면 위치를 지우기 위해 기억한다)
		var pickedName = $lat.val() ? $name.val() : null;
		var seq = 0; // 늦게 도착한 이전 검색 결과가 최신 결과를 덮지 않게

		function showMsg(text) { $msg.text(text || ''); }

		function showStatus() {
			if ($lat.val() && $lng.val()) {
				$status.html('<i class="fa fa-map-marker"></i> 지도 위치가 지정되었습니다').removeClass('text-muted').addClass('text-success');
			} else if ($.trim($name.val())) {
				$status.text(MSG_NAME_ONLY).removeClass('text-success').addClass('text-muted');
			} else {
				$status.text('').removeClass('text-success text-muted');
			}
		}

		function clearPosition() {
			$lat.val('');
			$lng.val('');
			pickedName = null;
		}

		function closeList() { $list.empty().hide(); }

		// 2A. 지도 API를 쓸 수 없음 → 검색 버튼을 끄고 이름만 입력하게 한다
		function mapUnavailable() {
			$btn.prop('disabled', true);
			closeList();
			showMsg(MSG_MAP_UNAVAILABLE);
		}

		// 5. 고른 장소를 입력칸에 채운다 (카카오 결과: y = 위도, x = 경도)
		function pick(place) {
			$name.val(place.place_name);
			$lat.val(round7(place.y));
			$lng.val(round7(place.x));
			pickedName = place.place_name;
			closeList();
			showMsg('');
			showStatus();
		}

		// 3. 결과 목록 그리기 (값은 text()로 넣어 화면에 그대로 글자로만 보이게 한다)
		function render(places) {
			$list.empty();
			$.each(places.slice(0, MAX_RESULTS), function (i, place) {
				var $item = $('<a href="#" class="list-group-item tt-place-item"></a>');
				$('<strong></strong>').text(place.place_name).appendTo($item);
				if (place.category_group_name) {
					$('<span class="text-muted small"></span>').text(' · ' + place.category_group_name).appendTo($item);
				}
				$('<div class="small text-muted"></div>')
					.text(place.road_address_name || place.address_name || '').appendTo($item);
				$item.on('click', function (e) { e.preventDefault(); pick(place); });
				$list.append($item);
			});
			$list.show();
		}

		// 1~2. 검색
		function search() {
			var keyword = $.trim($name.val());
			if (!keyword) { showMsg(MSG_EMPTY); closeList(); return; }
			var mySeq = ++seq;
			showMsg('');
			$btn.prop('disabled', true);

			ttKakao.load(appKey, function () {
				var places = new kakao.maps.services.Places();
				places.keywordSearch(keyword, function (data, status) {
					if (mySeq !== seq) return;
					$btn.prop('disabled', false);
					if (status === kakao.maps.services.Status.OK && data.length) {
						render(data);
					} else if (status === kakao.maps.services.Status.ZERO_RESULT || !data || !data.length) {
						closeList();
						showMsg(MSG_NO_RESULT); // 3A
					} else {
						closeList();
						showMsg(MSG_ERROR);
					}
				}, { size: MAX_RESULTS });
			}, function () {
				if (mySeq !== seq) return;
				mapUnavailable(); // 2A
			});
		}

		$btn.on('click', search);

		// 장소 입력칸에서 Enter를 누르면 일정 폼이 제출되지 않고 검색한다
		$name.on('keydown', function (e) {
			if (e.which === 13) { e.preventDefault(); search(); }
			if (e.which === 27) { closeList(); }
		});

		// 고른 뒤에 이름을 고치면 위치를 지운다 → 이름만 있는 장소로 저장된다
		$name.on('input', function () {
			if (pickedName !== null && $name.val() !== pickedName) clearPosition();
			showStatus();
		});

		// 목록 바깥을 누르면 목록을 닫는다
		$(document).on('click', function (e) {
			if (!$.contains($root[0], e.target)) closeList();
		});

		showStatus();
	}

	function initAll() {
		$('.tt-place-search').each(function () { init(this); });
	}

	$(initAll);
	return { init: init, initAll: initAll };
})(jQuery);
