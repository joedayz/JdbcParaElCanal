import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;


public class Application {

	public static void main(String[] args) throws SQLException {
		DataSource ds = createDataSource();

		try(Connection connection = ds.getConnection()) {
			int senderId = createUser(connection);
			int receiverId = createUser(connection);
			int transferId = sendMoney(connection, senderId, receiverId, 50);
			System.out.println("Created users with senderId = "+senderId +
					" | receiverId= " + receiverId + " | transferId = " + transferId);

		}

	}

	private static int sendMoney(Connection connection, int senderId, int receiverId, int amount) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement(
				"update users set balance= (balance - ?) where id = ?")) {
			stmt.setInt(1, amount);
			stmt.setInt(2, senderId);
			// implicit: open transaction
			stmt.executeUpdate();
			// implicit: commit/close transaction
		}

		try (PreparedStatement stmt = connection.prepareStatement(
				"update users set balance= (balance + ?) where id = ?")) {
			stmt.setInt(1, amount);
			stmt.setInt(2, receiverId);
			// implicit: open transaction
			stmt.executeUpdate();
			// implicit: commit/close transaction
		}

		try (PreparedStatement stmt = connection.prepareStatement(
				"insert into transfers (sender_id, receiver_id, amount) "
						+ "values (?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
			stmt.setInt(1, senderId);
			stmt.setInt(2, receiverId);
			stmt.setInt(3, amount);
			// implicit: open transaction
			stmt.executeUpdate();
			// implicit: commit/close transaction
			ResultSet rs = stmt.getGeneratedKeys();
			rs.next();
			return rs.getInt(1);
		}

	}

	private static int createUser(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("insert into users (first_name, last_name, registration_date) "
				+ "values(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
			stmt.setString(1, "[first name]");
			stmt.setString(2, "[last name]");
			stmt.setObject(3, LocalDateTime.now());
			stmt.executeUpdate();

			ResultSet rs = stmt.getGeneratedKeys();
			rs.next();
			return rs.getInt(1);
		}

	}


	public static DataSource createDataSource(){

		//docker run -p 3306:3306 --name mysql8 -e MYSQL_ROOT_PASSWORD=joedayz -e MYSQL_DATABASE=mydb -d mysql:8
		HikariDataSource hikariDs = new HikariDataSource();
		hikariDs.setJdbcUrl("jdbc:h2:~/mydatabase;INIT=RUNSCRIPT FROM 'classpath:schema.sql'");
		hikariDs.setUsername("sa");
		hikariDs.setPassword("password");

		DataSource proxyDataSource = ProxyDataSourceBuilder.create(hikariDs)
				.logQueryToSysOut()
				.build();

		return proxyDataSource;
	}
}
