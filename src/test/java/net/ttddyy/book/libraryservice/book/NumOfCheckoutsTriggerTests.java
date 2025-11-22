package net.ttddyy.book.libraryservice.book;

import net.ttddyy.book.libraryservice.DbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@DbTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NumOfCheckoutsTriggerTests {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void incrementByCheckoutInsert() {
		String sql;
		sql = """
				INSERT INTO members (id, firstname_en, lastname_en, school_id, grade)
				VALUES	(10, 'foo', 'foo', 'sky', 7),
						(20, 'bar', 'bar', 'ocean', 2);
				""";
		this.jdbcTemplate.update(sql);
		sql = """
				INSERT INTO books (id, school_id, title, author, isbn, publisher, book_category_id, num_checkouts)
				VALUES  (1, 'sky', 'foo', 'foo', 'foo', 'foo', 3, 0)
				""";
		this.jdbcTemplate.update(sql);

		// nothing changed
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * FROM books WHERE id = ?", 1L);
		assertThat(map.get("num_checkouts")).isEqualTo(0);

		// checkout
		sql = """
				INSERT INTO checkouts (book_id, member_id, checkout_date, due_date)
				VALUES	( 1, 10, '2020-02-03', '2020-02-10');
				""";
		this.jdbcTemplate.update(sql);
		map = this.jdbcTemplate.queryForMap("SELECT * FROM books WHERE id = ?", 1L);
		assertThat(map.get("num_checkouts")).isEqualTo(1);

		// delete(returning the book) should not change the value
		this.jdbcTemplate.update("DELETE FROM checkouts WHERE book_id = ?", 1L);
		map = this.jdbcTemplate.queryForMap("SELECT * FROM books WHERE id = ?", 1L);
		assertThat(map.get("num_checkouts")).isEqualTo(1);

		// checkout again
		sql = """
				INSERT INTO checkouts (book_id, member_id, checkout_date, due_date)
				VALUES	( 1, 10, '2020-05-01', '2020-05-15');
				""";
		this.jdbcTemplate.update(sql);
		map = this.jdbcTemplate.queryForMap("SELECT * FROM books WHERE id = ?", 1L);
		assertThat(map.get("num_checkouts")).isEqualTo(2);
	}

}
