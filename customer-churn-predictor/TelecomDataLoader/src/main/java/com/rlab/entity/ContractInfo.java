/**
 * @author Riaz Mohammed
 *
 * 
 */

package com.rlab.entity;

import java.io.Serializable;

import com.rlab.cache.BigMemoryItem;

public class ContractInfo    implements Serializable,BigMemoryItem{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int accountLength;
	private int churn;
	private int intlPlan;
	private int vMailPlan;
	private String state;
	private int areaCode;
	private String phone;
	private String key;
	
	public ContractInfo(int accountLength, int churn, int intlPlan, int vMailPlan, String state, int areaCode,
			String phone) {
		super();
		this.accountLength = accountLength;
		this.churn = churn;
		this.intlPlan = intlPlan;
		this.vMailPlan = vMailPlan;
		this.state = state;
		this.areaCode = areaCode;
		this.phone = phone;
		calculateKey();
	}

	public int getAccountLength() {
		return accountLength;
	}

	public void setAccountLength(int accountLength) {
		this.accountLength = accountLength;
	}

	public int getChurn() {
		return churn;
	}

	public void setChurn(int churn) {
		this.churn = churn;
	}

	public int getIntlPlan() {
		return intlPlan;
	}

	public void setIntlPlan(int intlPlan) {
		this.intlPlan = intlPlan;
	}

	public int getvMailPlan() {
		return vMailPlan;
	}

	public void setvMailPlan(int vMailPlan) {
		this.vMailPlan = vMailPlan;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public int getAreaCode() {
		return areaCode;
	}

	public void setAreaCode(int areaCode) {
		this.areaCode = areaCode;
		calculateKey();
	}
	
	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
		calculateKey();
	}
	public String getKey() {
		return key;
	}
	
	private String calculateKey(){
		this.key=this.areaCode+"-"+this.phone;
		return this.key;
	}

	@Override
	public String toString() {
		return "ContractInfo [accountLength=" + accountLength + ", churn=" + churn + ", intlPlan=" + intlPlan
				+ ", vMailPlan=" + vMailPlan + ", state=" + state + ", areaCode=" + areaCode + ", phone=" + phone
				+ ", key=" + key + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContractInfo other = (ContractInfo) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}
	
	
	
}
