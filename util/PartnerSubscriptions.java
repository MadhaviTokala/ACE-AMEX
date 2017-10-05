package com.americanexpress.smartserviceengine.common.util;

import com.americanexpress.smartserviceengine.common.constants.ApiConstants;

public enum PartnerSubscriptions {
	
	INTACCT(ApiConstants.SERVICE_CODE_CHECK, ApiConstants.SERVICE_CODE_ACH, ApiConstants.SERVICE_CODE_CARD),
	REGALPAY(ApiConstants.SERVICE_CODE_CHECK),
	TRADESHIFT(ApiConstants.SERVICE_CODE_CHECK, ApiConstants.SERVICE_CODE_ACH, ApiConstants.SERVICE_CODE_CARD),
	SAGE(ApiConstants.SERVICE_CODE_CHECK, ApiConstants.SERVICE_CODE_ACH, ApiConstants.SERVICE_CODE_CARD);
	
	private String[] subscriptions;
	
	private PartnerSubscriptions(String... subscriptions){
		this.subscriptions = subscriptions;
	}

	public static String[] getSubscriptions(String partnerName) {
		for(PartnerSubscriptions partnerSubscription : values()){
			if(partnerSubscription.name().equalsIgnoreCase(partnerName)){
				return partnerSubscription.subscriptions;
			}
		}
		return null;
	}
	
}
