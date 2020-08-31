import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankService {

	@Transactional
	public void sendMoney(int senderId, int receiverId, int amount) {
//		Connection connection = dataSource.getConnection();
//		try (connection) {
//			connection.setAutoCommit(false);
//
//			// execute your sql SQL that e.g.
//
//			connection.commit();
//		} catch (SQLException e) {
//			connection.rollback();
//		}
	}
}
