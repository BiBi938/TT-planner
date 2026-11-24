/*
 * 지도 보기 (담당: 윤아연, UC-006)
 * 화면 조각: WEB-INF/views/trip/_map.jsp
 *
 * 흐름
 *   _map.jsp가 model schedules를 숨은 목록(.tt-map-data li)으로 그려 둔다
 *   → 이 파일이 그 목록을 읽고 → ttKakao.load()로 카카오 SDK 준비 → 지도에 핀을 찍는다
 *
 *   전체 보기 (3)   DAY마다 다른 색 핀, 번호 "DAY-순서" (예: 2-1)
 *   DAY 보기 (5)    그 DAY 핀만 1, 2, 3 번호, 이동 순서대로 선으로 잇는다
 *   핀 번호 = DAY 카드의 일정 목록 순서. 장소가 없는 일정(lat · lng 없음)은 핀 없이 번호만 건너뛴다
 *   2A 지도 API를 쓸 수 없음 → "지도를 표시할 수 없습니다."
 *   3A 장소가 있는 일정이 없음 → "장소가 있는 일정이 없습니다."
 *
 * 다른 곳에서 부르기
 *   ttMap.showDay(2)  → DAY 2 보기,  ttMap.showDay(0) → 전체 보기
 *   class="tt-map-day-btn" data-day="2" 인 버튼(DAY 카드의 [지도])을 누르면 ttMap.showDay(2) + 지도로 스크롤
 */
window.ttMap = window.ttMap || (function ($) {
	'use strict';

	var MSG_UNAVAILABLE = '지도를 표시할 수 없습니다.';
	var MSG_NO_PLACE = '장소가 있는 일정이 없습니다.';

	/** DAY 카드(_days.jsp)의 왼쪽 색과 같은 순서: 빨강, 파랑, 초록, 주황, 보라 → (DAY 번호 - 1) % 5 */
	var DAY_COLORS = ['#e74c3c', '#3498db', '#2ecc71', '#f39c12', '#9b59b6'];

	var $root, $canvas, $msg;
	var map = null;
	var places = [];      // 지도에 찍을 일정 [{day, order, lat, lng, name, time}]
	var dayCount = {};    // DAY별 일정 개수 (장소 없는 일정 포함)
	var overlays = [];    // 지금 지도에 올라간 핀 · 선 (보기를 바꿀 때 지운다)
	var info = null;      // 핀을 눌렀을 때 뜨는 설명 (하나만 쓴다)
	var currentDay = 0;
	var pendingDay = null; // 지도가 준비되기 전에 [지도] 버튼을 누른 경우

	function colorOf(dayNo) {
		return DAY_COLORS[(dayNo - 1) % DAY_COLORS.length];
	}

	/** 숨은 목록 읽기. 목록은 DAY 번호 → SLOT_NO 순이므로 DAY 안에서 센 순서가 곧 일정 목록 순서다 */
	function readData() {
		places = [];
		dayCount = {};
		$root.find('.tt-map-data li').each(function () {
			var $li = $(this);
			var day = parseInt($li.attr('data-day'), 10);
			if (isNaN(day)) return;
			dayCount[day] = (dayCount[day] || 0) + 1;
			var lat = parseFloat($li.attr('data-lat'));
			var lng = parseFloat($li.attr('data-lng'));
			if (isNaN(lat) || isNaN(lng)) return; // 장소 이름만 있는 일정 → 핀 없음 (UC-014 2A · 3A)
			places.push({
				day: day,
				order: dayCount[day],
				lat: lat,
				lng: lng,
				name: $li.attr('data-name') || '',
				time: $li.attr('data-time') || ''
			});
		});
	}

	function showMsg(text) {
		if (text) {
			$canvas.hide();
			$msg.text(text).show();
		} else {
			$msg.hide().text('');
			$canvas.show();
		}
	}

	function clearOverlays() {
		for (var i = 0; i < overlays.length; i++) overlays[i].setMap(null);
		overlays = [];
		if (info) info.setMap(null);
	}

	/** 핀을 누르면 장소 이름과 시간을 보여 준다 (text()로 넣어 특수문자가 글자로만 보인다) */
	function openInfo(p, label) {
		var $box = $('<div class="tt-map-info"></div>');
		$('<strong></strong>').text(label + '. ' + (p.name || '(장소 이름 없음)')).appendTo($box);
		if (p.time) $('<span class="text-muted"></span>').text(p.time).appendTo($box);
		if (info) info.setMap(null);
		info = new kakao.maps.CustomOverlay({
			position: new kakao.maps.LatLng(p.lat, p.lng),
			content: $box[0],
			yAnchor: 1,
			zIndex: 10
		});
		info.setMap(map);
	}

	function addPin(p, label) {
		var $pin = $('<div class="tt-map-pin"></div>')
			.text(label)
			.css('background', colorOf(p.day))
			.attr('title', label + ' ' + p.name + (p.time ? ' (' + p.time + ')' : ''));
		$pin.on('click', function () { openInfo(p, label); });
		var pin = new kakao.maps.CustomOverlay({
			position: new kakao.maps.LatLng(p.lat, p.lng),
			content: $pin[0],
			yAnchor: 0.5,
			clickable: true
		});
		pin.setMap(map);
		overlays.push(pin);
	}

	/** 찍힌 핀이 모두 보이게 지도 범위를 맞춘다 */
	function fit(list) {
		if (list.length === 1) {
			map.setLevel(4);
			map.setCenter(new kakao.maps.LatLng(list[0].lat, list[0].lng));
			return;
		}
		var bounds = new kakao.maps.LatLngBounds();
		for (var i = 0; i < list.length; i++) bounds.extend(new kakao.maps.LatLng(list[i].lat, list[i].lng));
		map.setBounds(bounds);
	}

	/** dayNo = 0이면 전체 보기 (3), 1 이상이면 그 DAY 보기 (5) */
	function draw(dayNo) {
		currentDay = dayNo;
		$root.find('.tt-map-tab').each(function () {
			var on = parseInt($(this).attr('data-day'), 10) === dayNo;
			$(this).toggleClass('btn-primary', on).toggleClass('btn-default', !on);
		});
		if (!map) return; // 지도가 아직 준비 전이거나 쓸 수 없는 상태

		clearOverlays();
		var list = $.grep(places, function (p) { return dayNo === 0 || p.day === dayNo; });
		if (!list.length) { showMsg(MSG_NO_PLACE); return; } // 3A
		showMsg('');
		map.relayout(); // 숨겨져 있던 지도를 다시 보이게 했으면 크기를 다시 계산해야 한다

		var path = [];
		for (var i = 0; i < list.length; i++) {
			var p = list[i];
			addPin(p, dayNo === 0 ? p.day + '-' + p.order : String(p.order));
			path.push(new kakao.maps.LatLng(p.lat, p.lng));
		}
		// DAY 보기: 이동 순서대로 선으로 잇는다
		if (dayNo !== 0 && path.length > 1) {
			var line = new kakao.maps.Polyline({
				path: path,
				strokeWeight: 4,
				strokeColor: colorOf(dayNo),
				strokeOpacity: 0.8,
				strokeStyle: 'solid'
			});
			line.setMap(map);
			overlays.push(line);
		}
		fit(list);
	}

	function unavailable() { // 2A
		map = null;
		$root.find('.tt-map-tabs').hide();
		showMsg(MSG_UNAVAILABLE);
	}

	function init() {
		$root = $('.tt-map').first();
		if (!$root.length || $root.data('ttMapReady')) return;
		$root.data('ttMapReady', true);
		$canvas = $root.find('.tt-map-canvas');
		$msg = $root.find('.tt-map-msg');

		readData();
		$root.on('click', '.tt-map-tab', function () {
			draw(parseInt($(this).attr('data-day'), 10));
		});

		ttKakao.load($root.attr('data-kakao-key') || '', function () {
			try {
				map = new kakao.maps.Map($canvas[0], {
					center: new kakao.maps.LatLng(37.5665, 126.9780), // 핀이 없을 때의 기본 위치 (서울)
					level: 8
				});
			} catch (e) {
				unavailable();
				return;
			}
			draw(pendingDay !== null ? pendingDay : 0); // 1. 처음에는 전체 보기
		}, unavailable);
	}

	/** DAY 카드의 [지도] 버튼 등 바깥에서 부른다 */
	function showDay(dayNo) {
		dayNo = parseInt(dayNo, 10) || 0;
		if (!map) { pendingDay = dayNo; draw(dayNo); return; }
		draw(dayNo);
	}

	// DAY 카드의 [지도] 버튼 (4): <button class="tt-map-day-btn" data-day="2">
	$(document).on('click', '.tt-map-day-btn', function (e) {
		e.preventDefault();
		showDay($(this).attr('data-day'));
		if ($root && $root.length && $root[0].scrollIntoView) {
			$root[0].scrollIntoView({ behavior: 'smooth', block: 'center' });
		}
	});

	$(init);
	return { showDay: showDay, current: function () { return currentDay; } };
})(jQuery);
