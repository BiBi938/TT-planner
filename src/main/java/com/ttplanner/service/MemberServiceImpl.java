package com.ttplanner.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ttplanner.mapper.MemberMapper;
import com.ttplanner.vo.InviteVO;
import com.ttplanner.vo.MemberVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

@Log4j
@Service
public class MemberServiceImpl implements MemberService {

	@Setter(onMethod_ = @Autowired)
	private MemberMapper mapper;

	@Override
	public String getRole(Long tripId, Long userId) {
		// 로그인하지 않았으면 DB에 물어볼 필요 없이 "멤버 아님"
		if (userId == null) {
			return null;
		}
		String role = mapper.getRole(tripId, userId);
		log.info("getRole...... tripId=" + tripId + ", userId=" + userId + " → " + role);
		return role;
	}

	@Override
	public List<MemberVO> searchUsers(Long tripId, String keyword, Long userId) {
		log.info("searchUsers...... tripId=" + tripId + ", keyword=" + keyword + ", userId=" + userId);

		// ① 권한 검사: 이 여행의 OWNER만 (로그인 안 했으면 getRole이 null → 여기서 막힘)
		//    Ajax 주소는 누구나 직접 부를 수 있으므로 화면에서 버튼을 숨기는 것만으로는 부족하다
		String role = getRole(tripId, userId);
		if (!"OWNER".equals(role)) {
			throw new IllegalStateException("멤버를 초대할 권한이 없습니다.");
		}

		// ② 검색어 검사: 비었거나 공백만 있으면 막음 ('%%'가 되면 모든 회원이 나오므로)
		//    앞뒤 공백은 잘라서 검색한다 ("  이 " → "이")
		if (keyword == null || keyword.trim().length() == 0) {
			throw new IllegalStateException("닉네임을 입력하세요.");
		}

		// ③ 검색 (나 자신은 빼고). 0명이면 빈 목록 그대로 → 화면에서 "일치하는 회원이 없습니다."
		return mapper.searchUsers(keyword.trim(), userId);
	}

	@Override
	public String invite(Long tripId, Long targetUserId, String role, Long userId) {
		log.info("invite...... tripId=" + tripId + ", target=" + targetUserId + ", role=" + role + ", userId=" + userId);

		// ① 초대하는 사람이 이 여행의 OWNER인지 (검색과 같은 검사)
		if (!"OWNER".equals(getRole(tripId, userId))) {
			throw new IllegalStateException("멤버를 초대할 권한이 없습니다.");
		}

		// ② 7A. 회원을 고르지 않음
		if (targetUserId == null) {
			throw new IllegalStateException("검색 결과에서 초대할 회원을 고르세요.");
		}

		// ③ 권한은 EDITOR 또는 VIEWER만 (주소를 조작해 OWNER 등을 보내는 경우 대비)
		if (!"EDITOR".equals(role) && !"VIEWER".equals(role)) {
			throw new IllegalStateException("초대할 권한(EDITOR 또는 VIEWER)을 고르세요.");
		}

		// ④ 있는 회원인지 (없거나 탈퇴했으면 null). 닉네임은 결과 문구에도 쓴다
		String nickname = mapper.getNickname(targetUserId);
		if (nickname == null) {
			throw new IllegalStateException("회원을 찾을 수 없습니다.");
		}

		// ⑤ 이 여행과 이미 관계가 있는지 (줄이 있으면 INSERT 때 PK 오류가 나므로 먼저 확인)
		//    나 자신(OWNER, JOINED)을 보내도 여기서 7B로 막힌다
		String status = mapper.getStatus(tripId, targetUserId);
		if ("JOINED".equals(status)) {
			throw new IllegalStateException("이미 이 여행의 멤버입니다.");                          // 7B
		}
		if ("INVITED".equals(status)) {
			throw new IllegalStateException("이미 초대한 회원입니다. 응답을 기다리고 있습니다."); // 7C
		}
		if ("REQUESTED".equals(status)) {
			throw new IllegalStateException("참여 요청을 보낸 회원입니다. 요청을 수락해 주세요.");
		}

		// ⑥ 초대 보내기 (STATUS = 'INVITED')
		mapper.insertInvite(tripId, targetUserId, role);

		return nickname + " 님에게 " + role + "로 초대를 보냈습니다. 수락하면 멤버가 됩니다.";
	}

	@Override
	public List<MemberVO> findByTrip(Long tripId) {
		log.info("findByTrip......" + tripId);
		return mapper.findByTrip(tripId);
	}

	// UC-018 권한 변경 (EDITOR ↔ VIEWER)
	// 메뉴 [권한 변경]과 드래그(3A) 모두 이 메서드를 부른다 (MemberController Ajax)
	// UPDATE 한 줄뿐이라 @Transactional은 붙이지 않는다
	@Override
	public String changeRole(Long tripId, Long targetUserId, String role, Long userId) {
		log.info("changeRole......trip " + tripId + ", target " + targetUserId + " → " + role + ", by " + userId);

		// ① 바꾸는 사람이 이 여행의 OWNER인가 (로그인 안 함 → getRole이 null → 여기서 막힘)
		if (!"OWNER".equals(getRole(tripId, userId))) {
			throw new IllegalStateException("멤버 권한을 바꿀 권한이 없습니다.");
		}

		// ② 새 권한은 EDITOR 또는 VIEWER만 (OWNER로 바꾸기는 OWNER 넘기기에서)
		if (!"EDITOR".equals(role) && !"VIEWER".equals(role)) {
			throw new IllegalStateException("바꿀 권한(EDITOR 또는 VIEWER)을 고르세요.");
		}

		// ③ 대상이 참여 중인 멤버인가 (없는 회원 · 초대 중 · 참여 요청 중은 막음)
		MemberVO target = (targetUserId == null) ? null : mapper.getMember(tripId, targetUserId);
		if (target == null || !"JOINED".equals(target.getStatus())) {
			throw new IllegalStateException("멤버를 찾을 수 없습니다.");
		}

		// ④ 대상이 OWNER면 막음 (OWNER인 나 자신도 여기서 막힌다)
		if ("OWNER".equals(target.getRole())) {
			throw new IllegalStateException("OWNER의 권한은 바꿀 수 없습니다. OWNER 넘기기를 이용하세요.");
		}

		// ⑤ 이미 같은 권한이면 DB를 바꾸지 않고 안내만
		if (role.equals(target.getRole())) {
			return target.getNickname() + " 님은 이미 " + role + "입니다.";
		}

		// ⑥ 바꾸기 (그사이 누가 내보냈으면 0줄)
		int updated = mapper.updateRole(tripId, targetUserId, role);
		if (updated == 0) {
			throw new IllegalStateException("멤버를 찾을 수 없습니다.");
		}
		return target.getNickname() + " 님 권한을 " + role + "로 바꿨습니다.";
	}

	// UC-018 내보내기 (5A)
	// 지우는 건 TRIP_MEMBER 한 줄뿐 → 그 사람이 넣은 일정은 여행에 남는다 (SCHEDULE에 작성자 칸 없음)
	// DELETE 한 줄뿐이라 @Transactional은 붙이지 않는다
	@Override
	public String kick(Long tripId, Long targetUserId, Long userId) {
		log.info("kick......trip " + tripId + ", target " + targetUserId + ", by " + userId);

		// ① 내보내는 사람이 이 여행의 OWNER인가
		if (!"OWNER".equals(getRole(tripId, userId))) {
			throw new IllegalStateException("멤버를 내보낼 권한이 없습니다.");
		}

		// ② 대상이 참여 중인 멤버인가 (초대 중 · 참여 요청 중은 내보내기 대상 아님)
		MemberVO target = (targetUserId == null) ? null : mapper.getMember(tripId, targetUserId);
		if (target == null || !"JOINED".equals(target.getStatus())) {
			throw new IllegalStateException("멤버를 찾을 수 없습니다.");
		}

		// ③ OWNER는 내보낼 수 없음 (OWNER인 나 자신도 여기서 막힌다)
		if ("OWNER".equals(target.getRole())) {
			throw new IllegalStateException("OWNER는 내보낼 수 없습니다.");
		}

		// ④ 지우기 (그사이 다른 곳에서 지워졌으면 0줄)
		int deleted = mapper.deleteMember(tripId, targetUserId);
		if (deleted == 0) {
			throw new IllegalStateException("멤버를 찾을 수 없습니다.");
		}
		return target.getNickname() + " 님을 내보냈습니다.";
	}

	// UC-018 OWNER 넘기기 (5B)
	// UPDATE 두 번을 한 묶음으로: ④ 나를 EDITOR로 내리기 → ⑤ 대상을 OWNER로 올리기
	//  - 순서: 올리기를 먼저 하면 OWNER가 잠깐 2명 → uk_trip_member_owner(여행당 OWNER 1명) 오류 ORA-00001
	//  - @Transactional: ⑤가 실패하면 ④도 취소 → OWNER가 0명인 여행이 생기지 않는다 (팀 규칙: OWNER 넘기기)
	@Transactional
	@Override
	public String transferOwner(Long tripId, Long targetUserId, Long userId) {
		log.info("transferOwner......trip " + tripId + ", " + userId + " → " + targetUserId);

		// ① 넘기는 사람이 이 여행의 OWNER인가
		if (!"OWNER".equals(getRole(tripId, userId))) {
			throw new IllegalStateException("OWNER를 넘길 권한이 없습니다.");
		}

		// ② 대상이 참여 중인 멤버인가 (EDITOR · VIEWER 모두 가능, 초대 중 · 참여 요청 중은 안 됨)
		MemberVO target = (targetUserId == null) ? null : mapper.getMember(tripId, targetUserId);
		if (target == null || !"JOINED".equals(target.getStatus())) {
			throw new IllegalStateException("멤버를 찾을 수 없습니다.");
		}

		// ③ 대상이 OWNER = 나 자신 (①에서 내가 OWNER임을 확인했고, OWNER는 여행당 1명)
		if ("OWNER".equals(target.getRole())) {
			throw new IllegalStateException("자기 자신에게는 OWNER를 넘길 수 없습니다.");
		}

		// 결과 문구에 쓸 내 닉네임
		MemberVO me = mapper.getMember(tripId, userId);

		// ④ 나를 먼저 EDITOR로 내리기
		if (mapper.updateRole(tripId, userId, "EDITOR") == 0) {
			throw new IllegalStateException("OWNER를 넘기지 못했습니다. 다시 시도해 주세요.");
		}

		// ⑤ 대상을 OWNER로 올리기 (그사이 대상이 나갔으면 0줄 → 예외 → ④도 롤백)
		if (mapper.updateRole(tripId, targetUserId, "OWNER") == 0) {
			throw new IllegalStateException("멤버를 찾을 수 없습니다.");
		}

		return target.getNickname() + " 님이 새 OWNER가 되었습니다. " + me.getNickname() + " 님은 EDITOR입니다.";
	}

	// UC-019 여행 나가기
	// UC-018 내보내기(kick)와 같은 SQL(deleteMember). 다른 점: 지우는 사람이 OWNER가 아니라 "나 자신"
	// 나간 여행은 내 여행 목록에서 저절로 사라진다 (박준성 getMyTrips가 JOINED 줄만 봄)
	@Override
	public String leave(Long tripId, Long userId) {
		log.info("leave......trip " + tripId + ", user " + userId);

		// ① 이 여행의 참여 중인 멤버인가 (로그인 안 함 · 초대 중 · 참여 요청 중 · 줄 없음은 막음)
		MemberVO me = (userId == null) ? null : mapper.getMember(tripId, userId);
		if (me == null || !"JOINED".equals(me.getStatus())) {
			throw new IllegalStateException("이 여행의 멤버가 아닙니다.");
		}

		// ② OWNER는 나갈 수 없음 (2A: 화면에서도 버튼을 안 보여 주지만 주소로 직접 보내는 경우 대비)
		if ("OWNER".equals(me.getRole())) {
			throw new IllegalStateException("OWNER는 여행에서 나갈 수 없습니다. 다른 멤버에게 OWNER를 넘긴 뒤 나가세요.");
		}

		// ③ 내 줄 지우기 (그사이 OWNER가 내보냈으면 0줄)
		if (mapper.deleteMember(tripId, userId) == 0) {
			throw new IllegalStateException("이 여행의 멤버가 아닙니다.");
		}
		return "여행에서 나갔습니다.";
	}

	// UC-025 받은 초대 목록
	@Override
	public List<InviteVO> findInvites(Long userId) {
		log.info("findInvites......" + userId);
		if (userId == null) {
			return new ArrayList<InviteVO>(); // 로그인 안 함 → 빈 목록
		}
		return mapper.findInvites(userId);
	}

	// UC-025 초대 수락
	// UPDATE 한 줄뿐이라 @Transactional은 붙이지 않는다
	@Override
	public String acceptInvite(Long tripId, Long userId) {
		log.info("acceptInvite......trip " + tripId + ", user " + userId);

		// ① 아직 응답할 초대가 있는가 (+ 문구에 쓸 여행 제목 · 권한)
		InviteVO invite = (userId == null) ? null : mapper.getInvite(tripId, userId);
		if (invite == null) {
			throw new IllegalStateException("초대를 찾을 수 없습니다.");
		}

		// ② INVITED → JOINED (그사이 OWNER가 초대를 지웠으면 0줄)
		if (mapper.acceptInvite(tripId, userId) == 0) {
			throw new IllegalStateException("초대를 찾을 수 없습니다.");
		}
		return "'" + invite.getTitle() + "' 여행의 " + invite.getRole() + "가 되었습니다.";
	}

	// UC-025 초대 거절 (3A): UC-018 deleteMember로 초대 줄을 지운다
	@Override
	public String rejectInvite(Long tripId, Long userId) {
		log.info("rejectInvite......trip " + tripId + ", user " + userId);

		// ① 아직 응답할 초대가 있는가 (멤버 · 참여 요청 줄을 지우지 않도록 꼭 먼저 확인)
		InviteVO invite = (userId == null) ? null : mapper.getInvite(tripId, userId);
		if (invite == null) {
			throw new IllegalStateException("초대를 찾을 수 없습니다.");
		}

		// ② 초대 줄 지우기
		if (mapper.deleteMember(tripId, userId) == 0) {
			throw new IllegalStateException("초대를 찾을 수 없습니다.");
		}
		return "초대를 거절했습니다.";
	}

	// UC-026 이 여행과의 관계 (UC-017 getStatus SQL 재사용)
	@Override
	public String getStatus(Long tripId, Long userId) {
		if (userId == null) {
			return null; // 로그인 안 함
		}
		return mapper.getStatus(tripId, userId);
	}

	// UC-026 참여 요청
	// 화면(_join.jsp)도 상태에 따라 버튼을 숨기지만, 저장 직전에 다시 확인한다 (6A: 다른 창에서 이미 요청 · 그사이 초대받음)
	// INSERT 한 줄뿐이라 @Transactional은 붙이지 않는다
	@Override
	public String requestJoin(Long tripId, Long userId, String token) {
		log.info("requestJoin......trip " + tripId + ", user " + userId + ", token " + token);

		// ① 로그인 (LoginInterceptor가 먼저 막지만 안전장치)
		if (userId == null) {
			throw new IllegalStateException("로그인이 필요합니다.");
		}

		// ② 볼 수 있는 여행인가: 삭제 안 됨 + (전체 공개 또는 공유 링크 토큰) → 아니면 6B 문구
		String trimmedToken = null;
		if (token != null && !token.trim().isEmpty()) {
			trimmedToken = token.trim();
		}
		if (mapper.countRequestable(tripId, trimmedToken) == 0) {
			throw new IllegalStateException("여행을 찾을 수 없습니다.");
		}

		// ③ 이 여행과 이미 관계가 있는가 (UC-017 getStatus 재사용)
		String status = mapper.getStatus(tripId, userId);
		if ("JOINED".equals(status)) {
			throw new IllegalStateException("이미 이 여행의 멤버입니다.");                       // 2B
		}
		if ("REQUESTED".equals(status)) {
			throw new IllegalStateException("이미 참여를 요청했습니다. OWNER의 수락을 기다리고 있습니다."); // 2C
		}
		if ("INVITED".equals(status)) {
			throw new IllegalStateException("받은 초대가 있습니다. 내 여행 화면에서 수락하세요.");    // 2D
		}

		// ④ 요청 줄 넣기
		mapper.insertRequest(tripId, userId);
		return "참여를 요청했습니다. OWNER가 수락하면 멤버가 됩니다.";
	}

	// 회원 탈퇴 연결: 검사할 규칙이 없어 Mapper를 그대로 부른다
	// 윤아연 withdraw()의 @Transactional 안에서 불리므로, 탈퇴 중 오류가 나면 이 삭제도 함께 되돌려진다
	@Override
	public int deleteByUser(Long userId) {
		log.info("deleteByUser......" + userId);
		if (userId == null) {
			return 0;
		}
		return mapper.deleteByUser(userId);
	}
}
