package com.mza.Libreria;

import static org.assertj.core.api.Assertions.assertThat;

import com.mza.Libreria.support.TestDatabaseInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseInitializer.class)
class LibreriaWebApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void baseDeTestEsLaEsperada(@Autowired JdbcTemplate jdbcTemplate) {
		String db = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
		assertThat(db).isEqualTo("libreriaweb_testsuite");
	}

}
