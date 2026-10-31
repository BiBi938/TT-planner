package com.ttplanner.persistence;

import static org.junit.Assert.fail;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.junit.Test;
import lombok.extern.log4j.Log4j;

/**
 * Oracle 접속만 단독으로 확인하는 테스트 (스프링 없이).
 * 계정 정보는 src/main/resources/db.properties 에서 읽는다.
 */
@Log4j
public class JDBCTests {

	@Test
	public void testConnection() {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
			if (in == null) {
				fail("db.properties 가 없습니다. db.properties.example 을 복사해 만드세요.");
			}
			Properties p = new Properties();
			p.load(in);

			// jdbc:log4jdbc:oracle:... → jdbc:oracle:... (드라이버 직접 접속)
			String url = p.getProperty("db.url").replace("jdbc:log4jdbc:", "jdbc:");

			Class.forName("oracle.jdbc.OracleDriver");
			try (Connection con = DriverManager.getConnection(url,
					p.getProperty("db.username"), p.getProperty("db.password"))) {
				log.info(con);
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
