package com.americanexpress.smartserviceengine.service.impl;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.payload.CommonContext;
import com.americanexpress.smartserviceengine.common.payload.SearchBankRequestData;
import com.americanexpress.smartserviceengine.common.payload.SearchBankResponse;
import com.americanexpress.smartserviceengine.common.payload.SearchBankResponseData;
import com.americanexpress.smartserviceengine.common.payload.SearchBankResponseType;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.validator.SearchBankRequestValidator;
import com.americanexpress.smartserviceengine.manager.PartnerValidationManager;
import com.americanexpress.smartserviceengine.manager.SearchBankManager;
import com.americanexpress.smartserviceengine.service.SearchBankService;

@Service
public class SearchBankServiceImpl implements SearchBankService {
	
	private static final AmexLogger logger = AmexLogger.create(SearchBankServiceImpl.class);
	
	@Resource
	private PartnerValidationManager partnerValidationManager;
	
	@Resource
	private SearchBankManager searchBankManager;
		
	@Override
	public SearchBankResponseType searchBank(SearchBankRequestData requestData,	String apiMsgId) {
		logger.info(apiMsgId, "SmartServiceEngine", "SearchBankServiceImpl","SearchBankServiceImpl:searchBank", "Search Bank API - Start", AmexLogger.Result.success, "");
		SearchBankResponseType responseType = new SearchBankResponseType();
		SearchBankResponse response = new SearchBankResponse();
		CommonContext commonResponseContext = new CommonContext();
		String responseCode = null;
		try{
           /* responseCode = SearchBankRequestValidator.validateCommonContextRequest(requestData);
            if ( StringUtils.isNotBlank(responseCode)) {// If common field validation is failure, return error response
            	SearchBankRequestValidator.setFailureResponse(commonResponseContext, responseCode, response);
                responseType.setSearchBankResponse(response);
                return responseType;
            }

            CommonContext commonContext = requestData.getCommonRequestContext();
            commonResponseContext.setClientId(commonContext.getClientId());
            commonResponseContext.setRequestId(commonContext.getRequestId());
            commonResponseContext.setPartnerName(commonContext.getPartnerName());
            commonResponseContext.setTimestamp(ApiUtil.getCurrentTimeStamp());*/
			
            String errorCode = SearchBankRequestValidator.validateRequestData(requestData);
            if(StringUtils.isNotBlank(errorCode)){
            	SearchBankRequestValidator.setFailureResponse(commonResponseContext, errorCode, response);
                responseType.setSearchBankResponse(response);
                return responseType;
            }
            
            /*logger.info(apiMsgId,"SmartServiceEngine", "Search Bank API","Start of Validating Partner ID",
					"Validating Partner ID",AmexLogger.Result.success, "", "partnerId", requestData.getCommonRequestContext().getPartnerId());

			String partnerIdErrorCd = partnerValidationManager.validatePartnerId(requestData.getCommonRequestContext().getPartnerId(),apiMsgId);
			
			if (partnerIdErrorCd != null) {
				if(ApiConstants.PARTNER_ID_FAIL_NOT_FOUND.equals(partnerIdErrorCd)) {
					responseCode = ApiErrorConstants.SSEAPIEN078;
				} else {
					responseCode = ApiErrorConstants.PRTR_ID_SP_INTERNAL_SERVER_ERR_CD;
				}
					SearchBankRequestValidator.setFailureResponse(commonResponseContext, responseCode, response);
				logger.error(apiMsgId,"SmartServiceEngine", "Search Bank API","End of Validating Partner ID",
						"Validating Partner ID was unsuccessful",AmexLogger.Result.failure, "Error in validating partnerId ", "partnerId", requestData.getCommonRequestContext().getPartnerId(),
						"responseCode", response.getData().getResponseCode(), "responseDesc", response.getData().getResponseDesc());
			}else{
				logger.info(apiMsgId,"SmartServiceEngine", "Search Bank API","End of Validating Partner ID",
						"Validating Partner ID was successful",AmexLogger.Result.success, "", "partnerId", requestData.getCommonRequestContext().getPartnerId());
			}*/
            
            SearchBankResponse searchBankResponse = searchBankManager.searchBank(requestData, apiMsgId);
            responseType.setSearchBankResponse(searchBankResponse);
        }catch(Exception exception){
            responseCode = ApiErrorConstants.SEARCH_BANK_INTERNAL_SERVER_ERR_CD;
            setFailureResponse(commonResponseContext, responseCode, response);
            logger.error(apiMsgId,"SmartServiceEngine","SearchBankServiceImpl", "SearchBankServiceImpl:searchBank","Exception occured while processing Search Bank API Request",
                AmexLogger.Result.failure,"Exception in Search Bank API service layer ", exception, "resp_code", responseCode, "resp_msg",ApiUtil.getErrorDescription(responseCode));
        }
		logger.info(apiMsgId, "SmartServiceEngine", "SearchBankServiceImpl","SearchBankServiceImpl:searchBank", "Search Bank API - End", AmexLogger.Result.success, "");
		return responseType;
	}
	
	public void setFailureResponse(CommonContext commonResponseContext, String errorCode, SearchBankResponse response) {
		SearchBankResponseData failureResponseData = new SearchBankResponseData();
		//failureResponseData.setCommonResponseContext(commonResponseContext);
		failureResponseData.setResponseCode(errorCode);
		failureResponseData.setResponseDesc(ApiUtil.getErrorDescription(errorCode));
		//failureResponseData.getCommonResponseContext().setTimestamp(ApiUtil.getCurrentTimeStamp());
		response.setData(failureResponseData);
		response.setStatus(ApiConstants.FAIL);
	 }
}
