package conf;

public class Constant {
	public final static String root_path = "C:\\Users";
	public final static String output_add = root_path + "\\exp results\\confJournal_result.csv";
	
	public final static boolean read_schema_level_from_local = true;//import schema level from local file
	
	public final static boolean read_schema_level_from_database = false;//import schema level from database
	
	public final static boolean read_schema_level_from_database_in_scope = false;//import specified schema level from database
	public final static boolean write_separately = true;//write a schema level into file when computed
	public final static int start = 2477;//start index of Sigma_a
	public final static int end = 2485;//end index of Sigma_a (inclusive)
	
//	public final static String dataset_name = "abalone";
//	public final static int col_num = 9;
//	public final static int row_num = 4177;
//	public final static String split = ",";
	
//	public final static String dataset_name = "adult";
//	public final static int col_num = 15;
//	public final static String split = ";";
	
//	public final static String dataset_name = "breast";
//	public final static int col_num = 11;
//	public final static String split = ",";
	
//	public final static String dataset_name = "bridges";
//	public final static int col_num = 13;
//	public final static String split = ",";
	
//	public final static String dataset_name = "diabetic";
//	public final static int col_num = 30;
//	public final static String split = ",";
	
//	public final static String dataset_name = "echo";
//	public final static int col_num = 13;
//	public final static String split = ",";
	
//	public final static String dataset_name = "fd-red";
//	public final static int col_num = 30;
//	public final static String split = ",";
	
//	public final static String dataset_name = "hepatitis";
//	public final static int col_num = 20;
//	public final static String split = ",";
	
//	public final static String dataset_name = "letter";
//	public final static int col_num = 17;
//	public final static String split = ",";
	
	public final static String dataset_name = "lineitem";
	public final static int col_num = 16;
	public final static int row_num = 6001215;
	public final static String split = ",";
	
//	public final static String dataset_name = "ncvoter";
//	public final static int col_num = 19;
//	public final static String split = ",";
	
//	public final static String dataset_name = "pdbx";
//	public final static int col_num = 13;
//	public final static String split = ";";
	
//	public final static String dataset_name = "uniprot";//512000 rows,30 columns version
//	public final static int col_num = 30;
//	public final static String split = ",";
	
//	public final static String dataset_name = "china_weather";
//	public final static int col_num = 18;
//	public final static String split = ",";
	
	
	public final static String file_add = root_path + "\\dataset\\data\\"+dataset_name+".csv";
	public final static String fd_add = root_path + "\\dataset\\FD\\"+dataset_name+".json";
	public final static String subschema_level_add = root_path + "\\dataset\\subschema_level\\"+"subschema_level("+dataset_name+").txt";
}
