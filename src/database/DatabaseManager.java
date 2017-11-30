package database;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
//import com.mysql.jdbc.PreparedStatement;
import java.util.Map;

import com.sun.prism.paint.RadialGradient;

public class DatabaseManager {
	String addr = "jdbc:mysql://localhost:3306/spenpal";
	Connection conn;
	Statement statement;
	PreparedStatement preparedStatement;
	ResultSet resultSet;

	public DatabaseManager(String address) {
		if (!address.equals("")) {
			this.addr = address;
		}
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(addr, "root", "toor");
			// conn = DriverManager.getConnection(addr, "root", "");
			statement = conn.createStatement();
		} catch (SQLException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		// File file = new java.io.File(addr);
		System.out.print("Loading database...");
		try {
			// user table
			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS users(username char(64) PRIMARY KEY, password char(64) NOT NULL,gravatar TEXT, profile TEXT)");
			// friend relation table(many to many)
			statement.executeUpdate(//
					"CREATE TABLE IF NOT EXISTS friendlist(user char(64),friend char(64),FOREIGN KEY (user) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE, FOREIGN KEY (friend) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE)");
			// group list
			// statement.executeUpdate(
			// "CREATE TABLE IF NOT EXISTS groups(groupname char(64) PRIMARY
			// KEY, profile TEXT,leader char(64), FOREIGN KEY (leader)
			// REFERENCES users(username) ON DELETE CASCADE ON UPDATE
			// CASCADE)");
			// group relation table(many to many)
			// statement.executeUpdate(
			// "CREATE TABLE IF NOT EXISTS grouplist(user char(64),groups
			// char(64),FOREIGN KEY (user) REFERENCES users(username) ON DELETE
			// CASCADE ON UPDATE CASCADE, FOREIGN KEY (groups) REFERENCES
			// groups(groupname))");

			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS groupinfo(groupname CHAR(64) NOT NULL, username CHAR(64) NOT NULL, payment FLOAT, FOREIGN KEY(username) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE)");

			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS userprivatedata(user char(64),FOREIGN KEY (user) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE, query TEXT,time char(40))");

			statement.executeUpdate("CREATE TABLE IF NOT EXISTS userdata"
					+ " (user char(64),id INTEGER, category char(30), amount FLOAT, date char(30), comment char(30), image BLOB,time char(40),share INTEGER)");
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("finish");

	}

	// public void delgroup

	/**
	 * Close this DatabaseManager
	 */
	public void close() throws SQLException {
		statement.close();
		preparedStatement.close();
		resultSet.close();
		conn.close();
	}

	/**
	 * Add a friend
	 * 
	 * @param user
	 *            user's name
	 * @param friendname
	 *            friend's name
	 * @return false if there's an error occurred
	 */
	public boolean addFriend(String user, String friendname) {
		try {
			String sql = "select count(*) from users where username ='" + friendname + "';";
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(sql);
			rs.next();
			if (rs.getInt(1) == 1) {// friend to add exist
				preparedStatement = (PreparedStatement) conn.prepareStatement("INSERT INTO friendlist values(?, ?)");
				preparedStatement.setString(1, user);
				preparedStatement.setString(2, friendname);
				preparedStatement.executeUpdate();
				preparedStatement.close();
			} else {
				System.out.println("User :" + friendname + "do not exist ,can not add friend");
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Get friend list
	 * 
	 * @param user
	 *            User name
	 * @return A List of friend
	 */
	public List<String> getFriends(String user) {
		List<String> friendList = new LinkedList<>();
		try {
			preparedStatement = (PreparedStatement) conn.prepareStatement("SELECT * FROM friendlist WHERE user=?");
			preparedStatement.setString(1, user);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				friendList.add(resultSet.getString("friend"));
			}
			preparedStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return friendList;
	}

	/**
	 * Delete friend
	 * 
	 * @param user
	 *            user's name
	 * @param friendname
	 *            friend's name
	 * @return false if there's an error occurred
	 */
	public boolean deleteFriend(String user, String friendname) {
		try {
			preparedStatement = (PreparedStatement) conn
					.prepareStatement("DELETE FROM friendlist WHERE user =? AND friend=?");
			preparedStatement.setString(1, user);
			preparedStatement.setString(2, friendname);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Create a new group
	 * 
	 * @param ownername
	 *            Name of the creator
	 * @param groupname
	 *            Name of the group
	 * @return
	 */
	public boolean createGroup(String ownername, String groupname) {
		try {
			// Create a new group
			preparedStatement = (PreparedStatement) conn.prepareStatement("INSERT INTO groups(?, NULL,?)");
			preparedStatement.setString(1, groupname);
			preparedStatement.setString(2, ownername);
			preparedStatement.executeUpdate();
			// Add creator to the group
			preparedStatement = (PreparedStatement) conn.prepareStatement("INSERT INTO grouplist(?,?)");
			preparedStatement.setString(1, ownername);
			preparedStatement.setString(2, groupname);
			preparedStatement.executeUpdate();
			preparedStatement.close();

			Statement st = conn.createStatement();
			String sql = "create table " + groupname + "(member char(64),amount float);";
			st.executeUpdate(sql);

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Add a member to a group
	 * 
	 * @param membername
	 *            Name of the member
	 * @param groupname
	 *            Name of the group
	 * @return
	 */
	public boolean addMember(String username, String groupname) {
		try {
			preparedStatement = (PreparedStatement) conn.prepareStatement("INSERT INTO grouplist(?,?)");
			preparedStatement.setString(1, username);
			preparedStatement.setString(2, groupname);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public List<String> getgroupdata(String groupname) {
		List<String> toreturn = new LinkedList<String>();
		String sql = "select * from groupname;";
		Statement st;
		try {
			st = conn.createStatement();
			ResultSet rs = st.executeQuery(sql);
			while (rs.next()) {
				String s = rs.getString(1) + " " + rs.getFloat(2);
				toreturn.add(s);

			}

		} catch (SQLException e) {

			e.printStackTrace();
		}
		return toreturn;

	}

	/**
	 * Quit from a group
	 * 
	 * @param user
	 *            The user name who leaves the group
	 * @param group
	 *            The group name that user leaves
	 * @return
	 */
	public boolean quitGroup(String user, String group) {
		try {
			preparedStatement = (PreparedStatement) conn
					.prepareStatement("DELETE FROM grouplist WHERE user=? AND group=?");
			preparedStatement.setString(1, user);
			preparedStatement.setString(2, group);
			preparedStatement.executeUpdate();
			preparedStatement = (PreparedStatement) conn.prepareStatement("SELECT * FROM grouplist WHERE group=?");
			preparedStatement.setString(1, group);
			if (preparedStatement.executeQuery() == null) {
				preparedStatement = (PreparedStatement) conn.prepareStatement("DELETE FROM group WHERE groupname=?");
				preparedStatement.setString(1, group);
				preparedStatement.executeUpdate();
			}
			preparedStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean login(String username, String password) {

		try {
			// PreparedStatement preparedStatement = (PreparedStatement) conn
			// .prepareStatement("SELECT count(*) FROM users WHERE user ='?' AND
			// password='?'");
			// preparedStatement.setString(1, username);
			// preparedStatement.setString(2, password);
			// ResultSet rs = preparedStatement.executeQuery();
			String sql = "select count(*) from users where username ='" + username + "' and password='" + password
					+ "';";
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(sql);
			rs.next();
			if (rs.getInt(1) == 1) {// if the record exist ,it will return 1
				System.out.println("ok");
				st.close();
				return true;
			} else {
				System.out.println("wrong username or password");
				st.close();
				return false;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean register(String username, String password) {

		try {
			String sql = "select count(*) from users where username ='" + username + "';";
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(sql);
			rs.next();
			if (rs.getInt(1) == 1) {// if the record exist ,it will return 1
				System.out.println("user already exist");
				st.close();
				return false;
			} else {
				String sql_ins = "INSERT INTO users VALUES ('" + username + "', '" + password + "',NULL,NULL)";
				// System.out.println(sql_ins);
				st.executeUpdate(sql_ins);
				st.close();

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	public List<String> update_new(String username, String time) {
		List<String> toreturn = new LinkedList<String>();
		try {
			preparedStatement = (PreparedStatement) conn
					.prepareStatement("SELECT * FROM userdata WHERE user =? and time>?");
			preparedStatement.setString(1, username);
			preparedStatement.setString(2, time);
			ResultSet rs = preparedStatement.executeQuery();
			while (rs.next()) {
				String ta = "insert into " + username + " (id, category, amount,date,comment,shared) values("
						+ rs.getString("id") + ",'" + rs.getString("category") + "', " + rs.getString("amount") + ",'"
						+ rs.getString("date") + "','" + rs.getString("comment") + "'," + rs.getInt("share") + ");";

				System.out.println(ta);
				toreturn.add(ta);
			}
			return toreturn;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return toreturn;
	}

	public String updatecount(String username, String time) {
		try {
			preparedStatement = (PreparedStatement) conn
					.prepareStatement("SELECT count(*) FROM userdata WHERE user =? and time>?");
			preparedStatement.setString(1, username);
			preparedStatement.setString(2, time);
			ResultSet rs = preparedStatement.executeQuery();
			rs.next();
			String toreturn = rs.getString(1);
			rs.close();
			return toreturn;
		} catch (SQLException e) {

		}
		return "0";
	}

	public List<String> getfriendshare(String user) {
		List<String> toreturn = new LinkedList<String>();
		try {
			preparedStatement = (PreparedStatement) conn.prepareStatement("SELECT * FROM userdata where share=1;");
			ResultSet rs = preparedStatement.executeQuery();
			while (rs.next()) {
				String ta = "insert into sharings (user, category, amount,date,comment) values('" + rs.getString("user")
						+ "','" + rs.getString("category") + "', " + rs.getString("amount") + ",'"
						+ rs.getString("date") + "','" + rs.getString("comment") + "');";
				System.out.println(ta);
				toreturn.add(ta);
			}
			return toreturn;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return toreturn;
	}

	public String insertUserdata_new(String username, String id, String category, String amount, String date,
			String comment, String image) {
		try {
			String time = LocalDateTime.now().toString();

			String sql = "INSERT INTO userdata values(?,?,?,?,?,?,?,?,0)";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, username);
			ps.setString(2, id);
			ps.setString(3, category);
			ps.setString(4, amount);
			ps.setString(5, date);
			ps.setString(6, comment);
			ps.setString(7, image);
			ps.setString(8, time);
			ps.executeUpdate();
			return time;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "0";

	}

	public String delUserdata(String username, String id, String date) {
		try {

			String sql = "delete from userdata where user=? and id=? and date=?";
			String changeid = "update userdata set id=id-1 where id>" + id + " and user=? and date=?;";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, username);
			ps.setString(2, id);
			ps.setString(3, date);
			ps.executeUpdate();
			PreparedStatement ps2 = conn.prepareStatement(changeid);
			ps2.setString(1, username);
			ps2.setString(2, date);
			int r = ps2.executeUpdate();
			System.out.println("delUserdata: " + r);
			return "ok";

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "0";

	}

	public void change_share_status(String username, String id, String date, int i) {
		try {
			String sql = "update userdata set share=" + i + " where user=? and id=? and date=?";

			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, username);
			ps.setString(2, id);
			ps.setString(3, date);
			int r = ps.executeUpdate();
			System.out.println("change_share_status: " + r);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean newAAPayment(String username, float amount, String groupname) {
		String query = "INSERT INTO groupinfo(groupname, username, payment) values (?, ?, ?)";
		try {
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, groupname);
			ps.setString(2, username);
			ps.setFloat(3, amount);
			int r = ps.executeUpdate();
			System.out.println("Add AA state: " + r);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean quitAAGroup(String username, String groupname) {
		String query = "DELETE FROM groupinfo WHERE groupname=? AND username=?";
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, groupname);
			ps.setString(2, username);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public List<Object> getAAResult(String username, String groupname) {
		List<Object> resultlist = new LinkedList<>();
		// Content in the list:
		// [average spend]
		// [member's position that you should give money]
		// [amount you should pay]
		// [member1][member1spend]...[member n][member n spend]
		try {
			// get total spend
			PreparedStatement ps;
			ResultSet rs;
			// get average spend of all members
			ps = conn.prepareStatement(
					"SELECT AVG(total_payment) FROM (SELECT sum(payment) as total_payment FROM groupinfo WHERE groupname=? group by username)as payments");
			ps.setString(1, groupname);
			rs = ps.executeQuery();
			rs.next();
			float avg = rs.getFloat(1);
			// calculate the amount this user should pay others
			// and fetch friend list
			ps = conn.prepareStatement(
					"SELECT username, sum(payment) as total_payment FROM groupinfo WHERE groupname=? GROUP BY username ORDER BY total_payment DESC");
			ps.setString(1, groupname);
			rs = ps.executeQuery();
			rs.last();
			newAAPayment(username, 0, groupname);
			rs = ps.executeQuery();
			rs.last();
			String members[] = new String[rs.getRow()];
			float spends[] = new float[rs.getRow()];
			float deserve[] = new float[rs.getRow()];
			rs.beforeFirst();
			int pos = 0;// position in list - 1
			int count = 0;

			while (rs.next()) {
				members[count] = rs.getString(1);
				spends[count] = rs.getFloat(2);
				if (count > 0) {
					deserve[count] = deserve[count - 1] + spends[count] - avg;
				} else {
					deserve[count] = spends[count] - avg;
				}
				if (members[count].equals(username)) {
					pos = count - 1;
				}
				System.out.println("Deserve: " + deserve[count]);
				count++;
			}

			resultlist.add(avg);
			resultlist.add(pos);
			if (pos < 0) {
				resultlist.add(0);
			} else {
				resultlist.add(deserve[pos]);
			}

			for (int i = 0; i < count; i++) {
				resultlist.add(members[i]);
				resultlist.add(spends[i]);
			}

			ps.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return resultlist;
	}
}
