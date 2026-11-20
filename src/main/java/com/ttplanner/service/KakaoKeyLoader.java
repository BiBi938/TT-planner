package com.ttplanner.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import lombok.extern.log4j.Log4j;

/**
 * 카카오맵 JavaScript 키 읽기 (담당: 윤아연, UC-006 지도 · UC-014 장소 검색)
 *
 * 구성정의서 v1.5 8장 다.: "카카오맵 API 키는 한 명이 발급해 공유하고 … 키는 저장소에 올리지 않는다."
 * 그래서 키는 src/main/resources/secret.properties(.gitignore에 들어 있음)에 적고, 서버가 켜질 때 한 번 읽어
 * 애플리케이션 영역(application scope)에 "kakaoAppKey"로 넣어 둔다.
 *
 *   secret.properties          kakao.js.key=발급받은_JavaScript_키
 *   JSP에서 꺼내기              ${applicationScope.kakaoAppKey}
 *
 * 파일이 없거나 키가 비어 있으면 빈 문자열을 넣는다. 그러면 지도 · 장소 검색 화면은
 * "지도를 표시할 수 없습니다."를 보여 주고 나머지 기능은 그대로 동작한다 (UC-006 2A, UC-014 2A).
 *
 * root-context.xml의 component-scan(com.ttplanner.service)으로 등록되므로 설정 파일은 고치지 않는다.
 */
@Log4j
@Component
public class KakaoKeyLoader implements ServletContextAware {

	/** JSP에서 ${applicationScope.kakaoAppKey}로 꺼내는 이름 */
	public static final String ATTRIBUTE = "kakaoAppKey";

	private static final String FILE = "secret.properties";
	private static final String PROPERTY = "kakao.js.key";

	/** 카카오 JavaScript 키는 영문 · 숫자로만 되어 있다. 예시 문구 그대로면 빈 값으로 본다 */
	private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

	@Override
	public void setServletContext(ServletContext servletContext) {
		String key = readKey();
		servletContext.setAttribute(ATTRIBUTE, key);
		log.info("카카오맵 키 " + (key.isEmpty() ? "없음 → 지도 · 장소 검색은 안내 문구만 표시" : "읽음"));
	}

	private String readKey() {
		InputStream in = KakaoKeyLoader.class.getClassLoader().getResourceAsStream(FILE);
		if (in == null) {
			log.warn(FILE + " 파일이 없습니다. secret.properties.example을 복사해 만들어 주세요.");
			return "";
		}
		try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			Properties props = new Properties();
			props.load(reader);
			String key = props.getProperty(PROPERTY, "").trim();
			return KEY_PATTERN.matcher(key).matches() ? key : "";
		} catch (IOException e) {
			log.warn(FILE + " 파일을 읽지 못했습니다: " + e.getMessage());
			return "";
		}
	}
}
