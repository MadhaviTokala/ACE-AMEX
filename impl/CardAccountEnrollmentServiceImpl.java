package com.americanexpress.smartserviceengine.service.impl;

import static com.americanexpress.smartserviceengine.common.constants.ServiceConstants.APPLICATION_NAME;
import static com.americanexpress.smartserviceengine.common.constants.ServiceConstants.DEFAULT_PAGINATION_COUNT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import com.americanexpress.ace.vng.bean.CardAccountType;
import com.americanexpress.ace.vng.dao.FetchCardAccountsDAO;
import com.americanexpress.ace.vng.enrollment.helper.VNGEnrollmentHelper;
import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.corporate.client.data.CardAccountInfoResponseVO;
import com.americanexpress.corporate.client.service.CardAccountInfoService;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.ServiceConstants;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.PartnerIdMapping;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.CardAccountEnrollmentDetailsVO;
import com.americanexpress.smartserviceengine.common.vo.PaginationDataVO;
import com.americanexpress.smartserviceengine.common.vo.VNGEnrollmentVO;
import com.americanexpress.smartserviceengine.dao.CardAccountDetailsDAO;
import com.americanexpress.smartserviceengine.dao.InsertExceptionDataDAO;
import com.americanexpress.smartserviceengine.service.CardAccountEnrollmentService;

/**
 * This class is responsible for fetching active card details from database and
 * periodically validating the card account information. If the card status has been modified to cancelled, suspended or replaced, 
 * this job will mark the card account as inactive in the database.
 * @author dkakka
 */
public class CardAccountEnrollmentServiceImpl implements CardAccountEnrollmentService {

	private static AmexLogger LOGGER = AmexLogger.create(CardAccountEnrollmentService.class);
	private static final String EVENT_NAME = CardAccountEnrollmentService.class.getSimpleName();

	@Resource
	private TivoliMonitoring tivoliMonitoring;

	@Autowired
	private CardAccountInfoService cardAccountInfoService;

    @Autowired
    private PartnerIdMapping partnerIdMapping;
	
	@Resource
	private CardAccountDetailsDAO cardAccountDetailsDAO;

	@Resource
	private InsertExceptionDataDAO insertExceptionDataDAO;
	
	@Resource
	private FetchCardAccountsDAO fetchCardAccountsDAO;
	
	@Resource
	private VNGEnrollmentHelper vngEnrollmentHelper;
	
	@Value("${PAGINATION_COUNT}")
	private String paginationCount;
	

	public void validateCardAccounts(){
		String apiMsgId = ThreadLocalManager.getApiMsgId();
		try{
			LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "validateCardAccounts", "Initiating periodic card account validation", 
					AmexLogger.Result.success, "START");
			PaginationDataVO<CardAccountEnrollmentDetailsVO> paginationDataVO = new PaginationDataVO<CardAccountEnrollmentDetailsVO>();
			paginationDataVO.setLastUpdateTime(ServiceConstants.MIN_DEFAULT_TIMESTAMP);
			boolean isContinueLoop = true;
			boolean isFirstAttempt = true;
			while(isContinueLoop){
				isContinueLoop = false; // Making it false in the beginning to avoid deadlock issues. will set to true if needed later.
				cardAccountDetailsDAO.getActiveCardAccounts(paginationDataVO); //fetching active card accounts from the ACE database
				List<CardAccountEnrollmentDetailsVO> results = paginationDataVO.getResults();
				if(isFirstAttempt){
					isFirstAttempt = false;
					LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "validateCardAccounts", "PERIODIC CARD ACCOUNT VALIDATION", AmexLogger.Result.success, "START", 
						 "TOTAL NUMBER OF CARDS TO BE VALIDATED ON" + GenericUtilities.getTodaysDateMMDDYYYY(), 
						 String.valueOf(paginationDataVO.getTotalRecordCount()));
				}
				//System.out.println("results - "+results.size());
				if(!GenericUtilities.isNullOrEmpty(results)){
					String lastUpdateTime = null;
					for(CardAccountEnrollmentDetailsVO cardAccountVO : results){	//TODO Executors to be used here.
						/*if(cardAccountVO.getCardNumber().equalsIgnoreCase("378528724702000")||
								cardAccountVO.getCardNumber().equalsIgnoreCase("379463140012009") || 
								cardAccountVO.getCardNumber().equalsIgnoreCase("378730702851003") ||
								cardAccountVO.getCardNumber().equalsIgnoreCase("379611179001000") ){*/
							
						boolean libertyPartner = EnvironmentPropertiesUtil.getProperty(ServiceConstants.LIBERTY_PARTNER_ID).equals(cardAccountVO.getPartnerMacId());
						LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "validateCardAccounts", "Initiating periodic card account validation", 
								AmexLogger.Result.success,"", "libertyPartner",String.valueOf(libertyPartner),"partnerId",cardAccountVO.getPartnerMacId(),
                                "partnerName", partnerIdMapping.getPartnerName(cardAccountVO.getPartnerMacId(), apiMsgId));
						Map<String, Object> resultsMap = cardAccountInfoService.validateCardAccount(cardAccountVO.getCardNumber(),cardAccountVO.getPartnerMacId(), libertyPartner,false);
						boolean isActiveUSCorpLiabCard = (Boolean) resultsMap.get(ServiceConstants.IS_VALID_CARD);
						
						boolean success = (Boolean) resultsMap.get(ServiceConstants.OPERATION_SUCCESSFUL);
						String responseCode = (String) resultsMap.get(ServiceConstants.RESPONSE_CODE);
						String responseDesc = (String) resultsMap.get(ServiceConstants.RESPONSE_MSG);
						Object responseVO = resultsMap.get(ServiceConstants.RESPONSE_VO);
						boolean isValidExpDate = true;
						
						if(responseVO != null && responseVO instanceof CardAccountInfoResponseVO){							
							// Commented below line to check to skip the updating of the record if card is expired as part of US543792
							// isValidExpDate = CommonUtils.isValidCardExpiryDate(((CardAccountInfoResponseVO)responseVO).getExpiryDate(),apiMsgId);
							LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "validateCardAccounts", "CARD ACCOUNT", AmexLogger.Result.success, 
									"COMPLETED", "isPeriodicCheckValidExpDate", String.valueOf(isValidExpDate),
									"expiryDate",((CardAccountInfoResponseVO)responseVO).getExpiryDate(),"partnerId",cardAccountVO.getPartnerMacId(),
                                    "partnerName", partnerIdMapping.getPartnerName(cardAccountVO.getPartnerMacId(), apiMsgId));
						}
						if(success && (!isActiveUSCorpLiabCard || !isValidExpDate)){
							
							if(responseVO != null && responseVO instanceof CardAccountInfoResponseVO){

								cardAccountVO.setCardStatus(((CardAccountInfoResponseVO)responseVO).getCardStatus().getStatusCode());
								cardAccountVO.setCardStatusRspCd(responseCode);
								cardAccountVO.setCardStatusRspDesc(responseDesc);
								LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "validateCardAccounts", "DEACTIVATING CARD ACCOUNT", AmexLogger.Result.success, 
										"COMPLETED", "Card Account #", cardAccountVO.getCardAccountID(), "CARD STATUS", cardAccountVO.getCardStatus(),"partnerId",cardAccountVO.getPartnerMacId(),
                                        "partnerName", partnerIdMapping.getPartnerName(cardAccountVO.getPartnerMacId(), apiMsgId));
								cardAccountDetailsDAO.deactivateCardAccount(cardAccountVO,isValidExpDate, false);
								boolean exceptionLoggingPartner = false;
								//String supplierExcptionPrtrIds = EnvironmentPropertiesUtil.getProperty(ServiceConstants.SUPPLIER_PERIOD_EXCEP_ACE_PRTNR_IDS);
								String supplierExcptionPrtrIds = CommonUtils.getParnerIdForAccess(ServiceConstants.SUPPLIER_PERIOD_EXCEP_ACE_PRTNR_IDS,"");								
								
								if(supplierExcptionPrtrIds!=null){
									exceptionLoggingPartner = supplierExcptionPrtrIds.contains(
											StringUtils.isNotBlank(cardAccountVO.getPartnerMacId())?cardAccountVO.getPartnerMacId():"");	
								}
								
								LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "validateCardAccounts", "supplierExcptionPrtrIds", AmexLogger.Result.success, 
										"COMPLETED", "supplierExcptionPrtrIds", supplierExcptionPrtrIds,"partnerId",cardAccountVO.getPartnerMacId(),
                                        "partnerName", partnerIdMapping.getPartnerName(cardAccountVO.getPartnerMacId(), apiMsgId));
								
								if(exceptionLoggingPartner){
									LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "validateCardAccounts", "Exception Logging for CARD ACCOUNT", AmexLogger.Result.success, 
											"COMPLETED", "log_exception_inactive_card","true",
											"Card Account #", cardAccountVO.getCardAccountID(), "CARD STATUS", cardAccountVO.getCardStatus(),"partnerId",cardAccountVO.getPartnerMacId(),
                                            "partnerName", partnerIdMapping.getPartnerName(cardAccountVO.getPartnerMacId(), apiMsgId));
									// log the exception data here
									Map<String, Object> inMap = formCardAccountPeriodChkExceptionDetailsInMap(cardAccountVO,isValidExpDate);
									insertExceptionDataDAO.execute(inMap,apiMsgId);
							 }							 
							}
						}
						lastUpdateTime = cardAccountVO.getLastUpdateTime(); //resetting lastUpdateTime for fetching next set of records
					//}
					isContinueLoop = hasMoreRecords(apiMsgId, lastUpdateTime, paginationDataVO);
					if(isContinueLoop){
						paginationDataVO = new PaginationDataVO<CardAccountEnrollmentDetailsVO>();
						paginationDataVO.setLastUpdateTime(lastUpdateTime);
					}
				}
			}
		}
		}catch(Exception ex){
			LOGGER.error(apiMsgId, APPLICATION_NAME, EVENT_NAME, "validateCardAccounts", "Periodic Card Account Validation Failed", 
					AmexLogger.Result.failure, "Unexpected Exception while validating card accounts job.", ex);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_ACCT_VALIDATION_PROCESS_ERR_CD, TivoliMonitoring.SSE_ACCT_VALIDATION_PROCESS_ERR_MSG, apiMsgId));
		}
		inactivateCardAccounts(apiMsgId);
	}

	private Map<String,Object> formCardAccountPeriodChkExceptionDetailsInMap(CardAccountEnrollmentDetailsVO cardAccountVO,boolean isValidExpDate){
		Map<String,Object> inMap=new HashMap<String,Object>();
		inMap.put(ApiConstants.IN_PRTR_MAC_ID,cardAccountVO.getPartnerMacId());
		
		if (!isValidExpDate){
				inMap.put(ApiConstants.IN_EXCPT_CD , StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_EXPIRY_EXCP_CD)));
				inMap.put(ApiConstants.IN_EXCPT_DS , StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_ACCOUNT_EXCP_DS)));
				inMap.put(ApiConstants.IN_EXCPT_TYPE_TX,StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_EXPIRY_EXCP_TYPE)));
		}else if(cardAccountVO.getCardStatus().trim().equalsIgnoreCase("X")){
			inMap.put(ApiConstants.IN_EXCPT_CD , StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_ACCOUNT_CAN_EXCP_CD)));
			inMap.put(ApiConstants.IN_EXCPT_DS , StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_ACCOUNT_EXCP_DS)));
			inMap.put(ApiConstants.IN_EXCPT_TYPE_TX,StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_ACCOUNT_CAN_EXCP_TYPE)));
		}else if (cardAccountVO.getCardStatus().equalsIgnoreCase("S")){
			inMap.put(ApiConstants.IN_EXCPT_CD , StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_ACCOUNT_SUS_EXCP_CD)));
			inMap.put(ApiConstants.IN_EXCPT_DS , StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_ACCOUNT_EXCP_DS)));
			inMap.put(ApiConstants.IN_EXCPT_TYPE_TX,StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.CARD_ACCOUNT_SUS_EXCP_TYPE)));
		}
				
		inMap.put(ApiConstants.IN_PRTR_CUST_ORG_ID,cardAccountVO.getPartnerOrgId());
		inMap.put(ApiConstants.IN_PRTR_SPLY_ORG_ID , ApiConstants.CHAR_SPACE);
		
		
		inMap.put(ApiConstants.IN_PRTR_ACCT_ID, cardAccountVO.getPartnerAcctId());  
		inMap.put(ApiConstants.IN_BUY_PYMT_ID, ApiConstants.CHAR_SPACE);      
		inMap.put(ApiConstants.IN_SPLY_ACCT_ID, ApiConstants.CHAR_SPACE);     
		return inMap;
	}
	private boolean hasMoreRecords(String apiMsgId, String lastUpdateTime, PaginationDataVO<CardAccountEnrollmentDetailsVO> paginationDataVO){
		boolean isContinueLoop = false;
		if(paginationDataVO.getTotalRecordCount() > DEFAULT_PAGINATION_COUNT && !GenericUtilities.isNullOrEmpty(lastUpdateTime)){
			isContinueLoop = true;			
		}
		return isContinueLoop;
	}
	
	private void inactivateCardAccounts(String eventId) {
		LOGGER.info(eventId, APPLICATION_NAME, EVENT_NAME, "inactivateCardAccounts", "Initiating periodic card account status update", AmexLogger.Result.success, "Start");
		try {
			boolean isContinueLoop = true;
			boolean isFirstTime = true;
	    	String lastUpdatedTimestamp = StringUtils.EMPTY;
	    	PaginationDataVO<CardAccountType> paginationDataVO = new PaginationDataVO<CardAccountType>();
	    	paginationDataVO.setFlagInd(ApiConstants.CHAR_C);
		    while (isContinueLoop) {
		    	if(isFirstTime){
		    		paginationDataVO.setLastUpdateTime(ServiceConstants.MIN_DEFAULT_TIMESTAMP);
	        		isFirstTime = false;
	        	}else{
	        		paginationDataVO.setLastUpdateTime(lastUpdatedTimestamp);
	        	}
		    	
				Map<String, Object> outMap = fetchCardAccountsDAO.getCardAccounts(paginationDataVO, eventId);
				if(outMap != null && !outMap.isEmpty()){
					String responseCode = StringUtils.stripToEmpty((String)outMap.get(SchedulerConstants.RESP_CD));
					if(ApiErrorConstants.ACIA3000.equalsIgnoreCase(responseCode)){
						LOGGER.info(eventId, APPLICATION_NAME, EVENT_NAME, "inactivateCardAccounts", "Check out for the outMap for SP E3GCA730", AmexLogger.Result.success, "responseCode:"+responseCode);
						List<CardAccountType> cardAccounts = paginationDataVO.getResults();
						if(!CollectionUtils.isEmpty(cardAccounts)){
							for (CardAccountType cardAccountDetails : cardAccounts) {
								if(cardAccountDetails != null){
									VNGEnrollmentVO vngEnrollmentVO = createVNGUpdateEnrollmentVO(cardAccountDetails);
									vngEnrollmentHelper.vngUpdateEnrollment(vngEnrollmentVO, eventId);
									if(!vngEnrollmentVO.isError()){
										CardAccountEnrollmentDetailsVO accountEnrollmentDetailsVO = createCardAccountEnrollmentVO(cardAccountDetails, vngEnrollmentVO.getBillingAccountId());
										cardAccountDetailsDAO.deactivateCardAccount(accountEnrollmentDetailsVO, true, true);
									}
								}
								lastUpdatedTimestamp = cardAccountDetails.getLstUpdateTs();//resetting lastUpdateTime to fetch next set of records
							}
						}
						
						int totalRecords = paginationDataVO.getTotalRecordCount();
						if(totalRecords > Integer.parseInt(paginationCount)){
							isContinueLoop = true;
						}else {
							isContinueLoop = false;
						}
					}else {
						isContinueLoop = false;
						LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_730_ERR_CD, TivoliMonitoring.SSE_SP_730_ERR_MSG, eventId));
						LOGGER.info(eventId, APPLICATION_NAME, EVENT_NAME, "inactivateCardAccounts", "Check out for the outMap for SP E3GCA730", AmexLogger.Result.failure, "responseCode:"+responseCode);
					}
				}else {
					isContinueLoop = false;
					LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_730_ERR_CD, TivoliMonitoring.SSE_SP_730_ERR_MSG, eventId));
					LOGGER.info(eventId, APPLICATION_NAME, EVENT_NAME, "inactivateCardAccounts", "Check out for the outMap for SP E3GCA730", AmexLogger.Result.failure, "");
				}
		    }
		} catch (Exception exception) {
			LOGGER.error(eventId, APPLICATION_NAME, EVENT_NAME, "inactivateCardAccounts", "Periodic Card Account Inactivation Failed", 
					AmexLogger.Result.failure, "Unexpected Exception while Inactivating card accounts job", exception);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_VNG_ACC_INACTIVE_ERR_CD, TivoliMonitoring.SSE_VNG_ACC_INACTIVE_ERR_MSG, eventId));
		}
		LOGGER.info(eventId, APPLICATION_NAME, EVENT_NAME, "inactivateCardAccounts", "Initiating periodic card account status update", AmexLogger.Result.success, "End");
	}

	private CardAccountEnrollmentDetailsVO createCardAccountEnrollmentVO(CardAccountType cardAccountDetails, String vNGAccountId) {
		CardAccountEnrollmentDetailsVO accountEnrollmentDetailsVO = new CardAccountEnrollmentDetailsVO();
		accountEnrollmentDetailsVO.setCardNumber(cardAccountDetails.getCardNo());
		accountEnrollmentDetailsVO.setCardStatus(cardAccountDetails.getCardAcctStatusIndicator());
		accountEnrollmentDetailsVO.setCardStatusDesc(cardAccountDetails.getCardAcctStatusDesc());
		accountEnrollmentDetailsVO.setCardAccountID(cardAccountDetails.getSseAccountId());
		accountEnrollmentDetailsVO.setvNGAccountId(vNGAccountId);
		return accountEnrollmentDetailsVO;
	}
	
	private VNGEnrollmentVO createVNGUpdateEnrollmentVO(CardAccountType cardAccountDetails) {
		VNGEnrollmentVO vngEnrollmentVO = new VNGEnrollmentVO();
		vngEnrollmentVO.setBillingAccountId(cardAccountDetails.getPymtSysAcctId());
		vngEnrollmentVO.setBillingAccountStatus(ApiConstants.STATUSDESC_INACTIVE);
		return vngEnrollmentVO;
	}
}