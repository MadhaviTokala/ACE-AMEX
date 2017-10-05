package com.americanexpress.smartserviceengine.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.americanexpress.smartserviceengine.common.util.PartnerIdMapping;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.ServiceConstants;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.CardAccountEnrollmentDetailsVO;
import com.americanexpress.smartserviceengine.common.vo.GetCardAccountStatusVO;
import com.americanexpress.smartserviceengine.dao.CardActivationDAO;
import com.americanexpress.smartserviceengine.dao.GetCardAccountStatusDAO;
import com.americanexpress.smartserviceengine.dao.InsertExceptionDataDAO;
import com.americanexpress.smartserviceengine.service.CardAccountStatusService;
import com.itextpdf.text.log.Logger;

public class CardAccountStatusServiceImpl implements CardAccountStatusService {
	private AmexLogger LOGGER=AmexLogger.create(CardAccountStatusServiceImpl.class);
	
	@Resource
	private GetCardAccountStatusDAO getCardAccountStatusDAO;
	
	@Resource
	private CardActivationDAO cardActivationDAO;
	
	@Resource
	private InsertExceptionDataDAO insertExceptionDataDAO;
	
	@Resource
	private TivoliMonitoring tivoliMonitoring;

    @Autowired
    private PartnerIdMapping partnerIdMapping;

	@Override
	public void upadteCardAccounts(String apiMsgId) {
		LOGGER.info(apiMsgId, "smartserviceengine Scheduler", "CardAccountStatusServiceImpl", "upadteCardAccounts", 
				"Initiating inactivate card account validation", AmexLogger.Result.success, "START");
		Map<String,Object> inMap=new HashMap<String,Object>();
		Map<String,Object> outMap=null;
		String lastUpdateTime=null;
		int totalCount=0;
		inMap.put(SchedulerConstants.IN_LST_UPDT_TS,ServiceConstants.MIN_DEFAULT_TIMESTAMP);
		boolean isFirstAttempt=true;
		boolean isContinueLoop=true;
		while(isContinueLoop){
			isContinueLoop=false;
			LOGGER.info("inMap to fetch records"+inMap.toString());
			outMap=getCardAccountStatusDAO.execute(inMap,apiMsgId);
			if(isFirstAttempt){
				isFirstAttempt = false;
				LOGGER.info(apiMsgId, "SmartServiceEngine Scheduler", "CardAccountStatusServiceImpl", "upadteCardAccounts", "Accounts that has to be made inactive", AmexLogger.Result.success, 
					 "START", "TOTAL NUMBER OF Accounts TO BE PROCESSED ON " + GenericUtilities.getCurrentTimeAS_YYYY_MM_DD_T_HH_MM_SS(), 
					 String.valueOf(outMap.get(SchedulerConstants.OU_PYMT_REQ_MSG_CNT)));
			}
			if(outMap!=null && !outMap.isEmpty()){
				totalCount=(Integer)outMap.get(SchedulerConstants.OU_PYMT_REQ_MSG_CNT);
				if(StringUtils.stripToEmpty((String)outMap.get(SchedulerConstants.RESP_CD)).equalsIgnoreCase(SchedulerConstants.ACEC3000)){
					lastUpdateTime=updateAccountStatus(outMap,apiMsgId);
				}
				else{
					LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_GCA_102_ERR_CD, TivoliMonitoring.SSE_SP_GCA_102_ERR_MSG, apiMsgId));
					LOGGER.info(apiMsgId, "SmartServiceEngine scheduler", "CardAccountStatusServiceImpl", "updateAccountStatus",
							"Get Card Account Status Sp Execution is Failure",AmexLogger.Result.success,"Success");
				}
			}
			isContinueLoop = hasMoreRecords(apiMsgId, lastUpdateTime, totalCount);
			if(isContinueLoop && lastUpdateTime != null){
				inMap=new HashMap<String,Object>();
				inMap.put(SchedulerConstants.IN_LST_UPDT_TS,lastUpdateTime);
			}
		}
		LOGGER.info(apiMsgId, "smartserviceengine Scheduler", "CardAccountStatusServiceImpl", "upadteCardAccounts", 
				"End of inactivating card account", AmexLogger.Result.success, "END");
	}
	
	@SuppressWarnings("unchecked")
	public String updateAccountStatus(Map<String,Object> outMap,String apiMsgId){
		List<GetCardAccountStatusVO> results=(List<GetCardAccountStatusVO>)outMap.get(SchedulerConstants.RESULT_SET1);
		String lastUpdateTS=null;
		Map<String,Object> updateInMap=new HashMap<String,Object>();
		Map<String,Object> updateOutMap=null;
		if(!GenericUtilities.isNullOrEmpty(results)){
			for(GetCardAccountStatusVO accVO:results){
				LOGGER.info(apiMsgId, "SmartServiceEngine scheduler", "CardAccountStatusServiceImpl", "updateAccountStatus",
						"Updating card Account status as Inactive",AmexLogger.Result.success,"","Data",accVO.toString());
				updateInMap.put(SchedulerConstants.IN_PRTR_MAC_ID,accVO.getPrtrMacId());
				updateInMap.put(SchedulerConstants.IN_CARD_NO,accVO.getCardNO());
				updateInMap.put(SchedulerConstants.IN_CARD_ACT_STA,SchedulerConstants.STATUS_RSVP_EXPIRED);
				updateInMap.put(SchedulerConstants.IN_TNC_TIMESTAMP,ServiceConstants.MAX_DEFAULT_TIMESTAMP);
				updateInMap.put(SchedulerConstants.IN_RSVP_CD,accVO.getRsvpCode());
				updateInMap.put(SchedulerConstants.IN_PYMT_SYS_STA_CD,SchedulerConstants.STATUS_INACTIVE);
				updateInMap.put(SchedulerConstants.IN_PYMT_SYS_STA_DS,SchedulerConstants.STATUSDESC_INACTIVE);
				updateInMap.put(SchedulerConstants.IN_CARD_ACCT_STA_DS,SchedulerConstants.STATUSDESC_RSVP_EXPIRED);
				updateInMap.put(SchedulerConstants.IN_UPD_STATUS_IND,SchedulerConstants.STATUS_INACTIVE);
				updateInMap.put(SchedulerConstants.IN_PYMT_SYS_RESP_CD,SchedulerConstants.VNGCANCL);
				updateInMap.put(SchedulerConstants.IN_PYMT_SYS_RESP_DS,SchedulerConstants.VNGCANCL_DESC);
				LOGGER.info("inmap to update status"+updateInMap.toString());
				updateOutMap=cardActivationDAO.execute(updateInMap, apiMsgId);
				if(updateOutMap != null && !updateOutMap.isEmpty()){
					if(StringUtils.stripToEmpty((String)updateOutMap.get(SchedulerConstants.RESP_CD)).equalsIgnoreCase(SchedulerConstants.ACEC3000)){
						LOGGER.info(apiMsgId, "SmartServiceEngine scheduler", "CardAccountStatusServiceImpl", "updateAccountStatus",
								"Updating card Account status as Inactive is success",AmexLogger.Result.success,"Success","partnerId",accVO.getPrtrMacId(),
                                "partnerName", partnerIdMapping.getPartnerName(accVO.getPrtrMacId(), apiMsgId));
						// log the exception data here
						Map<String, Object> inMap = formCardStatusPeriodChkExceptionDetailsInMap(accVO);
						Map<String,Object> exceptionOutMap=insertExceptionDataDAO.execute(inMap,apiMsgId);
						if(exceptionOutMap!=null && !exceptionOutMap.isEmpty()){
							if(StringUtils.stripToEmpty((String)updateOutMap.get(SchedulerConstants.RESP_CD)).equalsIgnoreCase(SchedulerConstants.ACEC3000)){
								LOGGER.info(apiMsgId, "SmartServiceEngine scheduler", "CardAccountStatusServiceImpl", "updateAccountStatus",
										"Insert Exception Data Execution is success",AmexLogger.Result.success,"Success","partnerId",accVO.getPrtrMacId(),
                                        "partnerName", partnerIdMapping.getPartnerName(accVO.getPrtrMacId(), apiMsgId));
							} else {
								LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_GCM_004_ERR_CD, TivoliMonitoring.SSE_SP_GCM_004_ERR_MSG, apiMsgId));
								LOGGER.info(apiMsgId, "SmartServiceEngine scheduler", "CardAccountStatusServiceImpl", "updateAccountStatus",
										"Insert Exception Data Execution is Failure",AmexLogger.Result.success,"Success","partnerId",accVO.getPrtrMacId(),
                                        "partnerName", partnerIdMapping.getPartnerName(accVO.getPrtrMacId(), apiMsgId));
							}
						}
						
					} else {
						LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_GCA_101_ERR_CD, TivoliMonitoring.SSE_SP_GCA_101_ERR_MSG, apiMsgId));
						LOGGER.info(apiMsgId, "SmartServiceEngine scheduler", "CardAccountStatusServiceImpl", "updateAccountStatus",
								"Updating card Account status as Inactive failed",AmexLogger.Result.failure,"Fail","partnerId",accVO.getPrtrMacId(),
                                "partnerName", partnerIdMapping.getPartnerName(accVO.getPrtrMacId(), apiMsgId));
					}
				}
				lastUpdateTS=StringUtils.stripToEmpty(accVO.getLastUpdateTS().toString());

			}
		}
		return lastUpdateTS;
	}
	
	private boolean hasMoreRecords(String apiMsgId, String lastUpdateTime, int totalRecordCount){
		boolean isContinueLoop = false;
		if(totalRecordCount > SchedulerConstants.DEFAULT_PAGINATION_COUNT && !GenericUtilities.isNullOrEmpty(lastUpdateTime)){
			isContinueLoop = true;
		}
		return isContinueLoop;
	}
	
	private Map<String,Object> formCardStatusPeriodChkExceptionDetailsInMap(GetCardAccountStatusVO accVO){
		Map<String,Object> inMap=new HashMap<String,Object>();
		inMap.put(ApiConstants.IN_PRTR_MAC_ID,accVO.getPrtrMacId());		
			inMap.put(ApiConstants.IN_EXCPT_CD , StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_RSVP_EXCP_CD)));
			inMap.put(ApiConstants.IN_EXCPT_DS , StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_ACCOUNT_EXCP_DS)));		
		
		inMap.put(ApiConstants.IN_PRTR_CUST_ORG_ID,accVO.getPrtrOrgId());  
		inMap.put(ApiConstants.IN_PRTR_SPLY_ORG_ID , ApiConstants.CHAR_SPACE);
		
		inMap.put(ApiConstants.IN_EXCPT_TYPE_TX,StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_RSVP_EXCP_TYPE)));
		inMap.put(ApiConstants.IN_PRTR_ACCT_ID, accVO.getPrtrAcctId());   
		inMap.put(ApiConstants.IN_BUY_PYMT_ID, ApiConstants.CHAR_SPACE);      
		inMap.put(ApiConstants.IN_SPLY_ACCT_ID, ApiConstants.CHAR_SPACE);     
		return inMap;
	}

}
