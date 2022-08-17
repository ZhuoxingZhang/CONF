package conf;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * start to execute all algorithms
 *
 */
public class Main {
	
	public static void start_to_execute_exp() throws IOException, SQLException {
		List<String> R = null;
		List<FD> Sigma_a = null;
		if(Constant.read_schema_level_from_database) {
			List<Object> res = Utils.import_schema_and_FDs_with_subschema_level();
			R = (List<String>) res.get(0);
			Sigma_a = (List<FD>) res.get(1);
			Utils.write_subschema_level_into_local(R, Sigma_a);//write schema level results into local file for later use
		}else if(Constant.read_schema_level_from_local){
			List<Object> res = Utils.import_schema_and_FDs_with_subschema_level_from_local();
			R = (List<String>) res.get(0);
			Sigma_a = (List<FD>) res.get(1);
		}else if(Constant.read_schema_level_from_database_in_scope){//read schema level from database in parallel (i.e., read specified sub-schema level)
			List<Object> res = Utils.import_schema_and_FDs_with_subschema_level_in_scope(Constant.start, Constant.end);
			R = (List<String>) res.get(0);
			Sigma_a = (List<FD>) res.get(1);
			if(!Constant.write_separately)
				Utils.write_subschema_level_into_local_in_scope(R, Sigma_a, Constant.start, Constant.end);//write schema level results into local file for later use
//			else
//				Utils.write_subschema_level_into_local_in_scope_separately(R, Sigma_a, Constant.start, Constant.end);
		}
		System.out.println("Sigma size : "+Sigma_a.size());
		
		
//		System.out.println("\nexecuting CONF CC decomp...");
//		CONF_CC cc = new CONF_CC();
//		cc.decomp_and_output(R,Sigma_a);
		
		System.out.println("\nexecuting CONF decomp...");
		CONF conf = new CONF();
		conf.decomp_and_output(R,Sigma_a);
		
		System.out.println("\nexecuting CONF Comp decomp...");
		CONF_Comp comp = new CONF_Comp();
		comp.decomp_and_output(R,Sigma_a);
		
		System.out.println("\nexecuting 3NF decomp...");
		CONF_3NF thirdNF = new CONF_3NF();
		thirdNF.decomp_and_output(R,Sigma_a);
		
		System.out.println("\nexecuting basic decomp...");
		CONF_Basic basic = new CONF_Basic();
		basic.decomp_and_output(R,Sigma_a);
	}
	
	public static void main(String[] args) throws IOException, SQLException {
		Main.start_to_execute_exp();
	}

}
