package com.americanexpress.smartserviceengine.service.impl;

import java.io.IOException;

import javax.annotation.Resource;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.payload.CardActivationRequestType;
import com.americanexpress.smartserviceengine.common.payload.CardActivationResponse;
import com.americanexpress.smartserviceengine.common.payload.CardActivationResponseType;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.validator.CardActivationRequestValidator;
import com.americanexpress.smartserviceengine.manager.CardActivationManager;
import com.americanexpress.smartserviceengine.manager.PartnerValidationManager;
import com.americanexpress.smartserviceengine.service.CardActivationService;

@Service
public class CardActivationServiceImpl implements CardActivationService{
	
	private static AmexLogger logger = AmexLogger.create(CardActivationServiceImpl.class);
	
	@Resource
	private CardActivationRequestValidator cardActivationRequestValidator;
	
	@Resource
	private PartnerValidationManager partnerValidationManager;
	
	@Resource
	private CardActivationManager cardActivationManager;

	@Override
	public ResponseEntity<String> getCardStatusDetails(CardActivationRequestType requestType,HttpHeaders headers, String apiMsgId) throws JsonGenerationException, JsonMappingException, IOException {
		logger.info(apiMsgId,"SmartServiceEngine", "Card Activation API","Start of getCardStatusDetails service",
				"Processing Card Activation API request",AmexLogger.Result.success, "");
	              final long startTimeStamp = System.currentTimeMillis();
	    CardActivationResponseType responseType = new CardActivationResponseType();
	    CardActivationResponse response = new CardActivationResponse();
	    HttpHeaders responseHeader=new HttpHeaders();
	    ResponseEntity<String> responseEntity = null;
	    String responseCode = null;
	    try {

			/*
			 * Validate the Request Payload for common fields
			 */
			response = cardActivationRequestValidator.validateCommonEnrollmentRequest(requestType,headers,apiMsgId);
			if (response != null && response.getResponseCode() == null) {
				logger.info(apiMsgId,"SmartServiceEngine", "Card Activation API","Start of Validating Partner ID",
						"Validating Partner ID",AmexLogger.Result.success, "", "partnerId",requestType.getCardActivationRequest().getPartnerId());
				/**
				 * Partner Id eh-cache changes-Comparing partner id in request with list of partner ids in cache.
				 * If partner Id is valid PartnerValidationManager returns corresponding partner name.  
				 */
				String partnerName = partnerValidationManager.validatePartnerId(requestType.getCardActivationRequest().getPartnerId(),apiMsgId);

				if (partnerName != null) {
						logger.info(apiMsgId,"SmartServiceEngine", "Card Activation API","End of Validating Partner ID",
								"Validating Partner ID was successful",AmexLogger.Result.success, "", "partnerId", requestType.getCardActivationRequest().getPartnerId());

				}else{
					responseCode = ApiErrorConstants.SSEAPICA009;
			    	cardActivationRequestValidator.setFailureResponse(responseCode,response);


				}
			}
			if(response != null && response.getResponseCode() != null) {
				responseType.setCardActivationResponse(response);
				final long endTimeStamp = System.currentTimeMillis();
				logger.info(apiMsgId,"SmartServiceEngine", "Card Activation API","End of  Card Activation API service",
                                    "Time taken to execute Card Activation API",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
				if(CommonUtils.getCMRequestId(headers) != null)
					responseHeader.set(ApiConstants.REQUEST_ID, CommonUtils.getCMRequestId(headers));
				responseHeader.set(ApiConstants.TIMESTAMP, ApiUtil.getCurrentTimeStamp());
				responseEntity=new ResponseEntity<String>(ApiUtil.jsonToString(responseType), responseHeader, HttpStatus.BAD_REQUEST);
				return responseEntity;
			}
			
			response=cardActivationManager.getCardActive(requestType.getCardActivationRequest(),response, apiMsgId);
			responseType.setCardActivationResponse(response);
			responseHeader.set(ApiConstants.REQUEST_ID, CommonUtils.getCMRequestId(headers));
			responseHeader.set(ApiConstants.TIMESTAMP, ApiUtil.getCurrentTimeStamp());
			if (response != null && response.getResponseCode() == null)
				responseEntity = new ResponseEntity<String>(
						ApiUtil.jsonToString(responseType), responseHeader,
						HttpStatus.OK);

			else
				responseEntity = new ResponseEntity<String>(
						ApiUtil.jsonToString(responseType), responseHeader,
						HttpStatus.BAD_REQUEST);


	    }catch(Exception exception){
	    	responseCode = ApiErrorConstants.SSEAPICA003;
	    	cardActivationRequestValidator.setFailureResponse(responseCode,response);
	    	if(CommonUtils.getCMRequestId(headers) != null)
	    		responseHeader.set(ApiConstants.REQUEST_ID, CommonUtils.getCMRequestId(headers));
			responseHeader.set(ApiConstants.TIMESTAMP, ApiUtil.getCurrentTimeStamp());
			responseEntity=new ResponseEntity<String>(ApiUtil.jsonToString(responseType), responseHeader, HttpStatus.BAD_REQUEST);
			logger.error(apiMsgId,"SmartServiceEngine","Card Activation API","Exception in Card Activation API service",
					"Exception occured while processing service layer of ManageEnrollment Service",AmexLogger.Result.failure,
					"Exception in Card Activation API service layer ", exception,"errorCode", responseCode, "errorDesc",ApiUtil.getErrorDescription(responseCode));
	    }
	    return responseEntity;
	}

}
