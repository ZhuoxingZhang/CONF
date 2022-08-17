package conf;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JSONParser {
	ArrayList<String> schema;
	ArrayList<FD> fd_list;
	
	public JSONParser() {
		this.schema = new ArrayList<String>();
		this.fd_list = new ArrayList<FD>();
	}
	
	
	
	public ArrayList<String> getSchema() {
		return schema;
	}



	public void setSchema(ArrayList<String> schema) {
		this.schema = schema;
	}



	public ArrayList<FD> getFd_list() {
		return fd_list;
	}



	public void setFd_list(ArrayList<FD> fd_list) {
		this.fd_list = fd_list;
	}



	public void parseJSON(String jsonFileAdd) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(jsonFileAdd);
        int len;
        byte[] bytes = new byte[1024];
        StringBuilder stringBuffer = new StringBuilder();
        while ((len = fileInputStream.read(bytes)) != -1) {
            stringBuffer.append(new String(bytes, 0, len));
        }
        fileInputStream.close();
        JSONObject jsonObject = JSONObject.parseObject(stringBuffer.toString());
        
        int schema_size = Integer.parseInt(jsonObject.get("R").toString());
        for(int j = 0;j < schema_size;j ++) {
        	this.schema.add(""+j);
        }
        String fd_objects = jsonObject.get("fds").toString();
        JSONArray fds = JSONArray.parseArray(fd_objects);
        for(int j = 0;j < fds.size();j ++) {
        	JSONObject fd_json = fds.getJSONObject(j);
        	String lhs = fd_json.get("lhs").toString();
        	lhs = lhs.replaceAll("\\[", "");
        	lhs = lhs.replaceAll("\\]", "");
        	lhs = lhs.replaceAll(" ", "");
        	String[] attr_set_left = lhs.split(",");
        	
        	String rhs = fd_json.get("rhs").toString();
        	rhs = rhs.replaceAll("\\[", "");
        	rhs = rhs.replaceAll("\\]", "");
        	rhs = rhs.replaceAll(" ", "");
        	String[] attr_set_right = rhs.split(",");
        	
        	if(attr_set_left.length == 1 && "".equals(attr_set_left[0]) && attr_set_right.length == 1 && "".equals(attr_set_right[0])) {
        		FD f = new FD(new ArrayList<String>(),new ArrayList<String>());
        		if(!fd_list.contains(f)) {
        			fd_list.add(f);
        		}
        		continue;
        	}
        	
        	if(attr_set_left.length == 1 && "".equals(attr_set_left[0])) {
        		for(String r : attr_set_right) {
            		FD f = new FD(new ArrayList<String>(),Arrays.asList(r));
            		if(!fd_list.contains(f)) {
            			fd_list.add(f);
            		}
            	}
        		continue;
        	}
        	
        	if(attr_set_right.length == 1 && "".equals(attr_set_right[0])) {
        		FD f = new FD(Arrays.asList(attr_set_left),new ArrayList<String>());
        		if(!fd_list.contains(f)) {
        			fd_list.add(f);
        		}
        		continue;
        	}
        	
        	for(String r : attr_set_right) {
        		FD f = new FD(Arrays.asList(attr_set_left),Arrays.asList(r));
        		if(!fd_list.contains(f)) {
        			fd_list.add(f);
        		}
        	}
        }
	}
    public static void main(String[] args) throws IOException {
        JSONParser json = new JSONParser();
        json.parseJSON("C:\\Users\\FD Results\\Complete Data\\adult.json");
        ArrayList<String> schema = json.getSchema();
        ArrayList<FD> fds = json.getFd_list();
        System.out.println("schema : "+schema);
        System.out.println("fds : ");
        int count = 0;
        for(FD fd : fds) {
        	System.out.println(count ++ + " : "+fd.getLeftHand()+" -> "+fd.getRightHand());
        }
    }
}
