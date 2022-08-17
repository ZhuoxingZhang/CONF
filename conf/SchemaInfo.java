package conf;

import java.util.ArrayList;

public class SchemaInfo {
	private FD fd;
	private ArrayList<String> subschema;
	private int n_key;
	private int schema_level;
	
	public SchemaInfo(FD fd,ArrayList<String> subschema,int n_key) {
		this.fd = fd;
		this.subschema = subschema;
		this.n_key = n_key;
		this.schema_level = 0;
	}
	
	public SchemaInfo() {
		
	}
	
	public SchemaInfo(FD fd,ArrayList<String> subschema,int n_key,int schema_level) {
		this.fd = fd;
		this.subschema = subschema;
		this.n_key = n_key;
		this.schema_level = schema_level;
	}
	
	public FD getFd() {
		return fd;
	}

	public void setFd(FD fd) {
		this.fd = fd;
	}

	public ArrayList<String> getSubschema() {
		return subschema;
	}



	public void setSubschema(ArrayList<String> subschema) {
		this.subschema = subschema;
	}



	public int getN_key() {
		return n_key;
	}



	public void setN_key(int n_key) {
		this.n_key = n_key;
	}


	public int getSchema_level() {
		return schema_level;
	}

	public void setSchema_level(int schema_level) {
		this.schema_level = schema_level;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SchemaInfo) {
			SchemaInfo pair = (SchemaInfo)obj;
			if(this.fd.equals(pair.getFd()) && this.subschema.containsAll(pair.getSubschema())	&& this.subschema.size() == pair.getSubschema().size())
				return true;
			else
				return false;
				
		}else
			return false;
	}
	

}
