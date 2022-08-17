package conf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class RealWorldSelectAndUpdate {
	public static Connection connectDB() {
		String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";  
	    String DB_URL = "jdbc:mysql://localhost:3306/freeman?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
	    		+ "&useServerPrepStmts=true&cachePrepStmts=true&rewriteBatchedStatements=true";
	    String USER = "root";
	    String PASS = "zzxzzx";
	    Connection conn = null;
        try{
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL,USER,PASS);
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        return conn;

	}
	
	public static void createTable(String tableName,List<String> R) throws SQLException {
		Connection conn =connectDB();
		String sql = "CREATE TABLE `"+tableName+"` ( `id` int AUTO_INCREMENT, ";//aid 自增 primary key
		for(int i = 0;i< R.size();i ++) {
			String columnName = R.get(i);
			sql += "`"+columnName +"` varchar(50),\n";
		}
		sql += " PRIMARY KEY(`id`)) CHARSET=utf8mb3";
//		System.out.println(sql);
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();
		conn.close();
	}
	
	public static void dropTable(String tableName) throws SQLException {
		Connection conn =connectDB();
		String sql = "DROP TABLE IF EXISTS `"+tableName+"`";
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();
		conn.close();
	}
	
	/**
	 * get projection table from original table onto projection R
	 * and store it into databases
	 * @param originalTableName
	 * @param originR
	 * @param projTableName
	 * @param projR
	 * @throws SQLException 
	 */
	public static void getProjectionTable(String originalTableName,List<String> originR,String projTableName,List<String> projR) throws SQLException {
		dropTable(projTableName);//if existing same name table, drop it
		
		Connection conn =connectDB();
		conn.setAutoCommit(false);
		
		String attrList = "";
		for(int i = 0;i < projR.size();i ++) {
			if(i != projR.size() - 1)
				attrList += "`" + projR.get(i) + "`, ";
			else
				attrList += "`" + projR.get(i) + "`";
		}
		String sql0 = "SELECT "+attrList+" FROM `"+originalTableName+"` GROUP BY "+attrList+" HAVING COUNT(*) > 1";//record redundant tuples
		System.out.println("current schema : "+projR);
		System.out.println("checking duplicate rows...");
		System.out.println(sql0);
		Statement stmt1 = conn.createStatement();
		ResultSet redundantRecords = stmt1.executeQuery(sql0);//tuples
		conn.commit();
		HashMap<ArrayList<String>,Boolean> duplicateRow = new HashMap<ArrayList<String>,Boolean>();//record a tuple whether having been inserted in a projection table
		while(redundantRecords.next()) {
			ArrayList<String> duplicateList = new ArrayList<String>();
			for(int i = 0;i < projR.size();i ++) {
				duplicateList.add(redundantRecords.getString(i+1));
			}
			duplicateRow.put(duplicateList, false);
		}

		
		String sql1 = "SELECT * FROM `" + originalTableName+"`";
//		Statement stmt1 = conn.createStatement();
		ResultSet rs1 = stmt1.executeQuery(sql1);//tuples
		conn.commit();
		int duplicate = 0;//redundant tuple number
		ArrayList<ArrayList<String>> newTable = new ArrayList<ArrayList<String>>();
		while(rs1.next()) {
			ArrayList<String> tuple = new ArrayList<String>();
			for(int i = 0;i < projR.size();i ++) {
				String attr = projR.get(i);//projection attribute
				tuple.add(rs1.getString(attr));
			}
			if(duplicateRow.containsKey(tuple)) {//deleting redundant tuples
				if(!duplicateRow.get(tuple)){//duplicate row not inserted yet
					newTable.add(tuple);
					duplicateRow.put(tuple, true);
				}else {
					duplicate ++;
					System.out.println("deleting duplicate row : "+ tuple);
				}
			}else
				newTable.add(tuple);
		}
		
		System.out.println("deleting "+duplicate+" redundant tuples!");
		
		createTable(projTableName,projR);//create projection table in databases
		
		String insertSql = "INSERT INTO `"+projTableName + "` VALUES ( null ,";
		for(int i = 0;i < projR.size();i ++) {
			if(i != (projR.size() - 1))
				insertSql += " ? ,";
			else
				insertSql += " ? )";
		}
		PreparedStatement prepStmt1 = conn.prepareStatement(insertSql);
		System.out.println("creating projection table...");
		for(ArrayList<String> data : newTable) {
			for(int i = 1;i <= projR.size();i ++) {
				prepStmt1.setString(i, data.get(i-1));
			}
			prepStmt1.addBatch();//加入批量处理
		}
		prepStmt1.executeBatch();//执行批量处理
		prepStmt1.clearBatch();
		conn.commit();
		
		
		System.out.println("Successfully create projection table of "+originalTableName+" !");
		
		prepStmt1.close();
		stmt1.close();
		conn.close();
	}
	
	
	
	/**
	 * execute query
	 * SELECT R.A1,...,R.An,count(*) FROM R GROUP BY R.A1,...,R.An HAVING COUNT(*) > 1
	 * @param key
	 * @param tableName
	 * @param needIndex 
	 * @return query time
	 * @throws SQLException
	 */
	public static long selectData_one_key(List<String> key,String tableName,boolean needIndex) throws SQLException {
		Connection conn = connectDB();
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		String sql = "SELECT ";
		for(String attr : key) {
			sql += "`"+tableName+"`.`"+attr+"`, ";
		}
		sql += "COUNT(*) FROM `"+tableName+"` GROUP BY ";
		for(int i = 0;i < key.size();i ++) {
			String attr = key.get(i);
			if(i != (key.size() - 1))
			    sql += "`"+tableName+"`.`"+attr+"`, ";
			else
				sql += "`"+tableName+"`.`"+attr+"` ";
		}
		sql += "HAVING COUNT(*) > 1";
		String index ="ALTER TABLE `"+tableName+"` ADD UNIQUE `myindexOn"+tableName+"` ( ";//创建名为myindexOn...的索引
		for(int i =0;i < key.size();i ++) {
			if(i != (key.size() -1))
				index += "`"+key.get(i) + "`,";
			else
				index += "`"+key.get(i) + "` )";
		}
		String indexRemoval = "DROP INDEX `myindexOn"+tableName+"` ON `"+tableName+"`";//删除索引
		System.out.println("==================");
		System.out.println(sql);
		System.out.println("index: "+needIndex);
		if(needIndex) {
		    System.out.println("built index: "+index);
		    stmt.executeUpdate(index);
		}
		
		long start = new Date().getTime();
		ResultSet rs = stmt.executeQuery(sql);
		rs.last();
		System.out.println("search result size: "+rs.getRow());
		long end = new Date().getTime();
		
		if(needIndex) {
		    stmt.executeUpdate(indexRemoval);
		    System.out.println("removed index: "+indexRemoval);
		}
		rs.close();
		stmt.close();
		conn.close();
		System.out.println("execution time(ms): "+(end - start));
		return (end - start);
	}
	
	/**
	 * execute query
	 * SELECT R.A1,...,R.An FROM R, R AS R1 WHERE R.A1 = R1.A1 AND ... AND R.An = R1.An AND R.id <> R1.id
	 * @param key
	 * @param tableName
	 * @param needIndex 
	 * @return query time
	 * @throws SQLException
	 */
	public static long selectData_two_key(List<String> key,String tableName,boolean needIndex) throws SQLException {
		Connection conn = connectDB();
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		String sql = "SELECT ";
		for(int i = 0;i < key.size();i ++) {
			String attr = key.get(i);
			if(i != (key.size() - 1))
			    sql += "`"+tableName+"`.`"+attr+"`, ";
			else
				sql += "`"+tableName+"`.`"+attr+"` ";
		}
		sql += "FROM `"+tableName+"` , `"+tableName+"` AS `"+tableName+"_1` WHERE ";
		for(int i = 0;i < key.size();i ++) {
			String attr = key.get(i);
			if(i != (key.size() - 1))
			    sql += "`"+tableName+"`.`"+attr+"` = `"+tableName+"_1`.`"+attr+"` AND ";
			else
				sql += "`"+tableName+"`.`"+attr+"` = `"+tableName+"_1`.`"+attr+"`";
		}
		sql += " AND `"+tableName+"`.`id` <> `"+tableName+"_1`.`id`";
		String index ="ALTER TABLE `"+tableName+"` ADD UNIQUE `myindexOn"+tableName+"` ( ";
		for(int i =0;i < key.size();i ++) {
			if(i != (key.size() -1))
				index += "`"+key.get(i) + "`,";
			else
				index += "`"+key.get(i) + "` )";
		}
		String indexRemoval = "DROP INDEX `myindexOn"+tableName+"` ON `"+tableName+"`";
		System.out.println("==================");
		System.out.println(sql);
		System.out.println("index: "+needIndex);
		if(needIndex) {
		    System.out.println("built index: "+index);
		    stmt.executeUpdate(index);
		}
		
		long start = new Date().getTime();
		ResultSet rs = stmt.executeQuery(sql);
		rs.last();
		System.out.println("search result size: "+rs.getRow());
		long end = new Date().getTime();
		
		if(needIndex) {
		    stmt.executeUpdate(indexRemoval);
		    System.out.println("removed index: "+indexRemoval);
		}
		rs.close();
		stmt.close();
		conn.close();
		System.out.println("execution time(ms): "+(end - start));
		return (end - start);
	}
	
	/**
	 * query
	 * validate FD A1...An-> B1...Bm 
	 * SELECT R.A1,....,R.An
	 * FROM R
     * GROUP BY R.A1,....,R.An HAVING COUNT(DISTINCT R.B1...R.Bm)>1
	 * @param fd
	 * @param tableName
	 * @param needIndex 
	 * @return query time
	 * @throws SQLException
	 */
	public static long selectData_three_fd(FD fd,String tableName,boolean needIndex) throws SQLException {
		Connection conn = connectDB();
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		
		List<String> left = fd.getLeftHand();
		List<String> right = fd.getRightHand();
		
		String sql = "SELECT ";
		for(int i = 0;i < left.size();i ++) {
			if(i != left.size() - 1)
				sql += "`"+left.get(i)+"`, ";
			else
				sql += "`"+left.get(i)+"`";
		}
		sql += " FROM `"+tableName+"` GROUP BY ";
		for(int i = 0;i < left.size();i ++) {
			String attr = left.get(i);
			if(i != (left.size() - 1))
			    sql += "`"+attr+"`, ";
			else
				sql += "`"+attr+"` ";
		}
		sql += "HAVING COUNT( DISTINCT ";
		for(int i = 0;i < right.size();i ++) {
			if(i != right.size() -1)
				sql += "`"+right.get(i)+"`, ";
			else
				sql += "`"+right.get(i)+"` ) > 1";
		}
		String index ="ALTER TABLE `"+tableName+"` ADD UNIQUE `myindexOn"+tableName+"` ( ";
		for(int i =0;i < left.size();i ++) {
			if(i != (left.size() -1))
				index += "`"+left.get(i) + "`,";
			else
				index += "`"+left.get(i) + "` )";
		}
		String indexRemoval = "DROP INDEX `myindexOn"+tableName+"` ON `"+tableName+"`";
		System.out.println("==================");
		System.out.println(sql);
		System.out.println("index: "+needIndex);
		if(needIndex) {
		    System.out.println("built index: "+index);
		    stmt.executeUpdate(index);
		}
		
		long start = new Date().getTime();
		ResultSet rs = stmt.executeQuery(sql);
		rs.last();
		System.out.println("search result size: "+rs.getRow());
		long end = new Date().getTime();
		
		if(needIndex) {
		    stmt.executeUpdate(indexRemoval);
		    System.out.println("removed index: "+indexRemoval);
		}
		rs.close();
		stmt.close();
		conn.close();
		System.out.println("execution time(ms): "+(end - start));
		return (end - start);
	}
	
	/**
	 * 
	 * validate fd  A1...An-> B1...Bm
	 * SELECT R.A1,....,R.An
     * FROM R, R AS R'
     * WHERE R.id<>R'.id AND R.A1=R'.A1 AND ... AND R.An=R'.An AND (R.B1<>R.B1 OR ... OR R.Bm<>R.Bm)
	 * @param fd
	 * @param tableName
	 * @param needIndex 
	 * @return query time
	 * @throws SQLException
	 */
	public static long selectData_four_fd(FD fd,String tableName,boolean needIndex) throws SQLException {
		Connection conn = connectDB();
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		
		List<String> left = fd.getLeftHand();
		List<String> right = fd.getRightHand();
		
		String sql = "SELECT ";
		for(int i = 0;i < left.size();i ++) {
			String attr = left.get(i);
			if(i != (left.size() - 1))
			    sql += "`"+tableName+"`.`"+attr+"`, ";
			else
				sql += "`"+tableName+"`.`"+attr+"` ";
		}
		sql += "FROM `"+tableName+"` , `"+tableName+"` AS `"+tableName+"_1` WHERE ";
		for(int i = 0;i < left.size();i ++) {
			String attr = left.get(i);
			if(i != (left.size() - 1))
			    sql += "`"+tableName+"`.`"+attr+"` = `"+tableName+"_1`.`"+attr+"` AND ";
			else
				sql += "`"+tableName+"`.`"+attr+"` = `"+tableName+"_1`.`"+attr+"`";
		}
		sql += " AND `"+tableName+"`.`id` <> `"+tableName+"_1`.`id` AND ( ";
		for(int i = 0;i < right.size();i ++) {
			String attr = right.get(i);
			if(i != (right.size() - 1))
			    sql += "`"+tableName+"`.`"+attr+"` <> `"+tableName+"_1`.`"+attr+"` OR ";
			else
				sql += "`"+tableName+"`.`"+attr+"` <> `"+tableName+"_1`.`"+attr + "` )";
		}
		String index ="ALTER TABLE `"+tableName+"` ADD UNIQUE `myindexOn"+tableName+"` ( ";
		for(int i =0;i < left.size();i ++) {
			if(i != (left.size() -1))
				index += "`"+left.get(i) + "`,";
			else
				index += "`"+left.get(i) + "` )";
		}
		String indexRemoval = "DROP INDEX `myindexOn"+tableName+"` ON `"+tableName+"`";
		System.out.println("==================");
		System.out.println(sql);
		System.out.println("index: "+needIndex);
		if(needIndex) {
		    System.out.println("built index: "+index);
		    stmt.executeUpdate(index);
		}
		
		long start = new Date().getTime();
		ResultSet rs = stmt.executeQuery(sql);
		rs.last();
		System.out.println("search result size: "+rs.getRow());
		long end = new Date().getTime();
		
		if(needIndex) {
		    stmt.executeUpdate(indexRemoval);
		    System.out.println("removed index: "+indexRemoval);
		}
		rs.close();
		stmt.close();
		conn.close();
		System.out.println("execution time(ms): "+(end - start));
		return (end - start);
	}
	
	/**
	 * randomly delete specific ratio of data from table
	 * @return deleted data
	 * @throws SQLException 
	 */
	public static ArrayList<ArrayList<String>> deleteAndGetDataFromTable(String tableName,Double ratio) throws SQLException{
		ArrayList<ArrayList<String>> records = new ArrayList<ArrayList<String>>();
		Connection conn = connectDB();
		conn.setAutoCommit(false);
		
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		String sql = "SELECT * FROM `"+tableName+"`";
		ResultSet rs = stmt.executeQuery(sql);
		conn.commit();
		
		ArrayList<Integer> idList = new ArrayList<Integer>();//all data's id
		while(rs.next()) {
			idList.add(rs.getInt("id"));
		}
		int rowCount = idList.size();
		int deleteNum = (int)(rowCount*ratio);//removal number
		ArrayList<Integer> deletedIdList = new ArrayList<Integer>();//id that will be deleted
		Random rand = new Random();
		for(int i = 0;i < deleteNum;i ++) {
			int index = rand.nextInt(idList.size());
			int id = idList.get(index);
			idList.remove(index);
			deletedIdList.add(id);
		}
		
		rs.beforeFirst();
		while(rs.next()) {//store records that will be deleted afterwards
			int currentId = rs.getInt("id");
			if(deletedIdList.contains(currentId)) {
				ArrayList<String> record = new ArrayList<String>();
				int colNum = rs.getMetaData().getColumnCount();
				for(int i = 2;i <= colNum;i ++) {//dont get id value
					record.add(rs.getString(i));
				}
				records.add(record);
			}
		}
		
		System.out.println("starting to delete "+deleteNum+" records...");
		PreparedStatement stmt1 = conn.prepareStatement("delete from `"+tableName+"` where `id` = ?");
		for(Integer id : deletedIdList) {
			stmt1.setString(1, id+"");
			stmt1.addBatch();
		}
		stmt1.executeBatch();
		stmt1.clearBatch();
		conn.commit();
		System.out.println("successfully delete records!");
		
		rs.close();
		stmt.close();
		stmt1.close();
		conn.close();
		
		return records;
	}
	
	public static long updateData_one_key(List<String> R,List<String> key,String tableName,Double copyRatio,boolean needIndex) throws SQLException {
		Connection conn = connectDB();
		conn.setAutoCommit(false);
		
		Statement stmt = null;
		String index = "";
		String indexRemoval = "";
		
		System.out.println("==================");
		ArrayList<ArrayList<String>> insertCopy = deleteAndGetDataFromTable(tableName,copyRatio);//delete data first and get deleted data copy
		
		System.out.println("insert data into "+tableName +", inserted tuples size : "+insertCopy.size());
		System.out.println("index: "+needIndex);
		if(needIndex) {
			index ="ALTER TABLE `"+tableName+"` ADD UNIQUE `myindexOn"+tableName+"` ( ";
			for(int i =0;i < key.size();i ++) {
				if(i != (key.size() -1))
					index += "`"+key.get(i) + "`,";
				else
					index += "`"+key.get(i) + "` )";
			}
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		    System.out.println("built index: "+index);
		    stmt.executeUpdate(index);
		    conn.commit();
		}
		
		
		String insertSql = "INSERT INTO `"+tableName + "` VALUES ( null ,";
		for(int i = 0;i < R.size();i ++) {
			if(i != (R.size() - 1))
				insertSql += " ? ,";
			else
				insertSql += " ? )";
		}
		PreparedStatement prepStmt1 = conn.prepareStatement(insertSql);
		System.out.println("current sql is (batch process) : "+insertSql);
		long start = new Date().getTime();
		for(ArrayList<String> data : insertCopy) {
			for(int i = 1;i <= R.size();i ++) {
				prepStmt1.setString(i, data.get(i-1));
			}
			prepStmt1.addBatch();
		}
		prepStmt1.executeBatch();
		prepStmt1.clearBatch();
		conn.commit();
		long end = new Date().getTime();
		
		
		if(needIndex) {
		    indexRemoval = "DROP INDEX `myindexOn"+tableName+"` ON `"+tableName+"`";
		    stmt.executeUpdate(indexRemoval);
		    conn.commit();
		    System.out.println("removed index: "+index);
		}
		
		if(null != stmt)
		    stmt.close();
		prepStmt1.close();
		conn.close();
		System.out.println("execution time(ms): "+(end - start));
		return (end - start);
	}
	
	
	/**
	 * for given FD a0,a3 -> a5,a6 with relation R=a0,..,a10
	 * 
	 * INSERT into table_name (a0,...,a10) select a0_value,...,a10_value
     * from dual where NOT EXISTS(SELECT a0,a3 FROM table_name WHERE a0 = a0_value and a3 = a3_value 
     * and (a5 != a5_value or a6 != a6_value))
     * 
	 * @param R
	 * @param fd
	 * @param tableName
	 * @param copyRatio
	 * @param needIndex
	 * @return
	 * @throws SQLException
	 */
	public static long updateData_two_fd(List<String> R,FD fd,String tableName,Double copyRatio) throws SQLException {
		Connection conn = connectDB();
		conn.setAutoCommit(false);
		
		System.out.println("==================");
		ArrayList<ArrayList<String>> insertCopy = deleteAndGetDataFromTable(tableName,copyRatio);//delete data first and get deleted data copy
		
		System.out.println("insert data into "+tableName +", inserted tuples size : "+insertCopy.size());
		
		String insertSql = "INSERT INTO `"+tableName + "` SELECT NULL , ";
		for(int i = 0;i < R.size();i ++) {
			if(i != (R.size() - 1))
				insertSql += " ? ,";
			else
				insertSql += " ? ";
		}
		insertSql += " FROM dual WHERE NOT EXISTS ( SELECT ";
		List<String> left = fd.getLeftHand();
		List<String> right = fd.getRightHand();
		
		
		for(int i = 0;i < left.size();i ++) {
			if(i != left.size() - 1)
				insertSql += "`"+left.get(i) + "` , ";
			else
				insertSql += "`"+left.get(i)+"`";
		}
		insertSql += " FROM `" + tableName + "` WHERE ";
		for(int i = 0;i < left.size();i ++) {
				insertSql += "`"+left.get(i) + "` = ? AND ";
		}
		insertSql += " ( ";
		for(int i = 0;i < right.size();i ++) {
			if(i != right.size() - 1)
				insertSql += "`"+right.get(i) + "` != ? OR ";
			else
				insertSql += "`"+right.get(i) + "` != ? ))";
		}
		
		System.out.println("current sql is (batch process) : "+insertSql);

		PreparedStatement prepStmt1 = conn.prepareStatement(insertSql);
		
		long start = new Date().getTime();
		for(ArrayList<String> data : insertCopy) {
			int index = 1;
			for(int i = 0;i < R.size();i ++) {
				prepStmt1.setString(index ++, data.get(i));
			}
			for(int i = 0;i < left.size();i ++) {
				int attrIndex = R.indexOf(left.get(i));//left hand size's attribute position on schema
				prepStmt1.setString(index ++, data.get(attrIndex));
			}
			for(int i = 0;i < right.size();i ++) {
				int attrIndex = R.indexOf(right.get(i));//right hand size's attribute position on schema
				prepStmt1.setString(index ++, data.get(attrIndex));
			}
			prepStmt1.addBatch();
		}
		prepStmt1.executeBatch();
		prepStmt1.clearBatch();
		conn.commit();
		long end = new Date().getTime();
		

		prepStmt1.close();
		conn.close();
		System.out.println("execution time(ms): "+(end - start));
		return (end - start);
	}
	
	public static void addTrigger(List<String> R,FD fd,String tableName) throws SQLException {
		String select_sql = "SELECT * FROM `"+tableName+"` WHERE ";
		for(String attr : fd.getLeftHand()) {
			select_sql += "`"+tableName+"`.`"+attr+"` = new.`"+attr+"` AND ";
		}
		select_sql += " ( ";
		for(int i = 0;i < fd.getRightHand().size();i ++) {
			if(i != fd.getRightHand().size() - 1)
				select_sql += "`"+tableName+"`.`"+fd.getRightHand().get(i)+"` != new.`"+fd.getRightHand().get(i)+"` OR ";
			else
				select_sql += "`"+tableName+"`.`"+fd.getRightHand().get(i)+"` != new.`"+fd.getRightHand().get(i)+"` )";
		}
		String delete_sql = "DELETE FROM `"+tableName+"` WHERE `"+tableName+"`.`id` = new.`id`";
		String TRIGGER = "CREATE TRIGGER `"+tableName+"_trigger`\r\n"
				+ "AFTER INSERT ON `"+tableName+"`\r\n"
				+ "FOR EACH ROW\r\n"
				+ "BEGIN \r\n"
				+ "   set @violation = IF(EXISTS("+select_sql+"),'YES','NO');\r\n"
				+ "   if @violation = 'YES' THEN\r\n"
				+ "       "+delete_sql+" ;\r\n"
				+ "   end if;\r\n"
				+ "END ";
		Connection conn = connectDB();
		Statement stmt = conn.createStatement();
		System.out.println("add TRIGGER into table "+tableName+"...");
		System.out.println(TRIGGER);
		stmt.executeUpdate(TRIGGER);
		
		stmt.close();
		conn.close();
	}
	
	public static void removeTrigger(String tableName) throws SQLException {
		String remove_sql = "DROP TRIGGER IF EXISTS `"+tableName+"_trigger`";
		Connection conn = connectDB();
		Statement stmt = conn.createStatement();
		System.out.println("remove TRIGGER from table "+tableName+"...");
		System.out.println(remove_sql);
		stmt.executeUpdate(remove_sql);
		
		stmt.close();
		conn.close();
	}
	
	
	/**
	 * for given FD 0,1,2,4,6 -> 3 with relation lineitem = 0,..,10
	 * 
	 * add trigger to validate FD constraints when inserting tuples
	 * 
	 * the TRIGGERT is like :
	 * 
	 * CREATE TRIGGER before_insert_trigger
	 * BEFORE INSERT ON lineitem
	 * FOR EACH ROW
	 * BEGIN 
     * set @exist = IF(EXISTS(SELECT `0`,`1`,`2`,`4`,`6` FROM lineitem WHERE lineitem.`0` = new.`0` and lineitem.`1` = new.`1` and lineitem.`2` = new.`2` and lineitem.`4` = new.`4` and lineitem.`6`=new.`6` AND (lineitem.`3` != new.`3`)),1,0);
     * if @exist = 1 THEN
     * insert  into lineitem VALUES(NULL ,new.`0`,new.`1`,new.`2`,new.`3`,new.`4`,new.`5`,new.`6`,new.`7`,new.`8`,new.`9`,new.`10`) ;
     * end if;
     * END 
     * 
	 * @param R
	 * @param fd
	 * @param tableName
	 * @param copyRatio
	 * @param needIndex
	 * @return
	 * @throws SQLException
	 */
	public static long updateData_three_fd_trigger(List<String> R,FD fd,String tableName,Double copyRatio) throws SQLException {
		Connection conn = connectDB();
		conn.setAutoCommit(false);
		
		System.out.println("==================");
		ArrayList<ArrayList<String>> insertCopy = deleteAndGetDataFromTable(tableName,copyRatio);//delete data first and get deleted data copy
		
		System.out.println("insert data into "+tableName +", inserted tuples size : "+insertCopy.size());
		
		addTrigger(R,fd,tableName);
		
		String insertSql = "INSERT INTO `"+tableName + "` VALUES( NULL , ";
		for(int i = 0;i < R.size();i ++) {
			if(i != (R.size() - 1))
				insertSql += " ? ,";
			else
				insertSql += " ? )";
		}
		
		System.out.println("current sql is (batch process) : "+insertSql);

		PreparedStatement prepStmt1 = conn.prepareStatement(insertSql);
		
		long start = new Date().getTime();
		for(ArrayList<String> data : insertCopy) {
			for(int i = 0;i < R.size();i ++) {
				prepStmt1.setString(i + 1, data.get(i));
			}
			prepStmt1.addBatch();
		}
		prepStmt1.executeBatch();
		prepStmt1.clearBatch();
		conn.commit();
		long end = new Date().getTime();
		
		removeTrigger(tableName);

		prepStmt1.close();
		conn.close();
		System.out.println("execution time(ms): "+(end - start));
		return (end - start);
	}
	
	
	/**
	 * 
	 * @param list
	 * @return
	 */
	public static long getMedian(ArrayList<Long> list) {
		Collections.sort(list,new Comparator<Long>(){

			@Override
			public int compare(Long o1, Long o2) {
				if(o1 > o2)
					return 1;
				else if(o1 < o2)
					return -1;
				else
					return 0;
			}
		});
		int middleIndex = -1;
		if(list.size() % 2 == 0)
			middleIndex = list.size()/2 -1;
		else
			middleIndex = list.size()/2;
//		System.out.println("list ： "+list);
//		System.out.println("median: "+list.get(middleIndex));
		return list.get(middleIndex);
	}
	
	
	/**
	 * 
	 * @param list
	 * @return
	 */
	public static double getAve_Long(ArrayList<Long> list) {
		DecimalFormat df = new DecimalFormat("0.00");
		long sum = 0;
		for(Long data : list) {
			sum += data;
		}
		return Double.parseDouble(df.format(sum/(double)list.size()));
	}
	
	public static double getAve_Double(ArrayList<Double> list) {
		DecimalFormat df = new DecimalFormat("0.00");
		double sum = 0;
		for(double data : list) {
			sum += data;
		}
		return Double.parseDouble(df.format(sum/list.size()));
	}
	
	
	
	
	/**
	 * experiments for BCNF data table
	 * @param tableName
	 * @param insertCopyRatio
	 * @param R
	 * @param keys
	 * @param repeatTime
	 * @return
	 * @throws SQLException
	 * @throws IOException 
	 */
	public static ArrayList<Object> exeSingleExp_key(String output_add,String tableName,List<Double> insertCopyRatio,List<String> R,ArrayList<List<String>> keys,int repeatTime) throws SQLException, IOException {
		System.out.println("current table name : "+tableName+", schema size : "+R.size()+", key number : "+keys.size()+" , in BCNF.");
		
		
		ArrayList<Long> selectDataOneNoIndex = new ArrayList<Long>();
		ArrayList<Long> selectDataOneHavingIndex = new ArrayList<Long>();
		ArrayList<Long> selectDataTwoNoIndex = new ArrayList<Long>();
		ArrayList<Long> selectDataTwoHavingIndex = new ArrayList<Long>();
		ArrayList<Long> insertOneTenthDataNoIndex = new ArrayList<Long>();
		ArrayList<Long> insertOneTenthDataHavingIndex = new ArrayList<Long>();
		ArrayList<Long> insertTwoTenthDataNoIndex = new ArrayList<Long>();
		ArrayList<Long> insertTwoTenthDataHavingIndex = new ArrayList<Long>();
		ArrayList<Long> insertThreeTenthDataNoIndex = new ArrayList<Long>();
		ArrayList<Long> insertThreeTenthDataHavingIndex = new ArrayList<Long>();
		int count = 1;
		for(List<String> key : keys) {
			System.out.println("\ncurrent num : "+count++ +" , key : "+key);
			for(int i = 0;i< repeatTime;i ++) {//每个key重复n次
				long selectDataOneWithoutIndex = selectData_one_key(key,tableName,false);//无索引执行查询1
				selectDataOneNoIndex.add(selectDataOneWithoutIndex);
				long selectDataOneWithIndex = selectData_one_key(key,tableName,true);//有索引执行查询1
				selectDataOneHavingIndex.add(selectDataOneWithIndex);
				
				long selectDataTwoWithoutIndex = selectData_two_key(key,tableName,false);//无索引执行查询2
				selectDataTwoNoIndex.add(selectDataTwoWithoutIndex);
				long selectDataTwoWithIndex = selectData_two_key(key,tableName,true);//有索引执行查询2
				selectDataTwoHavingIndex.add(selectDataTwoWithIndex);
				
				long insertOneTenthDataWithoutIndex = updateData_one_key(R,key,tableName,insertCopyRatio.get(0),false);//无索引插入1/10数据
				insertOneTenthDataNoIndex.add(insertOneTenthDataWithoutIndex);
				long insertOneTenthDataWithIndex = updateData_one_key(R,key,tableName,insertCopyRatio.get(0),true);//有索引插入1/10数据
				insertOneTenthDataHavingIndex.add(insertOneTenthDataWithIndex);
				
				long insertTwoTenthDataWithoutIndex = updateData_one_key(R,key,tableName,insertCopyRatio.get(1),false);//无索引插入2/10数据
				insertTwoTenthDataNoIndex.add(insertTwoTenthDataWithoutIndex);
				long insertTwoTenthDataWithIndex = updateData_one_key(R,key,tableName,insertCopyRatio.get(1),true);//有索引插入2/10数据
				insertTwoTenthDataHavingIndex.add(insertTwoTenthDataWithIndex);
				
				long insertThreeTenthDataWithoutIndex = updateData_one_key(R,key,tableName,insertCopyRatio.get(2),false);//无索引插入3/10数据
				insertThreeTenthDataNoIndex.add(insertThreeTenthDataWithoutIndex);
				long insertThreeTenthDataWithIndex = updateData_one_key(R,key,tableName,insertCopyRatio.get(2),true);//有索引插入3/10数据
				insertThreeTenthDataHavingIndex.add(insertThreeTenthDataWithIndex);
			}
		}
		long v1 = getMedian(selectDataOneNoIndex);
		long v2 = getMedian(selectDataOneHavingIndex);
		long v3 = getMedian(selectDataTwoNoIndex);
		long v4 = getMedian(selectDataTwoHavingIndex);
		long v5 = getMedian(insertOneTenthDataNoIndex);
		long v6 = getMedian(insertOneTenthDataHavingIndex);
		long v7 = getMedian(insertTwoTenthDataNoIndex);
		long v8 = getMedian(insertTwoTenthDataHavingIndex);
		long v9 = getMedian(insertThreeTenthDataNoIndex);
		long v10 = getMedian(insertThreeTenthDataHavingIndex);
		double v11 = getAve_Long(selectDataOneNoIndex);
		double v12 = getAve_Long(selectDataOneHavingIndex);
		double v13 = getAve_Long(selectDataTwoNoIndex);
		double v14 = getAve_Long(selectDataTwoHavingIndex);
		double v15 = getAve_Long(insertOneTenthDataNoIndex);
		double v16 = getAve_Long(insertOneTenthDataHavingIndex);
		double v17 = getAve_Long(insertTwoTenthDataNoIndex);
		double v18 = getAve_Long(insertTwoTenthDataHavingIndex);
		double v19 = getAve_Long(insertThreeTenthDataNoIndex);
		double v20 = getAve_Long(insertThreeTenthDataHavingIndex);
		System.out.println("Before building index,query one median is "+v1+" ms, ave is "+v11+" ms");
		System.out.println("After building index,query one median is "+v2+" ms, ave is "+v12+" ms");
		System.out.println("Before building index,query two median is "+v3+" ms, ave is "+v13+" ms");
		System.out.println("After building index,query two median is "+v4+" ms, ave is "+v14+" ms");
		System.out.println("Before building index,insert 1/10 data median is "+v5+" ms, ave is "+v15+" ms");
		System.out.println("After building index,insert 1/10 data median is "+v6+" ms, ave is "+v16+" ms");
		System.out.println("Before building index,insert 2/10 data median is "+v7+" ms, ave is "+v17+" ms");
		System.out.println("After building index,insert 2/10 data median is "+v8+" ms, ave is "+v18+" ms");
		System.out.println("Before building index,insert 3/10 data median is "+v9+" ms, ave is "+v19+" ms");
		System.out.println("After building index,insert 3/10 data median is "+v10+" ms, ave is "+v20+" ms");
		System.out.println("median : "+v1+","+v2+","+v3+","+v4+","+v5+","+v6+","+v7+","+v8+","+v9+","+v10);
		System.out.println("average : "+v11+","+v12+","+v13+","+v14+","+v15+","+v16+","+v17+","+v18+","+v19+","+v20);
		System.out.println("*******************************");
		
		String append = "median,"+v1+","+v2+","+v3+","+v4+","+v5+","+v6+","+v7+","+v8+","+v9+","+v10+", ,"
				+"average,"+ v11+","+v12+","+v13+","+v14+","+v15+","+v16+","+v17+","+v18+","+v19+","+v20;
		output(output_add,append);
		
		ArrayList<Long> result = new ArrayList<Long>();
		result.add(v1);result.add(v2);result.add(v3);result.add(v4);result.add(v5);
		result.add(v6);result.add(v7);result.add(v8);result.add(v9);result.add(v10);
		ArrayList<Double> result2 = new ArrayList<Double>();
		result2.add(v11);result2.add(v12);result2.add(v13);result2.add(v14);result2.add(v15);
		result2.add(v16);result2.add(v17);result2.add(v18);result2.add(v19);result2.add(v20);
		ArrayList<Object> medianAndAve = new ArrayList<Object>();
		medianAndAve.add(result);
		medianAndAve.add(result2);
		return medianAndAve;
	}
	
	public static ArrayList<Object> exeSingleExp_fd(String output_add,String tableName,List<Double> insertCopyRatio,List<String> R,List<FD> fds,int repeatTime) throws SQLException, IOException {
		System.out.println("current table name : "+tableName+", schema size : "+R.size()+", fd number : "+fds.size()+" , in 3NF.");
		
		
		ArrayList<Long> selectDataThreeNoIndex = new ArrayList<Long>();
		ArrayList<Long> selectDataFourNoIndex = new ArrayList<Long>();
		ArrayList<Long> insertOneTenthDataNoIndex = new ArrayList<Long>();
		ArrayList<Long> insertOneTenthDataHavingIndex = new ArrayList<Long>();
		ArrayList<Long> insertTwoTenthDataNoIndex = new ArrayList<Long>();
		ArrayList<Long> insertTwoTenthDataHavingIndex = new ArrayList<Long>();
		ArrayList<Long> insertThreeTenthDataNoIndex = new ArrayList<Long>();
		ArrayList<Long> insertThreeTenthDataHavingIndex = new ArrayList<Long>();
		int count = 1;
		for(FD fd : fds) {
			System.out.println("\ncurrent num : "+count++ +" , fd : "+fd.getLeftHand()+" -> "+fd.getRightHand());
			for(int i = 0;i< repeatTime;i ++) {
//				long selectDataThreeWithoutIndex = selectData_three_fd(fd,tableName,false);
//				selectDataThreeNoIndex.add(selectDataThreeWithoutIndex);
//				
//				long selectDataFourWithoutIndex = selectData_four_fd(fd,tableName,false);
//				selectDataFourNoIndex.add(selectDataFourWithoutIndex);
//				
//				long insertOneTenthDataWithoutIndex = updateData_one_key(R,fd.getLeftHand(),tableName,insertCopyRatio.get(0),false);
//				insertOneTenthDataNoIndex.add(insertOneTenthDataWithoutIndex);
//				long insertOneTenthDataWithIndex = updateData_two_fd(R,fd,tableName,insertCopyRatio.get(0));
//				insertOneTenthDataHavingIndex.add(insertOneTenthDataWithIndex);
				long insertOneTenthDataWithIndex = updateData_three_fd_trigger(R,fd,tableName,insertCopyRatio.get(0));
				insertOneTenthDataHavingIndex.add(insertOneTenthDataWithIndex);
				
//				long insertTwoTenthDataWithoutIndex = updateData_one_key(R,fd.getLeftHand(),tableName,insertCopyRatio.get(1),false);
//				insertTwoTenthDataNoIndex.add(insertTwoTenthDataWithoutIndex);
//				long insertTwoTenthDataWithIndex = updateData_two_fd(R,fd,tableName,insertCopyRatio.get(1));
//				insertTwoTenthDataHavingIndex.add(insertTwoTenthDataWithIndex);
				long insertTwoTenthDataWithIndex = updateData_three_fd_trigger(R,fd,tableName,insertCopyRatio.get(1));
				insertTwoTenthDataHavingIndex.add(insertTwoTenthDataWithIndex);
				
//				long insertThreeTenthDataWithoutIndex = updateData_one_key(R,fd.getLeftHand(),tableName,insertCopyRatio.get(2),false);
//				insertThreeTenthDataNoIndex.add(insertThreeTenthDataWithoutIndex);
//				long insertThreeTenthDataWithIndex = updateData_two_fd(R,fd,tableName,insertCopyRatio.get(2));
//				insertThreeTenthDataHavingIndex.add(insertThreeTenthDataWithIndex);
				long insertThreeTenthDataWithIndex = updateData_three_fd_trigger(R,fd,tableName,insertCopyRatio.get(2));
				insertThreeTenthDataHavingIndex.add(insertThreeTenthDataWithIndex);
			}
		}
//		long v1 = getMedian(selectDataThreeNoIndex);
//		long v3 = getMedian(selectDataFourNoIndex);
//		long v5 = getMedian(insertOneTenthDataNoIndex);
		long v6 = getMedian(insertOneTenthDataHavingIndex);
//		long v7 = getMedian(insertTwoTenthDataNoIndex);
		long v8 = getMedian(insertTwoTenthDataHavingIndex);
//		long v9 = getMedian(insertThreeTenthDataNoIndex);
		long v10 = getMedian(insertThreeTenthDataHavingIndex);
//		double v11 = getAve_Long(selectDataThreeNoIndex);
//		double v13 = getAve_Long(selectDataFourNoIndex);
//		double v15 = getAve_Long(insertOneTenthDataNoIndex);
		double v16 = getAve_Long(insertOneTenthDataHavingIndex);
//		double v17 = getAve_Long(insertTwoTenthDataNoIndex);
		double v18 = getAve_Long(insertTwoTenthDataHavingIndex);
//		double v19 = getAve_Long(insertThreeTenthDataNoIndex);
		double v20 = getAve_Long(insertThreeTenthDataHavingIndex);
//		System.out.println("query three median is "+v1+" ms, ave is "+v11+" ms");
//		System.out.println("query four median is "+v3+" ms, ave is "+v13+" ms");
//		System.out.println("Before building index,insert 1/10 data median is "+v5+" ms, ave is "+v15+" ms");
		System.out.println("After building index,insert 1/10 data median is "+v6+" ms, ave is "+v16+" ms");
//		System.out.println("Before building index,insert 2/10 data median is "+v7+" ms, ave is "+v17+" ms");
		System.out.println("After building index,insert 2/10 data median is "+v8+" ms, ave is "+v18+" ms");
//		System.out.println("Before building index,insert 3/10 data median is "+v9+" ms, ave is "+v19+" ms");
		System.out.println("After building index,insert 3/10 data median is "+v10+" ms, ave is "+v20+" ms");
		
//		System.out.println("median : "+v1+","+v3+","+v5+","+v6+","+v7+","+v8+","+v9+","+v10);
//		System.out.println("average : "+v11+","+v13+","+v15+","+v16+","+v17+","+v18+","+v19+","+v20);
		
		System.out.println("median : "+v6+","+v8+","+v10);
		System.out.println("average : "+v16+","+v18+","+v20);
		System.out.println("*******************************");
		
//		String append = "median,"+v1+","+v3+","+v5+","+v6+","+v7+","+v8+","+v9+","+v10+", ,"
//				+"average,"+ v11+","+v13+","+v15+","+v16+","+v17+","+v18+","+v19+","+v20;
		
		String append = "median,"+v6+","+v8+","+v10+", ,"
				+"average,"+ v16+","+v18+","+v20;
		
		output(output_add,append);
		
		ArrayList<Long> result = new ArrayList<Long>();
//		result.add(v1);result.add(v3);result.add(v5);
//		result.add(v6);result.add(v7);result.add(v8);result.add(v9);result.add(v10);
		ArrayList<Double> result2 = new ArrayList<Double>();
//		result2.add(v11);result2.add(v13);result2.add(v15);
//		result2.add(v16);result2.add(v17);result2.add(v18);result2.add(v19);result2.add(v20);
		ArrayList<Object> medianAndAve = new ArrayList<Object>();
		medianAndAve.add(result);
		medianAndAve.add(result2);
		return medianAndAve;
	}
	
	/**
	 * execute single experiment
	 * @param threeNF_fds
	 * @param OriginSchema
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public static void exeSingleExp(String output_add,ArrayList<FD> threeNF_fds,String OriginTableName,List<String> OriginSchema,String ProjTableName,List<Double> insertRatio,int repeatTimeOfEach) throws SQLException, IOException {
		for(FD fd : threeNF_fds) {
			List<String> subschema = new ArrayList<String>();
			for(String attr : fd.getLeftHand()) {
				if(!subschema.contains(attr))
					subschema.add(attr);
			}
			for(String attr : fd.getRightHand()) {
				if(!subschema.contains(attr))
					subschema.add(attr);
			}
			Collections.sort(subschema, new Comparator<String>() {//increasing order

				@Override
				public int compare(String o1, String o2) {
					int int1 = Integer.parseInt(o1);
					int int2 = Integer.parseInt(o2);
					if(int1 > int2)
						return 1;
					else if(int1 < int2)
						return -1;
					else
						return 0;
				}
				
			});
			
			getProjectionTable(OriginTableName,OriginSchema,ProjTableName,subschema);
			
			ArrayList<List<String>> keys = new ArrayList<List<String>>();
			keys.add(fd.getLeftHand());
			
			RealWorldSelectAndUpdate.exeSingleExp_key(output_add,ProjTableName, insertRatio, subschema, keys, repeatTimeOfEach);
		}
	}
	
	//append a line into file
	public static void output(String path,String append) throws IOException {
		FileWriter fw = new FileWriter(path,true);
		BufferedWriter br = new BufferedWriter(fw);
		
		br.write(append+"\n");
		
		br.close();
		fw.close();
	}
	
	
	public static void main(String[] args) throws SQLException, IOException {
		String outputPath = "F:\\javaworkspace\\result\\lineitem";
		File file = new File(outputPath);
		if(!file.exists())
			file.createNewFile();
		
		String dataset_add = "F:\\javaworkspace\\result\\FD Results\\Complete Data\\lineitem.json";
		String decom_output_add = outputPath+"\\decom-result.csv";
		String dataset = "lineitem";
		int dataset_schema_size = 16;
		
		List<String> schema = new ArrayList<String>();
		for(int i = 0;i < dataset_schema_size;i ++) {
			schema.add(i+"");
		}
	    
	    CONF alg1 = new CONF();
	    List<Object> res = Utils.import_schema_and_FDs_with_subschema_level();
		List<String> R = (List<String>) res.get(0);
		List<FD> Sigma_a = (List<FD>) res.get(1);
		ArrayList<Schema> BCNF_3NF_maxN = alg1.decomp_and_output(R,Sigma_a);
//		CONFComparison alg2 = new CONFComparison(dataset_add,decom_output_add);
//		ArrayList<Pair> BCNF_3NF_maxN = alg2.decomp_and_output();
//		CONF_3NF alg3 = new CONF_3NF(dataset_add,decom_output_add);
//		ArrayList<Pair> BCNF_3NF_maxN = alg3.decomp_and_output();
//		CONF_Basic alg4 = new CONF_Basic(dataset_add,decom_output_add);
//		ArrayList<Pair> BCNF_3NF_maxN = alg4.decomp_and_output();
		
		Schema BCNF = BCNF_3NF_maxN.get(0);
		List<FD> BCNF_fds = BCNF.getFd_set();
		ArrayList<List<String>> BCNF_keys = new ArrayList<List<String>>();
		for(FD fd : BCNF_fds) {
			BCNF_keys.add(fd.getLeftHand());
		}
		
		Schema threeNF = BCNF_3NF_maxN.get(1);
		List<FD> threeNF_fds = threeNF.getFd_set();
		
		int repeatTimeOfEachKey = 1;
		List<Double> insertRatio = Arrays.asList(0.00001,0.00002,0.00003);
		Collections.sort(threeNF.getAttr_set(), new Comparator<String>() {//schema increasing order

			@Override
			public int compare(String o1, String o2) {
				int int1 = Integer.parseInt(o1);
				int int2 = Integer.parseInt(o2);
				if(int1 > int2)
					return 1;
				else if(int1 < int2)
					return -1;
				else
					return 0;
			}
			
		});
		
		for(FD fd : threeNF_fds) {
			Collections.sort(fd.getLeftHand(), new Comparator<String>() {//schema increasing order

				@Override
				public int compare(String o1, String o2) {
					int int1 = Integer.parseInt(o1);
					int int2 = Integer.parseInt(o2);
					if(int1 > int2)
						return 1;
					else if(int1 < int2)
						return -1;
					else
						return 0;
				}
				
			});
			Collections.sort(fd.getRightHand(), new Comparator<String>() {//schema increasing order

				@Override
				public int compare(String o1, String o2) {
					int int1 = Integer.parseInt(o1);
					int int2 = Integer.parseInt(o2);
					if(int1 > int2)
						return 1;
					else if(int1 < int2)
						return -1;
					else
						return 0;
				}
				
			});
		}
		
		System.out.println("exp 1 :");
		output(outputPath+"\\"+dataset+".csv","exp 1 :");
		RealWorldSelectAndUpdate.getProjectionTable(dataset, schema,dataset+"-alg1-3nf-proj", threeNF.getAttr_set());
		RealWorldSelectAndUpdate.exeSingleExp_fd(outputPath+"\\"+dataset+".csv",dataset+"-alg1-3nf-proj", insertRatio, threeNF.getAttr_set(), threeNF_fds, repeatTimeOfEachKey);
	    
//		System.out.println("exp 2 :");
//		output(outputPath+"\\"+dataset+".csv","exp 2 :");
//		RealWorldSelectAndUpdate.exeSingleExp(outputPath+"\\"+dataset+".csv",threeNF_fds, dataset+"-alg1-3nf-proj", threeNF.getAttr_set(), dataset+"-alg1-3nf-proj-proj", insertRatio, repeatTimeOfEachKey);
	}

}
