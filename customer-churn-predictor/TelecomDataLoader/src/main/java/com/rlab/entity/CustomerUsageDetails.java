/**
 * @author Riaz Mohammed
 *
 * 
 */

package com.rlab.entity;

import java.io.Serializable;

import com.rlab.cache.BigMemoryItem;

public class CustomerUsageDetails implements Serializable,BigMemoryItem {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int vMailMessage;
	private float dayMins;
	private float eveMins;
	private float nightMins;
	private float intlMins;
	
	private int customerSrvCalls;
	
	private int dayCalls;
	private float dayCharge;
	
	private int eveCalls;
	private float eveCharge;
	
	private int nightCalls;
	private float nightCharge;
	
	private int intlCalls;
	private float intlCharge;
	
	private String areaCode;
	private String phone;
	
	private String key;
	
	
	
	

	public CustomerUsageDetails(int vMailMessage, float dayMins, float eveMins, float nightMins, float intlMins,
			int customerSrvCalls, int dayCalls, float dayCharge, int eveCalls, float eveCharge, int nightCalls,
			float nightCharge, int intlCalls, float intlCharge, String areaCode, String phone) {
		super();
		this.vMailMessage = vMailMessage;
		this.dayMins = dayMins;
		this.eveMins = eveMins;
		this.nightMins = nightMins;
		this.intlMins = intlMins;
		this.customerSrvCalls = customerSrvCalls;
		this.dayCalls = dayCalls;
		this.dayCharge = dayCharge;
		this.eveCalls = eveCalls;
		this.eveCharge = eveCharge;
		this.nightCalls = nightCalls;
		this.nightCharge = nightCharge;
		this.intlCalls = intlCalls;
		this.intlCharge = intlCharge;
		this.areaCode = areaCode;
		this.phone = phone;
		calculateKey();
	}


	public float getEveMins() {
		return eveMins;
	}

	public void setEveMins(float eveMins) {
		this.eveMins = eveMins;
	}

	
	public int getvMailMessage() {
		return vMailMessage;
	}

	public void setvMailMessage(int vMailMessage) {
		this.vMailMessage = vMailMessage;
	}

	public float getDayMins() {
		return dayMins;
	}

	public void setDayMins(float dayMins) {
		this.dayMins = dayMins;
	}

	public float getNightMins() {
		return nightMins;
	}

	public void setNightMins(float nightMins) {
		this.nightMins = nightMins;
	}

	public float getIntlMins() {
		return intlMins;
	}

	public void setIntlMins(float intlMins) {
		this.intlMins = intlMins;
	}

	public int getCustomerSrvCalls() {
		return customerSrvCalls;
	}

	public void setCustomerSrvCalls(int customerSrvCalls) {
		this.customerSrvCalls = customerSrvCalls;
	}

	public int getDayCalls() {
		return dayCalls;
	}

	public void setDayCalls(int dayCalls) {
		this.dayCalls = dayCalls;
	}

	public float getDayCharge() {
		return dayCharge;
	}

	public void setDayCharge(float dayCharge) {
		this.dayCharge = dayCharge;
	}

	public int getEveCalls() {
		return eveCalls;
	}

	public void setEveCalls(int eveCalls) {
		this.eveCalls = eveCalls;
	}

	public float getEveCharge() {
		return eveCharge;
	}

	public void setEveCharge(float eveCharge) {
		this.eveCharge = eveCharge;
	}

	public int getNightCalls() {
		return nightCalls;
	}

	public void setNightCalls(int nightCalls) {
		this.nightCalls = nightCalls;
	}

	public float getNightCharge() {
		return nightCharge;
	}

	public void setNightCharge(float nightCharge) {
		this.nightCharge = nightCharge;
	}

	public int getIntlCalls() {
		return intlCalls;
	}

	public void setIntlCalls(int intlCalls) {
		this.intlCalls = intlCalls;
	}

	public float getIntlCharge() {
		return intlCharge;
	}

	public void setIntlCharge(float intlCharge) {
		this.intlCharge = intlCharge;
	}

	public String getAreaCode() {
		return areaCode;
	}

	public void setAreaCode(String areaCode) {
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
		CustomerUsageDetails other = (CustomerUsageDetails) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}
	
	
	
}
