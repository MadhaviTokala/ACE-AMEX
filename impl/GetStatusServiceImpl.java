/**
 * <p>
 * Copyright © 2014 AMERICAN EXPRESS. All Rights Reserved.
 * </p>
 * <p>
 * AMERICAN EXPRESS CONFIDENTIAL. All information, copyrights, trade secrets<br>
 * and other intellectual property rights, contained herein are the property<br>
 * of AMERICAN EXPRESS. This document is strictly confidential and must not be
 * <br>
 * copied, accessed, disclosed or used in any manner, in whole or in part,<br>
 * without Amex's express written authorization.
 * </p>
 */
package com.americanexpress.smartserviceengine.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.AccountDetailsType;
import com.americanexpress.smartserviceengine.common.payload.AddressType;
import com.americanexpress.smartserviceengine.common.payload.CardAccountDetailsType;
import com.americanexpress.smartserviceengine.common.payload.CommonContext;
import com.americanexpress.smartserviceengine.common.payload.GetStatusRequestData;
import com.americanexpress.smartserviceengine.common.payload.GetStatusRequestType;
import com.americanexpress.smartserviceengine.common.payload.GetStatusResponse;
import com.americanexpress.smartserviceengine.common.payload.GetStatusResponseData;
import com.americanexpress.smartserviceengine.common.payload.GetStatusResponseType;
import com.americanexpress.smartserviceengine.common.payload.OrgCardDetailsType;
import com.americanexpress.smartserviceengine.common.payload.OrgStatusResponse;
import com.americanexpress.smartserviceengine.common.payload.ServiceType;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.validator.GetStatusRequestValidator;
import com.americanexpress.smartserviceengine.manager.GetAchStatusManager;
import com.americanexpress.smartserviceengine.manager.GetCardStatusManager;
import com.americanexpress.smartserviceengine.manager.GetStatusManager;
import com.americanexpress.smartserviceengine.manager.PartnerValidationManager;
import com.americanexpress.smartserviceengine.service.GetStatusService;

@Service
public class GetStatusServiceImpl implements GetStatusService {

	private static AmexLogger logger = AmexLogger.create(GetStatusServiceImpl.class);

	@Resource
	private GetStatusManager getStatusManager;

	@Resource
	private GetCardStatusManager getCardStatusManager;
	
	@Resource
	private GetAchStatusManager getAchStatusManager;

	@Resource
	private PartnerValidationManager partnerValidationManager;

	@Resource
    private TivoliMonitoring tivoliMonitoring;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.americanexpress.smartserviceengine.api.service.StatusService#getStatus
	 * (
	 * com.americanexpress.smartserviceengine.api.payload.pojo.GetStatusRequestType
	 * , java.lang.String)
	 */

	@Override
	public GetStatusResponseType getStatus(GetStatusRequestType requestType,
			String apiMsgId) {

		GetStatusResponseType responseType = new GetStatusResponseType();
		GetStatusResponse response = new GetStatusResponse();
		CommonContext commonResponseContext = new CommonContext();
		String responseCode = null;
		GetStatusRequestData requestData = new GetStatusRequestData();

		try {

			/*
			 * Validate the Request Payload for common fields
			 */
			response = GetStatusRequestValidator.validateCommonGetStatusRequest(requestType, apiMsgId);

			if(response != null && response.getData() !=  null &&response.getData().getResponseCode() != null) {
				responseType.setGetStatusResponse(response);
				return responseType;
			}

			commonResponseContext.setPartnerId(requestType.getGetStatusRequest().getData()
					.getCommonRequestContext().getPartnerId());
			commonResponseContext.setRequestId(requestType.getGetStatusRequest().getData()
					.getCommonRequestContext().getRequestId());
			commonResponseContext.setPartnerName(requestType.getGetStatusRequest().getData()
					.getCommonRequestContext().getPartnerName());
			commonResponseContext.setTimestamp(ApiUtil.getCurrentTimeStamp());

			logger.info("Validating Partner ID: "+requestType.getGetStatusRequest().getData().getCommonRequestContext().getPartnerId());


			/**
			 * Partner Id eh-cache changes-Comparing partner id in request with list of partner ids in cache.
			 * If partner Id is valid PartnerValidationManager returns corresponding partner name.  
			 */
			String partnerName = partnerValidationManager.validatePartnerId(requestType.getGetStatusRequest().getData().getCommonRequestContext().getPartnerId(),apiMsgId);

			if (partnerName != null) {
					logger.info(apiMsgId,"SmartServiceEngine", "Get Status API","End of Validating Partner ID",
							"Validating Partner ID was successful",AmexLogger.Result.success, "", "partnerId", requestType.getGetStatusRequest().getData().getCommonRequestContext().getPartnerId());

			}else{
				responseCode = ApiErrorConstants.SSEAPIST016;
				GetStatusRequestValidator.setFailureResponse(commonResponseContext,
						responseCode, requestData.getOrganizationId(), response);

			}
			/*
			 * If common field validation is failure, return error response
			 */

			if (response.getData() != null
					&& response.getData().getResponseCode() != null) {
				responseType.setGetStatusResponse(response);
				return responseType;
			}

			/*
			 * If common field validation is successful, continue further
			 * processing
			 */
			commonResponseContext.setPartnerId(requestType
					.getGetStatusRequest().getData().getCommonRequestContext()
					.getPartnerId());
			commonResponseContext.setRequestId(requestType
					.getGetStatusRequest().getData().getCommonRequestContext()
					.getRequestId());
			commonResponseContext.setPartnerName(requestType
					.getGetStatusRequest().getData().getCommonRequestContext()
					.getPartnerName());
			commonResponseContext.setTimestamp(ApiUtil.getCurrentTimeStamp());

			requestData = requestType.getGetStatusRequest().getData();

			
			//Card stud Response
		/*	if(requestData.getOrganizationInfo()!=null && requestData.getOrganizationInfo().getServiceCode()!=null && requestData.getOrganizationInfo().getServiceCode().equals("2")){
				response=createGetOrgResponse(requestData,response,apiMsgId);
				responseType.setGetStatusResponse(response);
				return responseType;
			}else if(requestData.getAccountDetails()!=null && ("CA".equalsIgnoreCase(requestData.getAccountDetails().getPaymentMethod())) && 
					StringUtils.isNotBlank(requestData.getAccountDetails().getPartnerAccountId())){
				response=createCardGetAccountResponse(requestData,response,apiMsgId);
				responseType.setGetStatusResponse(response);
				return responseType;
			}*/

			
			
			response = GetStatusRequestValidator.validateGetStatusRequestFields(requestData,
							commonResponseContext);
			if (response.getData() != null
					&& response.getData().getResponseCode() != null) {

				responseType.setGetStatusResponse(response);
				return responseType;
			}

			logger.debug(apiMsgId, "SmartServiceEngine",
					"Get Status API", "After getStatus service",
					"Field validation is successful",
					AmexLogger.Result.success,
					"Field validation is successful", "requestId", requestData
							.getCommonRequestContext().getRequestId());

			String statusCategory = requestData.getStatusCategory();


			if (statusCategory.equalsIgnoreCase(ApiConstants.CATEGORY_STATUS_ORG)) {
				if (requestData.getOrganizationInfo() != null && requestData.getOrganizationInfo().getServiceCode() !=null 
						&& ApiConstants.SERVICE_CODE_ACH.equalsIgnoreCase(requestData.getOrganizationInfo().getServiceCode().toString())) {
					response = getAchStatusManager.getAchOrgStatus(requestData,apiMsgId);
				} else if (requestData.getOrganizationInfo() != null && requestData.getOrganizationInfo().getServiceCode() !=null
                           && ApiConstants.SERVICE_CODE_CHECK.equalsIgnoreCase(requestData.getOrganizationInfo().getServiceCode().toString())){
					response = getStatusManager.getOrgStatus(requestData,apiMsgId);
				}else if (requestData.getOrganizationInfo() != null && requestData.getOrganizationInfo().getServiceCode() !=null  
						&& ApiConstants.SERVICE_CODE_CARD.equalsIgnoreCase(requestData.getOrganizationInfo().getServiceCode().toString())) {
					response = getCardStatusManager.getCardOrgStatus(requestData,apiMsgId);
				}else {
				    response = getStatusManager.getOrgDeleteStatus(requestData,apiMsgId);
				}
			} else if (statusCategory.equalsIgnoreCase(ApiConstants.CATEGORY_STATUS_ACCOUNT)) {
				if (requestData.getAccountDetails() != null && requestData.getAccountDetails().getPaymentMethod() != null
						&& ApiConstants.PAYMENT_MTD_ACH.equals(requestData.getAccountDetails().getPaymentMethod())) {
					response = getAchStatusManager.getAchAccountStatus(requestData, apiMsgId);
				} else if (requestData.getAccountDetails() != null && requestData.getAccountDetails().getPaymentMethod() != null 
						&& ApiConstants.PAYMENT_MTD_CARD.equals(requestData.getAccountDetails().getPaymentMethod())) {
					response = getCardStatusManager.getCardAccountStatus(requestData, apiMsgId);
				} else {
					response = getStatusManager.getAccountStatus(requestData,apiMsgId);
				}
			} else if (statusCategory.equalsIgnoreCase(ApiConstants.CATEGORY_STATUS_PAYMENT)) {
				if (requestData.getPaymentDetails() != null&& ApiConstants.PAYMENT_MTD_ACH.equals(requestData
								.getPaymentDetails().getPaymentMethod())) {
					response = getAchStatusManager.getAchPaymentStatus(requestData, apiMsgId);
				}else if (requestData.getPaymentDetails() != null&& ApiConstants.PAYMENT_MTD_CARD.equals(requestData
								.getPaymentDetails().getPaymentMethod())) {
					response = getCardStatusManager.getCardPaymentStatus(requestData, apiMsgId);
				} else {
					response = getStatusManager.getPaymentStatus(requestData,apiMsgId);
				}
			}

			if (response.getData() != null
					&& response.getData().getResponseCode() != null && !response.getData().getResponseCode().contains("RETR")) {

				responseType.setGetStatusResponse(response);

				return responseType;
			} else {
				buildSuccessResponse(requestType, response, apiMsgId);
			}

		} catch (SSEApplicationException e) {
			responseCode = e.getResponseCode();
			GetStatusRequestValidator.setFailureResponse(commonResponseContext,
					responseCode, requestData.getOrganizationId(), response);
			logger.error(
					apiMsgId,
					"SmartServiceEngine",
					"Get Status API",
					"Exception in getStatus service",
					"Exception occured while processing service layer of getStatus Service",
					AmexLogger.Result.failure,
					"Exception in getStatus service layer", e, "resp_code",
					responseCode, "resp_msg",
					ApiUtil.getErrorDescription(responseCode), "error_msg", e.getErrorMessage());

			//Tivoli Monitoring alert
			if (null != responseCode
					&& responseCode.equals(
							ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_ST)) {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
			}

		} catch (Exception e) {
			responseCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_ST;
			if(requestData.getOrganizationId() != null ) {
			GetStatusRequestValidator.setFailureResponse(commonResponseContext,
					responseCode, requestData.getOrganizationId(), response);
			} else  {
				GetStatusRequestValidator.setFailureResponse(commonResponseContext,
						responseCode, null, response);
			}
			logger.error(
					apiMsgId,
					"SmartServiceEngine",
					"Get Status API",
					"Exception in getStatus service",
					"Exception occured while processing service layer of getStatus Service",
					AmexLogger.Result.failure,
					"Exception in getStatus service layer ", e, "resp_code",
					responseCode, "resp_msg",
					ApiUtil.getErrorDescription(responseCode), "error_msg", e.getMessage());
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
		}
		responseType.setGetStatusResponse(response);
		return responseType;
	}

	private void buildSuccessResponse(GetStatusRequestType requestWrapper,
			GetStatusResponse response, String apiMsgId)
			throws SSEApplicationException {
		GetStatusResponseData responseData = null;
		if (response.getData() != null) {
			responseData = response.getData();
		} else {
			responseData = new GetStatusResponseData();
		}
		// CommonContext Object
		CommonContext commonResponseContext = new CommonContext();
		commonResponseContext.setPartnerId(requestWrapper.getGetStatusRequest()
				.getData().getCommonRequestContext().getPartnerId());
		commonResponseContext.setRequestId(requestWrapper.getGetStatusRequest()
				.getData().getCommonRequestContext().getRequestId());
		commonResponseContext.setPartnerName(requestWrapper
				.getGetStatusRequest().getData().getCommonRequestContext()
				.getPartnerName());

		String timestamp = null;
		try {
			TimeZone timeZone = DateTimeUtil.getTimeZone(requestWrapper
					.getGetStatusRequest().getData().getCommonRequestContext()
					.getTimestamp());
			timestamp = DateTimeUtil.getSystemTime(timeZone);
		} catch (Exception e) {
			logger.error(apiMsgId, "SmartServiceEngine", "Get Status API",
					"GetStatusServiceImpl: buildSuccessResponse",
					"Succesful execution of getStatus service",
					AmexLogger.Result.failure,
					"Exception while getting the system current date", "");

		}
		commonResponseContext.setTimestamp(timestamp);

		// Service Response
		responseData.setCommonResponseContext(commonResponseContext);
		responseData.setOrganizationId(requestWrapper.getGetStatusRequest()
				.getData().getOrganizationId());
		responseData.setStatusCategory(requestWrapper.getGetStatusRequest()
				.getData().getStatusCategory());

		if (requestWrapper.getGetStatusRequest().getData().getAssociatedOrgId() != null) {
			responseData.setAssociatedOrgId(requestWrapper
					.getGetStatusRequest().getData().getAssociatedOrgId());
		}
		response.setData(responseData);
		response.setStatus(ApiConstants.SUCCESS);
	}

	private GetStatusResponse createCardGetAccountResponse(GetStatusRequestData requestData, GetStatusResponse response,String apiMsgId) {
		GetStatusResponseData data = new GetStatusResponseData();		
		data.setCommonResponseContext(formCommonResponseContext(requestData, apiMsgId));
		data.setStatusCategory("AccStatus");
		data.setOrganizationId(requestData.getOrganizationId());
		data.setAssociatedOrgId(requestData.getAssociatedOrgId());
		
		AccountDetailsType accountDetails = new AccountDetailsType();		
		//accountDetails.setEnrollmentActType("CA");		
		List<CardAccountDetailsType> cardAccountDetailsTypeList = new ArrayList<CardAccountDetailsType>();
		CardAccountDetailsType cardDetails =new CardAccountDetailsType();
		cardDetails.setAccountStatus("P");
        cardDetails.setCardNumber("378291840051006");
        cardDetails.setAccountStatusDesc("InProgress");
        cardDetails.setCardHolderName(null);
        cardDetails.setExpiryDate("0817");
        cardDetails.setDefaultCardIndicator("N");
        cardDetails.setCardType(null);
        cardDetails.setPartnerAccountId(requestData.getAccountDetails().getPartnerAccountId());
        cardDetails.setCountryCode("US");
        //cardDetails.setTermsCondAcceptTime("2015-04-11 17:10:51.714");
        cardAccountDetailsTypeList.add(cardDetails);
		accountDetails.setCardAcctDetails(cardAccountDetailsTypeList);	
		data.setAccountDetails(accountDetails);
		response.setData(data);
		response.setStatus(ApiConstants.SUCCESS);
		return response;
	}
	
	private GetStatusResponse createGetOrgResponse(GetStatusRequestData requestData, GetStatusResponse response,String apiMsgId) {
		
		GetStatusResponseData data = new GetStatusResponseData();
		data.setCommonResponseContext(formCommonResponseContext(requestData, apiMsgId));
		data.setStatusCategory("OrgStatus");
		data.setOrganizationId(requestData.getOrganizationId());
		data.setAssociatedOrgId(requestData.getAssociatedOrgId());
		OrgStatusResponse organizationInfo=new OrgStatusResponse();
		organizationInfo.setServiceCode("2");
		organizationInfo.setParentOrgId("CUS3290");
		organizationInfo.setOrganizationStatus("A");
		ServiceType services=new ServiceType();
		services.setServiceCode("2");
		List<ServiceType> serList=new ArrayList<ServiceType>();
		serList.add(services);
		organizationInfo.setSubscribeServices(serList);
		organizationInfo.setOrgName("WALMARTCORP");
		organizationInfo.setOrgShortName("WALMART");
		OrgCardDetailsType cardOrgDetails=new OrgCardDetailsType();
		//cardOrgDetails.setTaxId("999333333");
		organizationInfo.setCardOrgDetails(cardOrgDetails);
		AddressType orgAddress=new AddressType();
		orgAddress.setAddressLine1("3232 Bell Rd");
		orgAddress.setCity("PHEONIX");
		orgAddress.setRegionCode("AZ");
		orgAddress.setRegionName("Arizona");
		orgAddress.setCountryName("USA");
		orgAddress.setCountryCode("US");
		orgAddress.setPostalCode("85032");
		organizationInfo.setOrgAddress(orgAddress);
		data.setOrganizationInfo(organizationInfo);
		response.setData(data);
		response.setStatus(ApiConstants.SUCCESS);
		return response;
	}
	
	private CommonContext formCommonResponseContext(GetStatusRequestData requestData, String apiMsgId) {
		
		CommonContext commonResponseContext = new CommonContext();
		commonResponseContext.setPartnerId(requestData.getCommonRequestContext().getPartnerId());
		commonResponseContext.setRequestId(requestData.getCommonRequestContext().getRequestId());
		commonResponseContext.setPartnerName(requestData.getCommonRequestContext()
				.getPartnerName());

		String timestamp = null;
		try {
			TimeZone timeZone = DateTimeUtil.getTimeZone(requestData.getCommonRequestContext()
					.getTimestamp());
			timestamp = DateTimeUtil.getSystemTime(timeZone);
		} catch (Exception e) {
			logger.error(apiMsgId, "SmartServiceEngine", "Get Status API",
					"GetStatusServiceImpl: buildSuccessResponse",
					"Succesful execution of getStatus service",
					AmexLogger.Result.failure,
					"Exception while getting the system current date", "");

		}
		commonResponseContext.setTimestamp(timestamp);
		return commonResponseContext;
	}
}
