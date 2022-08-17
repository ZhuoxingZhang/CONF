package conf;

import java.util.List;

public class FD {
	/**
	 * functional dependency
	 * format:
	 * leftHand -> rightHand
	 */
	private List<String> leftHand;
	private List<String> rightHand;
	private int n_key;//key number, for fd X -> A,use union of left and right as schema, Sigma[XA] as FD set, then get key number on that
	private int level;//Level of Cardinality Constraint(update inefficiency/join efficiency),the same FD would have different level under different schema and Sigma
	
	public FD() {
		
	}
	
	public FD(List<String> leftHand, List<String> rightHand) {
		this.leftHand = leftHand;
		this.rightHand = rightHand;
		this.level = 0;
	}
	
	public FD(List<String> leftHand, List<String> rightHand,int level,int n_key) {
		this.leftHand = leftHand;
		this.rightHand = rightHand;
		this.level = level;
		this.n_key = n_key;
	}

	public List<String> getLeftHand() {
		return leftHand;
	}

	public void setLeftHand(List<String> leftHand) {
		this.leftHand = leftHand;
	}

	public List<String> getRightHand() {
		return rightHand;
	}

	public void setRightHand(List<String> rightHand) {
		this.rightHand = rightHand;
	}
	
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}
	
	public int getN_key() {
		return n_key;
	}

	public void setN_key(int n_key) {
		this.n_key = n_key;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FD) {
			FD fd = (FD)obj;
			if(fd.getLeftHand().containsAll(this.leftHand) && fd.getLeftHand().size() == this.leftHand.size() &&
					fd.getRightHand().containsAll(this.rightHand) && fd.getRightHand().size() == this.rightHand.size())
				return true;
			else
				return false;
				
		}else
			return false;
	}

	@Override
	public String toString() {
		return "FD [leftHand=" + leftHand + ", rightHand=" + rightHand + "]";
	}
	
	
}
