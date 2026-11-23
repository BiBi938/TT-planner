package com.ttplanner.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ttplanner.mapper.MemberMapper;
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
}
