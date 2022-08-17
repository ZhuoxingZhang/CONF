package conf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
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
import java.util.Set;

/**
 * CONF decomposition with minimizing key number
 *
 */
public class CONF {
	private double ave_comp_min_keys_time;
	private long exe_time;
	private int max_key_num_on_all_schemata;//maximal number of minimal keys
	private int l_D;//max level on all schemata of D
	private int l_D_total;//sum of level on all schemata of D
	private double ave_l_for_each_schema;
	private double ave_l_for_3NF_schema;
	private int num_of_all_schemata;
	private double ave_key_num_on_all_schemata;
	private int num_of_BCNF_schemata;
	private double ave_key_num_on_BCNF_schemata;
	private int num_of_3NF_schemata;
	private double ave_key_num_on_3NF_schemata;
	private HashMap<Integer,Integer> map_BCNF_keynum_to_num;//in BCNF, the number of N-CONF
	private HashMap<Integer,Integer> map_3NF_keynum_to_num;//in 3NF, the number of N-CONF
	private long time_line_3_to_8;//runtime between line 3 - 8 of pseudo-code
	private long time_line_8;//sum of time on line 8
	private long time_line_9_to_14;//runtime between line 9 - 14 of pseudo-code
	private long time_line_15;//runtime between line 15 of pseudo-code
	private long time_line_16_to_18;//runtime between line 16 - 18 of pseudo-code
	
	
	public CONF() {
		this.ave_comp_min_keys_time = 0d;
		this.exe_time = 0l;
		this.max_key_num_on_all_schemata = 0;
		this.l_D = 0;
		this.l_D_total = 0;
		this.ave_l_for_each_schema = 0d;
		this.ave_l_for_3NF_schema = 0d;
		this.num_of_all_schemata = 0;
		this.ave_key_num_on_all_schemata = 0d;
		this.num_of_BCNF_schemata = 0;
		this.ave_key_num_on_BCNF_schemata = 0d;
		this.num_of_3NF_schemata = 0;
		this.ave_key_num_on_3NF_schemata = 0d;
		this.map_BCNF_keynum_to_num = new HashMap<Integer,Integer>();
		this.map_3NF_keynum_to_num = new HashMap<Integer,Integer>();
		this.time_line_3_to_8 = 0l;
		this.time_line_8 = 0l;
		this.time_line_9_to_14 = 0l;
		this.time_line_15 = 0l;
		this.time_line_16_to_18 = 0l;
	}
	
	
	public void output_results() throws IOException {
		File f = new File(Constant.output_add);
		FileWriter fw = new FileWriter(f,true);
		BufferedWriter bw = new BufferedWriter(fw);
		
		String dataset_name = Constant.dataset_name;
		
		Iterator<Integer> iter_bcnf = this.map_BCNF_keynum_to_num.keySet().iterator();
		String bcnf_dis = "[";//key number distributions in BCNF
		List<String> bcnf_dis_list = new ArrayList<String>();
		while(iter_bcnf.hasNext()) {
			int key = iter_bcnf.next();
			int value = this.map_BCNF_keynum_to_num.get(key);
			bcnf_dis_list.add(key+" : "+value);
		}
		Collections.sort(bcnf_dis_list,new Comparator<String>() {//decreasing order of n_KEY

			@Override
			public int compare(String o1, String o2) {
				int key1 = Integer.parseInt(o1.split(" : ")[0]);
				int key2 = Integer.parseInt(o2.split(" : ")[0]);
				if(key1 < key2)
					return 1;
				else if(key1 > key2)
					return -1;
				else
					return 0;
			}
			
		});
		for(int i = 0;i < bcnf_dis_list.size();i ++) {
			String s = bcnf_dis_list.get(i);
			if(i != bcnf_dis_list.size() - 1)
				bcnf_dis += "{ " + s + " } ";
			else
				bcnf_dis += "{ " + s + " }";
		}
		bcnf_dis += "]";
		
		Iterator<Integer> iter_3nf = this.map_3NF_keynum_to_num.keySet().iterator();
		String thirdnf_dis = "[";//key number distributions in 3NF
		List<String> thirdnf_dis_list = new ArrayList<String>();
		while(iter_3nf.hasNext()) {
			int key = iter_3nf.next();
			int value = this.map_3NF_keynum_to_num.get(key);
			thirdnf_dis_list.add(key+" : "+value);
		}
		Collections.sort(thirdnf_dis_list,new Comparator<String>() {//decreasing order of n_KEY

			@Override
			public int compare(String o1, String o2) {
				int key1 = Integer.parseInt(o1.split(" : ")[0]);
				int key2 = Integer.parseInt(o2.split(" : ")[0]);
				if(key1 < key2)
					return 1;
				else if(key1 > key2)
					return -1;
				else
					return 0;
			}
			
		});
		for(int i = 0;i < thirdnf_dis_list.size();i ++) {
			String s = thirdnf_dis_list.get(i);
			if(i != thirdnf_dis_list.size() - 1)
				thirdnf_dis += "{ " + s + " } ";
			else
				thirdnf_dis += "{ " + s + " }";
		}
		thirdnf_dis += "]";
		
		
		//a line of results's column names:
		//alg,dataset_name,runtime,average runtime of computing minimal keys,total runtime of computing minimal keys
		//runtime line 3 - 8,runtime line 9 - 14,runtime line 15, runtime 16 - 18
		//l_D,l_D_total,ave_l_for_each_schema,ave_l_for_3NF_schema,max key number on all schemata
		//number of all schemata,average key number on all schemata,
		//number of BCNF schemata,average key number on BCNF schemata,distributions of key number in BCNF
		//number of 3NF schemata,average key number on 3NF schemata,distributions of key number in 3NF
		String result = "CONF,"+dataset_name+","+this.exe_time+","+String.format("%.2f",this.ave_comp_min_keys_time)+","+this.time_line_8+","
		+this.time_line_3_to_8+","+this.time_line_9_to_14+","+this.time_line_15+","+this.time_line_16_to_18+","
		+this.l_D+","+this.l_D_total+","+String.format("%.2f",this.ave_l_for_each_schema)+","+String.format("%.2f",this.ave_l_for_3NF_schema)+","+this.max_key_num_on_all_schemata+","
		+this.num_of_all_schemata+","+String.format("%.2f",this.ave_key_num_on_all_schemata)+","
		+this.num_of_BCNF_schemata+","+String.format("%.2f",this.ave_key_num_on_BCNF_schemata)+","+bcnf_dis
		+","+this.num_of_3NF_schemata+","+String.format("%.2f",this.ave_key_num_on_3NF_schemata)+","+thirdnf_dis;
		
		bw.write(result+"\n");
		
		bw.close();
		fw.close();
	}
	
	/**
	 * 
	 * @param R
	 * @param Sigma functional dependency set 
	 * @param Sigma_a_bar an atomic cover of Sigma
	 * @return
	 */
	public  ArrayList<Schema> exe_decomp(List<String> R,List<FD> Sigma,List<FD> Sigma_a_bar) {
		System.out.println("dataset : "+Constant.file_add);
		System.out.println("FD : "+Constant.fd_add);
		
		ArrayList<FD> Sigma_a = new ArrayList<FD>();//Sigma_a
		for(FD fd : Sigma_a_bar) {
			if(!Sigma_a.contains(fd))
				Sigma_a.add(fd);
		}
		ArrayList<SchemaInfo> Sigma_a_with_dec_n = new ArrayList<SchemaInfo>();//decreasing order of n_key
		ArrayList<Long> comp_min_keys_time_list = new ArrayList<Long>();//time list of computing minimal keys 
		long start_time_line_3_to_8 = new Date().getTime();
		
		
		for(FD X_A : Sigma_a_bar) {
//			System.out.println("debug1 : "+X_A.getLeftHand()+" - > "+X_A.getRightHand());
			ArrayList<String> XA = new ArrayList<String>();
			for(String a : X_A.getLeftHand()) {//get XA set
				if(!XA.contains(a))
					XA.add(a);
			}
			for(String a : X_A.getRightHand()) {//get XA set
				if(!XA.contains(a))
					XA.add(a);
			}
			for(FD Y_B : Sigma_a_bar) {//get Y -> B
				ArrayList<String> YB = new ArrayList<String>();
				for(String a : Y_B.getLeftHand()) {//get YB set
					if(!YB.contains(a))
						YB.add(a);
				}
				for(String a : Y_B.getRightHand()) {//get YB set
					if(!YB.contains(a))
						YB.add(a);
				}
				
				if(XA.containsAll(YB) && !Utils.getAttrSetClosure(R, Y_B.getLeftHand(), Sigma_a_bar).containsAll(XA)) {
					//X -> A is critical
					ArrayList<FD> Sigma_a_NO_XA = new ArrayList<FD>();
					for(FD fd : Sigma_a) {
						if(!fd.equals(X_A))
							Sigma_a_NO_XA.add(fd);
					}
					ArrayList<String> X_closure = Utils.getAttrSetClosure(R, X_A.getLeftHand(), Sigma_a_NO_XA);
					if(X_closure.containsAll(X_A.getRightHand())){//Sigma_a - X -> A implies X -> A 
						Sigma_a.remove(X_A);//Sigma_a removes X -> A
					}
				}
			}
			if(Sigma_a.contains(X_A)) {//compute sub-schema{XA}'s number of minimal keys
				long start = new Date().getTime();
				ArrayList<FD> Sigma_a_bar_XA_projection = Utils.getProjection(Sigma_a_bar, XA);//get XA projection
				int n_XA = 0;//key number of sub-schema XA
				ArrayList<String> minKey = Utils.getRefinedMinKey(Sigma_a_bar_XA_projection, X_A.getLeftHand(), XA);
				List<List<String>> minKeys = Utils.getMinimalKeys(Sigma_a_bar_XA_projection, XA, minKey);
				long end = new Date().getTime();
				long comp_min_keys_time = end - start;//get time of computing minimal keys for a given schema
				comp_min_keys_time_list.add(comp_min_keys_time);
				
				n_XA = minKeys.size();
				
				Sigma_a_with_dec_n.add(new SchemaInfo(X_A,XA,n_XA));
			}
		}
		
		long end_time_line_3_to_8 = new Date().getTime();
		this.time_line_3_to_8 = end_time_line_3_to_8 - start_time_line_3_to_8;
		System.out.println("\n=============================\n");
		System.out.println("runtime between line 3 - 8 : "+this.time_line_3_to_8);
		
		//compute average time of computing each minimal keys
		long sum_time = 0l;
		for(long t : comp_min_keys_time_list) {
			sum_time += t;
		}
		System.out.println("\n=============================\n");
		this.ave_comp_min_keys_time = sum_time/(double)comp_min_keys_time_list.size();
		System.out.println("\n\naverage time of computing each minimal keys : "+String.format("%.2f", ave_comp_min_keys_time)+" ms.\n");
		
		System.out.println("\n=============================\n");
		this.time_line_8 = sum_time;
		System.out.println("\n\ntotal time of computing all minimal keys : "+this.time_line_8+" ms.\n");
		
//		System.out.println("\n\nAfter line 8 has been processed,\nthe intermediate result Sigma_a:");
//		for(FD fd : Sigma_a) {
//			System.out.println(fd.getLeftHand()+" -> "+fd.getRightHand());
//		}
		
		ArrayList<Schema> D = new ArrayList<Schema>();
		Collections.sort(Sigma_a_with_dec_n,new Comparator<SchemaInfo>() {//decreasing order of n_KEY

			@Override
			public int compare(SchemaInfo o1, SchemaInfo o2) {
				if(o1.getN_key() < o2.getN_key())
					return 1;
				else if(o1.getN_key() > o2.getN_key())
					return -1;
				else
					return 0;
			}
			
		});
		
		
		long start_time_line_9_to_14 = new Date().getTime();
		for(SchemaInfo pair : Sigma_a_with_dec_n) {//pair (FD,subschema,n_key)
			FD X_A = pair.getFd();
			ArrayList<String> XA = pair.getSubschema();
			int n_key = pair.getN_key();
			ArrayList<FD> Sigma_a_NO_XA = new ArrayList<FD>();
			for(FD fd : Sigma_a) {
				if(!fd.equals(X_A))
					Sigma_a_NO_XA.add(fd);
			}
			
			ArrayList<String> X_closure = Utils.getAttrSetClosure(R, X_A.getLeftHand(), Sigma_a_NO_XA);
			
			
			if(X_closure.containsAll(X_A.getRightHand())){//Sigma_a - X -> A implies X -> A 
				Sigma_a.remove(X_A);//Sigma_a removes X -> A
			}else {//D = D U {(XA,Sigma_a[XA])}
				ArrayList<FD> projection_XA = Utils.getProjection(Sigma_a_bar, XA);//get projection Sigma_a_bar[XA]
				Schema p = new Schema(XA,projection_XA,n_key,X_A.getLevel());
				
				if(!D.contains(p))
					D.add(p);
			}
		}
		long end_time_line_9_to_14 = new Date().getTime();
		this.time_line_9_to_14 = end_time_line_9_to_14 - start_time_line_9_to_14;
		System.out.println("\n=============================\n");
		System.out.println("runtime between line 9 - 14 : "+this.time_line_9_to_14);
		
		long start_time_line_15 = new Date().getTime();
		ArrayList<Schema> removal = new ArrayList<Schema>();//need to remove from D
		for(int i = 0;i < D.size();i ++) {
			Schema pair1 = D.get(i);
			List<String> S = pair1.getAttr_set();
			for(int j = 0;j < D.size();j ++) {
				if(i == j)
					continue;
				Schema pair2 = D.get(j);
				List<String> S_prime = pair2.getAttr_set();
				if(S_prime.containsAll(S)) {
					removal.add(pair1);
					break;
				}
			}
		}
		for(Schema p : removal) {
			D.remove(p);
		}
		long end_time_line_15 = new Date().getTime();
		this.time_line_15 = end_time_line_15 - start_time_line_15;
		System.out.println("\n=============================\n");
		System.out.println("runtime on line 15 : "+this.time_line_15);
		
		long start_time_line_16_to_18 = new Date().getTime();
		boolean exist = false;//whether existing or not (R',Sigma') in D, such that R' -> R in closure of Sigma (Sigma^+)
		for(Schema p : D) {
			List<String> R_prime = p.getAttr_set();
			ArrayList<String> R_prime_closure = Utils.getAttrSetClosure(R, R_prime, Sigma_a_bar);
			if(R_prime_closure.containsAll(R)) {
				exist = true;
				break;
			}
		}
		if(!exist) {
			ArrayList<String> K = Utils.getRefinedMinKey(Sigma, R, R);
			ArrayList<FD> K_projection = Utils.getProjection(Sigma_a_bar, K);
			ArrayList<String> minKey2 = Utils.getRefinedMinKey(K_projection, K, K);
			int n_K = Utils.getMinimalKeys(K_projection, K, minKey2).size();
			D.add(new Schema(K,K_projection,n_K));//D = D U {(K,Sigma_a[K])}
		}
		long end_time_line_16_to_18 = new Date().getTime();
		this.time_line_16_to_18 = end_time_line_16_to_18 - start_time_line_16_to_18;
		System.out.println("\n=============================\n");
		System.out.println("runtime between line 16 - 18 : "+this.time_line_16_to_18+"\n\n");
		
//		System.out.println("\nre-order with decreasing of n_key...\n");
//		Collections.sort(D,new Comparator<Schema>() {//decreasing order of n_key
//
//			@Override
//			public int compare(Schema o1, Schema o2) {
//				if(o1.getN_key() < o2.getN_key())
//					return 1;
//				else if(o1.getN_key() > o2.getN_key())
//					return -1;
//				else
//					return 0;
//			}
//			
//		});
		
		return D;
		
	}
	
	//return max key num schema in BCNF and 3NF
	public ArrayList<Schema> decomp_and_output(List<String> R,List<FD> Sigma_a) throws IOException, SQLException {
	    
	    long start = new Date().getTime();
		ArrayList<Schema> D = exe_decomp(R, Sigma_a, Sigma_a);//decreasing order of n_key
		long end = new Date().getTime();
		this.exe_time = end - start;
		
		//compute l_D and max key number of D, if level and key number of a schema are null, then compute them
//		long start_com_level_and_key_num_if_null = new Date().getTime();
		for(Schema schema : D) {
			schema.calKeyNumAndLevelIfNull(R);
			int schema_level = schema.getSchema_level();
			if(schema_level == 0) {
				System.out.println("debug : level = 0, schema info "+schema.toString());
			}
			int key_num = schema.getN_key();
			this.l_D = schema_level > this.l_D ? schema_level : this.l_D;
			this.l_D_total += schema_level;
			this.max_key_num_on_all_schemata = key_num > this.max_key_num_on_all_schemata ? key_num : this.max_key_num_on_all_schemata;
			
		}
		this.ave_l_for_each_schema = this.l_D_total/((double)D.size());
//		long end_com_level_and_key_num_if_null = new Date().getTime();
//		this.time_com_level_and_key_num_if_null = end_com_level_and_key_num_if_null - start_com_level_and_key_num_if_null;
//		System.out.println("\n=============================\n");
//		System.out.println("runtime of com_level_and_key_num_if_null : "+this.time_com_level_and_key_num_if_null+" ms.\n\n");
		
		//output statistics
		System.out.println("decompisitions finished, statistics as follows:\n");
		System.out.println("exe time : "+exe_time+" ms.");
		System.out.println("\n=============================\n");
		
		System.out.println("\nmaximal candidate key number for schemata : "+this.max_key_num_on_all_schemata+"\n");
		
		ArrayList<Schema> BCNF_schemata = new ArrayList<Schema>();//BCNF databases of all decomposition results
		ArrayList<Schema> thirdNF_schemata = new ArrayList<Schema>();//3NF databases of all decomposition results, not BCNF
		
		System.out.println("\n=============================\n");
		System.out.println("all decomposition results :\n");
		int sum_of_key_num_on_all_decomp = 0;
		for(Schema p : D) {
			List<String> schema = p.getAttr_set();
			List<FD> fds = p.getFd_set();
			System.out.println("schema info : "+p.toString());
			System.out.println("schemata : "+schema +"\nFD set : ");
			for(FD f : fds) {
				System.out.println(f.toString());
			}
			
			boolean isBCNF = Utils.isBCNF(schema,fds);
			if(isBCNF)
				BCNF_schemata.add(p);
			else
				thirdNF_schemata.add(p);
			
			sum_of_key_num_on_all_decomp += p.getN_key();
			System.out.println("number of minimal keys : "+p.getN_key());
			System.out.println("########################\n");
		}
		
		this.ave_key_num_on_all_schemata = sum_of_key_num_on_all_decomp/(double)D.size();
		System.out.println("\n=============================\n");
		System.out.println("average key num on all schematas : "+this.ave_key_num_on_all_schemata);
		
		System.out.println("\n=============================\n");
		this.num_of_all_schemata = D.size();
		System.out.println("final decomposition number of schemata : "+this.num_of_all_schemata);
		
		System.out.println("\n=============================\n");
		this.num_of_BCNF_schemata = BCNF_schemata.size();
		System.out.println("final decomposition number of schemata in BCNF : "+this.num_of_BCNF_schemata);
		
		System.out.println("\n=============================\n");
		System.out.println("BCNF schemata as follows :\n");
		int sum_of_key_num_on_BCNF_decomp = 0;
		for(Schema p : BCNF_schemata) {
			List<String> schema = p.getAttr_set();
			List<FD> fds = p.getFd_set();
			System.out.println("BCNF schema info : "+p.toString());
			System.out.println("BCNF schemata : "+schema +"\nFD set : ");
			for(FD f : fds) {
				System.out.println(f.toString());
			}
			sum_of_key_num_on_BCNF_decomp += p.getN_key();
			
			if(this.map_BCNF_keynum_to_num.containsKey(p.getN_key())) {//record distribution of different key number in BCNF
				int num_keynum = this.map_BCNF_keynum_to_num.get(p.getN_key());
				this.map_BCNF_keynum_to_num.put(p.getN_key(), ++ num_keynum);
			}else {
				this.map_BCNF_keynum_to_num.put(p.getN_key(), 1);
			}
			
			System.out.println("number of minimal keys : "+p.getN_key());
			System.out.println("########################\n");
		}
		
		if(BCNF_schemata.size() != 0)
			this.ave_key_num_on_BCNF_schemata = sum_of_key_num_on_BCNF_decomp/(double)BCNF_schemata.size();
		System.out.println("\n=============================\n");
		System.out.println("average key num on BCNF schematas : "+this.ave_key_num_on_BCNF_schemata);
		
		System.out.println("\n=============================\n");
		this.num_of_3NF_schemata = thirdNF_schemata.size();
		System.out.println("final decomposition number of schemata in 3NF, not in BCNF : "+this.num_of_3NF_schemata);
		
		System.out.println("\n=============================\n");
		System.out.println("3NF (not BCNF) schemata as follows :\n");
		int sum_of_key_num_on_3NF_decomp = 0;
		int sum_3NF_level = 0;
		for(Schema p : thirdNF_schemata) {
			sum_3NF_level += p.getSchema_level();
			List<String> schema = p.getAttr_set();
			List<FD> fds = p.getFd_set();
			System.out.println("3NF schema info : "+p.toString());
			System.out.println("3NF schemata : "+schema +"\nFD set : ");
			for(FD f : fds) {
				System.out.println(f.toString());
			}
			sum_of_key_num_on_3NF_decomp += p.getN_key();
			
			if(this.map_3NF_keynum_to_num.containsKey(p.getN_key())) {//record distribution of different key number in 3NF
				int num_keynum = this.map_3NF_keynum_to_num.get(p.getN_key());
				this.map_3NF_keynum_to_num.put(p.getN_key(), ++ num_keynum);
			}else {
				this.map_3NF_keynum_to_num.put(p.getN_key(), 1);
			}
			
			System.out.println("number of minimal keys : "+p.getN_key());
			System.out.println("########################\n");
		}
		
		if(thirdNF_schemata.size() != 0) {
			this.ave_key_num_on_3NF_schemata = sum_of_key_num_on_3NF_decomp/(double)thirdNF_schemata.size();
			this.ave_l_for_3NF_schema = sum_3NF_level/(double)thirdNF_schemata.size();
		}
		System.out.println("\n=============================\n");
		System.out.println("average key num on 3NF schematas : "+this.ave_key_num_on_3NF_schemata);
		
		//output distributions of key number in BCNF
		System.out.println("\n=============================\n");
		System.out.println("distributions of key number in BCNF : ");
		Iterator<Integer> iter1 = this.map_BCNF_keynum_to_num.keySet().iterator();
		while(iter1.hasNext()) {
			int key = iter1.next();
			int value = this.map_BCNF_keynum_to_num.get(key);
			System.out.println("key number of subschema : "+key+" , number : "+value);
		}
		
		//output distributions of key number in 3NF
		System.out.println("\n=============================\n");
		System.out.println("distributions of key number in 3NF : ");
		Iterator<Integer> iter2 = this.map_3NF_keynum_to_num.keySet().iterator();
		while(iter2.hasNext()) {
			int key = iter2.next();
			int value = this.map_3NF_keynum_to_num.get(key);
			System.out.println("key number of subschema : "+key+" , number : "+value);
		}
		
		//output results into file
		this.output_results();
		
		ArrayList<Schema> BCNF_3NF_maxN = new ArrayList<Schema>();//return max key num schema in BCNF and 3NF
//		BCNF_3NF_maxN.add(BCNF_schemata.get(0));
//		BCNF_3NF_maxN.add(thirdNF_schemata.get(0));
		return BCNF_3NF_maxN;
	}
	
	//batch execution of data sets under file
//	public static void batch_exe(File file,String output_add) throws IOException, SQLException {
//		if(file.isDirectory()) {
//			for(File f : file.listFiles()) {
//				batch_exe(f,output_add);
//			}
//		}else {
//			String dataset_add = file.getAbsolutePath();
//			CONF conf = new CONF();
//			conf.decomp_and_output();
//		}
//	}



}
