package com.ttplanner.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ttplanner.mapper.ScheduleMapper;
import com.ttplanner.vo.ScheduleVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * 일정 관리 · 순서 변경 구현 (담당: 송유림, UC-013)
 *
 * SLOT_NO 규칙
 *   - 처리가 끝나면 그 DAY의 SLOT_NO는 항상 1, 2, 3 ... 빈칸 없이 이어진다.
 *   - 시간이 적힌 일정끼리는 항상 시간 순서를 유지한다.
 *     (추가 · 수정은 시간에 맞는 자리에 끼우고, 순서 변경은 시간을 자리에 남기기 때문)
 */
@Log4j
@Service
public class ScheduleServiceImpl implements ScheduleService {

	/** TIME 'HH:MM' (00:00 ~ 23:59). schema.sql의 ck_schedule_time과 같은 규칙 */
	private static final Pattern TIME_PATTERN = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");

	private static final int MAX_PLACE_NAME = 100;  // PLACE_NAME varchar2(100 char)
	private static final int MAX_DESCRIPTION = 500; // DESCRIPTION varchar2(500 char)

	/** 화면설계서 S-05 11 문구 그대로 */
	private static final String MSG_LAST_SCHEDULE =
			"전체 공개 여행에는 일정이 1개 이상 있어야 합니다. 비공개로 바꾼 뒤 삭제하세요.";

	@Setter(onMethod_ = @Autowired)
	private ScheduleMapper mapper;

	@Override
	public List<ScheduleVO> findByTrip(Long tripId) {
		log.info("findByTrip......" + tripId);
		return mapper.findByTrip(tripId);
	}

	@Override
	public Long findTripIdByDay(Long dayId) {
		return mapper.findTripIdByDay(dayId);
	}

	// 여러 행(새 일정 insert + 다른 일정 SLOT_NO update)을 함께 바꾸므로 @Transactional
	@Transactional
	@Override
	public void add(Long tripId, ScheduleVO schedule) {
		log.info("add schedule......" + tripId + ", " + schedule);

		// 1. 여행 확인 + 입력값 정리 · 검사 (전체 공개면 시간 · 장소 필수)
		String scope = findScope(tripId);
		cleanAndCheck(schedule, "PUBLIC".equals(scope));

		// 2. DAY가 이 여행의 DAY인지 확인 (폼의 dayId를 바꿔 다른 여행에 넣는 것을 막음)
		if (schedule.getDayId() == null || !tripId.equals(mapper.findTripIdByDay(schedule.getDayId()))) {
			throw new IllegalStateException("DAY를 찾을 수 없습니다.");
		}

		// 3. 지금 DAY 일정(SLOT_NO 순)에서 넣을 자리를 계산한다
		List<ScheduleVO> list = mapper.findByDay(schedule.getDayId());
		int index = findInsertIndex(list, schedule.getTime());

		// 4. 새 일정을 그 자리 번호로 넣고, 목록에도 끼운 뒤 전체 번호를 1, 2, 3 ... 으로 맞춘다
		schedule.setSlotNo(index + 1);
		mapper.insert(schedule); // → schedule.getSchId()에 새 번호
		list.add(index, schedule);
		renumber(list);
	}

	@Transactional
	@Override
	public void update(Long tripId, ScheduleVO schedule) {
		log.info("update schedule......" + tripId + ", " + schedule);

		// 1. 이 여행의 일정인지 확인
		ScheduleVO old = mapper.findOne(tripId, schedule.getSchId());
		if (old == null) {
			throw new IllegalStateException("일정을 찾을 수 없습니다.");
		}

		// 2. 입력값 정리 · 검사. DAY는 바꾸지 않는다
		String scope = findScope(tripId);
		cleanAndCheck(schedule, "PUBLIC".equals(scope));
		schedule.setDayId(old.getDayId());

		mapper.update(schedule);

		// 3. 시간이 바뀌었고 새 시간이 있으면 → 빼서 시간에 맞는 자리에 다시 끼운다
		//    장소 · 설명만 바뀌었거나 시간을 지웠으면(비공개) → 자리 그대로
		if (schedule.getTime() != null && !Objects.equals(old.getTime(), schedule.getTime())) {
			List<ScheduleVO> list = mapper.findByDay(old.getDayId());
			ScheduleVO me = removeById(list, schedule.getSchId());
			list.add(findInsertIndex(list, me.getTime()), me);
			renumber(list);
		}
	}

	@Transactional
	@Override
	public void delete(Long tripId, Long schId) {
		log.info("delete schedule......" + tripId + ", SCH_ID " + schId);

		// 1. 이 여행의 일정인지 확인
		ScheduleVO old = mapper.findOne(tripId, schId);
		if (old == null) {
			throw new IllegalStateException("일정을 찾을 수 없습니다.");
		}

		// 2. 마지막 일정 보호: 전체 공개 여행에서 일정이 0개가 되면 막는다
		if ("PUBLIC".equals(findScope(tripId)) && mapper.countByTrip(tripId) <= 1) {
			throw new IllegalStateException(MSG_LAST_SCHEDULE);
		}

		// 3. 삭제 후 남은 일정 번호를 1, 2, 3 ... 으로 당긴다
		mapper.delete(schId);
		renumber(mapper.findByDay(old.getDayId()));
	}

	@Transactional
	@Override
	public List<ScheduleVO> reorder(Long dayId, List<Long> schIds) {
		log.info("reorder......DAY_ID " + dayId + " → " + schIds);

		// 1. 지금 DAY 일정 (SLOT_NO 순)
		List<ScheduleVO> list = mapper.findByDay(dayId);

		// 2. 받은 목록이 이 DAY의 일정과 정확히 같은지 확인 (빠지거나, 겹치거나, 다른 DAY 일정이 섞이면 안 됨)
		if (schIds == null || schIds.size() != list.size() || new HashSet<>(schIds).size() != schIds.size()) {
			throw new IllegalStateException("일정 목록이 바뀌었습니다. 화면을 새로 고친 뒤 다시 시도하세요.");
		}
		Set<Long> current = new HashSet<>();
		for (ScheduleVO s : list) {
			current.add(s.getSchId());
		}
		if (!current.containsAll(schIds)) {
			throw new IllegalStateException("일정 목록이 바뀌었습니다. 화면을 새로 고친 뒤 다시 시도하세요.");
		}

		// 3. 자리마다 원래 시간을 기억한다. 예: [09:00, 12:00, 15:00]
		List<String> times = new ArrayList<>();
		for (ScheduleVO s : list) {
			times.add(s.getTime());
		}

		// 4. 새 순서의 i번째 일정 → SLOT_NO = i + 1, TIME = 그 자리의 원래 시간
		//    예: [C, A, B] → C 1번 09:00, A 2번 12:00, B 3번 15:00 (장소 · 설명만 움직인 셈)
		for (int i = 0; i < schIds.size(); i++) {
			mapper.updateSlotNoAndTime(schIds.get(i), i + 1, times.get(i));
		}

		return mapper.findByDay(dayId);
	}

	// ===================== 도우미 =====================

	// 여행의 공개 범위. 없거나 삭제된 여행이면 막는다
	private String findScope(Long tripId) {
		String scope = mapper.getTripScope(tripId);
		if (scope == null) {
			throw new IllegalStateException("여행을 찾을 수 없습니다.");
		}
		return scope;
	}

	// 입력값 정리(앞뒤 공백 제거, 빈 값 → null)와 검사
	private void cleanAndCheck(ScheduleVO s, boolean isPublic) {
		s.setTime(trimToNull(s.getTime()));
		s.setPlaceName(trimToNull(s.getPlaceName()));
		s.setDescription(trimToNull(s.getDescription()));

		if (s.getTime() != null && !TIME_PATTERN.matcher(s.getTime()).matches()) {
			throw new IllegalStateException("시간을 다시 골라 주세요. (예: 09:30)");
		}
		if (s.getPlaceName() != null && s.getPlaceName().length() > MAX_PLACE_NAME) {
			throw new IllegalStateException("장소 이름은 " + MAX_PLACE_NAME + "자까지 입력할 수 있습니다.");
		}
		if (s.getDescription() != null && s.getDescription().length() > MAX_DESCRIPTION) {
			throw new IllegalStateException("설명은 " + MAX_DESCRIPTION + "자까지 입력할 수 있습니다.");
		}

		// 위치는 장소 이름이 있고, 위도 · 경도가 둘 다 정상 범위일 때만 저장한다. 아니면 이름만 있는 장소
		if (s.getPlaceName() == null || s.getLat() == null || s.getLng() == null
				|| Math.abs(s.getLat()) > 90 || Math.abs(s.getLng()) > 180) {
			s.setLat(null);
			s.setLng(null);
		}

		// 전체 공개 여행은 시간과 장소 필수 (장소는 이름만 있어도 입력된 것으로 본다)
		if (isPublic && (s.getTime() == null || s.getPlaceName() == null)) {
			throw new IllegalStateException("전체 공개 여행은 시간과 장소를 모두 입력해야 저장할 수 있습니다.");
		}
		// 아무것도 입력하지 않은 빈 일정은 만들지 않는다
		if (s.getTime() == null && s.getPlaceName() == null && s.getDescription() == null) {
			throw new IllegalStateException("시간, 장소, 설명 중 하나 이상 입력하세요.");
		}
	}

	private String trimToNull(String text) {
		if (text == null) {
			return null;
		}
		String t = text.trim();
		return t.isEmpty() ? null : t;
	}

	// 넣을 자리(0부터): 시간이 있으면 자기보다 늦은 시간인 첫 일정의 자리, 없으면 맨 끝
	// 'HH:MM' 글자는 compareTo로 비교해도 시간 순서와 같다 ("09:30" < "12:00")
	private int findInsertIndex(List<ScheduleVO> list, String time) {
		if (time != null) {
			for (int i = 0; i < list.size(); i++) {
				String t = list.get(i).getTime();
				if (t != null && t.compareTo(time) > 0) {
					return i;
				}
			}
		}
		return list.size();
	}

	// 목록에서 schId 일정을 빼서 돌려준다
	private ScheduleVO removeById(List<ScheduleVO> list, Long schId) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getSchId().equals(schId)) {
				return list.remove(i);
			}
		}
		throw new IllegalStateException("일정을 찾을 수 없습니다.");
	}

	// 목록 순서대로 SLOT_NO를 1, 2, 3 ... 으로 맞춘다 (이미 맞는 일정은 update하지 않음)
	private void renumber(List<ScheduleVO> list) {
		for (int i = 0; i < list.size(); i++) {
			ScheduleVO s = list.get(i);
			if (s.getSlotNo() != i + 1) {
				mapper.updateSlotNo(s.getSchId(), i + 1);
				s.setSlotNo(i + 1);
			}
		}
	}
}
