package com.americanexpress.smartserviceengine.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.common.constants.ErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.FieldConstants;
import com.americanexpress.smartserviceengine.common.constants.ServiceConstants;
import com.americanexpress.smartserviceengine.common.enums.MultiTokenPaymentStatus;
import com.americanexpress.smartserviceengine.common.enums.PostAuthNotificationResponseCodes;
import com.americanexpress.smartserviceengine.common.exception.AceException;
import com.americanexpress.smartserviceengine.common.payload.v2.PostAuthNotificationErrorType;
import com.americanexpress.smartserviceengine.common.payload.v2.PostAuthNotificationRequestType;
import com.americanexpress.smartserviceengine.common.payload.v2.PostAuthNotificationResponseType;
import com.americanexpress.smartserviceengine.common.payload.v2.PostAuthNotificationStatusType;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.validator.v2.PostAuthNotificationRequestValidator;
import com.americanexpress.smartserviceengine.dao.PostAuthNotificationDAO;
import com.americanexpress.smartserviceengine.dao.PostAuthNotificationValidationDAO;
import com.americanexpress.smartserviceengine.service.PostAuthNotificationService;


@Service
public class PostAuthNotificationServiceImpl implements PostAuthNotificationService{
	
	private static final String EVENT_NAME = PostAuthNotificationService.class.getSimpleName();
    
    private static AmexLogger LOGGER = AmexLogger.create(PostAuthNotificationService.class);
    
    @Resource
    private PostAuthNotificationRequestValidator postAuthNotificationRequestValidator;
    
    @Resource
    private PostAuthNotificationDAO postAuthNotificationDAO;
    
    @Resource
    private PostAuthNotificationValidationDAO postAuthNotificationValidationDAO;
    
	@Autowired
	protected TivoliMonitoring tivoliMonitoring;

	@Override
	public PostAuthNotificationResponseType processAuthNotifications(String apiMsgId, PostAuthNotificationRequestType requestVO){
		LOGGER.info(apiMsgId, "", EVENT_NAME, "processAuthNotifications", "START of Post Auth Notification", AmexLogger.Result.success, "", "requestId", requestVO.getRequestId());
		PostAuthNotificationResponseType responseVO = new PostAuthNotificationResponseType();
		String errorCode = null;
		long startTimeStamp = System.currentTimeMillis();
		boolean isError = false;
		try {
			if (requestVO != null) {			
				String 	reqPayload = GenericUtilities.javaToJson(requestVO);
				LOGGER.debug(apiMsgId, "", EVENT_NAME, "processAuthNotifications",
						    "Processing started for Post Auth Notification API Request", AmexLogger.Result.success, "", 
						    "request_payload",reqPayload);
				
				errorCode = postAuthNotificationRequestValidator.validateRequest(requestVO, apiMsgId);
				if (errorCode != null && errorCode.trim().length() > 0) {
					boolean notValidInput= false;
					
					StringBuilder responseCd = new StringBuilder();
					String[] processErrorCode =  errorCode.split(",");
					for (String errorCodeArr : processErrorCode){	
						if(!errorCodeArr.equalsIgnoreCase("null")){
							notValidInput= true;
							responseCd.append(errorCodeArr).append(",");
						}
					}
					if(!notValidInput){

						Map<String, Object> inAuthValidMap = prepareAuthTokenValidationInputMap(requestVO);
						Map<String, Object> outAuthValidMap = postAuthNotificationValidationDAO.execute(inAuthValidMap, apiMsgId);
						
						
						BigDecimal consolidatedTransAuthUsdAmount =  null;						
						int calcPymtRespCd = 0;
						String calcPymtRespDesc = "";								
						String calcTranStatusCd = "";
						
						
						if (outAuthValidMap != null && !outAuthValidMap.isEmpty()) {
							Object object = outAuthValidMap.get(ApiConstants.SP_RESP_CODE);
							String responseCode = object != null ? object.toString().trim() : StringUtils.EMPTY;
							Integer pymtRespCode = ((Integer) outAuthValidMap.get(ApiConstants.O_PYMT_RESP_CD));
							BigDecimal transUsdAmt = ((BigDecimal) outAuthValidMap.get("O_TRANS_USD_AM"));
							BigDecimal transAuthUsdAmt = ((BigDecimal) outAuthValidMap.get(ApiConstants.O_TRANS_AUTH_USD_AM));
							
							if (ApiConstants.SP_SUCCESS_ACEC3000.equals(responseCode)) {
								
								if(requestVO.getPostAuthNotificationData()!=null && 
										(ApiConstants.CHAR_A).equalsIgnoreCase(requestVO.getPostAuthNotificationData().getTransactionStatus())){
									consolidatedTransAuthUsdAmount =  prepareTransAuthUsdAmt(requestVO,transAuthUsdAmt,apiMsgId);
									if(consolidatedTransAuthUsdAmount!=null){
										calcPymtRespCd = findPymtRespCd(consolidatedTransAuthUsdAmount, transUsdAmt,apiMsgId);	
									}else{
										consolidatedTransAuthUsdAmount =  transAuthUsdAmt;
										calcPymtRespCd = pymtRespCode != null ? pymtRespCode : 0;
									}								
									
									if(calcPymtRespCd==99){
										//if the NGGS auth billing amount or the accumulated consolidatedTransAuthUsdAmount is > the  transAuthUsdAmt,
										// we are setting the prev txn code and desc back again there by rejecting the current txn, usually wont happen
										// as we will get  the txn status as D in this case
										consolidatedTransAuthUsdAmount =  transAuthUsdAmt;
										calcPymtRespCd = pymtRespCode != null ? pymtRespCode : 0;
									}
								}else{
									//if the NGGS auth billing amount or the accumulated consolidatedTransAuthUsdAmount is > the  transAuthUsdAmt,
									// we are setting the prev txn code and desc back again there by rejecting the current txn, 
									// as we will get  the txn status as D in this case
									consolidatedTransAuthUsdAmount =  transAuthUsdAmt;
									calcPymtRespCd = pymtRespCode != null ? pymtRespCode : 0;
								}
								
								
								//calcPymtRespDesc = TokenPaymentStatus.getStatusDescriptionForCode(String.valueOf(calcPymtRespCd));
								
								MultiTokenPaymentStatus tokenPymtStatus = MultiTokenPaymentStatus.getTokenPaymentStatusForStatusCode(String.valueOf(calcPymtRespCd));
								
								if(tokenPymtStatus!=null){
									calcPymtRespDesc = tokenPymtStatus.getTokenPaymentDescription();
								}else{
									LOGGER.error(apiMsgId, "", EVENT_NAME, "processAuthNotifications", "processAuthNotifications Exception while finding the PymtRespDesc",
											AmexLogger.Result.failure, 
											"Unexpected Exception while finding the PymtRespDesc", "calcPymtRespDesc",calcPymtRespDesc);
								}
																		
								calcTranStatusCd = MultiTokenPaymentStatus.getDbStatusCode(String.valueOf(calcPymtRespCd));
								Map<String, Object> inMap = prepareAuthTokenInputMap(requestVO,
						 				calcPymtRespCd,calcPymtRespDesc,calcTranStatusCd,consolidatedTransAuthUsdAmount);
						 		
								Map<String, Object> outMap = postAuthNotificationDAO.execute(inMap, apiMsgId);
								if(!GenericUtilities.isNullOrEmpty(outMap)){
									if(outMap.containsKey("RESP_CD")){
										prepareResponse(StringUtils.stripToEmpty((String) outMap.get("RESP_CD")),StringUtils.stripToEmpty((String) outMap.get("RESP_MSG")),requestVO,responseVO,ServiceConstants.RESPONSE_CODE_00000);
									}else{
										prepareResponse(PostAuthNotificationResponseCodes.INTERNAL_ERROR.getPostAuthRespCode(),"", requestVO, responseVO,ServiceConstants.POST_AUTH_FAILURE_CODE);
										LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_722_ERR_CD,TivoliMonitoring.SSE_SP_722_ERR_MSG, apiMsgId));
										LOGGER.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "PostAuthNotificationServiceImpl - processAuthNotifications",
												"SP E3GCP722 has been failed", AmexLogger.Result.failure,"");
									}							
								}else {
									prepareResponse(PostAuthNotificationResponseCodes.INTERNAL_ERROR.getPostAuthRespCode(),"", requestVO, responseVO,ServiceConstants.POST_AUTH_FAILURE_CODE);
									LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_722_ERR_CD,TivoliMonitoring.SSE_SP_722_ERR_MSG, apiMsgId));
									LOGGER.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "PostAuthNotificationServiceImpl - processAuthNotifications",
											"SP E3GCP722 has been failed", AmexLogger.Result.failure,"");
								}									
							}else if(ApiErrorConstants.ACEC3N01.equals(responseCode)){
								prepareResponse(ErrorConstants.ERR_PAYMENT_NOT_FOUND_FOR_DPAN,EnvironmentPropertiesUtil.getProperty(ErrorConstants.ERR_PAYMENT_NOT_FOUND_FOR_DPAN),requestVO,responseVO,ServiceConstants.POST_AUTH_FAILURE_CODE);
							}else {
								prepareResponse(PostAuthNotificationResponseCodes.INTERNAL_ERROR.getPostAuthRespCode(),"", requestVO, responseVO,ServiceConstants.POST_AUTH_FAILURE_CODE);
								LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_725_ERR_CD,TivoliMonitoring.SSE_SP_725_ERR_MSG, apiMsgId));
								LOGGER.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "PostAuthNotificationServiceImpl - processAuthNotifications",
										"SP E3GCA725 has been failed", AmexLogger.Result.failure,"");
							}
						} else {
							prepareResponse(PostAuthNotificationResponseCodes.INTERNAL_ERROR.getPostAuthRespCode(),"", requestVO, responseVO,ServiceConstants.POST_AUTH_FAILURE_CODE);
							LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_725_ERR_CD,TivoliMonitoring.SSE_SP_725_ERR_MSG, apiMsgId));
							LOGGER.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "PostAuthNotificationServiceImpl - processAuthNotifications",
									"SP E3GCA725 has been failed", AmexLogger.Result.failure,"");
						}
						
				 		/*Map<String, Object> inMap = prepareAuthTokenInputMap(requestVO);
						Map<String, Object> outMap = postAuthNotificationDAO.insertTokenAuthDetails(inMap);
						if(!GenericUtilities.isNullOrEmpty(outMap)){
							if(outMap.containsKey("RESP_CD")){
								prepareResponse(StringUtils.stripToEmpty((String) outMap.get("RESP_CD")),StringUtils.stripToEmpty((String) outMap.get("RESP_MSG")),requestVO,responseVO,ServiceConstants.RESPONSE_CODE_00000);
							}else{
								prepareResponse(PostAuthNotificationResponseCodes.INTERNAL_ERROR.getPostAuthRespCode(),"", requestVO, responseVO,ServiceConstants.POST_AUTH_FAILURE_CODE);	
							}							
						}*/
					}else{						
						    prepareResponse(responseCd.toString(),"", requestVO, responseVO,ServiceConstants.POST_AUTH_FAILURE_CODE);
					}
			 	}
				LOGGER.info(apiMsgId, "", EVENT_NAME, "processAuthNotifications", "Preparing the response status from database", AmexLogger.Result.success, "",
					    "responseStatus",responseVO.getStatus().getStatusCode());
			}
			LOGGER.info(apiMsgId, "", EVENT_NAME, "processAuthNotifications", "END of postAuth Notification", AmexLogger.Result.success,"");
		}
		catch(AceException ex){
			isError= true;
			LOGGER.error(apiMsgId, "", EVENT_NAME, "processAuthNotifications", "Process Auth Notification Request Failed with Exception", AmexLogger.Result.failure, 
					"Unexpected Exception while post auth notification.", ex);
			//tivoliMonitoring.logStatement(TivoliMonitoring.C3_POST_AUTH_RESPONSE_ERR_CD, TivoliMonitoring.C3_POST_AUTH_RESPONSE_ERR_MSG, apiMsgId);
		}catch(Exception ex){
			isError= true;
			LOGGER.error(apiMsgId, "", EVENT_NAME, "processAuthNotifications", "Process Auth Notification Request Failed with Exception", AmexLogger.Result.failure, 
					"Unexpected Exception while post auth notification.", ex);
			//tivoliMonitoring.logStatement(TivoliMonitoring.C3_POST_AUTH_RESPONSE_ERR_CD, TivoliMonitoring.C3_POST_AUTH_RESPONSE_ERR_MSG, apiMsgId);
		}finally{
			try{
			long endTimeStamp = System.currentTimeMillis();
			LOGGER.info(apiMsgId, "", EVENT_NAME, "processAuthNotifications", "Auth Notification Request Completed", 
					 isError? AmexLogger.Result.failure : AmexLogger.Result.success, "Post Auth Notification Request Completed", "postAuthResponseTime",(endTimeStamp-startTimeStamp) + " milliseconds("+(endTimeStamp-startTimeStamp)/1000.00 + " seconds)");
			}catch(Exception ex){
				LOGGER.error(apiMsgId, "", EVENT_NAME, "processAuthNotifications", "Process Auth Notification Request Failed with Exception", AmexLogger.Result.failure, 
						"Unexpected Exception while post auth notification.", ex);
				//tivoliMonitoring.logStatement(TivoliMonitoring.C3_POST_AUTH_RESPONSE_ERR_CD, TivoliMonitoring.C3_POST_AUTH_RESPONSE_ERR_MSG, apiMsgId);
			}
		}
		return responseVO;
	}

	private void prepareResponse(String responseCode, String responseDescription,PostAuthNotificationRequestType requestVO, PostAuthNotificationResponseType responseVO,String statusCode) {
		PostAuthNotificationStatusType postAuthNotificationStatusVO = new PostAuthNotificationStatusType();
		PostAuthNotificationErrorType postAuthNotificationErrorVO = new PostAuthNotificationErrorType();
		if (statusCode.equalsIgnoreCase(ServiceConstants.RESPONSE_CODE_00000)) {			
			if (FieldConstants.SP_SUCCESS_RESPONSE_CODE.equalsIgnoreCase(responseCode)){
				postAuthNotificationStatusVO.setStatusCode(ServiceConstants.POST_AUTH_SUCESSS_CODE);
				postAuthNotificationStatusVO.setShortMessage(ServiceConstants.SUCCESS_TITLE_CASE);
				postAuthNotificationStatusVO.setDescription(ServiceConstants.SUCESS_STATUS);
			}else{
				prepareFailureResponse(responseCode,responseDescription,postAuthNotificationStatusVO,postAuthNotificationErrorVO,true);
			}			
		}else {
			    prepareFailureResponse(responseCode,responseDescription,postAuthNotificationStatusVO,postAuthNotificationErrorVO,false);
		}
		responseVO.setRequestId(requestVO.getRequestId());
		responseVO.setTimestamp(requestVO.getTimestamp());
		responseVO.setStatus(postAuthNotificationStatusVO);
	}
	
	private PostAuthNotificationStatusType prepareFailureResponse(String responseCode,String responseDescription,PostAuthNotificationStatusType postAuthNotificationStatusVO,PostAuthNotificationErrorType postAuthNotificationErrorVO,boolean isDBRespCode) {
		List<PostAuthNotificationErrorType> postAuthNotificationErrorVOList = new ArrayList<PostAuthNotificationErrorType>();
		postAuthNotificationStatusVO.setStatusCode(ServiceConstants.POST_AUTH_FAILURE_CODE);
		postAuthNotificationStatusVO.setShortMessage(ServiceConstants.POST_AUTH_FAILURE_MSG);		
		if(isDBRespCode){
			postAuthNotificationErrorVO.setErrorCode(responseCode);
			postAuthNotificationErrorVO.setErrorDescription(responseDescription);
			postAuthNotificationErrorVO.setErrorMsg(responseDescription);		
			postAuthNotificationErrorVOList.add(postAuthNotificationErrorVO);
		}else{
			String[] responseCodes = responseCode.split(",");			
		//	for(String tmpResponseCodes : responseCodes){
				PostAuthNotificationErrorType	tmpPostAuthNotificationErrorVO = new PostAuthNotificationErrorType();
				tmpPostAuthNotificationErrorVO.setErrorCode(responseCodes[0]);
				Map<String, String> responseMapping = PostAuthNotificationResponseCodes.getResponseCodeAndDesc(responseCodes[0]);
				Map.Entry<String, String> entry = responseMapping.entrySet().iterator().next();				
				tmpPostAuthNotificationErrorVO.setErrorDescription(entry.getValue());
				tmpPostAuthNotificationErrorVO.setErrorMsg(entry.getValue());
				postAuthNotificationErrorVOList.add(tmpPostAuthNotificationErrorVO);
		//	}
		}
		postAuthNotificationStatusVO.setError(postAuthNotificationErrorVOList);	
	 return postAuthNotificationStatusVO;
	}
	
	private Map<String, Object> prepareAuthTokenInputMap(PostAuthNotificationRequestType postAuthNotificationRequestVO
			,int pymtRespCode,String pymtRespDs,String transStatusCd,BigDecimal transAuthUsdAmt) {
		Map<String, Object> inMap = new HashMap<String, Object>();
		
		inMap.put(FieldConstants.IN_ATH_TRAN_ID, postAuthNotificationRequestVO.getPostAuthNotificationData().getTransactionID());
		inMap.put(FieldConstants.IN_ATH_TIMESTAMP,postAuthNotificationRequestVO.getPostAuthNotificationData().getTrxSubmitTime());
		inMap.put(FieldConstants.IN_TOKEN_NUM, postAuthNotificationRequestVO.getPostAuthNotificationData().getDpanNumber());
		inMap.put(FieldConstants.IN_ATH_SE_NUM, postAuthNotificationRequestVO.getPostAuthNotificationData().getSeNumber());
		inMap.put(FieldConstants.IN_ATH_SE_NAME, postAuthNotificationRequestVO.getPostAuthNotificationData().getTransactingSEName());
		inMap.put(FieldConstants.IN_ATH_TRAN_STATUS, postAuthNotificationRequestVO.getPostAuthNotificationData().getTransactionStatus());
		inMap.put(FieldConstants.IN_ATH_TRAN_AMT,new BigDecimal(postAuthNotificationRequestVO.getPostAuthNotificationData().getActualTrxAmt().trim()));
		inMap.put(FieldConstants.IN_ATH_CURR_CODE, postAuthNotificationRequestVO.getPostAuthNotificationData().getActualTrxCurrCd());
		inMap.put(FieldConstants.IN_ATH_BILL_AMT,new BigDecimal(postAuthNotificationRequestVO.getPostAuthNotificationData().getAuthBillingAmt().trim()));
		inMap.put(FieldConstants.IN_ATH_BILL_CURR_CODE, postAuthNotificationRequestVO.getPostAuthNotificationData().getBillingCurrCd());
		inMap.put(FieldConstants.IN_ATH_USD_AMT, new BigDecimal(postAuthNotificationRequestVO.getPostAuthNotificationData().getUsdAmount().trim()));
		inMap.put(FieldConstants.IN_ATH_CM_CTRY_CODE, postAuthNotificationRequestVO.getPostAuthNotificationData().getCmCountryCode());
		inMap.put(FieldConstants.IN_WALLET_ID, postAuthNotificationRequestVO.getPostAuthNotificationData().getWalletProviderID());
		inMap.put(FieldConstants.IN_PYMT_RESP_CD, pymtRespCode);
		inMap.put(FieldConstants.IN_PYMT_RESP_DS, pymtRespDs);
		inMap.put(FieldConstants.IN_TRANS_STA_CD, transStatusCd);
		inMap.put(FieldConstants.IN_TRANS_AUTH_USD_AM, transAuthUsdAmt);
		inMap.put(FieldConstants.IN_AUTH_APRV_CD, postAuthNotificationRequestVO.getPostAuthNotificationData().getApprovalCode().trim());
		 
		return inMap;
	}
	
	
	private Map<String, Object> prepareAuthTokenValidationInputMap(PostAuthNotificationRequestType postAuthNotificationRequestVO) {
		Map<String, Object> inMap = new HashMap<String, Object>();		
		inMap.put(FieldConstants.IN_TOKEN_NUM, postAuthNotificationRequestVO.getPostAuthNotificationData().getDpanNumber());				 
		return inMap;
	}
	
	private BigDecimal prepareTransAuthUsdAmt(PostAuthNotificationRequestType postAuthNotificationRequestVO,BigDecimal transAuthUsdAmt,String apiMsgId ) {
		transAuthUsdAmt = transAuthUsdAmt != null ? transAuthUsdAmt : new BigDecimal(0.00);
		LOGGER.info(apiMsgId,"Commerce Concierge","Manage Enrollment API","prepareAuthTokenValidationInputMap", 
				"prepareAuthTokenValidationInputMap starts", AmexLogger.Result.success,"","transAuthUsdAmt",transAuthUsdAmt != null ? transAuthUsdAmt.toString() : "0.00");		
		BigDecimal consolidatedTransAuthUsdAmount = null;
		BigDecimal authbillingAmount = null;
		try{
		authbillingAmount = new BigDecimal(postAuthNotificationRequestVO.getPostAuthNotificationData().getAuthBillingAmt().trim());
		/* since this AuthBillingAmt will always comes along with cent values/decimal values ,to get the actual amount , we divide by 100
		 	srivathsan 29/7/2016 srivathsan S*/
		authbillingAmount = authbillingAmount.divide(new BigDecimal(100));
		consolidatedTransAuthUsdAmount = transAuthUsdAmt.add(authbillingAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
		}catch(Exception ex){
			consolidatedTransAuthUsdAmount = null;
			LOGGER.error(apiMsgId, "", EVENT_NAME, "prepareAuthTokenValidationInputMap", "prepareAuthTokenValidationInputMap Failed with Exception", AmexLogger.Result.failure, 
					"Unexpected Exception while post auth notification.", ex);
		}
		LOGGER.info(apiMsgId,"Commerce Concierge","Manage Enrollment API","prepareAuthTokenValidationInputMap", 
				"prepareAuthTokenValidationInputMap ends", AmexLogger.Result.success,"","consolidatedTransAuthUsdAmount",consolidatedTransAuthUsdAmount.toString());
		return consolidatedTransAuthUsdAmount;
	}
	
	private int findPymtRespCd(BigDecimal consolidatedTransAuthUsdAmount ,BigDecimal transUsdAmount,String apiMsgId) {		
		int pymtRespCd = 0;
		/*both amts are equal*/
		if(consolidatedTransAuthUsdAmount.compareTo(transUsdAmount) ==0){
			pymtRespCd =  13;
			
		}else if (consolidatedTransAuthUsdAmount.compareTo(transUsdAmount) ==1 ){
			/*consolid amt is > trans usd amt*/
			pymtRespCd =  99;
			
		}else if (consolidatedTransAuthUsdAmount.compareTo(transUsdAmount) == -1 ){
			/*consolid amt is < trans usd amt*/
			pymtRespCd =  15;
		}
		return pymtRespCd;
	}
}