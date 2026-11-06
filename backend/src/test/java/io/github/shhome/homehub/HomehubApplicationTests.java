package io.github.shhome.homehub;

import java.sql.Connection;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class HomehubApplicationTests {
	@Autowired
	private DataSource dataSource;

	@Test
	void databaseConnectionIsValid() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertTrue(connection.isValid(1));
		}
	}

}
