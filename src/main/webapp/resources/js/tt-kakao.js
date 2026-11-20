/*
 * 카카오맵 SDK 불러오기 (담당: 윤아연, UC-006 지도 · UC-014 장소 검색)
 *
 * 여행 정보 화면에는 지도 조각(_map.jsp)과 장소 검색 조각(_placeSearch.jsp)이 함께 들어갈 수 있다.
 * 두 조각이 SDK <script>를 각각 넣으면 SDK가 두 번 실리므로, 이 파일이 한 번만 불러오고 나눠 쓴다.
 *
 * 사용법
 *   ttKakao.load(appKey, function () { ... 성공: kakao.maps 사용 ... },
 *                        function () { ... 실패: "지도를 표시할 수 없습니다." ... });
 *
 * appKey는 JSP에서 ${applicationScope.kakaoAppKey}로 꺼낸다 (KakaoKeyLoader가 secret.properties에서 읽음).
 * 키가 비었거나, 인터넷이 안 되거나, 카카오에 http://localhost:8080이 등록되지 않았으면 실패 함수를 부른다.
 */
window.ttKakao = window.ttKakao || (function () {
	'use strict';

	var TIMEOUT_MS = 7000; // 이 시간 안에 준비되지 않으면 실패로 본다
	var state = 'idle';    // idle → loading → ready | failed
	var readyQueue = [];
	var failQueue = [];

	function run(queue) {
		var fns = queue.splice(0, queue.length);
		for (var i = 0; i < fns.length; i++) {
			try { fns[i](); } catch (e) { if (window.console) console.error(e); }
		}
	}

	function succeed() {
		if (state !== 'loading') return;
		state = 'ready';
		failQueue.length = 0;
		run(readyQueue);
	}

	function fail() {
		if (state === 'ready' || state === 'failed') return;
		state = 'failed';
		readyQueue.length = 0;
		run(failQueue);
	}

	function load(appKey, onReady, onFail) {
		if (state === 'ready') { onReady(); return; }
		if (state === 'failed') { if (onFail) onFail(); return; }

		readyQueue.push(onReady);
		if (onFail) failQueue.push(onFail);
		if (state === 'loading') return; // 이미 불러오는 중이면 끝나기를 기다린다

		if (!appKey) { state = 'loading'; fail(); return; }

		state = 'loading';
		var timer = setTimeout(fail, TIMEOUT_MS);
		var script = document.createElement('script');
		// autoload=false: SDK 파일을 받은 뒤 kakao.maps.load()로 준비를 끝낸다
		// libraries=services: 장소 검색(Places)을 쓰기 위해 필요하다
		script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?appkey=' + encodeURIComponent(appKey)
			+ '&libraries=services&autoload=false';
		script.onload = function () {
			if (!(window.kakao && kakao.maps && kakao.maps.load)) { clearTimeout(timer); fail(); return; }
			kakao.maps.load(function () { clearTimeout(timer); succeed(); });
		};
		script.onerror = function () { clearTimeout(timer); fail(); };
		document.head.appendChild(script);
	}

	return { load: load };
})();
