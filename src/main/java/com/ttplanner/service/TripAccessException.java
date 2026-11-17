package com.ttplanner.service;

/**
 * 여행을 볼 수 없거나 바꿀 수 없을 때 (담당: 박준성)
 * TripController의 @ExceptionHandler가 받아서 trip/denied.jsp에 문구를 보여 준다.
 */
public class TripAccessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status; // 응답 상태 코드 (404, 403)

	private TripAccessException(int status, String message) {
		super(message);
		this.status = status;
	}

	/** 없는 여행이거나 삭제한 여행 */
	public static TripAccessException notFound() {
		return new TripAccessException(404, "여행을 찾을 수 없습니다. 삭제되었거나 주소가 잘못되었습니다.");
	}

	/** UC-005 2A: 비공개 여행인데 멤버가 아니고 공유 링크로 들어오지도 않음 */
	public static TripAccessException privateTrip() {
		return new TripAccessException(403,
				"이 여행은 비공개입니다. 여행 멤버이거나 공유 링크를 가진 사람만 볼 수 있습니다.");
	}

	/** 여행 정보 수정 · 삭제 · 공개 범위 변경은 OWNER만 */
	public static TripAccessException notOwner() {
		return new TripAccessException(403, "여행 정보 수정과 삭제, 공개 범위 변경은 OWNER만 할 수 있습니다.");
	}

	public int getStatus() {
		return status;
	}
}
