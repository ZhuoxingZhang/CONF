package conf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * some tools
 *
 */
public class DataProcess {
	
	/**
	 * add double quotation marks for attributes except 'id' attribute
	 * @param tableName
	 * @throws SQLException 
	 */
	public static void addDoubleQuotationMarksForAttrs(String tableName,String finalTableName,int schemaSize) throws SQLException {
		Connection conn = RealWorldSelectAndUpdate.connectDB();
		conn.setAutoCommit(false);
		
		String sql = "SELECT * FROM `"+tableName+"`";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		conn.commit();
		ArrayList<ArrayList<String>> relation = new ArrayList<ArrayList<String>>();
		while(rs.next()) {
			ArrayList<String> tuple = new ArrayList<String>();
			for(int i = 0;i < schemaSize;i ++) {
				tuple.add("\""+rs.getString(i+1)+"\"");
			}
			relation.add(tuple);
		}
		
		ArrayList<String> R = new ArrayList<String>();//create table
		for(int i = 0;i < schemaSize;i ++) {
			R.add(""+i);
		}
		RealWorldSelectAndUpdate.createTable(finalTableName, R);
		
		String insertSql = "INSERT INTO `"+finalTableName + "` VALUES ( null ,";
		for(int i = 0;i < R.size();i ++) {
			if(i != (R.size() - 1))
				insertSql += " ? ,";
			else
				insertSql += " ? )";
		}
		PreparedStatement prepStmt1 = conn.prepareStatement(insertSql);
		System.out.println("creating  table...");
		int count = 0;
		for(ArrayList<String> data : relation) {
			count ++;
			for(int i = 1;i <= R.size();i ++) {
				prepStmt1.setString(i, data.get(i-1));
			}
			prepStmt1.addBatch();
			if(count % 10000 == 0) {
				prepStmt1.executeBatch();
				prepStmt1.clearBatch();
				conn.commit();
			}
		}
		prepStmt1.executeBatch();
		prepStmt1.clearBatch();
		conn.commit();
		
		stmt.close();
		prepStmt1.close();
		conn.close();
	}

	public static void main(String[] args) throws SQLException {
		DataProcess.addDoubleQuotationMarksForAttrs("lineitem", "lineitem_tmp", 16);
	}

}
