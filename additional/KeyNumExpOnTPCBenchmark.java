package additional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mysql.cj.x.protobuf.MysqlxDatatypes.Array;

import conf.Constant;
import util.StatMinKeysOnSchemaAndFDs;
/**
 * we study key numbers influence on tpc benchmark
 *
 */
public class KeyNumExpOnTPCBenchmark {
	
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
	    String DB_URL = "jdbc:mysql://localhost:3306/tpch?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
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
		String insertSql = "INSERT INTO `"+tableName + "` VALUES (";
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
	 * just insert one tuple
	 * @param tableName
	 * @param row
	 * @param attribute_type if it is string we set "string" in relative position,if int we set "int",if date fromat we set "date"
	 * for a tuple ['a',2] the attribute_type should be ['string','int']
 	 * @return cost time for inserting records(ms)
	 * @throws SQLException
	 */
	public static double insert_one_tuple(String tableName,List<String> row,List<String> attribute_type) throws SQLException {
		if(row == null)
			return -1;
		if(row.isEmpty())
			return -1;
		Connection conn = connectDB();
		String insertSql = "INSERT INTO `"+tableName + "` VALUES (";
		for(int i = 0;i < row.size();i ++) {
			if(i != (row.size() - 1))
				insertSql += (attribute_type.get(i).equals("int") ? row.get(i) : "'"+row.get(i)+"'")+" ,";
			else
				insertSql += (attribute_type.get(i).equals("int") ? row.get(i) : "'"+row.get(i)+"'")+" )";
		}
		Statement Stmt = conn.createStatement();
//		System.out.println("\n==================");
//		System.out.println("insert only one row into table : "+tableName);
//		System.out.println(insertSql);
		
		long start = System.currentTimeMillis();
		Stmt.executeUpdate(insertSql);
		long end = System.currentTimeMillis();
//		System.out.println("==================\n");
		Stmt.close();
		conn.close();
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
	 * for TPC-H, we have 22 query sqls.
	 * @param sql id (1-22)
	 * @param sql
	 * @return query execution time
	 * @throws SQLException
	 */
	public static double query(String sql_id,String sql) throws SQLException {
		Connection conn = connectDB();
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		
		System.out.println("\n==================");
		System.out.println("executing sql with id ["+sql_id+"]");
		System.out.println(sql);
		
		long start = new Date().getTime();
		String[] sqls = sql.split(";");
		for(String s : sqls) {
			String empty_str_test = s.replace(" ", "").replace("\n", "");;
			if(!"".equals(empty_str_test))
				stmt.execute(s);
		}
		long end = new Date().getTime();
		
		stmt.close();
		conn.close();
		System.out.println("execution time(ms): "+(end - start));
		System.out.println("==================\n");
		return (double)(end - start);
	}
	
	/**
	 * the refresh function is like :
	 * LOOP (SF * 1500) TIMES
	 * 	INSERT a new row into the ORDERS table
	 * 	LOOP RANDOM(1, 7) TIMES
	 * 		INSERT a new row into the LINEITEM table
	 * 	END LOOP
	 * END LOOP
	 * But for better showing influence of key number, we SF and second loop time as variable
	 * SF : scale factor that shows the whole data quantity, its value like 1G,2G,...
	 * @param first_loop_time
	 * @param second_loop_time
	 * @param orders_data all needed inserted data into orders
	 * @param orders_attr_type if it is string we set "string" in relative position,if int we set "int"
	 * for a tuple ['a',2] the attribute_type should be ['string','int']
	 * @param lineitem_data 
	 * @param lineitem_attr_type
	 * @return insertion execute time
	 * @throws SQLException
	 */
	public static double refreshFunction(int first_loop_time, int second_loop_time,List<List<String>> orders_data,List<String> orders_attr_type,List<List<String>> lineitem_data,List<String> lineitem_attr_type) throws SQLException {
		System.out.println("\n==================");
		System.out.println("executing refresh function(RF1)...");
		
		long start = System.currentTimeMillis();
		int count = -1;
		for(int i = 0;i < first_loop_time;i ++) {
			insert_one_tuple("orders",orders_data.get(i),orders_attr_type);
			for(int j = 0;j < second_loop_time;j ++) {
				count ++;
				insert_one_tuple("lineitem",lineitem_data.get(count),lineitem_attr_type);
			}
		}
		long end = System.currentTimeMillis();
		
		System.out.println("execution time(ms): "+(end - start));
		System.out.println("==================\n");
		return (double)(end - start);
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
	 * @param col_num a data's column number
	 * @param row_num data number of the data set
	 * @param attr_type
	 * @return a data set with fixed col_num and row_num
	 */
	public static List<List<String>> gen_inserted_dataset_for_customer(int row_num,int col_num,List<String> attr_type){
		List<List<String>> dataset = new ArrayList<List<String>>();
		for(int i = 0;i < row_num;i ++) {
			List<String> data = new ArrayList<String>();
			for(int j = 0;j < col_num;j ++) {
				if(attr_type.get(j).equals("string"))
					data.add(i+"_"+j);
				else if(attr_type.get(j).equals("date")) {
					data.add("1996-01-01");
				}else {//if column value should be integer, we start to generate value from -1, then -2,-3,...
					if(j == 2){//nation key [0,24]
						Random rand = new Random();
						int rand_num = rand.nextInt(25);
						data.add(rand_num+"");
						continue;
					}
					if(dataset.isEmpty()) {
						data.add("6000001");
					}else {
						int last_tuple_value = Integer.parseInt(dataset.get(dataset.size() - 1).get(j));
						data.add((last_tuple_value + 1) + "");
					}
				}
			}
			dataset.add(data);
		}
		return dataset;
	}
	
	/**
	 * 
	 * @param col_num a data's column number
	 * @param row_num data number of the data set
	 * @param attr_type
	 * @return a data set with fixed col_num and row_num
	 */
	public static List<List<String>> gen_inserted_dataset_for_nation(int row_num,int col_num,List<String> attr_type){
		List<List<String>> dataset = new ArrayList<List<String>>();
		for(int i = 0;i < row_num;i ++) {
			List<String> data = new ArrayList<String>();
			for(int j = 0;j < col_num;j ++) {
				if(attr_type.get(j).equals("string"))
					data.add(i+"_"+j);
				else if(attr_type.get(j).equals("date")) {
					data.add("1996-01-01");
				}else {//if column value should be integer, we start to generate value from -1, then -2,-3,...
					if(j == 2){//region key [0,4]
						Random rand = new Random();
						int rand_num = rand.nextInt(5);
						data.add(rand_num+"");
						continue;
					}
					if(dataset.isEmpty()) {
						data.add("6000001");
					}else {
						int last_tuple_value = Integer.parseInt(dataset.get(dataset.size() - 1).get(j));
						data.add((last_tuple_value + 1) + "");
					}
				}
			}
			dataset.add(data);
		}
		return dataset;
	}
	
	public static List<List<String>> gen_inserted_dataset_for_part(int row_num,int col_num,List<String> attr_type){
		List<List<String>> dataset = new ArrayList<List<String>>();
		for(int i = 0;i < row_num;i ++) {
			List<String> data = new ArrayList<String>();
			for(int j = 0;j < col_num;j ++) {
				if(attr_type.get(j).equals("string"))
					if(j == 3 || j == 5)//p_brand p_container
						data.add("a");
					else
						data.add(i+"_"+j);
				else if(attr_type.get(j).equals("date")) {
					data.add("1996-01-01");
				}else {//if column value should be integer, we start to generate value from -1, then -2,-3,...
					if(dataset.isEmpty()) {
						data.add("6000001");
					}else {
						int last_tuple_value = Integer.parseInt(dataset.get(dataset.size() - 1).get(j));
						data.add((last_tuple_value + 1) + "");
					}
				}
			}
			dataset.add(data);
		}
		return dataset;
	}
	
	public static List<List<String>> gen_inserted_dataset_for_partsupp(int row_num,int col_num,List<String> attr_type){
		List<List<String>> dataset = new ArrayList<List<String>>();
		int partkey_count = 1;
		for(int i = 0;i < row_num;i ++) {
			List<String> data = new ArrayList<String>();
			for(int j = 0;j < col_num;j ++) {
				if(attr_type.get(j).equals("string"))
					data.add(i+"_"+j);
				else if(attr_type.get(j).equals("date")) {
					data.add("1996-01-01");
				}else {//if column value should be integer, we start to generate value from -1, then -2,-3,...
					if(j == 0) {//ps_parkey [1,200000]
//						Random rand = new Random();
//						int rand_num = rand.nextInt(200000) + 1;
						if(dataset.isEmpty()) {
							data.add("1");
						}else if (partkey_count < 5){
							int last_tuple_value = Integer.parseInt(dataset.get(dataset.size() - 1).get(j));
							data.add(last_tuple_value + "");
						}else {
							partkey_count = 1;
							int last_tuple_value = Integer.parseInt(dataset.get(dataset.size() - 1).get(j));
							data.add((last_tuple_value + 1) + "");
						}
						partkey_count ++;
						continue;	
					}
					if(j == 1) {//ps_suppkey [1,10000]
						if(dataset.isEmpty()) {
							String partkey_v = data.get(0);
							data.add(partkey_v);
						}else {
							int last_tuple_value = Integer.parseInt(dataset.get(dataset.size() - 1).get(j));
							if(partkey_count == 2){
								if(Integer.parseInt(data.get(0)) > 10000) {
									if(Integer.parseInt(data.get(0))%1000 == 0)
										data.add((Integer.parseInt(data.get(0))%1000 + 2)+"");
									else
										data.add((Integer.parseInt(data.get(0))%1000)+"");
								}else
									data.add(data.get(0));
							}else if((last_tuple_value + 1000) > 10000 && partkey_count <= 5){
								if((last_tuple_value + 1000 - 10000) == 0)
									data.add((last_tuple_value + 1000 - 10000 + 2)+"");
								else
									data.add((last_tuple_value + 1000 - 10000)+"");
							}else {
								data.add((last_tuple_value + 1000)+"");
							}
						}
						
						continue;
					}
					if(dataset.isEmpty()) {
						data.add("6000001");
					}else {
						int last_tuple_value = Integer.parseInt(dataset.get(dataset.size() - 1).get(j));
						data.add((last_tuple_value + 1) + "");
					}
				}
			}
			dataset.add(data);
		}
		return dataset;
	}
	
	public static List<List<String>> gen_inserted_dataset_for_region(int row_num,int col_num,List<String> attr_type){
		List<List<String>> dataset = new ArrayList<List<String>>();
		for(int i = 0;i < row_num;i ++) {
			List<String> data = new ArrayList<String>();
			for(int j = 0;j < col_num;j ++) {
				if(attr_type.get(j).equals("string"))
					data.add(i+"_"+j);
				else if(attr_type.get(j).equals("date")) {
					data.add("1996-01-01");
				}else {//if column value should be integer, we start to generate value from -1, then -2,-3,...
					if(dataset.isEmpty()) {
						data.add("6000001");
					}else {
						int last_tuple_value = Integer.parseInt(dataset.get(dataset.size() - 1).get(j));
						data.add((last_tuple_value + 1) + "");
					}
				}
			}
			dataset.add(data);
		}
		return dataset;
	}
	
	public static List<List<String>> gen_inserted_dataset_for_supplier(int row_num,int col_num,List<String> attr_type){
		List<List<String>> dataset = new ArrayList<List<String>>();
		for(int i = 0;i < row_num;i ++) {
			List<String> data = new ArrayList<String>();
			for(int j = 0;j < col_num;j ++) {
				if(attr_type.get(j).equals("string"))
					data.add(i+"_"+j);
				else if(attr_type.get(j).equals("date")) {
					data.add("1996-01-01");
				}else {//if column value should be integer, we start to generate value 
					if(j == 1) {//nation key [0-24]
						Random rand = new Random();
						int rand_num = rand.nextInt(25);
						data.add(rand_num+"");
						continue;
					}
					if(dataset.isEmpty()) {
						data.add("6000001");
					}else {
						int last_tuple_value = Integer.parseInt(dataset.get(dataset.size() - 1).get(j));
						data.add((last_tuple_value + 1) + "");
					}
				}
			}
			dataset.add(data);
		}
		return dataset;
	}
	
	/**
	 * 
	 * @param col_num a data's column number
	 * @param row_num data number of the data set
	 * @param attr_type
	 * @return a data set with fixed col_num and row_num
	 */
	public static List<List<String>> gen_inserted_dataset_for_orders(int row_num,int col_num,List<String> attr_type){
		List<List<String>> dataset = new ArrayList<List<String>>();
		for(int i = 0;i < row_num;i ++) {
			List<String> data = new ArrayList<String>();
			for(int j = 0;j < col_num;j ++) {
				if(attr_type.get(j).equals("string"))
					if(j == 6)
						data.add("a");//satisfy char with length 1
					else
						data.add(i+"_"+j);
				else if(attr_type.get(j).equals("date")) {
					data.add("1996-01-01");
				}else {//if column value should be integer, we start to generate value from -1, then -2,-3,...
					if(j == 2){//orders table's o_custkey should betweent 1-1000
						Random rand = new Random();
						int rand_num = rand.nextInt(1000) + 1;
						data.add(rand_num+"");
						continue;
					}
					if(dataset.isEmpty()) {
						data.add("6000001");
					}else {
						int last_tuple_value = Integer.parseInt(dataset.get(dataset.size() - 1).get(j));
						data.add((last_tuple_value + 1) + "");
					}
				}
			}
			dataset.add(data);
		}
		return dataset;
	}
	
	public static List<List<String>> gen_inserted_dataset_for_lineitem(int row_num,int col_num,List<String> attr_type){
		List<List<String>> dataset = new ArrayList<List<String>>();
		for(int i = 0;i < row_num;i ++) {
			List<String> data = new ArrayList<String>();
			for(int j = 0;j < col_num;j ++) {
				if(attr_type.get(j).equals("string"))
					if(j == 6 || j == 8)
						data.add("a");//satisfy char with length 1
					else
						data.add(i+"_"+j);
				else if(attr_type.get(j).equals("date")) {
					data.add("1996-01-01");
				}else {//if column value should be integer, we start to generate value from -1, then -2,-3,...
					if(j == 7){//lineitem table's l_partkey should be integer between 1-250
//						Random rand = new Random();
//						int rand_num = rand.nextInt(250) + 1;
						data.add("1");
						continue;
					}
					if(j == 4) {//lineitem table's l_suppkey
						List<Integer> set = Arrays.asList(2,2502,5002,7502);
						Random rand = new Random();
						int rand_i = rand.nextInt(set.size());
						data.add(set.get(rand_i)+"");
						continue;
					}
					if(j == 1){//l_orderkey value should be in value domain of orders table's o_orderkey
						data.add("6000001");
						continue;
					}
					if(dataset.isEmpty()) {
						data.add("6000001");
					}else {
						int last_tuple_value = Integer.parseInt(dataset.get(dataset.size() - 1).get(j));
						data.add((last_tuple_value + 1) + "");
					}
				}
			}
			dataset.add(data);
		}
		return dataset;
	}
	
	
	/**
	 * 
	 * @param round
	 * @param first_loop_time
	 * @param second_loop_time
	 * @param orders_attr_type
	 * @param lineitem_attr_type
	 * @param mostKeyNum
	 * @param allTables
	 * @param table_key_map
	 * @param query_sqls_map
	 * @param query_names
	 * @return
	 * @throws SQLException
	 */
	public static List<Double> single_query_and_update_exp_with_fixed_most_key_num(int round,List<Integer> insert_tuples,int first_loop_time, List<Integer> second_loop_time,Map<String,List<String>> attr_type_map,int mostKeyNum,List<String> allTables,Map<String,List<List<String>>> table_key_map,Map<String,String> query_sqls_map,List<String> query_names) throws SQLException {
		Map<String,List<List<String>>> table_key_map_for_exp = new HashMap<String,List<List<String>>>();//key = table name,value = the most possible number keys of all keys of the table
		for(String table : allTables) {
			List<List<String>> selected_keys = new ArrayList<List<String>>();//keys we used for experiments with the table
			Random random = new Random();
			List<List<String>> table_keys = table_key_map.get(table);//all keys of the table except for primary key
			while(selected_keys.size() != mostKeyNum && selected_keys.size() != table_keys.size()) {
				int i = random.nextInt(table_keys.size());
				if(!selected_keys.contains(table_keys.get(i)))
					selected_keys.add(table_keys.get(i));
			}
			table_key_map_for_exp.put(table, selected_keys);
		}
		
		
		System.out.println("##############");
		System.out.println("most key number | "+mostKeyNum);
		
		//add n key numbers in form of unique constraints for each table
		Map<String,List<String>> table_uc_map = new HashMap<String,List<String>>();//we record all unique index names for each table
		Iterator<String> iter_unique = table_key_map_for_exp.keySet().iterator();
		while(iter_unique.hasNext()) {
			String t = iter_unique.next();//specific table
			List<List<String>> t_keys_for_exp = table_key_map_for_exp.get(t);
			List<String> uc_for_table = new ArrayList<String>();
			System.out.println("adding unique constraint for table : "+t+" | uc num : "+t_keys_for_exp.size());
			for(int i = 0;i < t_keys_for_exp.size();i ++) {
				List<String> key = t_keys_for_exp.get(i);
				String ucName = "uc_"+t+"_"+i;
				uc_for_table.add(ucName);
				System.out.println("adding unique constraint ["+ucName+"]...");
				add_unique_constraint(key, t, ucName);	//for lineitem, we set unique id as uc_lineitem_0,uc_lineitem_1,....
			}
			table_uc_map.put(t, uc_for_table);
			System.out.println("finished to add "+t_keys_for_exp.size()+" unqiue constraints for table ["+t+"]...");
		}
		
		
		//query experiment
		List<Double> query_time = new ArrayList<Double>();
		for(String q_name : query_names) {
			String sql = query_sqls_map.get(q_name);//getting query sql with query name,we start from q1 to q22
			double q_time = query(q_name,sql);
			query_time.add(q_time);
		}
		
		
		//refresh function experiment
		List<Double> rf_update_time = new ArrayList<Double>();
		for(int second_loop_t : second_loop_time) {
			//first loop times : first_loop_time; second loop times : second_loop_t
			//then we generate data for orders table : first_loop_time rows
			//we generate data for lineitem table : first_loop_time * second_loop_t
			List<List<String>> orders_data = gen_inserted_dataset_for_orders(first_loop_time,9,attr_type_map.get("orders"));
			List<List<String>> lineitem_data = gen_inserted_dataset_for_lineitem(first_loop_time * second_loop_t,16,attr_type_map.get("lineitem"));
			double rf_time = refreshFunction(first_loop_time, second_loop_t,orders_data,attr_type_map.get("orders"),lineitem_data,attr_type_map.get("lineitem"));
			rf_update_time.add(rf_time);
			//delete all inserted records
			delete_data("orders"," `o_orderkey` > 6000000");
			delete_data("lineitem"," `l_orderkey` > 6000000");
		}
		
		//insertion experiment
		//insert 1k/2k/3k tuples for each table
		List<Double> insert_time = new ArrayList<Double>();
		for(int insertion_row : insert_tuples) {
			List<List<String>> orders_data = gen_inserted_dataset_for_orders(insertion_row,9,attr_type_map.get("orders"));
			List<List<String>> lineitem_data = gen_inserted_dataset_for_lineitem(insertion_row,16,attr_type_map.get("lineitem"));
			List<List<String>> customer_data = gen_inserted_dataset_for_customer(insertion_row,8,attr_type_map.get("customer"));
			List<List<String>> nation_data = gen_inserted_dataset_for_nation(insertion_row,4,attr_type_map.get("nation"));
			List<List<String>> part_data = gen_inserted_dataset_for_part(insertion_row,9,attr_type_map.get("part"));
			List<List<String>> partsupp_data = gen_inserted_dataset_for_partsupp(insertion_row,5,attr_type_map.get("partsupp"));
			List<List<String>> region_data = gen_inserted_dataset_for_region(insertion_row,3,attr_type_map.get("region"));
			List<List<String>> supplier_data = gen_inserted_dataset_for_supplier(insertion_row,7,attr_type_map.get("supplier"));
			Map<String,List<List<String>>> table_data = new HashMap<String,List<List<String>>>();
			table_data.put("orders", orders_data);
			table_data.put("lineitem", lineitem_data);
			table_data.put("customer", customer_data);
			table_data.put("nation", nation_data);
			table_data.put("part", part_data);
			table_data.put("partsupp", partsupp_data);
			table_data.put("region", region_data);
			table_data.put("supplier", supplier_data);
			
			long start = System.currentTimeMillis();
			for(String table : allTables) {
				for(int i = 0;i < insertion_row;i ++) {
					insert_one_tuple(table,table_data.get(table).get(i),attr_type_map.get(table));
				}
			}
			long end = System.currentTimeMillis();
			insert_time.add((double)(end-start));//record inserting time of  all tuples of all tables
			
			//deleting inserted tuples
			delete_data("orders"," `o_orderkey` > 6000000");
			delete_data("lineitem"," `l_orderkey` > 6000000");
			delete_data("customer"," `c_custkey` > 6000000");
			delete_data("nation"," `n_nationkey` > 6000000");
			delete_data("part"," `p_partkey` > 6000000");
			delete_data("partsupp"," `ps_availqty` > 6000000");
			delete_data("region"," `r_regionkey` > 6000000");
			delete_data("supplier"," `s_suppkey` > 6000000");
		}
		
		//delete all unique constraints
		Iterator<String> iter_table = table_uc_map.keySet().iterator();
		while(iter_table.hasNext()) {
			String t = iter_table.next();//table
			List<String> uc_for_t = table_uc_map.get(t);
			System.out.println("deleting uniques for table : ["+t+"]...");
			for(String uc_name : uc_for_t) {
				System.out.println("deleting unique constraint : ["+uc_name+"]...");
				del_unique_constraint(t, uc_name);
			}
			System.out.println("--------------------------");
		}
		
		
		//stat query time for each query , refresh function and insertion time
		List<Double> result_list = new ArrayList<Double>();
		String result = "";
		for(Double q_t : query_time) {
			result += q_t+",";
			result_list.add(q_t);
		}
		
		for(int i = 0;i < rf_update_time.size();i ++) {
			result_list.add(rf_update_time.get(i));
			result += rf_update_time.get(i)+",";
		}
		
		for(int i = 0;i < insert_time.size();i ++) {
			result_list.add(insert_time.get(i));
			if(i != insert_time.size() - 1)
				result += insert_time.get(i)+",";
			else
				result += insert_time.get(i);
		}
		
		System.out.println("\nround = "+round+" | most key num = "+mostKeyNum+" stats : "+result);
		System.out.println("##############");
		return result_list;
	}
	
	/**
	 * execute specific round random key experiments
	 * @param round experiment rounds with fixed key number
	 * @param tableName
	 * @param table_col_num
	 * @param table_row_num
	 * @param inserted_row_num
	 * @param keyNum
	 * @param all_keys
	 * @return a result list in form of <q1_median,q1_ave,q2_median,q2_ave,
	 * insert_time1_median,insert_time1_ave,insert_time2_median,insert_time2_ave,insert_time3_median,insert_time3_ave>
	 * @throws SQLException
	 */
	public static List<List<Double>> query_and_update_exp_with_fixed_most_key_num(int round,List<Integer> insert_tuples,int first_loop_time, List<Integer> second_loop_time,Map<String,List<String>> attr_type_map,int mostKeyNum,List<String> allTables,Map<String,List<List<String>>> table_key_map,Map<String,String> query_sqls_map,List<String> query_names) throws SQLException{
		List<List<Double>> output = new ArrayList<List<Double>>();
		
		List<List<Double>> record = new ArrayList<List<Double>>();//record all results
		for(int i = 0;i < round;i ++) {
			System.out.println("round : "+i+" | most key num : " + mostKeyNum);
			List<Double> res = single_query_and_update_exp_with_fixed_most_key_num(i,insert_tuples,first_loop_time, second_loop_time,attr_type_map,mostKeyNum,allTables,table_key_map,query_sqls_map,query_names);
			record.add(res);
			System.out.println("******************\n");
		}
		
		List<Double> ave_result = getEachColValue(record,false);
		List<Double> median_result = getEachColValue(record,true);
		
		
		output.add(ave_result);
		output.add(median_result);
		
		System.out.println("\nkey num = "+mostKeyNum+" average stats : "+ave_result.toString());
		System.out.println("\nkey num = "+mostKeyNum+" median stats : "+median_result.toString());
		System.out.println("==========================");
		return output;
	}
	
	public static List<Object> query_and_update_exp(int round,List<Integer> insert_tuples,int first_loop_time, List<Integer> second_loop_time,Map<String,List<String>> attr_type_map,List<Integer> most_key_num_list,List<String> allTables,Map<String,List<List<String>>> table_key_map,Map<String,String> query_sqls_map,List<String> query_names) throws SQLException{
		List<List<Double>> all_ave_results = new ArrayList<List<Double>>();
		List<List<Double>> all_median_results = new ArrayList<List<Double>>();
		for(int most_key_num : most_key_num_list) {
			System.out.println("most key num : "+most_key_num);
			List<List<Double>> res = query_and_update_exp_with_fixed_most_key_num(round,insert_tuples,first_loop_time,second_loop_time,attr_type_map,most_key_num,allTables,table_key_map,query_sqls_map,query_names);
			all_ave_results.add(res.get(0));
			all_median_results.add(res.get(1));
		}
		List<Object> all_results = new ArrayList<Object>();
		all_results.add(all_ave_results);
		all_results.add(all_median_results);
		return all_results;
	}
	
	public static Map<String,List<List<String>>> get_table_uniques_from_TPCH(){
		Map<String,List<List<String>>> table_key_map = new HashMap<String,List<List<String>>>();
		
		//customer
		List<List<String>> keys_cus = new ArrayList<List<String>>();
		List<String> k1_cus = Arrays.asList("c_name");
		List<String> k2_cus = Arrays.asList("c_address");
		List<String> k3_cus = Arrays.asList("c_mktsegment","c_phone");
		List<String> k4_cus = Arrays.asList("c_nationkey","c_phone","c_comment");
		keys_cus.add(k1_cus);keys_cus.add(k2_cus);keys_cus.add(k3_cus);keys_cus.add(k4_cus);
		table_key_map.put("customer", keys_cus);
		
		//lineitem
		List<List<String>> keys_line = new ArrayList<List<String>>();
		List<String> k1_line = Arrays.asList("l_orderkey","l_partkey","l_comment");
		List<String> k2_line = Arrays.asList("l_shipdate","l_orderkey","l_suppkey","l_comment");
		List<String> k3_line = Arrays.asList("l_orderkey","l_suppkey","l_quantity","l_comment");
		List<String> k4_line = Arrays.asList("l_orderkey","l_suppkey","l_comment");
		List<String> k5_line = Arrays.asList("l_orderkey","l_suppkey","l_receiptdate","l_comment");
		keys_line.add(k1_line);keys_line.add(k2_line);keys_line.add(k3_line);keys_line.add(k4_line);keys_line.add(k5_line);
		table_key_map.put("lineitem", keys_line);
		
		//nation
		List<List<String>> keys_nation = new ArrayList<List<String>>();
		List<String> k1_nation = Arrays.asList("n_name");
		List<String> k2_nation = Arrays.asList("n_comment");
		keys_nation.add(k1_nation);keys_nation.add(k2_nation);
		table_key_map.put("nation", keys_nation);
		
		//orders
		List<List<String>> keys_orders = new ArrayList<List<String>>();
		List<String> k1_orders = Arrays.asList("o_orderdate","o_custkey","o_clerk");
		List<String> k2_orders = Arrays.asList("o_orderdate","o_clerk","o_comment");
		List<String> k3_orders = Arrays.asList("o_custkey","o_clerk","o_comment");
		List<String> k4_orders = Arrays.asList("o_orderdate","o_custkey","o_comment");
		keys_orders.add(k1_orders);keys_orders.add(k2_orders);keys_orders.add(k3_orders);keys_orders.add(k4_orders);
		table_key_map.put("orders", keys_orders);
		
		//part
		List<List<String>> keys_part = new ArrayList<List<String>>();
		List<String> k1_part = Arrays.asList("p_name","p_mfgr");
		keys_part.add(k1_part);
		table_key_map.put("part", keys_part);
		
		//partsupp
		List<List<String>> keys_partsupp = new ArrayList<List<String>>();
		List<String> k1_partsupp = Arrays.asList("ps_supplycost","ps_comment");
		keys_partsupp.add(k1_partsupp);
		table_key_map.put("partsupp", keys_partsupp);
		
		//region
		List<List<String>> keys_region = new ArrayList<List<String>>();
		List<String> k1_region = Arrays.asList("r_name");
		List<String> k2_region = Arrays.asList("r_comment");
		keys_region.add(k1_region);keys_region.add(k2_region);
		table_key_map.put("region", keys_region);
		
		//supplier
		List<List<String>> keys_supplier = new ArrayList<List<String>>();
		List<String> k1_supplier = Arrays.asList("s_name");
		List<String> k2_supplier = Arrays.asList("s_address");
		List<String> k3_supplier = Arrays.asList("s_phone");
		List<String> k4_supplier = Arrays.asList("s_nationkey","s_comment");
		keys_supplier.add(k1_supplier);keys_supplier.add(k2_supplier);keys_supplier.add(k3_supplier);keys_supplier.add(k4_supplier);
		table_key_map.put("supplier", keys_supplier);
		
		
		return table_key_map;
	}
	
	/**
	 * get 22 sqls from local file
	 * @param path
	 * @return a map which key = sql id, value = sql
	 * @throws IOException 
	 */
	public static void get_query_sqls_map(String root_dir,Map<String,String> query_sqls_map) throws IOException{
		File f = new File(root_dir);
		if(f.isDirectory()) {
			for(File sql_file : f.listFiles()) {
				if(sql_file.isFile()) {
					String sql_id = sql_file.getName().split("\\.")[0];//q1-q22
					String sql = read_content_from_local(sql_file.getAbsolutePath());
					query_sqls_map.put(sql_id, sql);
				}else {
					get_query_sqls_map(sql_file.getAbsolutePath(),query_sqls_map);
				}
			}
		}else {
			String sql_id = f.getName().split(".")[0];//q1-q22
			String sql = read_content_from_local(f.getAbsolutePath());
			query_sqls_map.put(sql_id, sql);
		}
	}
	
	public static String read_content_from_local(String path) throws IOException {
		String output = "";
		FileReader fr = new FileReader(path);
		BufferedReader br = new BufferedReader(fr);
		String line;
		while((line = br.readLine()) != null) {
			output += line+"\n";
		}
		br.close();
		fr.close();
		return output;
	}
	
	public static Map<String,List<String>> get_attr_type_map() {
		List<String> customer_attr_type = Arrays.asList("int","string","int","string","string","string","int","string");
		List<String> orders_attr_type = Arrays.asList("date","int","int","string","int","string","string","int","string");
		List<String> lineitem_attr_type = Arrays.asList("date","int","int","int","int","int","string","int","string","int","date","date","string","int","string","string");
		List<String> nation_attr_type = Arrays.asList("int","string","int","string");
		List<String> part_attr_type = Arrays.asList("int","string","int","string","string","string","string","int","string");
		List<String> partsupp_attr_type = Arrays.asList("int","int","int","int","string");
		List<String> region_attr_type = Arrays.asList("int","string","string");
		List<String> supplier_attr_type = Arrays.asList("int","int","string","string","string","string","int");
		
		Map<String,List<String>> attr_type_map = new HashMap<String,List<String>>();
		attr_type_map.put("customer", customer_attr_type);
		attr_type_map.put("orders", orders_attr_type);
		attr_type_map.put("lineitem", lineitem_attr_type);
		attr_type_map.put("nation", nation_attr_type);
		attr_type_map.put("part", part_attr_type);
		attr_type_map.put("partsupp", partsupp_attr_type);
		attr_type_map.put("region", region_attr_type);
		attr_type_map.put("supplier", supplier_attr_type);
		
		return attr_type_map;
	}
	
	
	
	public static void main(String[] args) throws IOException, SQLException {
		
		List<Integer> most_key_num_list = Arrays.asList(0,1,2,3,4,5);
		int round = 1;//for fixed key number, randomly generate 'round' different key sets to experiment
		
		int first_loop_time = 2*1500;//RF exp
		List<Integer> second_loop_time = Arrays.asList(1,2,3,4,5,6,7);
		
		List<Integer> insert_tuples = Arrays.asList(60000,120000,180000);//insertion exp, insert 1k/2k/3k tuples for each table
		
		Map<String,List<String>> attr_type_map = get_attr_type_map();
		
		List<String> allTables = Arrays.asList("orders","customer","lineitem","nation","part","partsupp","region","supplier");
		Map<String,List<List<String>>> table_key_map = get_table_uniques_from_TPCH();
		Map<String,String> query_sqls_map = new HashMap<String,String>();
		String sql_path = "C:\\Users\\Admin\\Desktop\\SIGMOD revision\\first experiment\\tcph-query-sql";
		get_query_sqls_map(sql_path,query_sqls_map);
		List<String> query_names = new ArrayList<String>();
		for(int i = 1;i <= 22;i ++) {
			query_names.add("q"+i);
		}
		
		
		//start experiment
		List<Object> results = KeyNumExpOnTPCBenchmark.query_and_update_exp(round,insert_tuples,first_loop_time,second_loop_time,attr_type_map,most_key_num_list,allTables,table_key_map,query_sqls_map,query_names);
		List<List<Double>> ave_res = (List<List<Double>>) results.get(0);
		List<List<Double>> median_res = (List<List<Double>>) results.get(1);
		System.out.println("\n#######################");
		System.out.println("final stats (average) : \n");
		for(int i = 0;i < ave_res.size();i ++) {
			System.out.println("most key num = "+most_key_num_list.get(i)+" | stat : "+ave_res.get(i)+"\n");
		}
		System.out.println("==============================");
		System.out.println("final stats (median) : \n");
		for(int i = 0;i < median_res.size();i ++) {
			System.out.println("most key num = "+most_key_num_list.get(i)+" | stat : "+median_res.get(i)+"\n");
		}
		System.out.println("\n#######################");
		
	}

}
