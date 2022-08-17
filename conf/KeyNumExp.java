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

public class KeyNumExp {
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
	public static void createTable(String tableName,ArrayList<String> R) throws SQLException {
		Connection conn =connectDB();
		String sql = "CREATE TABLE "+tableName+" ( aid int AUTO_INCREMENT PRIMARY KEY, ";//aid auto_crement primary key
		for(int i = 0;i< R.size();i ++) {
			String columnName = R.get(i);
			if(i == R.size() -1)
				sql += columnName +" varchar(30)";
			else
				sql += columnName +" varchar(30),\n";
		}
		sql += ") CHARSET=utf8mb3";
//		System.out.println(sql);
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();
		conn.close();
	}
	
	public static void deleteTable(String tableName) throws SQLException {
		Connection conn =connectDB();
		String sql = "DROP TABLE IF EXISTS "+tableName;
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();
		conn.close();
	}
	
	public static void insertData(String tableName,ArrayList<ArrayList<String>> dataset) throws SQLException {
		if(dataset == null)
			return;
		if(dataset.isEmpty())
			return;
		Connection conn = connectDB();
		conn.setAutoCommit(false);
		String insertSql = "INSERT INTO "+tableName + " VALUES ( null, ";
		for(int i = 0;i < dataset.get(0).size();i ++) {
			if(i != (dataset.get(0).size() - 1))
				insertSql += " ? ,";
			else
				insertSql += " ? )";
		}
		PreparedStatement prepStmt1 = conn.prepareStatement(insertSql);
		
		for(ArrayList<String> data : dataset) {
			for(int i = 1;i <= data.size();i ++) {
				prepStmt1.setString(i, data.get(i-1));
			}
			prepStmt1.addBatch();
		}
		prepStmt1.executeBatch();
		conn.commit();
		prepStmt1.close();
		conn.close();
	}
	
	/**
	 * 
	 * SELECT R.A1,...,R.An,count(*) FROM R GROUP BY R.A1,...,R.An HAVING COUNT(*) > 1
	 * @param key
	 * @param tableName
	 * @param needIndex 
	 * @return query time
	 * @throws SQLException
	 */
	public static long selectData_one(ArrayList<String> key,String tableName,boolean needIndex) throws SQLException {
		Connection conn = connectDB();
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		String sql = "SELECT ";
		for(String attr : key) {
			sql += "`"+tableName+"`."+attr+", ";
		}
		sql += "COUNT(*) FROM `"+tableName+"` GROUP BY ";
		for(int i = 0;i < key.size();i ++) {
			String attr = key.get(i);
			if(i != (key.size() - 1))
			    sql += "`"+tableName+"`."+attr+", ";
			else
				sql += "`"+tableName+"`."+attr+" ";
		}
		sql += "HAVING COUNT(*) > 1";
		String index ="ALTER TABLE `"+tableName+"` ADD UNIQUE myindexOn"+tableName+" ( ";
		for(int i =0;i < key.size();i ++) {
			if(i != (key.size() -1))
				index += key.get(i) + ",";
			else
				index += key.get(i) + " )";
		}
		String indexRemoval = "DROP INDEX myindexOn"+tableName+" ON `"+tableName+"`";
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
	 * SELECT R.A1,...,R.An FROM R, R AS R1 WHERE R.A1 = R1.A1 AND ... AND R.An = R1.An AND R.aid <> R1.aid
	 * @param key
	 * @param tableName
	 * @param needIndex 
	 * @return query time
	 * @throws SQLException
	 */
	public static long selectData_two(ArrayList<String> key,String tableName,boolean needIndex) throws SQLException {
		Connection conn = connectDB();
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		String sql = "SELECT ";
		for(int i = 0;i < key.size();i ++) {
			String attr = key.get(i);
			if(i != (key.size() - 1))
			    sql += "`"+tableName+"`."+attr+", ";
			else
				sql += "`"+tableName+"`."+attr+" ";
		}
		sql += "FROM `"+tableName+"` , `"+tableName+"` AS `"+tableName+"_1` WHERE ";
		for(int i = 0;i < key.size();i ++) {
			String attr = key.get(i);
			if(i != (key.size() - 1))
			    sql += "`"+tableName+"`."+attr+" = `"+tableName+"_1`."+attr+" AND ";
			else
				sql += "`"+tableName+"`."+attr+" = `"+tableName+"_1`."+attr;
		}
		sql += " AND `"+tableName+"`.aid <> `"+tableName+"_1`.aid";
		String index ="ALTER TABLE `"+tableName+"` ADD UNIQUE myindexOn"+tableName+" ( ";
		for(int i =0;i < key.size();i ++) {
			if(i != (key.size() -1))
				index += key.get(i) + ",";
			else
				index += key.get(i) + " )";
		}
		String indexRemoval = "DROP INDEX myindexOn"+tableName+" ON `"+tableName+"`";
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
	
	
	public static long updateData(ArrayList<String> R,ArrayList<String> key,ArrayList<ArrayList<String>> insertCopy,int currentCopyNum,String tableName,boolean needIndex) throws SQLException {
		Connection conn = connectDB();
		conn.setAutoCommit(false);
		
		Statement stmt = null;
		String index = "";
		String indexRemoval = "";
		
		System.out.println("==================");
		System.out.println("insert copy into "+tableName +", inserted copy tuples size : "+insertCopy.size());
		System.out.println("index: "+needIndex);
		if(needIndex) {
			index ="ALTER TABLE `"+tableName+"` ADD UNIQUE myindexOn"+tableName+" ( ";
			for(int i =0;i < key.size();i ++) {
				if(i != (key.size() -1))
					index += key.get(i) + ",";
				else
					index += key.get(i) + " )";
			}
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
		    System.out.println("built index: "+index);
		    stmt.executeUpdate(index);
		    conn.commit();
		}
		
		Savepoint mySavePoint = conn.setSavepoint("rollbackpoint");
		
		String insertSql = "INSERT INTO "+tableName + " VALUES ( null ,";
		for(int i = 0;i < R.size();i ++) {
			if(i != (R.size() - 1))
				insertSql += " ? ,";
			else
				insertSql += " ? )";
		}
		PreparedStatement prepStmt1 = conn.prepareStatement(insertSql);
		
		long start = new Date().getTime();
		for(ArrayList<String> data : insertCopy) {
			for(int i = 1;i <= R.size();i ++) {
				prepStmt1.setString(i, data.get(i-1));
			}
			prepStmt1.addBatch();
		}
		prepStmt1.executeBatch();
		long end = new Date().getTime();
		
		conn.rollback(mySavePoint);
		conn.commit();
		
		if(needIndex) {
		    indexRemoval = "DROP INDEX myindexOn"+tableName+" ON `"+tableName+"`";//删除索引
		    stmt.executeUpdate(indexRemoval);
		    conn.commit();
		}
		System.out.println("Transaction has been rolled back...");
		
		if(null != stmt)
		    stmt.close();
		prepStmt1.close();
		conn.close();
		System.out.println("execution time(ms): "+(end - start));
		return (end - start);
	}
	
	/**
	 * 给定schema下，返回指定数量的最小key
	 * given schema, return numOfKeys minimal keys
	 * @param schemaSize
	 * @param numOfKeys
	 * @return minimal keys we find
	 */
	public static ArrayList<ArrayList<String>> createDifferentMinimalKeys(ArrayList<String> R,int numOfKeys){
		int retryCount = 0;
		int retryThreshold = 100;
		int schemaSize = R.size();
		ArrayList<ArrayList<String>> minimalKeys = new ArrayList<ArrayList<String>>();
		Random rand = new Random();
		for(int i = 0;i < numOfKeys;i ++) {
			int randSize = rand.nextInt(schemaSize) + 1;
			ArrayList<String> key = new ArrayList<String>();
			for(int j = 0;j < randSize;j ++) {
				String attr = R.get(rand.nextInt(schemaSize));
				if(key.contains(attr)) {
					j --;//retry
				}else {
					key.add(attr);
				}
			}
			key.sort(null);
			if(minimalKeys.size() == 0 && numOfKeys > 1 && key.equals(R)) {
				i --;//retry to generate key
				retryCount ++;
				if(retryCount > retryThreshold)
					return null;
				continue;
			}
			if(isSuitableKey(key,minimalKeys)) {
				minimalKeys.add(key);
			}else {
				i --;//retry
				retryCount ++;
				if(retryCount > retryThreshold)
					return null;
			}
		}
		System.out.println("current minimal keys: ");
		for(ArrayList<String> key : minimalKeys) {
			System.out.println(key);
		}
		return minimalKeys;
	}
	
	/**
	 * given size of key and schema, return key set that have most keys
	 * @param R
	 * @param keySize |key|
	 * @return
	 */
	public static ArrayList<ArrayList<String>> createMaxMinimalKeys(ArrayList<String> R,int keySize){
		ArrayList<ArrayList<String>> maxMinimalKeys = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Integer>> maxMinimalKeysWithIndex = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> start = new ArrayList<Integer>();//例子:|R|=3 keySize=1 start={0}; keySize=2 start={0,1}
		ArrayList<Integer> current = new ArrayList<Integer>();
		for(int i = 0;i < keySize;i ++) {
			start.add(i);
			current.add(i);
		}
		maxMinimalKeysWithIndex.add(start);
		while(true) {
			ArrayList<Integer> next = new ArrayList<Integer>();
			for(int i = current.size()-1;i >= 0;i --) {
				int value_i = current.get(i);
				int maxValue_i = (R.size() - (keySize - i));
				if(value_i < maxValue_i) {
					for(int j =0;j < current.size();j ++) {
						if(j < i) 
							next.add(current.get(j));
						if(j == i) 
							next.add(value_i+1);
						if(j > i)
							next.add(next.get(next.size()-1)+1);
					}
					maxMinimalKeysWithIndex.add(next);
					current = next;
					break;
				}
			}
			int maxValue_0 = (R.size() - (keySize - 0));
			if(current.get(0) == maxValue_0) {//e.g. |R| = 6, keySize=3, current={3,4,5} then find
				break;
			}
		}
		
		System.out.println("Given schema :"+R+" ,Max minimal keys: ");
		for(ArrayList<Integer> keyIndex : maxMinimalKeysWithIndex) {
			ArrayList<String> key = new ArrayList<String>();
			for(Integer index : keyIndex) {
				key.add(R.get(index));
			}
			System.out.println(key);
			maxMinimalKeys.add(key);
		}
		
		return maxMinimalKeys;
	}
	
	/**
	 * judge all keys are minimal keys when a key is added into the current minimakKeys
	 * @param key
	 * @param minimalKeys
	 * @return
	 */
	public static boolean isSuitableKey(ArrayList<String> key,ArrayList<ArrayList<String>> minimalKeys) {
		boolean isSuitable = true;
		for(ArrayList<String> key1 : minimalKeys) {
			if(key1.containsAll(key) || key.containsAll(key1)) {
				isSuitable = false;
				return isSuitable;
			}
		}
		return isSuitable;
	}
	
	/**
	 * compute all minimal transversals
	 * @param schema
	 * @return
	 */
	public static ArrayList<HashSet<String>> mineMinTrans(int recursionLevel,ArrayList<HashSet<String>> minTrans, ArrayList<String> schema, int Threshold, ArrayList<ArrayList<String>> keys){
		ArrayList<ArrayList<String>> attrSets = new ArrayList<ArrayList<String>>();
		for(ArrayList<String> key : keys) {
			ArrayList<String> attrSet = new ArrayList<String>();
			for(String e : key) {
				attrSet.add(e);
			}
			attrSets.add(attrSet);
		}
		HashMap<String,ArrayList<Integer>> repeatedAttrsAndIndexes = new HashMap<String, ArrayList<Integer>>();
		for(String attr : schema){
			int count = 0;
			int flag = 0;
			ArrayList<Integer> indexes = new ArrayList<Integer>();
			while(count < attrSets.size()){
				if(attrSets.get(count).contains(attr)){
					flag ++;
					indexes.add(count);
				}
				count ++;
			}
			if(flag >= Threshold){
				repeatedAttrsAndIndexes.put(attr, indexes);
			}
		}
		
		
		boolean existAnySetEmpty = false;//exit flag
		if(repeatedAttrsAndIndexes.size() > 0){
			Iterator<String> iter = repeatedAttrsAndIndexes.keySet().iterator();
			while(iter.hasNext()){
				if(existAnySetEmpty)
					break;
				String attr1 = iter.next();
				ArrayList<ArrayList<String>> candidateSet = new ArrayList<ArrayList<String>>();
				
				ArrayList<Integer> IndexList = repeatedAttrsAndIndexes.get(attr1);
				for(Integer index : IndexList){
					attrSets.get(index).remove(attr1);
					if(attrSets.get(index).size() == 0)
						existAnySetEmpty = true;
				}
				if(IndexList.size() != attrSets.size()){
					for(int i = 0;i < attrSets.size();i ++){
						if(!IndexList.contains(i)){
							ArrayList<String> temp = (ArrayList<String>) attrSets.get(i).clone();
							candidateSet.add(temp);
						}
					}
				}
				if(candidateSet.size() == 0){
					HashSet<String> mt = new HashSet<String>();
					mt.add(attr1);
					if(!minTrans.contains(mt))
					    minTrans.add(mt);
				}else{
					
					ArrayList<HashSet<String>> minTrans1 = new ArrayList<HashSet<String>>();
					ArrayList<HashSet<String>> subMinTrans = mineMinTrans(recursionLevel + 1,minTrans1, schema, candidateSet.size(), candidateSet);
				    for(int i = 0;i < subMinTrans.size();i ++){
				    	subMinTrans.get(i).add(attr1);
				        minTrans.add(subMinTrans.get(i));
				    }
				}
			}
			if(!existAnySetEmpty){
				mineMinTrans(recursionLevel + 1,minTrans, schema, Threshold - 1, attrSets);
			}
		}else{
			mineMinTrans(recursionLevel,minTrans, schema, Threshold - 1, attrSets);
		}
		
		if(recursionLevel == 1){
			Collections.sort(minTrans,new Comparator<HashSet<String>>(){

				@Override
				public int compare(HashSet<String> o1, HashSet<String> o2) {
					if(o1.size() > o2.size())
						return 1;
					else if(o1.size() < o2.size())
						return -1;
					else
						return 0;
				}
			});
			ArrayList<HashSet<String>> copyMinTrans = (ArrayList<HashSet<String>>) minTrans.clone();
			for(int i = 0;i < copyMinTrans.size() - 1;i ++){
				for(int j = i + 1;j < copyMinTrans.size();j ++ ){
					if(copyMinTrans.get(j).containsAll(copyMinTrans.get(i))){
						minTrans.remove(copyMinTrans.get(j));
					}
				}
			}
		}
		
		return minTrans;
	}
	
	/**
	 * 
	 * @param schemaSize 
	 * @param keyNum 
	 * @return return minimal Armstrong Relation that has specific keys
	 */
	public static ArrayList<ArrayList<String>> createMinimalArmstrongRelation(ArrayList<String> R,int keyNum,ArrayList<ArrayList<String>> keys){
		int schemaSize = R.size();
		ArrayList<HashSet<String>> minTrans = mineMinTrans(1,new ArrayList<HashSet<String>>(), R, keyNum, keys);
		
		ArrayList<ArrayList<String>> minArmRel = new ArrayList<ArrayList<String>>();
		ArrayList<String> firstTuple = new ArrayList<String>();
		for(int i = 0;i < schemaSize;i ++) {
			firstTuple.add(0+"");
		}
		minArmRel.add(firstTuple);
		for(HashSet<String> minTran : minTrans) {
			ArrayList<String> newTuple = new ArrayList<String>();
			for(String attr : R) {
				if(minTran.contains(attr)) {
					for(int i = minArmRel.size()-1;i > -1;i --) {
						ArrayList<String> t = minArmRel.get(i);
						int value = Integer.parseInt(t.get(R.indexOf(attr)));
						if(value != 0) {
							newTuple.add((value+1)+"");
							break;
						}
						if(value == 0 && i == 0) {
							newTuple.add(1+"");
							break;
						}
					}
				}else {
					newTuple.add(0+"");
				}
			}
			minArmRel.add(newTuple);
		}
		return minArmRel;
	}
	
	/**
	 * due to minimal Armstrong relation, create some copies
	 * @param minArmRel 
	 * @param startCopyNum 
	 * @param endCopyNum 
	 * @return copies of Armstrong relation
	 */
	public static ArrayList<ArrayList<String>> createCopyOfMinArmRel(ArrayList<ArrayList<String>> minArmRel,int startCopyNum,int endCopyNum){
		ArrayList<ArrayList<String>> copy = new ArrayList<ArrayList<String>>();
//		copy.addAll(minArmRel);
		for(int i = startCopyNum;i <= endCopyNum;i++ ) {
			for(ArrayList<String> tuple : minArmRel) {
				ArrayList<String> t = new ArrayList<String>();
				for(String value : tuple) {
					t.add(value+"_"+i);
				}
				copy.add(t);
			}
		}
		return copy;
	}
	
	/**
	 * get median of list
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
	
	public static ArrayList<Long> getMedianList(ArrayList<ArrayList<Long>> lists){
		ArrayList<Long> medianList = new ArrayList<Long>();
		for(int i = 0;i < lists.get(0).size();i ++) {
			ArrayList<Long> l = new ArrayList<Long>();
			for(ArrayList<Long> list : lists) {
				l.add(list.get(i));
			}
			medianList.add(getMedian(l));
		}
		return medianList;
	}
	
	/**
	 * get average of list
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
	
	public static ArrayList<Double> getAveList(ArrayList<ArrayList<Double>> lists){
		ArrayList<Double> aveList = new ArrayList<Double>();
		for(int i = 0;i < lists.get(0).size();i ++) {
			ArrayList<Double> l = new ArrayList<Double>();
			for(ArrayList<Double> list : lists) {
				l.add(list.get(i));
			}
			aveList.add(getAve_Double(l));
		}
		return aveList;
	}
	
	public static void outputResult_median(String path,ArrayList<Long> result) throws IOException {
		FileWriter fw = new FileWriter(path,true);
		BufferedWriter bw = new BufferedWriter(fw);
		String line = "";
		for(int i = 0;i < result.size();i ++) {
			Long e = result.get(i);
			if(i == (result.size()-1))
			    line += e;
			else
				line += e+",";
		}
		bw.write(line+"\n");
		bw.close();
		fw.close();
	}
	
	public static void outputResult_ave(String path,ArrayList<Double> result) throws IOException {
		FileWriter fw = new FileWriter(path,true);
		BufferedWriter bw = new BufferedWriter(fw);
		String line = "";
		for(int i = 0;i < result.size();i ++) {
			double e = result.get(i);
			if(i == (result.size()-1))
			    line += e;
			else
				line += e+",";
		}
		bw.write(line+"\n");
		bw.close();
		fw.close();
	}
	
	public static void outputHeader(String path,List<String> header) throws IOException {
		FileWriter fw = new FileWriter(path,true);
		BufferedWriter bw = new BufferedWriter(fw);
		String line = "";
		for(int i = 0;i < header.size();i ++) {
			String e = header.get(i);
			if(i == (header.size()-1))
			    line += e;
			else
				line += e+",";
		}
		bw.write("\n\n"+line+"\n\n");
		bw.close();
		fw.close();
	}
	
	public static ArrayList<Object> exeSingleExp(ArrayList<ArrayList<String>> minArmRel,List<Double> insertCopyRatio,ArrayList<String> R,ArrayList<ArrayList<String>> keys,int copyNum,int repeatTime) throws SQLException {
		System.out.println("current schema size: "+R.size());
		System.out.println("current key number: "+keys.size());
		System.out.println("current copy number: "+copyNum);
		String tableName = "table_"+R.size()+"_"+keys.size()+"_"+copyNum;
		deleteTable(tableName);
		createTable(tableName,R);
		ArrayList<ArrayList<String>> copy = createCopyOfMinArmRel(minArmRel,1,copyNum);
		System.out.println("current relation size: "+copy.size());
		insertData(tableName,copy);
		
		ArrayList<ArrayList<ArrayList<String>>> differentRatioCopy = new ArrayList<ArrayList<ArrayList<String>>>();
		int currentIndex = copyNum + 1;
		for(int i = 0;i < insertCopyRatio.size();i ++) {
			double ratio = insertCopyRatio.get(i);
			ArrayList<ArrayList<String>> currentCopy = createCopyOfMinArmRel(minArmRel,currentIndex,(int)(copyNum+copyNum*ratio));
			currentIndex = (int)(copyNum+copyNum*ratio) + 1;
			ArrayList<ArrayList<String>> sum = new ArrayList<ArrayList<String>>();
			if(differentRatioCopy.isEmpty())
				differentRatioCopy.add(currentCopy);
			else {
				sum.addAll(differentRatioCopy.get(differentRatioCopy.size()-1));
				sum.addAll(currentCopy);
				differentRatioCopy.add(sum);
			}
		}
		
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
		for(ArrayList<String> key : keys) {
			for(int i = 0;i< repeatTime;i ++) {
				long selectDataOneWithoutIndex = selectData_one(key,tableName,false);
				selectDataOneNoIndex.add(selectDataOneWithoutIndex);
				long selectDataOneWithIndex = selectData_one(key,tableName,true);
				selectDataOneHavingIndex.add(selectDataOneWithIndex);
				
				long selectDataTwoWithoutIndex = selectData_two(key,tableName,false);
				selectDataTwoNoIndex.add(selectDataTwoWithoutIndex);
				long selectDataTwoWithIndex = selectData_two(key,tableName,true);
				selectDataTwoHavingIndex.add(selectDataTwoWithIndex);
				
				long insertOneTenthDataWithoutIndex = updateData(R,key,differentRatioCopy.get(0),copyNum,tableName,false);
				insertOneTenthDataNoIndex.add(insertOneTenthDataWithoutIndex);
				long insertOneTenthDataWithIndex = updateData(R,key,differentRatioCopy.get(0),copyNum,tableName,true);
				insertOneTenthDataHavingIndex.add(insertOneTenthDataWithIndex);
				
				long insertTwoTenthDataWithoutIndex = updateData(R,key,differentRatioCopy.get(1),copyNum,tableName,false);
				insertTwoTenthDataNoIndex.add(insertTwoTenthDataWithoutIndex);
				long insertTwoTenthDataWithIndex = updateData(R,key,differentRatioCopy.get(1),copyNum,tableName,true);
				insertTwoTenthDataHavingIndex.add(insertTwoTenthDataWithIndex);
				
				long insertThreeTenthDataWithoutIndex = updateData(R,key,differentRatioCopy.get(2),copyNum,tableName,false);
				insertThreeTenthDataNoIndex.add(insertThreeTenthDataWithoutIndex);
				long insertThreeTenthDataWithIndex = updateData(R,key,differentRatioCopy.get(2),copyNum,tableName,true);
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
		System.out.println("*******************************");
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
	
	public static void generalMode(String outputPath,int startSchemaSize,int maxSchemaSize,int repeatTimeOfSameNumKeys,int repeatTimeOfEachKey,List<Integer> copyNumList,List<Double> insertRatio) throws SQLException, IOException {
		for(int schemaSize = startSchemaSize;schemaSize <= maxSchemaSize;schemaSize++) {
			int maxKeyNum = schemaSize;
			ArrayList<String> R = new ArrayList<String>();//schema
			for(int i = 0;i < schemaSize;i ++) {
				R.add("a"+i);
			}
			for(int keyNum = 1;keyNum <= maxKeyNum;keyNum ++) {
				int sumOfAttrOfKeys = 0;
				int sumOfLineOfArmRel = 0;
				ArrayList<ArrayList<Long>> resultList_copy1_median = new ArrayList<ArrayList<Long>>();
				ArrayList<ArrayList<Long>> resultList_copy2_median = new ArrayList<ArrayList<Long>>();
				ArrayList<ArrayList<Long>> resultList_copy3_median = new ArrayList<ArrayList<Long>>();
				ArrayList<ArrayList<Double>> resultList_copy1_ave = new ArrayList<ArrayList<Double>>();
				ArrayList<ArrayList<Double>> resultList_copy2_ave = new ArrayList<ArrayList<Double>>();
				ArrayList<ArrayList<Double>> resultList_copy3_ave = new ArrayList<ArrayList<Double>>();
				for(int j = 0;j < repeatTimeOfSameNumKeys;j ++) {
					ArrayList<ArrayList<String>> keys = null;
					do {
						keys = createDifferentMinimalKeys(R,keyNum);
					}while(keys == null);
					for(ArrayList<String> key : keys) {
						sumOfAttrOfKeys += key.size();
					}
					ArrayList<ArrayList<String>> minArmRel = createMinimalArmstrongRelation(R,keys.size(),keys);
					sumOfLineOfArmRel += minArmRel.size();
					System.out.println("current minimal Armstrong relation size: "+minArmRel.size());
					System.out.println("current Armstrong relation: ");
					for(ArrayList<String> tuple : minArmRel) {
						System.out.println(tuple);
					}
					for(int i = 0;i < copyNumList.size();i++) {
						int copyNum = copyNumList.get(i);
						
						ArrayList<Object> result = exeSingleExp(minArmRel,insertRatio,R,keys,copyNum,repeatTimeOfEachKey);
						if(copyNum == copyNumList.get(0)){
						    resultList_copy1_median.add((ArrayList<Long>)result.get(0));
						    resultList_copy1_ave.add((ArrayList<Double>)result.get(1));
						}
						if(copyNum == copyNumList.get(1)){
							resultList_copy2_median.add((ArrayList<Long>)result.get(0));
							resultList_copy2_ave.add((ArrayList<Double>)result.get(1));
						}
						if(copyNum == copyNumList.get(2)){
							resultList_copy3_median.add((ArrayList<Long>)result.get(0));
							resultList_copy3_ave.add((ArrayList<Double>)result.get(1));
						}
					}
				}
				DecimalFormat df = new DecimalFormat("0.00");
				double aveKeySize1 = sumOfAttrOfKeys/(double)(repeatTimeOfSameNumKeys*keyNum);
				String aveKeySize = df.format(aveKeySize1);
				double aveTupleNumOfArmRel1 = sumOfLineOfArmRel/(double)repeatTimeOfSameNumKeys;
				String aveTupleNumOfArmRel = df.format(aveTupleNumOfArmRel1);
				outputHeader(outputPath,Arrays.asList("schemaSize:"+schemaSize,"keyNum:"+keyNum,"copyNum:"+copyNumList.get(0),"aveKeySize:"+aveKeySize,"aveTupleNumOfArmRel:"+aveTupleNumOfArmRel));
				ArrayList<Long> Result_one_median = getMedianList(resultList_copy1_median);
				outputResult_median(outputPath,Result_one_median);
				ArrayList<Double> Result_one_ave = getAveList(resultList_copy1_ave);
				outputResult_ave(outputPath,Result_one_ave);
				
				outputHeader(outputPath,Arrays.asList("schemaSize:"+schemaSize,"keyNum:"+keyNum,"copyNum:"+copyNumList.get(1),"aveKeySize:"+aveKeySize,"aveTupleNumOfArmRel:"+aveTupleNumOfArmRel));
				ArrayList<Long> Result_two_median = getMedianList(resultList_copy2_median);
				outputResult_median(outputPath,Result_two_median);
				ArrayList<Double> Result_two_ave = getAveList(resultList_copy2_ave);
				outputResult_ave(outputPath,Result_two_ave);
				
				outputHeader(outputPath,Arrays.asList("schemaSize:"+schemaSize,"keyNum:"+keyNum,"copyNum:"+copyNumList.get(2),"aveKeySize:"+aveKeySize,"aveTupleNumOfArmRel:"+aveTupleNumOfArmRel));
				ArrayList<Long> Result_three_median = getMedianList(resultList_copy3_median);
				outputResult_median(outputPath,Result_three_median);
				ArrayList<Double> Result_three_ave = getAveList(resultList_copy3_ave);
				outputResult_ave(outputPath,Result_three_ave);
			}
		}
	}
	
	public static void specialMode(String outputPath,int startSchemaSize,int maxSchemaSize,int repeatTimeOfSameNumKeys,int repeatTimeOfEachKey,List<Integer> copyNumList,List<Double> insertRatio) throws SQLException, IOException {
		for(int schemaSize = startSchemaSize;schemaSize <= maxSchemaSize;schemaSize++) {
			ArrayList<String> R = new ArrayList<String>();
			for(int i = 0;i < schemaSize;i ++) {
				R.add("a"+i);
			}
			int[] keySizeArray;
			if(schemaSize%2 == 0) {
				keySizeArray = new int[]{schemaSize/2};
			}else {
				keySizeArray = new int[]{(schemaSize-1)/2,(schemaSize+1)/2};
			}
			int repeatTimeOfSameNumKeys1 = (int)(repeatTimeOfSameNumKeys/keySizeArray.length);
			int sumOfAttrOfKeys = 0;
			int sumOfLineOfArmRel = 0;
			int keyNum = -1;
			int count = 0;
			ArrayList<ArrayList<Long>> resultList_copy1_median = new ArrayList<ArrayList<Long>>();
			ArrayList<ArrayList<Long>> resultList_copy2_median = new ArrayList<ArrayList<Long>>();
			ArrayList<ArrayList<Long>> resultList_copy3_median = new ArrayList<ArrayList<Long>>();
			ArrayList<ArrayList<Double>> resultList_copy1_ave = new ArrayList<ArrayList<Double>>();
			ArrayList<ArrayList<Double>> resultList_copy2_ave = new ArrayList<ArrayList<Double>>();
			ArrayList<ArrayList<Double>> resultList_copy3_ave = new ArrayList<ArrayList<Double>>();
 			for(int keySize : keySizeArray) {
 				count ++;
 				ArrayList<ArrayList<String>> keys = createMaxMinimalKeys(R,keySize);
 				ArrayList<ArrayList<String>> minArmRel = createMinimalArmstrongRelation(R,keys.size(),keys);
 				keyNum = keys.size();
				
				for(int j = 0;j < repeatTimeOfSameNumKeys1;j ++) {
					for(ArrayList<String> key : keys) {
						sumOfAttrOfKeys += key.size();
					}
					sumOfLineOfArmRel += minArmRel.size();
					System.out.println("current minimal Armstrong relation size: "+minArmRel.size());
					System.out.println("current Armstrong relation: ");
					for(ArrayList<String> tuple : minArmRel) {
						System.out.println(tuple);
					}
					for(int i = 0;i < copyNumList.size();i++) {
						int copyNum = copyNumList.get(i);
						ArrayList<Object> result = exeSingleExp(minArmRel,insertRatio,R,keys,copyNum,repeatTimeOfEachKey);//每个key重复五次
						if(copyNum == copyNumList.get(0)){
						    resultList_copy1_median.add((ArrayList<Long>)result.get(0));
						    resultList_copy1_ave.add((ArrayList<Double>)result.get(1));
						}
						if(copyNum == copyNumList.get(1)){
							resultList_copy2_median.add((ArrayList<Long>)result.get(0));
							resultList_copy2_ave.add((ArrayList<Double>)result.get(1));
						}
						if(copyNum == copyNumList.get(2)){
							resultList_copy3_median.add((ArrayList<Long>)result.get(0));
							resultList_copy3_ave.add((ArrayList<Double>)result.get(1));
						}
					}
				}
				
			}
 			DecimalFormat df = new DecimalFormat("0.00");
			double aveKeySize1 = sumOfAttrOfKeys/(double)(repeatTimeOfSameNumKeys1*keyNum*count);
			String aveKeySize = df.format(aveKeySize1);
			double aveTupleNumOfArmRel1 = sumOfLineOfArmRel/(double)(repeatTimeOfSameNumKeys1*count);
			String aveTupleNumOfArmRel = df.format(aveTupleNumOfArmRel1);
			outputHeader(outputPath,Arrays.asList("schemaSize:"+schemaSize,"keyNum:"+keyNum,"copyNum:"+copyNumList.get(0),"aveKeySize:"+aveKeySize,"aveTupleNumOfArmRel:"+aveTupleNumOfArmRel));
			ArrayList<Long> Result_one_median = getMedianList(resultList_copy1_median);
			outputResult_median(outputPath,Result_one_median);
			ArrayList<Double> Result_one_ave = getAveList(resultList_copy1_ave);
			outputResult_ave(outputPath,Result_one_ave);
			
			outputHeader(outputPath,Arrays.asList("schemaSize:"+schemaSize,"keyNum:"+keyNum,"copyNum:"+copyNumList.get(1),"aveKeySize:"+aveKeySize,"aveTupleNumOfArmRel:"+aveTupleNumOfArmRel));
			ArrayList<Long> Result_two_median = getMedianList(resultList_copy2_median);
			outputResult_median(outputPath,Result_two_median);
			ArrayList<Double> Result_two_ave = getAveList(resultList_copy2_ave);
			outputResult_ave(outputPath,Result_two_ave);
			
			outputHeader(outputPath,Arrays.asList("schemaSize:"+schemaSize,"keyNum:"+keyNum,"copyNum:"+copyNumList.get(2),"aveKeySize:"+aveKeySize,"aveTupleNumOfArmRel:"+aveTupleNumOfArmRel));
			ArrayList<Long> Result_three_median = getMedianList(resultList_copy3_median);
			outputResult_median(outputPath,Result_three_median);
			ArrayList<Double> Result_three_ave = getAveList(resultList_copy3_ave);
			outputResult_ave(outputPath,Result_three_ave);
		}
	}
	
	
	public static void testMaxKeys(String outputPath,int startSchemaSize,int maxSchemaSize,int repeatTimeOfSameNumKeys,int repeatTimeOfEachKey,List<Integer> copyNumList,List<Double> insertRatio) throws SQLException, IOException {
		for(int schemaSize = startSchemaSize;schemaSize <= maxSchemaSize;schemaSize++) {
			ArrayList<String> R = new ArrayList<String>();
			for(int i = 0;i < schemaSize;i ++) {
				R.add("a"+i);
			}
			int[] keySizeArray;
			if(schemaSize%2 == 0) {
				keySizeArray = new int[]{schemaSize/2};
			}else {
				keySizeArray = new int[]{(schemaSize-1)/2,(schemaSize+1)/2};
			}
			repeatTimeOfSameNumKeys = (int)(repeatTimeOfSameNumKeys/keySizeArray.length);
 			for(int keySize : keySizeArray) {
 				ArrayList<ArrayList<String>> keys = createMaxMinimalKeys(R,keySize);
 			}
		}
	}
	
	public static void main(String[] args) throws SQLException, IOException {
		String outputPath = "F:\\javaworkspace\\result\\result.txt";
		File file = new File(outputPath);
		if(!file.exists())
			file.createNewFile();
		
		int startSchemaSize = 3;//start size of schema
		int maxSchemaSize = 7;//max size of schema
		int repeatTimeOfSameNumKeys = 10;
		int repeatTimeOfEachKey = 1;
		List<Integer> copyNumList = Arrays.asList(100000,150000,200000);//copy number of Armstrong relation
		List<Double> insertRatio = Arrays.asList(0.1,0.2,0.3);//insertion ration when inserting data
		//keys size from 1 to |R|
//		generalMode(outputPath,startSchemaSize,maxSchemaSize,repeatTimeOfSameNumKeys,repeatTimeOfEachKey,copyNumList,insertRatio);
		//fixed keys
		specialMode(outputPath,startSchemaSize,maxSchemaSize,repeatTimeOfSameNumKeys,repeatTimeOfEachKey,copyNumList,insertRatio);
	}

}
