package com.americanexpress.smartserviceengine.common.util;

import java.util.Map;

/**
 * Class that is used to manage {@link ThreadLocal} variables in SSE. 
 * For now it manages apiMsgId and Statistics Map specific to a Request in ThreadLocal mode.
 * 
 * @version $Revision$ - $Date$ by $Author$ vvudem
 * You need to call clear() method for each ThreadLocal object once you are done with these.
 * If you don't call clear() method you may lead to Hung Thread or ThreadSafety issues
 */
public class ThreadLocalManager {
	public static final ThreadLocal<String> apiMsgId = new ThreadLocal<String>();
	
	public static final ThreadLocal<Map<String, String>> requestStatistics = new ThreadLocal<Map<String, String>>();
	
	public static final ThreadLocal<String> bipOrgId = new ThreadLocal<String>();
	
	public static final ThreadLocal<String> emmEsbServerName = new ThreadLocal<String>();
	
	public static ThreadLocal<String> partnerId = new ThreadLocal<String>();
	
	public static ThreadLocal<String> cidPin = new ThreadLocal<String>();

	/**
	 * @return the partnerid
	 */
	public static String getPartnerId() {
		return partnerId.get();
	}
	
	public static void setPartnerId(String newpartnerId){
		partnerId.set(newpartnerId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * You need to call clear() method for each ThreadLocal object once you are done with these.
	 * If you don't call clear() method you may lead to Hung Thread or ThreadSafety issues
	 */
    public static void clearPartnerId() {
    	partnerId.remove();
    }

    
	private ThreadLocalManager() {
        super();
    }

    public static String getBipOrgid() {
		return bipOrgId.get();
	}
    
    public static void setBipOrgid(String uniqueBipOrgId) {
    	bipOrgId.set(uniqueBipOrgId);
    }
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * You need to call clear() method for each ThreadLocal object once you are done with these.
	 * If you don't call clear() method you may lead to Hung Thread or ThreadSafety issues
	 */
    public static void clearBiporgid() {
    	bipOrgId.remove();
    }

    
    public static String getEmmEsbServerName() {
		return emmEsbServerName.get();
	}
    
    public static void setEmmEsbServerName(String uniqueEmmEsbServerName) {
    	emmEsbServerName.set(uniqueEmmEsbServerName);
    }
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * You need to call clear() method for each ThreadLocal object once you are done with these.
	 * If you don't call clear() method you may lead to Hung Thread or ThreadSafety issues
	 */
    public static void clearEmmEsbServerName() {
    	emmEsbServerName.remove();
    }

    public static String getApiMsgId() {
        return apiMsgId.get();
    }

    public static void setApiMsgId(String uniqueApiMsgId) {
    	apiMsgId.set(uniqueApiMsgId);
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * You need to call clear() method for each ThreadLocal object once you are done with these.
	 * If you don't call clear() method you may lead to Hung Thread or ThreadSafety issues
	 */
    public static void clearApiMsgId() {
    	apiMsgId.remove();
    }

    public static Map<String, String> getRequestStatistics() {
        return requestStatistics.get();
    }

    public static void setRequestStatistics(Map<String, String> requestStatisticsMap) {
    	requestStatistics.set(requestStatisticsMap);
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * You need to call clear() method for each ThreadLocal object once you are done with these.
	 * If you don't call clear() method you may lead to Hung Thread or ThreadSafety issues
	 */
    
    public static void clearRequestStatistics() {
    	requestStatistics.remove();
    }
	public static void setCidPin(String newcidpin){
		cidPin.set(newcidpin);
	}

    public static void clearCidPin() {
    	cidPin.remove();
    }
    
    public static String getCidPin() {
    	return cidPin.get();
    }
    
}
