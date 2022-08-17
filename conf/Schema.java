package conf;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Schema {
	/**
	 * store a schema info
	 */
	private List<String> attr_set;
	private List<FD> fd_set;
	private int n_key;
	private int schema_level;
	
	public Schema(List<String> attr_set,List<FD> fd_set) {
		this.attr_set = attr_set;
		this.fd_set = fd_set;
		this.n_key = 0;
		this.schema_level = 0;
	}
	
	public Schema(List<String> attr_set,List<FD> fd_set,int n_key) {
		this.attr_set = attr_set;
		this.fd_set = fd_set;
		this.n_key = n_key;
		this.schema_level = 0;
	}
	
	public Schema(List<String> attr_set,List<FD> fd_set,int n_key,int schema_level) {
		this.attr_set = attr_set;
		this.fd_set = fd_set;
		this.n_key = n_key;
		this.schema_level = schema_level;
	}
	
	public int getN_key() {
		return n_key;
	}

	public void setN_key(int n_key) {
		this.n_key = n_key;
	}

	public List<String> getAttr_set() {
		return attr_set;
	}
	public void setAttr_set(ArrayList<String> attr_set) {
		this.attr_set = attr_set;
	}
	public List<FD> getFd_set() {
		return fd_set;
	}
	public void setFd_set(ArrayList<FD> fd_set) {
		this.fd_set = fd_set;
	}
	
	public int getSchema_level() {
		return schema_level;
	}

	public void setSchema_level(int schema_level) {
		this.schema_level = schema_level;
	}
	
	/**
	 * if level and key number is null(0), calculate them
	 * @param R raw dataset's schema
	 * @throws SQLException 
	 */
	public void calKeyNumAndLevelIfNull(List<String> R) throws SQLException {
		if(this.n_key == 0) {
			ArrayList<String> minKey =  Utils.getRefinedMinKey(fd_set, attr_set, attr_set);
			List<List<String>> minKeys =  Utils.getMinimalKeys(fd_set, attr_set, minKey);
			this.n_key = minKeys.size();
		}
		if(this.schema_level == 0) {
			if(fd_set.isEmpty()) {//the entire schema is a key
				schema_level = 1;
				return;
			}
//			List<List<String>> r_on_attrset = Utils.getSubRelation(R, attr_set, r);
			for(FD Y_B : fd_set) {
				List<String> Y = new ArrayList<String>();
				for(String a : R) {
					if(Y_B.getLeftHand().contains(a))
						Y.add(a);
				}
//				int l_Y = Utils.getLevelOfAttrSet(attr_set, Y, r_on_attrset) ;
				int l_Y = Utils.get_level_from_database(attr_set, Y);
				schema_level = l_Y > schema_level ? l_Y : schema_level;
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Schema) {
			Schema pair = (Schema)obj;
			if(this.attr_set.containsAll(pair.getAttr_set()) && this.attr_set.size() == pair.getAttr_set().size()
					&& this.fd_set.containsAll(pair.getFd_set()) && this.fd_set.size() == pair.getFd_set().size())
				return true;
			else
				return false;
				
		}else
			return false;
	}

	@Override
	public String toString() {
		return "Schema [attr_set=" + attr_set + ", fd_set_size=" + fd_set.size() + ", n_key=" + n_key + ", schema_level="
				+ schema_level + "]";
	}
	
	
	
}
