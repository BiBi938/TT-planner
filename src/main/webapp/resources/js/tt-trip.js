/*
 * 여행 화면 동작 (담당: 박준성) — trip/list.jsp, trip/detail.jsp
 * Ajax는 쓰지 않는다 (구성정의서 4장). 입력 칸 보조와 링크 복사만 처리한다.
 * 입력값 검사는 서버(TripServiceImpl)에서 하고, 여기서는 고르기 쉽게만 돕는다.
 */
$(function () {

	// 종료일은 시작일과 같거나 이후만 고를 수 있게 한다 (UC-008 5C)
	function syncEndMin($form) {
		var start = $form.find('.js-start-date').val();
		$form.find('.js-end-date').attr('min', start || null);
	}
	$('.js-start-date').each(function () {
		syncEndMin($(this).closest('form'));
	});
	$(document).on('change', '.js-start-date', function () {
		var $form = $(this).closest('form');
		var $end = $form.find('.js-end-date');
		syncEndMin($form);
		if ($end.val() && $end.val() < this.value) {
			$end.val(this.value);
		}
	});

	// UC-009 1A: 여행 기간을 그 자리에서 바꾸는 칸 열기 · 닫기
	$('.js-period-edit').on('click', function () {
		$('#periodView').hide();
		$('#periodEdit').show().find('.js-start-date').focus();
	});
	$('.js-period-cancel').on('click', function () {
		$('#periodEdit').hide();
		$('#periodView').show();
	});

	// UC-016 [링크 복사] → "링크를 복사했습니다."
	// 3A. 복사가 안 되는 브라우저면 주소를 선택해 두고 직접 복사하라고 안내한다
	$('.js-copy-link').on('click', function () {
		var $input = $('#shareUrl');
		var $result = $('#copyResult');
		function done() {
			$result.removeClass('text-danger').addClass('text-success').text('링크를 복사했습니다.');
		}
		function fail() {
			$input.focus().select();
			$result.removeClass('text-success').addClass('text-danger')
				.text('복사하지 못했습니다. 선택된 주소를 Ctrl+C로 직접 복사하세요.');
		}
		if (navigator.clipboard && window.isSecureContext) {
			navigator.clipboard.writeText($input.val()).then(done, fail);
			return;
		}
		$input.focus().select();
		try {
			if (document.execCommand('copy')) {
				done();
			} else {
				fail();
			}
		} catch (e) {
			fail();
		}
	});
});
