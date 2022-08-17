package conf;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * some tools here
 *
 */
public class Utils {
	
	/**
	 * 
	 * @param add data set address
	 * @param split
	 * @return relation
	 * @throws IOException
	 */
	public static List<List<String>> import_dataset(String add, String split) throws IOException{
		List<List<String>> r = new ArrayList<List<String>>();
		FileReader fr = new FileReader(add);
		BufferedReader br = new BufferedReader(fr);
		String line;
		while((line = br.readLine()) != null) {
			if(Constant.dataset_name.equals("uniprot")) {//special care
				List<String> tuple = Utils.split(line,'"');
				if(tuple.size() != Constant.col_num) {
					System.out.println("split error at "+line);
					System.out.println(tuple.size()+" != "+Constant.col_num);
					System.exit(1);
				}
				if(!r.contains(tuple))
					r.add(tuple);
			}else {
				String[] data = line.split(split);
				if(data.length != Constant.col_num) {
					System.out.println("data split error!");
					System.exit(1);
				}
				List<String> tuple = new ArrayList<String>();
				for(String a : data) {
					tuple.add(a);
				}
				if(!r.contains(tuple))
					r.add(tuple);
			}
			
		}
		br.close();
		fr.close();
		return r;
	}
	
	/**
	 * through a minimal key, get all minimal keys
	 * @param Sigma
	 * @param R
	 * @param firstMinKey
	 * @return
	 */
	public static  List<List<String>> getMinimalKeys(List<FD> Sigma,List<String> R,List<String> firstMinKey) {
		List<List<String>> minimalKeys = new ArrayList<List<String>>();
		minimalKeys.add(firstMinKey);
		List<String> current_key = firstMinKey;
		while(current_key != null) {
//			System.out.println("k : "+current_key);
			for(FD fd : Sigma) {
				List<String> left = fd.getLeftHand();
				List<String> right = fd.getRightHand();
				ArrayList<String> S = new ArrayList<String>();//S = left union (current_key - right)
				S.addAll(left);
				for(String a : current_key) {
					if(!right.contains(a) && !S.contains(a))
						S.add(a);
				}
//				if(!CONF.isRedundant(minimalKeys, S)) {
//					minimalKeys.add(S);
//				}
				minimalKeys = getNonRedundant(minimalKeys, S);
			}
			int index = minimalKeys.indexOf(current_key);
			if(index+1 >= minimalKeys.size())
				current_key = null;
			else
				current_key = minimalKeys.get(index+1);
		}
		
		//delete redundant keys
		
		return minimalKeys;
	}
	
	/**
	 * add a key into list,then delete non-minimal keys
	 * @param minimalKeys
	 * @param key
	 * @return true minimal keys
	 */
	public  static List<List<String>> getNonRedundant(List<List<String>> minimalKeys,List<String> key) {
		List<List<String>> remove = new ArrayList<List<String>>();
		boolean redundantKey = false;
		for(List<String> minimalKey : minimalKeys) {
			if(key.containsAll(minimalKey)) {
				redundantKey = true;
			}
			if(minimalKey.containsAll(key) && minimalKey.size() != key.size()) {
				remove.add(minimalKey);
			}
			
		}
		for(List<String> e : remove) {
			minimalKeys.remove(e);
		}
		if(!redundantKey)
			minimalKeys.add(key);
			
		return minimalKeys;
	}
	
	/**
	 * through a key, after removing some attributes, it gets a minimal key
	 * @param Sigma FD set
	 * @param key not minimal key
	 * @param R schema
	 * @return minimal key
	 */
	public static  ArrayList<String> getRefinedMinKey(List<FD> Sigma,List<String> key,List<String> R){
		ArrayList<String> minKey = new ArrayList<String>();
		minKey.addAll(key);
		
		for(String a : key) {
			ArrayList<String> minKey_no_a = new ArrayList<String>();
			minKey_no_a.addAll(minKey);
			minKey_no_a.remove(a);
			ArrayList<String> closure = getAttrSetClosure(R, minKey_no_a, Sigma);
			if(closure.containsAll(R) && closure.size() == R.size())//after deleting a, it still is a key
				minKey.remove(a);
		}
		
		return minKey;
	}
	
	/**
	 * get the closure of attribute set with given FDs
	 * @param R all attributes
	 * @param attrSet attribute set
	 * @param Sigma functional dependencies
	 * @return the closure of specified attribute set
	 */
	public static ArrayList<String> getAttrSetClosure(List<String> R, List<String> attrSet,List<FD> Sigma){
		ArrayList<String> X_n = new ArrayList<String>();
		for(String a : attrSet) {
			if(!X_n.contains(a))
				X_n.add(a);
		}
		ArrayList<String> X_n_next = new ArrayList<String>();
		
		for(FD fd : Sigma) {//find 'abc' -> 'sth',then get 'sth'
			List<String> left = fd.getLeftHand();
			List<String> right = fd.getRightHand();
			if(X_n.containsAll(left)) {
				//find subset == left(must ensure each element of set is distinct)
				for(String a : right) {
					if(!X_n_next.contains(a)) {
						X_n_next.add(a);
					}
				}
			}
		}
		for(String a : X_n) {
			if(!X_n_next.contains(a))
				X_n_next.add(a);
		}
		if(X_n_next.containsAll(X_n) && X_n_next.size() == X_n.size()) //X_n == X_n+1
			return X_n_next;
		else if(X_n_next.containsAll(R) && X_n_next.size() == R.size())//X_n+1 == U
			return X_n_next;
		else {
			return getAttrSetClosure(R, X_n_next, Sigma);
		}
	}
	
	/**
	 * get sub relation of relation
	 * @param R
	 * @param sub_R
	 * @param r
	 * @return
	 */
	public static List<List<String>> getSubRelation(List<String> R,List<String> sub_R, List<List<String>> r){
		List<List<String>> sub_r = new ArrayList<List<String>>();
		for(List<String> tuple : r) {
			List<String> sub_tuple = new ArrayList<String>();
			for(String a : sub_R) {
				sub_tuple.add(tuple.get(R.indexOf(a)));
			}
			if(!sub_r.contains(sub_tuple))
				sub_r.add(sub_tuple);
		}
		return sub_r;
	}
	
	/**
	 * projection of attribute set 'XA', given FD set Sigma.
	 * e.g. Y -> B is one of projection of XA over Sigma, which XA includes YB
	 * @param Sigma
	 * @param XA
	 * @return Σ[XA] if exists, empty set otherwise
	 */
	public static  ArrayList<FD> getProjection(List<FD> Sigma,List<String> XA){
		ArrayList<FD> projection = new ArrayList<FD>();//project of attribute set 'XA', given FD set Sigma
		for(FD fd : Sigma) {
			List<String> left = fd.getLeftHand();
			List<String> right = fd.getRightHand();
			List<String> attrset_fd = new ArrayList<String>();
			for(String a : left) {
				if(!attrset_fd.contains(a))
					attrset_fd.add(a);
			}
			for(String a : right) {
				if(!attrset_fd.contains(a))
					attrset_fd.add(a);
			}
			if(XA.containsAll(attrset_fd) && !projection.contains(fd))
				projection.add(fd);
		}
		return projection;
	}
	
	
	
	
	public static  boolean isRedundant(ArrayList<ArrayList<String>> minimalKeys,ArrayList<String> key) {
		boolean redundant = false;
		for(ArrayList<String> minimalKey : minimalKeys) {
			if(key.containsAll(minimalKey)) {
				redundant = true;
				break;
			}
		}
		return redundant;
	}
	
	
	/**
	 * judge whether a database is on BCNF or not
	 * @param R schema
	 * @param fds functional dependencies
	 * @return
	 */
	public static boolean isBCNF(List<String> R, List<FD> fds) {
		boolean is_BCNF = true;
		ArrayList<String> firstMinKey = Utils.getRefinedMinKey(fds, R, R);
		List<List<String>> minKeys = Utils.getMinimalKeys(fds, R, firstMinKey);
		for(FD fd : fds) {//fd : X -> A
			List<String> X = fd.getLeftHand();
			List<String> A = fd.getRightHand();
			
			boolean XIsSuperKey = false;
			for(List<String> key : minKeys) {
				if(X.containsAll(key)) {//X is super key
					XIsSuperKey = true;
					break;
				}
			}
			
			if(A.containsAll(X) || XIsSuperKey) {//satisfy one of two conditions:1. FD  is trivial, 2. X is super key
				
			}else {
				is_BCNF = false;
				break;
			}
		}
		return is_BCNF;
	}
	
	/**
	 * get level of attribute set on a relation r
	 * @param R
	 * @param attrSet
	 * @param r
	 * @return
	 */
//	public static int getLevelOfAttrSet(List<String> R,List<String> attrSet, List<List<String>> r) {
//		int level = 0;
//		HashMap<List<String>,Integer> level_map = new HashMap<List<String>, Integer>();//key = tuple value = number of same tuple in the r
//		for(List<String> tuple : r) {
//			List<String> val_com = new ArrayList<String>();//value combination on attrSet
//			for(String a : attrSet) {
//				val_com.add(tuple.get(R.indexOf(a)));
//			}
//			if(level_map.containsKey(val_com)) {
//				int l = level_map.get(val_com);
//				level_map.put(val_com, ++l);
//			}else {
//				level_map.put(val_com, 1);
//			}
//		}
//		Set<List<String>> keyset = level_map.keySet();
//		for(List<String> key : keyset) {
//			int level1 = level_map.get(key);
//			level = level1 > level ? level1 : level;
//		}
//		return level;
//	}
	
	/**
	 * input string "a","","v", then closure = " , finally we return list ["a" "" "v"]
	 * @param raw_string
	 * @param closure
	 * @return
	 */
	public static List<String> split(String raw_string, char closure){
		List<String> res = new ArrayList<String>();
		char[] chars = raw_string.toCharArray();
		boolean start = false;//when start is true,we start to get a sub-string with closure of param 'closure'
		String sub_string = "";
		for(char c : chars) {
			if(c == closure && !start) {
				start = true;
				sub_string += c;
				continue;
			}
			if(start)
				sub_string += c;
			if(c == closure && start) {
				start = false;
				res.add(sub_string);
				sub_string = "";
			}
		}
		return res;
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
	 * @param R
	 * @param col_data_type varchar(255), text, etc...
	 * @throws SQLException
	 */
	public static void createTable(String tableName,List<String> R,String col_data_type) throws SQLException {
		Connection conn =connectDB();
		String sql = "CREATE TABLE "+tableName+" (  ";
		for(int i = 0;i< R.size();i ++) {
			String columnName = R.get(i);
			if(i == R.size() -1)
				sql += "`"+columnName +"` "+col_data_type;
			else
				sql += "`"+columnName +"` "+col_data_type+",\n";
		}
		sql += ") CHARSET=utf8mb3";
//		System.out.println(sql);
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();
		conn.close();
	}
	public static void insertData(String tableName,List<List<String>> dataset) throws SQLException {
		if(dataset == null)
			return;
		if(dataset.isEmpty())
			return;
		Connection conn = connectDB();
		conn.setAutoCommit(false);
		String insertSql = "INSERT INTO "+tableName + " VALUES ( ";
		for(int i = 0;i < dataset.get(0).size();i ++) {
			if(i != (dataset.get(0).size() - 1))
				insertSql += " ? ,";
			else
				insertSql += " ? )";
		}
		PreparedStatement prepStmt1 = conn.prepareStatement(insertSql);
		
		int count = 0;
		for(List<String> data : dataset) {
			for(int i = 1;i <= data.size();i ++) {
				prepStmt1.setString(i, data.get(i-1));
			}
			prepStmt1.addBatch();
			if(++count % 10000 == 0) {
				prepStmt1.executeBatch();
				conn.commit();
				prepStmt1.clearBatch();
			}
		}
		prepStmt1.executeBatch();
		conn.commit();
		prepStmt1.clearBatch();
		
		prepStmt1.close();
		conn.close();
	}
	
	/**
	 * import data set from local file into databases
	 * @param col_data_type varchar(255), text, etc...
	 * @throws IOException
	 * @throws SQLException
	 */
	public static void import_dataset_from_local_to_database(String col_data_type) throws IOException, SQLException {
		List<List<String>> r = Utils.import_dataset(Constant.file_add, Constant.split);
		System.out.println("successfully read dataset from local.");
		List<String> R = new ArrayList<String>();
		for(int i = 0;i < Constant.col_num;i ++) {
			R.add(i+"");
		}
		Utils.createTable(Constant.dataset_name, R, col_data_type);
		System.out.println("successfully create table.");
		Utils.insertData(Constant.dataset_name, r);
		System.out.println("successfully import dataset into databases.");
	}
	
	/**
	 * SELECT COUNT(*) as level from (SELECT DISTINCT `0`,`1`,`2`,`3` FROM pdbx) as tmp GROUP BY tmp.`0`,tmp.`2` ORDER BY level DESC
	 * @param subschema
	 * @param lhs left hand
	 * @return level of left hand side on subschema that is projection on schema
	 * @throws SQLException
	 */
	public static int get_level_from_database(List<String> subschema,List<String> lhs) throws SQLException {
		if(lhs.isEmpty() || subschema.isEmpty())
			return 1;
		int level = 0;
		String subR = "";
		String left = "";
		for(int i = 0;i < subschema.size();i ++) {
			if(i != subschema.size() - 1)
				subR += "`"+subschema.get(i)+"`,";
			else
				subR += "`"+subschema.get(i)+"`";
		}
		for(int i = 0;i < lhs.size();i ++) {
			if(i != lhs.size() - 1)
				left += "tmp.`"+lhs.get(i)+"`,";
			else
				left += "tmp.`"+lhs.get(i)+"`";
		}
		Connection conn = connectDB();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as level from (SELECT DISTINCT "+subR+" FROM `"+Constant.dataset_name+"`) as tmp GROUP BY "+left+" ORDER BY level DESC");
		rs.next();//move cursor to first row
		level = Integer.parseInt(rs.getString(1));//get first row first column value that has only one column
		
		rs.close();
		stmt.close();
		conn.close();
		return level;
	}
	
	public static List<Object> import_schema_and_FDs() throws SQLException, IOException {
		List<Object> output = new ArrayList<Object>();
		//import JSON file
		JSONParser json = new JSONParser();
		json.parseJSON(Constant.fd_add);
		List<String> R = json.getSchema();
		List<FD> Sigma_a = json.getFd_list();
			    
	    output.add(R);
	    output.add(Sigma_a);
	    return output;
	}
	
	/**
	 * Given each FD, we get all attributes of FD as a sub-schema,
	 * then we get level of the sub-schema on projection of atomic FD cover,
	 * finally we set the level value to each FD.
	 * @throws SQLException
	 * @throws IOException 
	 * @return list of two objects, first is schema R, second is FD list
	 */
	public static List<Object> import_schema_and_FDs_with_subschema_level() throws SQLException, IOException {
		List<Object> output = new ArrayList<Object>();
		//import JSON file
		JSONParser json = new JSONParser();
		json.parseJSON(Constant.fd_add);
		List<String> R = json.getSchema();
		List<FD> Sigma_a = json.getFd_list();
			    
		//compute CC for FDs
	    Map<Set<String>,Map<Set<String>,Integer>> attrset_level_cache = new HashMap<Set<String>,Map<Set<String>,Integer>>();
	    //key = sub-schema value = map(key=left-hand, value=level)
	    
	    int count = 0;
	    for(FD X_A : Sigma_a) {
	    	System.out.println(count ++ + "-th FD to deal with...");
			List<Integer> l_X_A_list = new ArrayList<Integer>();
			List<String> XA = new ArrayList<String>();
			for(String a : R) {
				if(X_A.getLeftHand().contains(a)) {
					XA.add(a);
					continue;
				}
				if(X_A.getRightHand().contains(a))
					XA.add(a);
			}
			
			Set<String> XA_set = new HashSet<String>(XA);
			
			for(FD Y_B : Sigma_a) {//get Y -> B
				ArrayList<String> YB = new ArrayList<String>();
				for(String a : R) {
					if(Y_B.getLeftHand().contains(a)) {
						YB.add(a);
						continue;
					}
					if(Y_B.getRightHand().contains(a))
						YB.add(a);
				}
				if(XA.containsAll(YB)) {//
					//compute min l_Y 
					//level on XA sub-relation
					List<String> Y = new ArrayList<String>();
					for(String a : R) {
						if(Y_B.getLeftHand().contains(a))
							Y.add(a);
					}
					Set<String> Y_set = new HashSet<String>(Y);
					int l_Y = 0;
					if(attrset_level_cache.containsKey(XA_set)) {
						Map<Set<String>,Integer> level_map = attrset_level_cache.get(XA_set);
						if(level_map.containsKey(Y_set)) {
							l_Y = level_map.get(Y_set);
						}else {
//							l_Y = Utils.getLevelOfAttrSet(XA, Y, r_on_XA);
							l_Y = Utils.get_level_from_database(XA, Y);
							level_map.put(Y_set, l_Y);
//							attrset_level_cache.put(XA_set, level_map);
						}
					}else {
//						l_Y = Utils.getLevelOfAttrSet(XA, Y, r_on_XA);
						l_Y = Utils.get_level_from_database(XA, Y);
						Map<Set<String>,Integer> level_map1 = new HashMap<Set<String>,Integer>();
						level_map1.put(Y_set, l_Y);
						attrset_level_cache.put(XA_set, level_map1);
					}
					l_X_A_list.add(l_Y);
				}
			}
			
			int l_X_A = 0;//compute level under X->A
			for(int l_Y : l_X_A_list) {
				l_X_A = (l_Y > l_X_A ? l_Y : l_X_A);//get max l_Y to l_X_A
			}
			X_A.setLevel(l_X_A);//set max l_Y to X -> A
		}
	    output.add(R);
	    output.add(Sigma_a);
	    return output;
	}
	
	/**
	 * compute specified sub-schema's level
	 * Given each FD, we get all attributes of FD as a sub-schema,
	 * then we get level of the sub-schema on projection of atomic FD cover,
	 * finally we set the level value to each FD.
	 * @param start start index of FD list
	 * @param end end index of FD list (inclusive)
	 * @throws SQLException
	 * @throws IOException 
	 * @return list of two objects, first is schema R, second is FD list
	 */
	public static List<Object> import_schema_and_FDs_with_subschema_level_in_scope(int start, int end) throws SQLException, IOException {
		List<Object> output = new ArrayList<Object>();
		//import JSON file
		JSONParser json = new JSONParser();
		json.parseJSON(Constant.fd_add);
		List<String> R = json.getSchema();
		List<FD> Sigma_a = json.getFd_list();
			    
		//compute CC for FDs
	    Map<Set<String>,Map<Set<String>,Integer>> attrset_level_cache = new HashMap<Set<String>,Map<Set<String>,Integer>>();
	    //key = sub-schema value = map(key=left-hand, value=level)
	    
	    int count = -1;
	    for(FD X_A : Sigma_a) {
	    	count ++;
	    	if(count < start || count > end)
	    		continue;
	    	System.out.println(count + "-th FD to deal with...");
			List<Integer> l_X_A_list = new ArrayList<Integer>();
			List<String> XA = new ArrayList<String>();
			for(String a : R) {
				if(X_A.getLeftHand().contains(a)) {
					XA.add(a);
					continue;
				}
				if(X_A.getRightHand().contains(a))
					XA.add(a);
			}
			
			Set<String> XA_set = new HashSet<String>(XA);
			
			for(FD Y_B : Sigma_a) {//get Y -> B
				ArrayList<String> YB = new ArrayList<String>();
				for(String a : R) {
					if(Y_B.getLeftHand().contains(a)) {
						YB.add(a);
						continue;
					}
					if(Y_B.getRightHand().contains(a))
						YB.add(a);
				}
				if(XA.containsAll(YB)) {//
					//compute min l_Y 
					//level on XA sub-relation
					List<String> Y = new ArrayList<String>();
					for(String a : R) {
						if(Y_B.getLeftHand().contains(a))
							Y.add(a);
					}
					Set<String> Y_set = new HashSet<String>(Y);
					int l_Y = 0;
					if(attrset_level_cache.containsKey(XA_set)) {
						Map<Set<String>,Integer> level_map = attrset_level_cache.get(XA_set);
						if(level_map.containsKey(Y_set)) {
							l_Y = level_map.get(Y_set);
						}else {
//							l_Y = Utils.getLevelOfAttrSet(XA, Y, r_on_XA);
							l_Y = Utils.get_level_from_database(XA, Y);
							level_map.put(Y_set, l_Y);
//							attrset_level_cache.put(XA_set, level_map);
						}
					}else {
//						l_Y = Utils.getLevelOfAttrSet(XA, Y, r_on_XA);
						l_Y = Utils.get_level_from_database(XA, Y);
						Map<Set<String>,Integer> level_map1 = new HashMap<Set<String>,Integer>();
						level_map1.put(Y_set, l_Y);
						attrset_level_cache.put(XA_set, level_map1);
					}
					l_X_A_list.add(l_Y);
				}
			}
			
			int l_X_A = 0;//compute level under X->A
			for(int l_Y : l_X_A_list) {
				l_X_A = (l_Y > l_X_A ? l_Y : l_X_A);//get max l_Y to l_X_A
			}
			X_A.setLevel(l_X_A);//set max l_Y to X -> A
			if(Constant.write_separately)
				Utils.write_subschema_level_into_local_in_scope_separately(R, X_A, Constant.start, Constant.end, count);
		}
	    output.add(R);
	    output.add(Sigma_a);
	    return output;
	}
	
	/**
	 * write each subschema's level into file under Sigma_a and the data set
	 * @param R
	 * @param Sigma_a
	 * @throws IOException
	 */
	public static void write_subschema_level_into_local(List<String> R, List<FD> Sigma_a) throws IOException {
		File f = new File(Constant.subschema_level_add);
		if(!f.exists())
			f.createNewFile();
		FileWriter fw = new FileWriter(Constant.subschema_level_add);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write("id;subschema;level\n");
		
		for(int i = 0;i < Sigma_a.size();i ++) {
			List<String> subschema = new ArrayList<String>();
			FD fd = Sigma_a.get(i);
			List<String> lhs = fd.getLeftHand();
			List<String> rhs = fd.getRightHand();
			String line = "";
			line += i + ";" ;
			for(String a : R) {
				if(lhs.contains(a)) {
					subschema.add(a);
					continue;
				}
				if(rhs.contains(a)){
					subschema.add(a);
				}
			}
			for(int j = 0;j < subschema.size();j ++) {
				if(j != subschema.size() - 1)
					line += subschema.get(j) + ",";
				else
					line += subschema.get(j) + ";";
			}
			line += fd.getLevel();
			bw.write(line+"\n");
		}
		
		bw.close();
		fw.close();
	}
	
	/**
	 * write each subschema's level in specified sub-schema into file under Sigma_a and the data set
	 * @param R
	 * @param Sigma_a
	 * @param start start index of FD in Sigma_a to get schema level
	 * @param end end index of FD in Sigma_a to get schema level
	 * @throws IOException
	 */
	public static void write_subschema_level_into_local_in_scope(List<String> R, List<FD> Sigma_a, int start, int end) throws IOException {
		String path = Constant.subschema_level_add.replace(".txt", "("+start+"-"+end+").txt");
		File f = new File(path);
		if(!f.exists())
			f.createNewFile();
		FileWriter fw = new FileWriter(path,true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write("id;subschema;level\n");
		
		for(int i = start;i <= end;i ++) {
			List<String> subschema = new ArrayList<String>();
			FD fd = Sigma_a.get(i);
			List<String> lhs = fd.getLeftHand();
			List<String> rhs = fd.getRightHand();
			String line = "";
			line += i + ";" ;
			for(String a : R) {
				if(lhs.contains(a)) {
					subschema.add(a);
					continue;
				}
				if(rhs.contains(a)){
					subschema.add(a);
				}
			}
			for(int j = 0;j < subschema.size();j ++) {
				if(j != subschema.size() - 1)
					line += subschema.get(j) + ",";
				else
					line += subschema.get(j) + ";";
			}
			line += fd.getLevel();
			bw.write(line+"\n");
		}
		
		bw.close();
		fw.close();
	}
	
	/**
	 * write single schema level for FD into file separately
	 * @param R
	 * @param Sigma_a
	 * @param start start index of FD in Sigma_a to get schema level
	 * @param end end index of FD in Sigma_a to get schema level
	 * @throws IOException
	 */
	public static void write_subschema_level_into_local_in_scope_separately(List<String> R, FD fd, int start, int end,int id) throws IOException {
		String path = Constant.subschema_level_add.replace(".txt", "("+start+"-"+end+").txt");
		File f = new File(path);
		if(!f.exists())
			f.createNewFile();
//		bw.write("id;subschema;level\n");
		
		FileWriter fw = new FileWriter(path,true);
		BufferedWriter bw = new BufferedWriter(fw);
		List<String> subschema = new ArrayList<String>();
		List<String> lhs = fd.getLeftHand();
		List<String> rhs = fd.getRightHand();
		String line = "";
		line += id + ";" ;
		for(String a : R) {
			if(lhs.contains(a)) {
				subschema.add(a);
				continue;
			}
			if(rhs.contains(a)){
				subschema.add(a);
			}
		}
		for(int j = 0;j < subschema.size();j ++) {
			if(j != subschema.size() - 1)
				line += subschema.get(j) + ",";
			else
				line += subschema.get(j) + ";";
		}
		line += fd.getLevel();
		bw.write(line+"\n");
		bw.close();
		fw.close();
		
		
	}
	
	/**
	 * from local file, get schema level of a schema
	 * @return a map with key = subschema and value = level
	 * @throws IOException 
	 */
	public static Map<Set<String>,Integer> import_subschema_with_level_from_local() throws IOException{
		Map<Set<String>,Integer> schema_level = new HashMap<Set<String>,Integer>();
		FileReader fr = new FileReader(Constant.subschema_level_add);
		BufferedReader br = new BufferedReader(fr);
		br.readLine();//skip header
		String line;
		while((line = br.readLine()) != null) {
			String[] entities = line.split(";");
			String[] sub_schema = entities[1].split(",");
			int level = Integer.parseInt(entities[2]);
			Set<String> subschema = new HashSet<String>();
			for(String a : sub_schema) {
				subschema.add(a);
			}
			schema_level.put(subschema, level);
		}
		br.close();
		fr.close();
		return schema_level;
	}
	
	/**
	 * Given each FD, we get all attributes of FD as a sub-schema,
	 * then we get level of the sub-schema on projection of atomic FD cover,
	 * finally we set the level value to each FD.
	 * @throws SQLException
	 * @throws IOException 
	 * @return list of two objects, first is schema R, second is FD list
	 */
	public static List<Object> import_schema_and_FDs_with_subschema_level_from_local() throws SQLException, IOException {
		List<Object> output = new ArrayList<Object>();
		//import JSON file
		JSONParser json = new JSONParser();
		json.parseJSON(Constant.fd_add);
		List<String> R = json.getSchema();
		List<FD> Sigma_a = json.getFd_list();
			    
		Map<Set<String>,Integer> schema_level = Utils.import_subschema_with_level_from_local();
	    
	    for(FD X_A : Sigma_a) {
	    	Set<String> subR = new HashSet<String>();
	    	subR.addAll(X_A.getLeftHand());
	    	subR.addAll(X_A.getRightHand());
	    	if(schema_level.containsKey(subR)) {
	    		int l_XA = schema_level.get(subR);
	    		X_A.setLevel(l_XA);
	    	}
	    }
	    output.add(R);
	    output.add(Sigma_a);
	    return output;
	}
	
	public static void main(String[] args) throws IOException, SQLException {
//		Utils.import_dataset_from_local_to_database("text");
		System.out.println("test : "+Utils.get_level_from_database(Arrays.asList("0","1","2","10"), Arrays.asList("0")));
	}
}
