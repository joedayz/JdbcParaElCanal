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

	private static DataSource ds = createDataSource();

	public static void main(String[] args) throws SQLException {
		// transaction isolation levels


		Connection connection = ds.getConnection();
		// optimistic locking

		int senderId = createUser(connection);  // default balance = 100
		int senderVersion = getVersion(senderId); // default id = 1;
		int receiverId = createUser(connection); // default balance = 100

		int amount = 99;


		try (connection) {
			connection.setAutoCommit(false);

			try (PreparedStatement stmt = connection.prepareStatement(
					"update users set balance = (balance - ?)," +
							"version = (version + 1) " +
							"where id = ? and " +
							"version = ?")) {

				stmt.setInt(1, amount);
				stmt.setInt(2, senderId);
				stmt.setInt(3, senderVersion);
				final int rowsUpdated = stmt.executeUpdate();
				if (rowsUpdated == 0) throw new RuntimeException("Optimistic " +
						"Locking Exception");
				System.out.println("rowsUpdated = " + rowsUpdated);
			}

			connection.commit();
		} catch (SQLException e) {
			connection.rollback();
		}
	}

	private static Integer getVersion(int userId) throws SQLException {
		Connection connection = ds.getConnection();
		Integer balance = null;

		try (connection; PreparedStatement stmt = connection.prepareStatement(
				"select version" +
						" " +
						"from users where id = ?")) {

			stmt.setInt(1, userId);

			ResultSet resultSet = stmt.executeQuery();
			while (resultSet.next()) {
				balance = resultSet.getInt("version");
				break;
			}
		}
		return balance;
	}

	private static int createUser(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("insert into " +
						"users (first_name, last_name, registration_date) values " +
						"(?,?,?)"
				, Statement.RETURN_GENERATED_KEYS)) {
			stmt.setString(1, "[Some FirstName]");
			stmt.setString(2, "[Some LastName]");
			stmt.setObject(3, LocalDateTime.now());
			stmt.executeUpdate();

			final ResultSet keysResultSet = stmt.getGeneratedKeys();
			keysResultSet.next();
			return keysResultSet.getInt(1);
		}
	}


	private static Integer getBalance(Connection connection, int userId) throws SQLException {
		Integer balance = null;

		try (PreparedStatement stmt = connection.prepareStatement(
				"select balance " +
						"from users where id = ?")) {

			stmt.setInt(1, userId);

			ResultSet resultSet = stmt.executeQuery();
			while (resultSet.next()) {
				balance = resultSet.getInt("balance");
				break;
			}
		}
		return balance;
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
