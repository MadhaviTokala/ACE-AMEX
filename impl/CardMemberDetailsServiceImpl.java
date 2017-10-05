package com.americanexpress.smartserviceengine.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.payload.CardMemberDetailsRequestType;
import com.americanexpress.smartserviceengine.common.payload.CardMemberDetailsResponse;
import com.americanexpress.smartserviceengine.common.payload.CardMemberDetailsResponseType;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.validator.CardMemberDetailsRequestValidator;
import com.americanexpress.smartserviceengine.manager.CardMembersDetailsManager;
import com.americanexpress.smartserviceengine.manager.PartnerValidationManager;
import com.americanexpress.smartserviceengine.service.CardMemberDetailsService;

@Service
public class CardMemberDetailsServiceImpl implements CardMemberDetailsService {
	
	private static AmexLogger logger = AmexLogger.create(CardMemberDetailsServiceImpl.class);
	
	@Resource
	private CardMemberDetailsRequestValidator cmDetailsRequestValidator;
	
	@Resource
	private PartnerValidationManager partnerValidationManager;
	
	@Resource
	private CardMembersDetailsManager cardMemnbersDetailsManager;

	@Override
	public ResponseEntity<String> getCardMemberDetails(CardMemberDetailsRequestType requestType,HttpHeaders headers, String apiMsgId) throws JsonGenerationException, JsonMappingException, IOException {
		logger.info(apiMsgId,"SmartServiceEngine", "Card Member Details API","Start of getCardMemberDetails service",
				"Processing Card Member Details API request",AmexLogger.Result.success, "");
	              final long startTimeStamp = System.currentTimeMillis();
	    CardMemberDetailsResponseType responseType = new CardMemberDetailsResponseType();
	    CardMemberDetailsResponse response = new CardMemberDetailsResponse();
	    HttpHeaders responseHeader=new HttpHeaders();
	    ResponseEntity<String> responseEntity = null;
	    String responseCode = null;
	    try {

			/*
			 * Validate the Request Payload for common fields
			 */
			response = cmDetailsRequestValidator.validateCommonEnrollmentRequest(requestType,headers, apiMsgId);

			if (response != null && response.getResponseCode() == null) {

				logger.info(apiMsgId,"SmartServiceEngine", "Card Member Details API","Start of Validating Partner ID",
						"Validating Partner ID",AmexLogger.Result.success, "", "partnerId",
						requestType.getCmDtls().getPartnerId());

				/**
				 * Partner Id eh-cache changes-Comparing partner id in request with list of partner ids in cache.
				 * If partner Id is valid PartnerValidationManager returns corresponding partner name.  
				 */
				String partnerName = partnerValidationManager.validatePartnerId(requestType.getCmDtls().getPartnerId(),apiMsgId);

				if (partnerName != null) {
						logger.info(apiMsgId,"SmartServiceEngine", "Card Member Details API","End of Validating Partner ID",
								"Validating Partner ID was successful",AmexLogger.Result.success, "", "partnerId", requestType.getCmDtls().getPartnerId());

				}else{
					responseCode = ApiErrorConstants.SSEAPICM008;
			    	cmDetailsRequestValidator.setFailureResponse(responseCode,response);


				}
			}
			if(response != null && response.getResponseCode() != null) {
				responseType.setCmDtlsResponse(response);
				if(CommonUtils.getCMRequestId(headers) != null)
					responseHeader.set(ApiConstants.REQUEST_ID, CommonUtils.getCMRequestId(headers));
				responseHeader.set(ApiConstants.TIMESTAMP, ApiUtil.getCurrentTimeStamp());
				final long endTimeStamp = System.currentTimeMillis();
				logger.info(apiMsgId,"SmartServiceEngine", "Card Member Details API","End of  getCardMemberDetails service",
                                    "Time taken to execute getCardMemberDetails",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
				responseHeader.setContentType(MediaType.APPLICATION_JSON);
				responseEntity=new ResponseEntity<String>(ApiUtil.jsonToString(responseType), responseHeader, HttpStatus.BAD_REQUEST);
				return responseEntity;
			}
			//stubResponse
			if(response != null && response.getResponseCode() == null) {
				response=cardMemnbersDetailsManager.getCardMembers(requestType, response, apiMsgId);
				
				if(response.getResponseCode() != null){
						cmDetailsRequestValidator.setFailureResponse(response.getResponseCode(),response);
				}
				responseType.setCmDtlsResponse(response);
				responseHeader.set(ApiConstants.REQUEST_ID, CommonUtils.getCMRequestId(headers));
				responseHeader.set(ApiConstants.TIMESTAMP, ApiUtil.getCurrentTimeStamp());
				
				if(response.getResponseCode() != null)
					responseEntity=new ResponseEntity<String>(ApiUtil.jsonToString(responseType), responseHeader, HttpStatus.BAD_REQUEST);
				else
					responseEntity=new ResponseEntity<String>(ApiUtil.jsonToString(responseType), responseHeader, HttpStatus.OK);
			}
	    }catch(Exception exception){
	    	responseCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD;
	    	cmDetailsRequestValidator.setFailureResponse(responseCode,response);
	    	if(CommonUtils.getCMRequestId(headers) != null)
	    		responseHeader.set(ApiConstants.REQUEST_ID, CommonUtils.getCMRequestId(headers));
			responseHeader.set(ApiConstants.TIMESTAMP, ApiUtil.getCurrentTimeStamp());
			responseEntity=new ResponseEntity<String>(ApiUtil.jsonToString(responseType), responseHeader, HttpStatus.BAD_REQUEST);
			logger.error(apiMsgId,"SmartServiceEngine","Card Member Details API","Exception in getCardMemberDetails service",
					"Exception occured while processing service layer of getCardMemberDetails Service",AmexLogger.Result.failure,
					"Exception in getCardMemberDetails service layer ", exception,"errorCode", responseCode, "errorDesc",ApiUtil.getErrorDescription(responseCode));
	    }
		return responseEntity;
	}

}
