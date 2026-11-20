package com.ttplanner.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ttplanner.mapper.TripMapper;
import com.ttplanner.vo.TripFormVO;
import com.ttplanner.vo.TripVO;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j;

/**
 * 여행 관리 (담당: 박준성)
 * 오류 문구는 레드마인 유스케이스 명세(UC-005 ~ UC-016)의 문구를 그대로 쓴다.
 */
@Log4j
@Service
@AllArgsConstructor
public class TripServiceImpl implements TripService {

	/** TRIP.TITLE 컬럼 길이. 입력 창은 50자(TITLE_MAX_LENGTH)지만 복사한 여행은 '원본 (복사)'라 길어질 수 있다 */
	private static final int TITLE_DB_LENGTH = 60;

	private TripMapper mapper;

	// ===================== UC-007 내 여행 목록 =====================

	@Override
	public List<TripVO> getMyTrips(Long userId) {
		log.info("getMyTrips......" + userId);
		return mapper.getMyTrips(userId);
	}

	// ===================== UC-008 여행 생성 =====================

	@Transactional
	@Override
	public Long create(Long userId, TripFormVO form) {
		log.info("create......" + userId + ", " + form);

		// 5. 입력값 검사 (5A ~ 5E)
		Map<String, String> errors = new LinkedHashMap<>();
		String title = checkTitle(form, errors);
		LocalDate[] period = checkPeriod(form, errors);
		List<String> regions = checkRegions(form, errors);
		if (!errors.isEmpty()) {
			throw new TripValidationException(errors);
		}
		LocalDate start = period[0];
		int dayCount = days(period[0], period[1]);

		// 6. 비공개 여행을 만들고 회원을 OWNER로 등록, 8. 공유 링크 자동 생성
		TripVO trip = new TripVO();
		trip.setTitle(title);
		trip.setStartDate(java.sql.Date.valueOf(start));
		Long tripId = create(trip, regions, userId);

		// 7. 여행 기간의 날짜마다 빈 DAY를 만든다
		// TODO(양지호): DayService.syncDays(tripId, start, end)가 develop에 들어오면 아래 반복문을 그 호출로 바꾼다.
		for (int dayNo = 1; dayNo <= dayCount; dayNo++) {
			mapper.insertDay(tripId, dayNo);
		}
		return tripId;
	}

	/** 연결 지점: TRIP · 지역 · OWNER · 공유 링크까지 만들고 DAY는 만들지 않는다 (구성정의서 8장 나.) */
	@Transactional
	@Override
	public Long create(TripVO trip, List<String> regions, Long ownerUserId) {
		log.info("create(trip)......" + trip.getTitle() + ", owner=" + ownerUserId);
		Map<String, String> errors = new LinkedHashMap<>();
		String title = trip.getTitle() == null ? "" : trip.getTitle().trim();
		if (title.isEmpty()) {
			errors.put("title", "여행 제목을 입력하세요.");
		} else if (title.length() > TITLE_DB_LENGTH) {
			errors.put("title", "여행 제목은 " + TITLE_DB_LENGTH + "자 이하로 입력하세요.");
		}
		if (trip.getStartDate() == null) {
			errors.put("period", "시작일과 종료일을 모두 입력하세요.");
		}
		TripFormVO regionForm = new TripFormVO();
		regionForm.setRegions(regions);
		List<String> cleanRegions = checkRegions(regionForm, errors);
		if (ownerUserId == null) {
			throw new IllegalArgumentException("여행을 만드는 회원(ownerUserId)이 없습니다.");
		}
		if (!errors.isEmpty()) {
			throw new TripValidationException(errors);
		}

		trip.setTitle(title);
		trip.setShareToken(UUID.randomUUID().toString()); // 공유 링크: 만들 때 자동 생성, 재발급 없음
		mapper.insert(trip);                              // 공개 범위는 항상 비공개, tripId가 채워진다
		mapper.insertRegions(trip.getTripId(), cleanRegions);
		mapper.insertOwner(trip.getTripId(), ownerUserId);
		return trip.getTripId();
	}

	@Override
	public TripVO find(Long tripId) {
		TripVO trip = tripId == null ? null : mapper.read(tripId);
		return trip == null ? null : complete(trip, null);
	}

	@Override
	public boolean canView(Long tripId, Long userId) {
		TripVO trip = tripId == null ? null : mapper.read(tripId); // 삭제한 여행은 읽히지 않는다
		if (trip == null) {
			return false;
		}
		// TODO(양지호): MemberService.getRole이 develop에 들어오면 mapper.getRole 대신 그것을 쓴다.
		return trip.isPublicScope() || (userId != null && mapper.getRole(tripId, userId) != null);
	}

	@Override
	public TripVO findByToken(String token) {
		if (isBlank(token)) {
			return null;
		}
		TripVO trip = mapper.readByShareToken(token.trim());
		return trip == null ? null : complete(trip, null);
	}

	@Transactional
	@Override
	public int deleteByOwner(Long userId) {
		log.info("deleteByOwner......" + userId);
		return userId == null ? 0 : mapper.deleteByOwner(userId);
	}

	// ===================== UC-005 여행 열람 =====================

	@Override
	public TripVO getTrip(Long tripId, Long userId) {
		TripVO trip = mapper.read(tripId);
		if (trip == null) {
			throw TripAccessException.notFound();
		}
		String role = userId == null ? null : mapper.getRole(tripId, userId);

		// 2A. 비공개 여행인데 멤버가 아님 (공유 링크로 들어온 경우는 getTripByShareToken)
		if (role == null && !trip.isPublicScope()) {
			throw TripAccessException.privateTrip();
		}
		return complete(trip, role);
	}

	@Override
	public TripVO getTripByShareToken(String shareToken, Long userId) {
		TripVO trip = mapper.readByShareToken(shareToken);
		if (trip == null) {
			throw TripAccessException.notFound();
		}
		// 7A. 멤버가 공유 링크로 들어오면 본인 권한을 적용한다
		String role = userId == null ? null : mapper.getRole(trip.getTripId(), userId);
		return complete(trip, role);
	}

	private TripVO complete(TripVO trip, String role) {
		trip.setMyRole(role);
		trip.setRegions(mapper.getRegions(trip.getTripId()));
		return trip;
	}

	// ===================== UC-009 여행 정보 수정 =====================

	@Transactional
	@Override
	public String modifyInfo(Long userId, Long tripId, TripFormVO form) {
		log.info("modifyInfo......" + tripId + ", " + form);
		requireOwner(tripId, userId);

		// 4A. 제목을 비우거나 지역을 모두 해제한 경우
		Map<String, String> errors = new LinkedHashMap<>();
		String title = checkTitle(form, errors);
		List<String> regions = checkRegions(form, errors);
		if (!errors.isEmpty()) {
			throw new TripValidationException(errors);
		}

		mapper.updateTitle(tripId, title);
		mapper.deleteRegions(tripId);
		mapper.insertRegions(tripId, regions);
		return "여행 정보를 저장했습니다.";
	}

	@Transactional
	@Override
	public String changePeriod(Long userId, Long tripId, TripFormVO form, boolean confirmDelete) {
		log.info("changePeriod......" + tripId + ", " + form + ", confirm=" + confirmDelete);
		TripVO trip = requireOwner(tripId, userId);

		// 1A3. UC-008의 5B · 5C · 5E와 같은 규칙으로 검사
		Map<String, String> errors = new LinkedHashMap<>();
		LocalDate[] period = checkPeriod(form, errors);
		if (!errors.isEmpty()) {
			throw new TripValidationException(errors);
		}
		int oldCount = trip.getDayCount();
		int newCount = days(period[0], period[1]);

		if (newCount < oldCount) {
			// 1C. 전체 공개 여행에서 남는 일정이 하나도 없게 되는 경우
			if (trip.isPublicScope() && mapper.countSchedulesUpTo(tripId, newCount) == 0) {
				errors.put("period", "전체 공개 여행에는 일정이 1개 이상 있어야 합니다. 비공개로 바꾼 뒤 기간을 줄이세요.");
				throw new TripValidationException(errors);
			}
			// 1B. 지워질 DAY에 일정이 있으면 확인을 받은 뒤에만 반영
			int lost = mapper.countSchedulesAfter(tripId, newCount);
			if (lost > 0 && !confirmDelete) {
				throw new TripPeriodConfirmException("기간이 줄어 " + dayRange(newCount + 1, oldCount)
						+ "과 일정 " + lost + "개가 함께 삭제됩니다.");
			}
		}

		// 시작일을 바꾸면 모든 DAY 날짜가 함께 이동한다
		mapper.updateStartDate(tripId, java.sql.Date.valueOf(period[0]));

		// TODO(양지호): DayService.syncDays(tripId, start, end)가 develop에 들어오면 아래 DAY 처리를 그 호출로 바꾼다.
		if (newCount < oldCount) {
			mapper.deleteDaysAfter(tripId, newCount); // 1A5. 뒤쪽 DAY 삭제 (일정도 함께 삭제)
			return dayRange(newCount + 1, oldCount) + "이 삭제되었습니다.";
		}
		if (newCount > oldCount) {
			for (int dayNo = oldCount + 1; dayNo <= newCount; dayNo++) {
				mapper.insertDay(tripId, dayNo); // 1A4. 마지막 DAY 뒤에 빈 DAY 추가
			}
			return dayRange(oldCount + 1, newCount) + "이 추가되었습니다.";
		}
		return "여행 정보를 저장했습니다."; // 일수는 같고 날짜만 이동
	}

	// ===================== UC-012 연결: 여행 시작일 변경 (양지호 요청) =====================

	@Override
	public void changeStartDate(Long tripId, Date startDate) {
		log.info("changeStartDate......" + tripId + " → " + startDate);
		mapper.updateStartDate(tripId, startDate); // 이미 있는 SQL 그대로 (권한 검사 · DAY 처리는 호출하는 쪽에서)
	}

	// ===================== UC-015 공개 범위 설정 =====================

	@Transactional
	@Override
	public String changeScope(Long userId, Long tripId, String scope) {
		log.info("changeScope......" + tripId + " → " + scope);
		requireOwner(tripId, userId);

		if (!"PUBLIC".equals(scope) && !"PRIVATE".equals(scope)) {
			throw validation("scope", "공개 범위는 비공개 또는 전체 공개 중에서 고르세요.");
		}
		// 2. 전체 공개 조건: 일정 1개 이상, 모든 일정에 시간과 장소 (2B. 비공개는 조건 없이 바로 바꾼다)
		if ("PUBLIC".equals(scope)
				&& (mapper.countSchedules(tripId) == 0 || mapper.countIncompleteSchedules(tripId) > 0)) {
			throw validation("scope", "전체 공개하려면 일정이 1개 이상 있고 모든 일정에 시간과 장소가 있어야 합니다.");
		}
		mapper.updateScope(tripId, scope);
		return "공개 범위를 " + ("PUBLIC".equals(scope) ? "전체 공개" : "비공개") + "로 바꿨습니다.";
	}

	// ===================== UC-010 여행 삭제 =====================

	@Transactional
	@Override
	public void remove(Long userId, Long tripId) {
		log.info("remove......" + tripId);
		requireOwner(tripId, userId);
		mapper.delete(tripId);
	}

	// ===================== 검사 =====================

	/** OWNER인지 확인하고 여행을 돌려준다 */
	private TripVO requireOwner(Long tripId, Long userId) {
		TripVO trip = mapper.read(tripId);
		if (trip == null) {
			throw TripAccessException.notFound();
		}
		if (userId == null || !"OWNER".equals(mapper.getRole(tripId, userId))) {
			throw TripAccessException.notOwner();
		}
		trip.setMyRole("OWNER");
		return trip;
	}

	/** 5A. 여행 제목 */
	private String checkTitle(TripFormVO form, Map<String, String> errors) {
		String title = form.getTitle() == null ? "" : form.getTitle().trim();
		if (title.isEmpty()) {
			errors.put("title", "여행 제목을 입력하세요.");
		} else if (title.length() > TITLE_MAX_LENGTH) {
			errors.put("title", "여행 제목은 " + TITLE_MAX_LENGTH + "자 이하로 입력하세요.");
		}
		return title;
	}

	/** 5B · 5C · 5E. 시작일 · 종료일 → [시작일, 종료일], 오류면 null */
	private LocalDate[] checkPeriod(TripFormVO form, Map<String, String> errors) {
		if (isBlank(form.getStartDate()) || isBlank(form.getEndDate())) {
			errors.put("period", "시작일과 종료일을 모두 입력하세요.");
			return null;
		}
		LocalDate start;
		LocalDate end;
		try {
			start = LocalDate.parse(form.getStartDate().trim());
			end = LocalDate.parse(form.getEndDate().trim());
		} catch (DateTimeParseException e) {
			errors.put("period", "시작일과 종료일을 모두 입력하세요.");
			return null;
		}
		if (end.isBefore(start)) {
			errors.put("period", "종료일은 시작일과 같거나 이후여야 합니다.");
			return null;
		}
		if (days(start, end) > MAX_DAYS) {
			errors.put("period", "여행 기간은 30일 이하로 정해 주세요.");
			return null;
		}
		return new LocalDate[] { start, end };
	}

	/** 5D. 여행 지역: 목록에 있는 지역만, 중복 없이 목록 순서로 */
	private List<String> checkRegions(TripFormVO form, Map<String, String> errors) {
		List<String> picked = form.getRegions() == null ? new ArrayList<String>() : form.getRegions();
		List<String> regions = new ArrayList<>();
		for (String region : REGIONS) {
			if (picked.contains(region)) {
				regions.add(region);
			}
		}
		if (regions.size() != new java.util.HashSet<>(picked).size()) {
			errors.put("regions", "목록에 있는 지역만 고를 수 있습니다.");
		} else if (regions.isEmpty()) {
			errors.put("regions", "여행 지역을 1개 이상 고르세요.");
		}
		return regions;
	}

	private static int days(LocalDate start, LocalDate end) {
		return (int) ChronoUnit.DAYS.between(start, end) + 1;
	}

	/** "DAY 3" 또는 "DAY 3~5" */
	private static String dayRange(int from, int to) {
		return from == to ? "DAY " + from : "DAY " + from + "~" + to;
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static TripValidationException validation(String field, String message) {
		Map<String, String> errors = new LinkedHashMap<>();
		errors.put(field, message);
		return new TripValidationException(errors);
	}
}
