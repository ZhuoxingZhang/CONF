package additional;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * we study subschema performance on queries and updates, which resulted from decomposition of CONF/some compared algorithms
 * the subschema we experiment will be with some FDs
 *
 */
public class SubschemaPerfExp {
	/*
	 * given a matrix, we return a list that each element of the list will be average/median value of each column of matrix
	 */
	public static List<Double> getEachColValue(List<List<Double>> matrix,boolean getMedianValue) {
		List<Double> output = new ArrayList<Double>();
		for(int col = 0;col < matrix.get(0).size();col ++) {//column
			List<Double> col_value = new ArrayList<Double>();
			for(int row = 0;row < matrix.size();row ++) {//row
				col_value.add(matrix.get(row).get(col));
			}
			if(getMedianValue) {
				double m = getMedian(col_value);
				output.add(m);
			}else {
				double a = getAve(col_value);
				output.add(a);
			}
		}
		return output;
	}
	
	/**
	 * get median number
	 * @param list
	 * @return
	 */
	public static double getMedian(List<Double> list) {
		Collections.sort(list,new Comparator<Double>(){

			@Override
			public int compare(Double o1, Double o2) {
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

		return list.get(middleIndex);
	}
	
	/**
	 * get average number
	 * @param list
	 * @return
	 */
	public static double getAve(List<Double> list) {
		DecimalFormat df = new DecimalFormat("0.00");
		double sum = 0;
		for(double data : list) {
			sum += data;
		}
		return Double.parseDouble(df.format(sum/list.size()));
	}
	
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
	
	/**
	 * 
	 * @param tableName
	 * @param dataset
	 * @return cost time for inserting records(ms)
	 * @throws SQLException
	 */
	public static double insert_data(String tableName,List<List<String>> dataset) throws SQLException {
		if(dataset == null)
			return -1;
		if(dataset.isEmpty())
			return -1;
		Connection conn = connectDB();
		conn.setAutoCommit(false);//manual commit
		String insertSql = "INSERT INTO `"+tableName + "` VALUES ( NULL,";//increment column
		for(int i = 0;i < dataset.get(0).size();i ++) {
			if(i != (dataset.get(0).size() - 1))
				insertSql += " ? ,";
			else
				insertSql += " ? )";
		}
		PreparedStatement prepStmt1 = conn.prepareStatement(insertSql);
		System.out.println("\n==================");
		System.out.println("insert "+dataset.size()+" records into table : "+tableName);
		System.out.println(insertSql);
		
		long start = System.currentTimeMillis();
		int count = 0;
		for(List<String> data : dataset) {
			count ++;
			for(int i = 1;i <= data.size();i ++) {
				prepStmt1.setString(i, data.get(i-1));
			}
			prepStmt1.addBatch();//batch process
			if(count % 10000 == 0) {
				prepStmt1.executeBatch();
				conn.commit();
				prepStmt1.clearBatch();
			}
		}
		prepStmt1.executeBatch();
		conn.commit();//commit
		long end = System.currentTimeMillis();
		System.out.println("==================\n");
		prepStmt1.close();
		conn.close();
		return (double)(end - start);
	}
	
	/**
	 * query 1
	 * SELECT R.A1,...,R.An,count(*) FROM R GROUP BY R.A1,...,R.An HAVING COUNT(*) > 1
	 * @param key
	 * @param tableName
	 * @return query time
	 * @throws SQLException
	 */
	public static double query_one(List<String> key,String tableName) throws SQLException {
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
		
		System.out.println("\n======================");
		System.out.println("executing query 1...");
		System.out.println(sql);
		
		long start = new Date().getTime();
		ResultSet rs = stmt.executeQuery(sql);
		rs.last();
		System.out.println("search result size: "+rs.getRow());
		long end = new Date().getTime();
		
		rs.close();
		stmt.close();
		conn.close();
		System.out.println("execution time(ms): "+(end - start));
		System.out.println("======================\n");
		return (double)(end - start);
	}
	
	/**
	 * query 2
	 * SELECT R.A1,...,R.An FROM R, R AS R1 WHERE R.A1 = R1.A1 AND ... AND R.An = R1.An AND R.id <> R1.id
	 * @param key
	 * @param tableName
	 * @return query time
	 * @throws SQLException
	 */
	public static double query_two(List<String> key,String tableName) throws SQLException {
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
		
		System.out.println("\n======================");
		System.out.println("executing query 2...");
		System.out.println(sql);
		
		long start = new Date().getTime();
		ResultSet rs = stmt.executeQuery(sql);
		rs.last();
		System.out.println("search result size: "+rs.getRow());
		long end = new Date().getTime();
		
		rs.close();
		stmt.close();
		conn.close();
		System.out.println("execution time(ms): "+(end - start));
		System.out.println("======================\n");
		return (double)(end - start);
	}
	
	/**
	 * delete all data from table with specific condition
	 * @param tableName
	 * @param dataset
	 * @param where_condition according condition to delete tuples
	 * @throws SQLException 
	 */
	public static void delete_data(String tableName,String where_condition) throws SQLException {
		Connection conn = connectDB();
		System.out.println("\n==================");
		System.out.println("delete inserted records from table : "+tableName);
		String delSql = "DELETE FROM `"+tableName + "` WHERE "+where_condition;//delete records

		System.out.println(delSql);
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(delSql);
		
		System.out.println("==================\n");
		stmt.close();
		conn.close();
	}
	
	/**
	 * specify a set of attributes (we call key) as an unique constraint on a table
	 * @param key
	 * @param tableName
	 * @throws SQLException
	 */
	public static void add_unique_constraint(List<String> key,String tableName,String unique_id) throws SQLException {
		Connection conn = connectDB();
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		
		
		String unique_constraint ="ALTER TABLE `"+tableName+"` ADD UNIQUE `"+unique_id+"` ( ";//create an unique constraint
		for(int i =0;i < key.size();i ++) {
			if(i != (key.size() -1))
				unique_constraint += "`"+key.get(i) + "`,";
			else
				unique_constraint += "`"+key.get(i) + "` )";
		}
		
		System.out.println("\n==================");
		System.out.println("create an unique constarint : ");
		System.out.println(unique_constraint);
		
		stmt.executeUpdate(unique_constraint);
		
		System.out.println("==================\n");
		
		stmt.close();
		conn.close();
	}
	
	/**
	 * remove an unique constraint on a table
	 * @param tableName
	 * @param unique_id
	 * @throws SQLException
	 */
	public static void del_unique_constraint(String tableName,String unique_id) throws SQLException {
		Connection conn = connectDB();
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		
		String del_unique_constraint = "DROP INDEX `"+unique_id+"` ON `"+tableName+"`";//delete an unique constraint
		
		System.out.println("\n==================");
		System.out.println("delete an unique constarint : ");
		System.out.println(del_unique_constraint);
		
		stmt.executeUpdate(del_unique_constraint);
		
		System.out.println("==================\n");
		
		stmt.close();
		conn.close();
	}
	
	/**
	 * 
	 * @param tableName
	 * @param R
	 * @throws SQLException
	 */
	public static void createTable(String tableName,List<String> R) throws SQLException {
		Connection conn =connectDB();
		String sql = "CREATE TABLE `"+tableName+"` (  \n";
		sql += "`id` int NOT NULL AUTO_INCREMENT,\n";
		for(int i = 0;i< R.size();i ++) {
			String columnName = R.get(i);
			sql += "`"+columnName +"` varchar(50),\n";
		}
		sql += "PRIMARY KEY (`id`)\n ) CHARSET=utf8mb3";
		System.out.println("\n======================");
		System.out.println("creating table with name : "+tableName+" | schema : "+R.toString());
		System.out.println(sql);
		System.out.println("======================\n");
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();
		conn.close();
	}
	
	public static void dropTable(String tableName) throws SQLException {
		Connection conn =connectDB();
		String sql = "DROP TABLE IF EXISTS `"+tableName+"`";
		System.out.println("\n======================");
		System.out.println("dropping table with name : "+tableName);
		System.out.println(sql);
		System.out.println("======================\n");
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();
		conn.close();
	}
	
	public static List<List<String>> gen_inserted_dataset(int row_num,int col_num){
		List<List<String>> dataset = new ArrayList<List<String>>();
		for(int i = 0;i < row_num;i ++) {
			List<String> data = new ArrayList<String>();
			for(int j = 0;j < col_num;j ++) {
				data.add(i+"_"+j);
			}
			dataset.add(data);
		}
		return dataset;
	}
	
	
	/**
	 * import subschemas with its keys from local file
	 * @param path file of subschema and its keys
	 * @return
	 * @throws IOException 
	 */
	public static Map<Set<String>,Set<Set<String>>> import_subschemas_with_keys_from_local(String path) throws IOException{
		Map<Set<String>,Set<Set<String>>> result = new HashMap<Set<String>,Set<Set<String>>>();//key = subschema, value = all minimal keys
		FileReader fr = new FileReader(path);
		BufferedReader br = new BufferedReader(fr);
		String line;
		Set<String> subschema = null;
		Set<Set<String>> keys = new HashSet<Set<String>>();
		while((line = br.readLine()) != null) {
			if(line.contains("BCNF schemata : [")) {//identify subschema info line
				int index1 = line.indexOf("[");
				int index2 = line.indexOf("]");
				String str1 = line.substring(index1+1, index2);
				String str2 = str1.replace(" ", "");
				String[] attr_array = str2.split(",");//all attributes of subschema
				subschema = new HashSet<String>();
				for(String a : attr_array) {
					subschema.add(a);
				}
				continue;
			}
			
			if(line.contains("FD [leftHand=[")) {//identify keys
				String str1 = line.split("=")[1];
				int index1 = str1.indexOf("[");
				int index2 = str1.indexOf("]");
				String str2 = str1.substring(index1 + 1, index2);
				String[] key_attrs = str2.replace(" ", "").split(",");
				Set<String> key = new HashSet<String>();
				for(String a : key_attrs) {
					key.add(a);
				}
				keys.add(key);
				continue;
			}
			
			
			if(line.contains("########################")) {//end flag of a subschema info
				if(subschema != null && !keys.isEmpty())
					result.put(subschema, keys);
				subschema = null;
				keys = new HashSet<Set<String>>();
			}
		}
		br.close();
		fr.close();
		return result;
	}
	
	public static List<List<String>> get_projection_on_subschema(List<String> subschema,String OriginTable) throws SQLException{
		List<List<String>> projection = new ArrayList<List<String>>();
		
		Connection conn = connectDB();
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
//		List<String> attrs = get_increasing_order_list(subschema);
		
		String str = "";
		for(int i = 0;i < subschema.size();i ++) {
			if(i != subschema.size() - 1)
				str += "`"+subschema.get(i)+"`, ";
			else
				str += "`"+subschema.get(i)+"` ";
		}
		String sql = "SELECT "+str+" FROM `"+OriginTable+"` GROUP BY "+str;
		System.out.println("\n======================");
		System.out.println("get projection on "+subschema.toString()+" from "+OriginTable);
		System.out.println(sql);
		
		ResultSet rs = stmt.executeQuery(sql);
		while(rs.next()) {
			int col_num = rs.getMetaData().getColumnCount();
			List<String> row = new ArrayList<String>();
			for(int i = 1;i <= col_num;i ++) {
				row.add(rs.getString(i));
			}
			projection.add(row);
		}
		System.out.println("projection row num : "+projection.size());
		System.out.println("======================\n");
		return projection;
	}
	
	/**
	 * according a set , generate a list with increasing order of value
	 * @param subschema
	 * @return
	 */
	public static List<String> get_increasing_order_list(Set<String> set){
		List<String> res = new ArrayList<String>(set);
		res.sort(new Comparator<String>() {//increasing order

			@Override
			public int compare(String o1, String o2) {
				int o1_int = Integer.parseInt(o1);
				int o2_int = Integer.parseInt(o2);
				if(o1_int > o2_int)
					return 1;
				else if(o1_int < o2_int)
					return -1;
				else
					return 0;
			}
			
		});
		return res;
	}
	
	public static void write_content_into_local(String output,List<String> line_list) throws IOException {
		File file = new File(output);
		if(!file.exists())
			file.createNewFile();
		FileWriter fw = new FileWriter(output,true);
		BufferedWriter bw = new BufferedWriter(fw);
		for(String line : line_list) {
			bw.write(line+"\n");
		}
		bw.close();
		fw.close();
	}
	
	/**
	 * execute query and insert experiments on projection of a table on "subschema"
	 * get average results on "round" times experiments
	 * @param round
	 * @param subschema
	 * @param keys
	 * @param OriginTable
	 * @param ProjTable
	 * @param insert_row_num_list
	 * @param col_num
	 * @return a list containing time of query 1, query 2, and three insertions
	 * @throws SQLException
	 * @throws IOException 
	 */
	public static List<Double> exe_single_schema_exp(String decomp_alg,String output, int round,Set<String> subschema, Set<Set<String>> keys,String OriginTable,String ProjTable, List<Integer> insert_row_num_list) throws SQLException, IOException {
		System.out.println("\n###############################\n");
		List<Double> result = new ArrayList<Double>();
		
		List<String> sub_schema = get_increasing_order_list(subschema);
		
		//create projection table on database
		createTable(ProjTable,sub_schema);
		
		//get projection table
		List<List<String>> proj_table_dataset = get_projection_on_subschema(sub_schema,OriginTable);
		
		//insert projection rows
		insert_data(ProjTable,proj_table_dataset);
		
		//add unique constraints
		List<String> all_uc_id = new ArrayList<String>();//record all unique constraint names
		int uc_id = 0;
		for(Set<String> k : keys) {
			List<String> key = get_increasing_order_list(k);
			String uc_name = "uc_"+ProjTable+"_"+uc_id ++;
			add_unique_constraint(key,ProjTable,uc_name);
			all_uc_id.add(uc_name);
		}
		
		//execute query one experiment
		List<Double> query_one_time = new ArrayList<Double>();
		for(Set<String> k : keys) {
			List<String> key = get_increasing_order_list(k);
			List<Double> cost_list = new ArrayList<Double>();
			for(int i = 0;i < round;i ++) {
				double cost = query_one(key,ProjTable);
				cost_list.add(cost);
			}
			query_one_time.add(getAve(cost_list));
		}
		double q1_ave = getAve(query_one_time);
		result.add(q1_ave);
		
		//query two experiment
		List<Double> query_two_time = new ArrayList<Double>();
		for(Set<String> k : keys) {
			List<String> key = get_increasing_order_list(k);
			List<Double> cost_list = new ArrayList<Double>();
			for(int i = 0;i < round;i ++) {
				double cost = query_two(key,ProjTable);
				cost_list.add(cost);
			}
			query_two_time.add(getAve(cost_list));
		}
		double q2_ave = getAve(query_two_time);
		result.add(q2_ave);
		
		//insertion experimenst
		for(int row_num : insert_row_num_list) {
			List<Double> cost_list = new ArrayList<Double>();
			List<List<String>> inserted_data = gen_inserted_dataset(row_num,proj_table_dataset.get(0).size());
			for(int i = 0;i < round;i ++) {
				double cost = insert_data(ProjTable,inserted_data);
				cost_list.add(cost);
				delete_data(ProjTable,"`id` > "+proj_table_dataset.size());
			}
			result.add(getAve(cost_list));
		}
		
		//drop the table
		dropTable(ProjTable);
		
		//output
		String stat = "";
		for(double a : result) {
			stat += ","+a;
		}
		String res_str1 = decomp_alg+","+keys.size()+","+proj_table_dataset.size()+stat;
		write_content_into_local(output+"query_update_result_"+decomp_alg+".txt",Arrays.asList(res_str1));
		
		String res_str = "\ndecomp_alg : "+decomp_alg+" | proj rows : "+proj_table_dataset.size()+" | sub_schema : "+sub_schema.toString()+" | key num : "+keys.size()+" | stat : "+result.toString()+"\n";
		System.out.println(res_str);
		System.out.println("###############################\n");
		
		return result;
	}
	
	public static void exe_exp(int sample_limit,String output,int round,String subschem_info_path,String decomp_alg,String OriginTable,String ProjTable, List<Integer> insert_row_num_list) throws IOException, SQLException {
		Map<Set<String>,Set<Set<String>>> subschema_info = SubschemaPerfExp.import_subschemas_with_keys_from_local(subschem_info_path);
		Iterator<Set<String>> iter = subschema_info.keySet().iterator();
		int count = 0;
		Map<Integer,Integer> sample_num_record = new HashMap<Integer,Integer>();//key = key number,value = count of subschema with specific key number that has done experiments
		while(iter.hasNext()) {
			Set<String> subschema = iter.next();
			Set<Set<String>> keys = subschema_info.get(subschema);
			if(sample_num_record.containsKey(keys.size())) {//check if the count exceed sample_limit for the key number
				int record = sample_num_record.get(keys.size());
				if(record < sample_limit) {//record
					sample_num_record.put(keys.size(), ++ record);
				}else {
					continue;
				}
			}else {
				sample_num_record.put(keys.size(), 1);
			}
			System.out.println("\n"+decomp_alg+" | "+ count + " | subschema : "+subschema.toString()+" | key num : "+keys.size()+"\n");
			exe_single_schema_exp(decomp_alg,output, round,subschema, keys,OriginTable,ProjTable+"_"+decomp_alg+"_"+count,insert_row_num_list);
			count ++;
		}
	}
	
	public static void main(String[] args) throws IOException, SQLException {
		int round = 10;//experiment repeat times
		int sample_limit = 10;//for each key number,we sample subschemas for experiments with limited numbers
		String output = "C:\\second experiment\\";
		String OriginTable = "lineitem";
		String ProjTable = "lineitem_proj";
		List<Integer> insert_row_num_list = Arrays.asList(60000,120000,180000);
		
		//CONF decomposition analysis
		new Thread(new Runnable() {

			@Override
			public void run() {
				String subschem_info_path1 = "C:\\second experiment\\CONF-BCNF-decomp.txt";
				String decomp_alg1 = "CONF";
				try {
					exe_exp(sample_limit,output,round,subschem_info_path1,decomp_alg1,OriginTable,ProjTable,insert_row_num_list);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
		}).start();
		
		
		//CONF-comp decomposition analysis
		new Thread(new Runnable() {

			@Override
			public void run() {
				String subschem_info_path2 = "C:\\second experiment\\CONF-comp-BCNF-decomp.txt";
				String decomp_alg2 = "CONF-comp";
				try {
					exe_exp(sample_limit,output,round,subschem_info_path2,decomp_alg2,OriginTable,ProjTable,insert_row_num_list);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
		}).start();
		
	}

}
