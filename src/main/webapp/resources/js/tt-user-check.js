/*
 * 닉네임 · 이메일 중복 확인, 비밀번호 규칙 안내 (담당: 윤아연, UC-001 · UC-023)
 * 회원가입(user/join.jsp)과 마이페이지(settings/mypage.jsp)에서 함께 쓴다.
 *
 * 흐름: 입력 → 0.3초 동안 더 입력이 없으면 GET /api/users/check?nickname=...
 *       → UserController.check() → {"field":"nickname","available":true,"message":"사용할 수 있는 닉네임입니다."}
 *       → 입력칸 아래에 문구 표시 (초록: 사용 가능, 빨강: 사용 불가)
 */
var ttUserCheck = (function ($) {

	var PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9\s]).{8,}$/;
	var PASSWORD_RULE_MSG = '8자리 이상, 영문·숫자·특수문자를 각각 1자 이상 넣어 주세요.';

	function show($msg, text, ok) {
		$msg.text(text || '')
			.removeClass('tt-field-ok tt-field-error')
			.addClass(ok ? 'tt-field-ok' : 'tt-field-error');
	}

	/**
	 * input:   입력칸 선택자 (예: '#nickname')
	 * field:   'nickname' 또는 'email'
	 * msg:     문구를 보여 줄 칸 선택자
	 * initial: (선택) 처음 값과 같으면 확인하지 않는다 (마이페이지의 내 현재 값)
	 */
	function watch(input, field, msg, initial) {
		var $input = $(input);
		var $msg = $(msg);
		var timer = null;
		var seq = 0; // 늦게 도착한 이전 응답이 최신 결과를 덮어쓰지 않도록 번호를 붙인다

		$input.on('input', function () {
			clearTimeout(timer);
			var value = $.trim($input.val());

			if (value === '') {
				show($msg, '', true);
				return;
			}
			if (initial !== undefined && value === initial) {
				show($msg, '', true);
				return;
			}

			timer = setTimeout(function () {
				var mySeq = ++seq;
				var params = {};
				params[field] = value;
				$.ajax({
					url: '/api/users/check',
					type: 'GET',
					data: params,
					dataType: 'json'
				}).done(function (res) {
					if (mySeq !== seq) {
						return;
					}
					show($msg, res.message, res.available);
				}).fail(function () {
					if (mySeq !== seq) {
						return;
					}
					show($msg, '중복 여부를 확인하지 못했습니다. 저장할 때 다시 확인합니다.', false);
				});
			}, 300);
		});
	}

	/** 비밀번호 규칙과 확인 입력 일치 여부를 바로 보여 준다 */
	function watchPassword(pw, pwMsg, confirm, confirmMsg, mismatchText) {
		var $pw = $(pw), $pwMsg = $(pwMsg), $confirm = $(confirm), $confirmMsg = $(confirmMsg);

		function checkConfirm() {
			if ($confirm.val() === '') {
				show($confirmMsg, '', true);
			} else if ($confirm.val() !== $pw.val()) {
				show($confirmMsg, mismatchText, false);
			} else {
				show($confirmMsg, '', true);
			}
		}

		$pw.on('input', function () {
			var v = $pw.val();
			if (v === '' || PASSWORD_RULE.test(v)) {
				show($pwMsg, '', true);
			} else {
				show($pwMsg, PASSWORD_RULE_MSG, false);
			}
			checkConfirm();
		});
		$confirm.on('input', checkConfirm);
	}

	return { watch: watch, watchPassword: watchPassword };
})(jQuery);
