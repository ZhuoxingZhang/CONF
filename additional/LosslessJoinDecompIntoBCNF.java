package additional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import conf.*;

/**
 * we implement a lossless join decomposition into BCNF algorithm here
 *
 */
public class LosslessJoinDecompIntoBCNF {
	public static void write_content_into_local(String output,List<String> line_list,boolean append) throws IOException {
		File file = new File(output);
		if(!file.exists())
			file.createNewFile();
		FileWriter fw = new FileWriter(output,append);
		BufferedWriter bw = new BufferedWriter(fw);
		for(String line : line_list) {
			bw.write(line+"\n");
		}
		bw.close();
		fw.close();
	}
	
	public static boolean isKey(List<String> attrSet, List<String> schema, List<FD> Sigma) {
		List<String> closure = Utils.getAttrSetClosure(schema, attrSet, Sigma);
		if(closure.containsAll(schema) && closure.size() == schema.size()) {
			return true;
		}else
			return false;
	}
	
	
	public static List<Schema> exe_decomp(List<String> U,List<FD> Sigma_a) throws SQLException, IOException {
		List<Schema> decomp = new ArrayList<Schema>();
		
		List<String> X = new ArrayList<String>();//X <- U
		X.addAll(U);
		List<String> Y = new ArrayList<String>();//Y <- empty set
		while(!(X.size() == Y.size() && X.containsAll(Y))) {//X != Y
			Y.clear();Y.addAll(X);//Y <- X
			String Z = null;//Z <- empty
			boolean repeat = true;
			while(Y.size() > 2 && repeat) {//|Y| > 2
				repeat = false;
				List<String> remove_from_Y = new ArrayList<String>();//attributes that need to be removed from Y
				outer:
				for(String A : Y) {
					for(String B : Y) {
						if(A.equals(B))
							continue;
						List<String> Y_minus_AB = new ArrayList<String>();
						Y_minus_AB.addAll(Y);
						Y_minus_AB.remove(A);
						Y_minus_AB.remove(B);
						List<String> CL = Utils.getAttrSetClosure(U, Y_minus_AB, Sigma_a);
//						System.out.println("CL | "+CL.toString()+" , A | "+A);
						if(CL.contains(A)) {//check if Y - AB -> A is logically implied by FD set
							remove_from_Y.add(B);//record B to be removed
							Z = A;
							repeat = true;
							break outer;//jump from double for loop
						}
					}
				}
//				System.out.println("Y | "+Y.toString());
				Y.removeAll(remove_from_Y);//update Y
			}
			//remove Z from X and output a relation schema Y
			X.remove(Z);
			List<String> s = new ArrayList<String>();
			s.addAll(Y);
			List<FD> FD_proj_over_Y = Utils.getProjection(Sigma_a, s);//for a sub-schema Y, all FD set over Y
			decomp.add(new Schema(s,FD_proj_over_Y));
//			System.out.println("schema info | "+s.toString()+" , FD size : "+FD_proj_over_Y.size());
		}
		return decomp;
	}
	public static void main(String[] args) throws SQLException, IOException {
		String main_output_path = "C:\\second experiment\\lossless_join_BCNF_decomp\\BCNF_decomp_results.txt";
		String output_path = "C:\\second experiment\\lossless_join_BCNF_decomp\\"+Constant.dataset_name+"_BCNF_decomp.txt";
		List<Object> schema_info = Utils.import_schema_and_FDs();
		List<String> U = (List<String>) schema_info.get(0);
		List<FD> Sigma_a = (List<FD>) schema_info.get(1);
		
		//main results
		int subschema_num = 0;
		int sum_min_key_num_on_subschema = 0;
		double ave_min_key_num_on_subschema = 0;
		String dist = null;//distribution
		double ratio_lost_FD = 0;//lost FDs/all FDs
		double ratio_lost_key = 0;//keys in lost FDs/all FDs
		double decomp_cost_time = 0;
		
		String main_res = Constant.dataset_name+",";//results
		List<String> output = new ArrayList<String>();//log
		
		Set<FD> all_preserved_FD = new HashSet<FD>();//all preserved FDs that are in any of sub-schemas
		
		System.out.println("starting to decompose...");
		
		long start = System.currentTimeMillis();
		List<Schema> decomp = LosslessJoinDecompIntoBCNF.exe_decomp(U,Sigma_a);
		long end = System.currentTimeMillis();
		
		decomp_cost_time = end - start;
		subschema_num = decomp.size();
		main_res += subschema_num+",";
		
		output.add("decomp cost time : "+decomp_cost_time+" ms...");
		decomp.sort(new Comparator<Schema>() {

			@Override
			public int compare(Schema o1, Schema o2) {//decreasing order of FD number
				int fd_num_1 = o1.getFd_set().size();
				int fd_num_2 = o2.getFd_set().size();
				if(fd_num_1 < fd_num_2)
					return 1;
				else if(fd_num_1 > fd_num_2)
					return -1;
				else
					return 0;
			}
			
		});
		System.out.println("finished to decompose...");
		output.add("lossless join decompsitions into BCNF as follows : \n");
		Map<Integer,Integer> distribution = new HashMap<Integer,Integer>();//key= key number of a schema,value = count of schemas with the number of key
		List<Integer> key_num_list = new ArrayList<Integer>();
		for(int i = 0;i < decomp.size();i ++) {
			output.add("count | "+i);
			Schema schema  = decomp.get(i);
			List<String> R = schema.getAttr_set();
			List<FD> sigma = schema.getFd_set();
			Set<Set<String>> key_set = new HashSet<Set<String>>();
			for(FD fd : sigma) {
				Set<String> k = new HashSet<String>(fd.getLeftHand());
				key_set.add(k);
			}
			sum_min_key_num_on_subschema += key_set.size();//sum up key numbers
			if(distribution.containsKey(key_set.size())) {
				int count = distribution.get(key_set.size());
				distribution.put(key_set.size(), ++ count);
			}else {
				distribution.put(key_set.size(), 1);
				key_num_list.add(key_set.size());
			}
			all_preserved_FD.addAll(sigma);
			output.add(R.toString());//output
			for(FD fd : sigma) {
				output.add(fd.toString());
				output.add("preserved FD is Key for sub-schema : "+isKey(fd.getLeftHand(),R,sigma));
			}
			output.add("\n##################\n");
		}
		
		ave_min_key_num_on_subschema = sum_min_key_num_on_subschema/(double)subschema_num;
		main_res += ave_min_key_num_on_subschema+",";
		
		output.add("distributions : ");
		dist = "[";
		key_num_list.sort(null);
		for(int i = key_num_list.size() - 1;i >= 0;i --) {
			dist += "["+key_num_list.get(i)+" : "+distribution.get(key_num_list.get(i))+"]";
		}
		dist += "]";
		output.add(dist);
		main_res += dist+",";
		
		List<FD> all_eliminated_FD = new ArrayList<FD>();
		all_eliminated_FD.addAll(Sigma_a);
		all_eliminated_FD.removeAll(all_preserved_FD);
		
		ratio_lost_FD = all_eliminated_FD.size()/(double)Sigma_a.size();
		main_res += ratio_lost_FD+",";
		
		output.add("\n*******************\n");
		output.add("all eliminated FDs as follows £º \n");
		int key_num_in_lost_FDs = 0;
		for(int i = 0;i < all_eliminated_FD.size();i ++) {
			output.add("count | "+i);
			output.add(all_eliminated_FD.get(i).toString());
			boolean isKey = isKey(all_eliminated_FD.get(i).getLeftHand(),U,Sigma_a);
			if(isKey)
				key_num_in_lost_FDs ++;
			output.add("eliminated FD is Key for schema : "+isKey);
			output.add("\n##################\n");
		}
		
		ratio_lost_key = key_num_in_lost_FDs/(double)Sigma_a.size();
		main_res += ratio_lost_key+","+decomp_cost_time;
		
		write_content_into_local(main_output_path,Arrays.asList(main_res),true);//output main results
		
		write_content_into_local(output_path,output,false);//output
	}

}
