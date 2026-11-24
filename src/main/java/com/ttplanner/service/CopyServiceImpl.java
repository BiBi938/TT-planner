package com.ttplanner.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ttplanner.mapper.CopyMapper;
import com.ttplanner.vo.CopyDayVO;
import com.ttplanner.vo.TripVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * 여행 복사 서비스 구현 (담당: 송유림, UC-020)
 *
 * 다른 팀원 메서드 사용
 *   TripService.getTrip / getTripByShareToken (박준성) : 볼 수 있는 여행인지 + 내 권한(myRole)
 *   TripService.find (박준성)                           : 넣을 여행 · 원본 여행 정보 (제목, 지역, 공개 범위)
 *   TripService.create(trip, regions, owner) (박준성)   : 복사한 DAY로 새 여행 만들기 (DAY는 만들지 않음)
 *   MemberService.getRole (양지호)                      : 넣을 여행에서 내 권한
 *   DayService.findDays (양지호)                        : 넣을 여행의 DAY 개수
 */
@Log4j
@Service
public class CopyServiceImpl implements CopyService {

	/** 새 여행 제목 뒤에 붙이는 말. 붙여도 50자를 넘지 않게 원본 제목을 자른다 (createTrip) */
	private static final String COPY_SUFFIX = " (복사)";

	@Setter(onMethod_ = @Autowired)
	private CopyMapper mapper;

	@Setter(onMethod_ = @Autowired)
	private TripService tripService;

	@Setter(onMethod_ = @Autowired)
	private DayService dayService;

	@Setter(onMethod_ = @Autowired)
	private MemberService memberService;

	// ===================== ① DAY 선택 화면 =====================

	@Override
	public TripVO getCopyableTrip(Long tripId, Long userId, String token) {
		if (token == null || token.trim().isEmpty()) {
			return tripService.getTrip(tripId, userId); // 볼 수 없으면 TripAccessException
		}
		// 공유 링크로 연 여행: 비공개여도 복사할 수 있다. 다른 여행의 토큰이면 막는다
		TripVO trip = tripService.getTripByShareToken(token.trim(), userId);
		if (!trip.getTripId().equals(tripId)) {
			throw TripAccessException.notFound();
		}
		return trip;
	}

	@Override
	public List<CopyDayVO> getCopyDays(Long tripId) {
		return mapper.getCopyDays(tripId);
	}

	// ===================== ② [복사] =====================

	@Override
	public List<Long> selectDays(Long tripId, Long userId, String token, List<Long> dayIds) {
		getCopyableTrip(tripId, userId, token); // 볼 수 있는 여행인지 다시 확인

		if (dayIds == null || dayIds.isEmpty()) {
			throw new IllegalStateException("복사할 DAY를 1개 이상 고르세요.");
		}

		// 이 여행의 DAY 중 고른 것만 DAY 번호 순으로 모은다 (다른 여행 DAY_ID가 들어와도 빠짐)
		Set<Long> picked = new LinkedHashSet<>(dayIds);
		List<Long> result = new ArrayList<>();
		for (CopyDayVO day : mapper.getCopyDays(tripId)) {
			if (picked.contains(day.getDayId())) {
				result.add(day.getDayId());
			}
		}
		if (result.size() != picked.size()) {
			throw new IllegalStateException("고른 DAY를 찾을 수 없습니다. 다시 골라 주세요.");
		}
		log.info("selectDays: tripId=" + tripId + ", copyDays=" + result);
		return result;
	}

	// ===================== ③ [여기에 DAY 넣기] =====================

	@Transactional
	@Override
	public String insert(Long userId, Long targetTripId, int position, List<Long> copyDays) {
		// 넣을 여행: 삭제되지 않았고(find), 내 권한이 OWNER · EDITOR (MemberService.getRole, 양지호)
		TripVO target = tripService.find(targetTripId);
		if (target == null) {
			throw new IllegalStateException("여행을 찾을 수 없습니다.");
		}
		String role = memberService.getRole(targetTripId, userId);
		if (!"OWNER".equals(role) && !"EDITOR".equals(role)) {
			throw new IllegalStateException("복사한 DAY는 내가 OWNER · EDITOR인 여행에만 넣을 수 있습니다.");
		}

		int scheduleCount = insertDays(targetTripId, position, copyDays);

		String msg = "복사한 DAY " + copyDays.size() + "개를 넣었습니다. (일정 " + scheduleCount + "개)";
		if (target.isPublicScope()) {
			msg += " 전체 공개 여행이라 시간 · 장소가 모두 있는 일정만 넣었습니다.";
		}
		return msg;
	}

	// ===================== [+ 복사한 DAY로 새 여행 만들기] =====================

	@Transactional
	@Override
	public Long createTrip(Long userId, List<Long> copyDays) {
		List<CopyDayVO> sources = checkSourceDays(copyDays);
		Long sourceTripId = sources.get(0).getTripId();

		// 입력 화면 없이 만든다: 제목 '원본 제목 (복사)', 시작일 오늘, 지역은 원본과 같게 (구성정의서 8장 나.)
		// 원본 제목 · 지역은 TripService.find(박준성)로 가져온다
		TripVO source = tripService.find(sourceTripId);
		if (source == null) {
			throw new IllegalStateException("복사한 DAY의 여행이 삭제되었습니다. [복사 취소] 후 다시 복사해 주세요.");
		}
		String title = source.getTitle();
		int maxLength = TripService.TITLE_MAX_LENGTH - COPY_SUFFIX.length();
		if (title.length() > maxLength) {
			title = title.substring(0, maxLength);
		}

		TripVO trip = new TripVO();
		trip.setTitle(title + COPY_SUFFIX);
		trip.setStartDate(java.sql.Date.valueOf(LocalDate.now()));

		// 비공개 여행 + 지역 + OWNER 등록 + 공유 링크까지 만든다 (DAY는 만들지 않음 → DAY 0개)
		Long newTripId = tripService.create(trip, source.getRegions(), userId);

		// 빈 여행의 맨 앞(position 0)에 복사한 DAY와 일정을 넣는다 → DAY 1, 2, … (새 여행은 비공개라 일정 전부)
		insertDays(newTripId, 0, copyDays);
		log.info("createTrip: newTripId=" + newTripId + ", copyDays=" + copyDays);
		return newTripId;
	}

	// ===================== insertDays (구성정의서 8장 나.) =====================

	/**
	 * 고른 DAY와 일정을 복사해 targetTripId 여행의 position(이 DAY 뒤, 0 = 맨 앞 · 빈 여행)에 넣고 뒤 DAY를 민다.
	 * 30일 초과는 이 메서드 안에서 막는다.
	 * @return 복사한 일정 수
	 *
	 * TODO(송유림): feature/yurim-insertdays에서 DayService.insertDays(Long targetTripId, int position, List<Long> sourceDayIds)를
	 *               만들고, 호출하는 곳을 dayService.insertDays(...)로 바꾸고,
	 *               이 메서드와 CopyMapper.xml의 insertDays용 SQL 3개를 지운다.
	 *               (이 메서드 내용과 SQL 3개를 그대로 옮겨 가면 된다)
	 */
	private int insertDays(Long targetTripId, int position, List<Long> sourceDayIds) {
		List<CopyDayVO> sources = checkSourceDays(sourceDayIds);
		int dayCount = dayService.findDays(targetTripId).size();

		if (position < 0 || position > dayCount) {
			throw new IllegalStateException("DAY를 넣을 위치가 잘못되었습니다.");
		}
		if (dayCount + sources.size() > TripService.MAX_DAYS) {
			throw new IllegalStateException("여행 기간은 최대 30일입니다. 지금 DAY " + dayCount + "개에 "
					+ sources.size() + "개를 더 넣을 수 없습니다.");
		}

		// ① position 뒤의 DAY를 넣을 개수만큼 민다
		mapper.shiftDaysFrom(targetTripId, position, sources.size());

		// ② 빈자리(position + 1부터)에 새 DAY를 만들고 ③ 원본 DAY의 일정을 복사한다
		int scheduleCount = 0;
		for (int i = 0; i < sources.size(); i++) {
			CopyDayVO newDay = new CopyDayVO();
			newDay.setTripId(targetTripId);
			newDay.setDayNo(position + 1 + i);
			mapper.insertCopyDay(newDay); // newDay.dayId가 채워진다

			scheduleCount += mapper.copySchedules(newDay.getDayId(), sources.get(i).getDayId(), targetTripId);
		}
		log.info("insertDays: target=" + targetTripId + ", position=" + position
				+ ", sources=" + sourceDayIds + ", schedules=" + scheduleCount);
		return scheduleCount;
	}

	/** 복사한 DAY가 아직 남아 있는지 확인 (그 사이 원본 여행이 삭제되었을 수 있다) */
	private List<CopyDayVO> checkSourceDays(List<Long> dayIds) {
		if (dayIds == null || dayIds.isEmpty()) {
			throw new IllegalStateException("복사한 DAY가 없습니다. 여행에서 [내 여행으로 복사]를 먼저 누르세요.");
		}
		List<CopyDayVO> sources = mapper.getSourceDays(dayIds);
		if (sources.size() != new LinkedHashSet<>(dayIds).size()) {
			throw new IllegalStateException("복사한 DAY 중 삭제된 DAY가 있습니다. [복사 취소] 후 다시 복사해 주세요.");
		}
		return sources;
	}
}
