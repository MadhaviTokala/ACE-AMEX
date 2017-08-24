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
package com.americanexpress.smartserviceengine.helper;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.payve.handler.ext.GetOrgAccountsServiceExt;
import com.americanexpress.payve.organizationaccountservice.v1.GetOrganizationAccountsFaultMsg;
import com.americanexpress.payve.organizationaccountservice.v1.IOrganizationAccountService;
import com.americanexpress.payve.organizationaccountservice.v1.getorganizationaccounts.AccountDetailsType;
import com.americanexpress.payve.organizationaccountservice.v1.getorganizationaccounts.CheckAccountDetailsType;
import com.americanexpress.payve.organizationaccountservice.v1.getorganizationaccounts.FaultType;
import com.americanexpress.payve.organizationaccountservice.v1.getorganizationaccounts.GetOrganizationAccountsType;
import com.americanexpress.payve.organizationaccountservice.v1.getorganizationaccounts.RequestType;
import com.americanexpress.payve.organizationaccountservice.v1.getorganizationaccounts.ResponseType;
import com.americanexpress.smartserviceengine.client.GetAccountInfoServiceClient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.SOAPOperationsLoggingHandler;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.AccRequestVO;
import com.americanexpress.smartserviceengine.common.vo.AccResponseVO;




/**
 * This class contains methods (soap client) to call the AddOrganizationAccounts
 * BIP API.
 *
 */
@Service
public class GetAccountInfoRequestHelper {

	private static AmexLogger logger = AmexLogger.create(GetAccountInfoRequestHelper.class);

	@Autowired
	private TivoliMonitoring tivoliMonitoring;

	/**
	 * This method invokes the service call and populates the success response
	 * from service to response beans.
	 * @param requestWrapper
	 * @param eventId
	 * @return ResponseType object with the Map
	 * @throws GeneralSecurityException
	 * @throws SSEApplicationException
	 */

	public Map<String, Object> getAccountInfo(AccRequestVO requestWrapper, String eventId) throws GeneralSecurityException, SSEApplicationException {
		Map<String, Object> map = new HashMap<String, Object>();
		logger.info(eventId,"SmartServiceEngine","GetAccountInfoRequestHelper","GetAccountInfoRequestHelper: getAccountInfo: starts",
				"Before invoking GetOrganizationAccount service", AmexLogger.Result.success, "");
		ResponseType responseType = null;
		map = callGetOrgAccountService(requestWrapper, eventId);
		responseType = (ResponseType) map.get(SchedulerConstants.RESPONSE_DETAILS);
		logger.info(eventId, "SmartServiceEngine", "GetAccountInfoRequestHelper","GetAccountInfoRequestHelper: getAccountInfo: ends",
				"After invoking GetOrganizationAccount service",AmexLogger.Result.success, "", "responseCode", responseType.getStatus().getRespCd());
		map = buildResponse(responseType, requestWrapper, eventId);// set the response attributes to AccResponseVO object
		return map;
	}

	/**
	 * This method creates request object, calls the soap service.
	 *
	 * @param requestWrapper
	 * @param payvePartnerId
	 * @param eventId
	 * @return map object
	 * @throws GeneralSecurityException
	 * @throws SSEApplicationException
	 */
	private Map<String, Object> callGetOrgAccountService( AccRequestVO requestWrapper, String eventId) throws GeneralSecurityException, SSEApplicationException {
		logger.info(eventId,"SmartServiceEngine","GetAccountInfoRequestHelper", "GetAccountInfoRequestHelper: callGetOrgAccountService: starts",
				"Before invoking getOrganizationInfo service", AmexLogger.Result.success, "");
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			GetOrgAccountsServiceExt getOrgInfoService = new GetOrgAccountsServiceExt();
			IOrganizationAccountService proxy = getOrgInfoService.getOrganizationAccountServicePort();

			BindingProvider bindingProvider = (BindingProvider) proxy;

			bindingProvider.getBinding().getHandlerChain().add(new SOAPOperationsLoggingHandler());

			Map<String, Object> requestContext = bindingProvider.getRequestContext();
			requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, EnvironmentPropertiesUtil.getProperty(SchedulerConstants.BIP_ACC_SOAP_SERVICE_URL));
			requestContext.put(ApiConstants.REQUEST_TIMEOUT, EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REQUEST_TIMEOUT_VALUE));

			logger.info(eventId,"SmartServiceEngine","GetAccountInfoRequestHelper","GetAccountInfoRequestHelper: callGetOrgAccountService",
					"Calling GetOrganizationInfo BIP service",AmexLogger.Result.success,"", "BIP SOAP Endpoint URL",
					EnvironmentPropertiesUtil.getProperty(SchedulerConstants.BIP_ACC_SOAP_SERVICE_URL));
			GetAccountInfoServiceClient client = new GetAccountInfoServiceClient();
			RequestType reqType = client.buildRequestType(requestWrapper, eventId);
			GetOrganizationAccountsType getOrgInfoType = client.buildGetOrganizationAccountType(reqType);
			final Holder<GetOrganizationAccountsType> requestHolder = new Holder<GetOrganizationAccountsType>(getOrgInfoType);
			ResponseType response = client.buildResponseType();
			final Holder<ResponseType> responseHolder = new Holder<ResponseType>(response);
			proxy.getOrganizationAccounts(reqType, requestHolder, responseHolder);// Calling getOrganizationInfo SOAP service
			ResponseType respType = responseHolder.value;

			if (null == respType) {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_ORG_ACCNT_ERR_CD, TivoliMonitoring.GET_ORG_ACCNT_ERR_MSG, eventId));
				logger.error(eventId,"SmartServiceEngine","GetAccountInfoRequestHelper","GetAccountInfoRequestHelper: callGetOrgAccountService",
						"After invoking GetOrganizationInfo service",AmexLogger.Result.failure, "Null response from GetOrganizationInfo service", "");
				throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD));
			}
			logger.info(eventId,"SmartServiceEngine","GetAccountInfoRequestHelper","GetAccountInfoRequestHelper: callGetOrgAccountService",
					"After calling GetOrganizationInfo BIP service",AmexLogger.Result.success, "", "responseCode",responseHolder.value.getStatus().getRespCd());
			map.put(SchedulerConstants.RESPONSE_DETAILS, respType);
		}catch (WebServiceException exception) {
			logger.error(eventId, "SmartServiceEngine", "GetAccountInfoRequestHelper", "GetAccountInfoRequestHelper: callGetOrgAccountService",
					"WebServiceException occured during callGetOrgAccountService service call", AmexLogger.Result.failure, exception.getMessage(), exception);
			if(exception.getMessage().contains("java.net.SocketTimeoutException")){
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_TIMEOUT_ERR_CD, TivoliMonitoring.BIP_TIMEOUT_ERR_CD, eventId));
				throw new SSEApplicationException("Exception occured during callGetOrgAccountService service call", ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901,
						EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901),exception);
			}else{
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_ORG_ACCNT_ERR_CD, TivoliMonitoring.GET_ORG_ACCNT_ERR_MSG, eventId));
				throw new SSEApplicationException("Exception occured during callGetOrgAccountService service call", ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
						EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),exception);
			}
		} catch (GetOrganizationAccountsFaultMsg ex) {	
			FaultType faultType = ex.getFaultInfo().getFault();
			String faultDetail = faultType.getFaultDetail();
			String faultCode = faultType.getFaultCode();
			String sseRespCode = null;
			String sseRespDesc = null;
			if (faultCode != null) {// Error response from AddOrgInfo SOAP API
				sseRespCode = EnvironmentPropertiesUtil.getProperty(faultCode);//Get the corresponding SSE response code and response desc from Properties files
				if (sseRespCode != null) {
					sseRespDesc = EnvironmentPropertiesUtil.getProperty(sseRespCode);
				} else {
					if (faultDetail.indexOf(" ") > 0) {//Get the response description from FaultDetail element in soap fault
						faultDetail = faultDetail.substring(faultDetail.indexOf(" ") + 1);
					}
					sseRespCode = SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD;
					sseRespDesc = EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD)+ ": " + faultDetail;

					if(faultCode.equals("07000")) {//If "Datapower Internal Server Error" is returned, raise Tivoli alert
						logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_ORG_ACCNT_ERR_CD, TivoliMonitoring.GET_ORG_ACCNT_ERR_MSG, eventId));
					}
				}
			} else {
				sseRespCode = SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD;
				sseRespDesc = EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD);
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_ORG_ACCNT_ERR_CD, TivoliMonitoring.GET_ORG_ACCNT_ERR_MSG, eventId));
			}
			logger.error(eventId, "SmartServiceEngine", "GetAccountInfoRequestHelper","GetAccountInfoRequestHelper: callGetOrgAccountService",
					"SOAP Fault Error occured during GetOrganizationInfo service call", AmexLogger.Result.failure, faultDetail, ex, "fault-Actor",
					faultType.getFaultActor(), "fault-Code", faultCode,"fault-String", faultType.getFaultString(), "fault-Detail",
					faultDetail, "SSEResponseCode", sseRespCode, "SSEResponseDesc", sseRespDesc);
			throw new SSEApplicationException(sseRespCode, sseRespDesc, ex);
		}
		catch (SSEApplicationException ex) {
			throw ex;
		}
		catch (Exception ex) {
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_ORG_ACCNT_ERR_CD, TivoliMonitoring.GET_ORG_ACCNT_ERR_MSG, eventId));
			logger.error(eventId,"SmartServiceEngine","GetAccountInfoRequestHelper","GetAccountInfoRequestHelper: callGetOrgAccountService",
					"Exception occured during GetOrganizationInfo service call", AmexLogger.Result.failure, ex.getMessage(), ex);
			throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), ex);
		}
		logger.info(eventId,"SmartServiceEngine","GetAccountInfoRequestHelper", "GetAccountInfoRequestHelper: callGetOrgAccountService: ends",
				"After invoking GetOrganizationInfo service", AmexLogger.Result.success, "");
		return map;
	}

	/**
	 * Utility method to populate the java beans from service response
	 *
	 * @param responseType
	 * @param requestWrapper
	 * @param eventId
	 * @return Map with response bean in it.
	 * @throws SSEApplicationException
	 */
	private Map<String, Object> buildResponse(ResponseType responseType, AccRequestVO requestWrapper, String eventId) throws SSEApplicationException {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			AccResponseVO response = new AccResponseVO();
			if (responseType != null) {
				AccountDetailsType checkAccDetails = responseType.getGetOrganizationAccountsRespGrp().getAccountDetails();
				List<CheckAccountDetailsType> accountDetailsTypes = checkAccDetails.getCheckAccountDetails();
				String payveAcctId = null;
				String partnerAccId = null;
				String accStatusCd = null;
				String accStatusDesc = null;
				String accStatusDate = null;
				String accRespCd = null;
				String accRespDesc=null;

				if(null!=responseType.getStatus()){
					accRespCd = responseType.getStatus().getRespCd();
					accRespDesc = responseType.getStatus().getRespDesc();
				}

				for(int i = 0;i<accountDetailsTypes.size();i++){
					payveAcctId = accountDetailsTypes.get(i).getPayveAcctId();
					partnerAccId = accountDetailsTypes.get(i).getPaymentInd();
					accStatusDesc = accountDetailsTypes.get(i).getStatusDesc();
					accStatusCd = accountDetailsTypes.get(i).getStatusCd();
					accStatusDate = accountDetailsTypes.get(i).getStatusDt();
				}

				if (responseType.getGetOrganizationAccountsRespGrp().getAccountDetails() != null) {
					response.setPaymentMethod("CH");
					response.setPartnerAccId(partnerAccId);
					response.setAccStatusCd(accStatusCd);
					response.setPayveAcctId(payveAcctId);
					response.setAccRespCd(accRespCd);
					response.setAccRespDesc(accRespDesc);
					if(accStatusDate !=  null) {
						response.setAccStatusDate(DateTimeUtil.getYYYYMMDDDb2DateValue(accStatusDate));
					}
					response.setAccStatusDesc(accStatusDesc);
				} else{
					logger.debug(eventId,"SmartServiceEngine","GetAccountInfoRequestHelper", "GetAccountInfoRequestHelper:buildResponse",
							"Response is null",AmexLogger.Result.failure, "Please check whether the reponse is is coming from SOAP requst");
				}
			}
			map.put(SchedulerConstants.RESPONSE_DETAILS, response);
		} catch (Exception ex) {
			logger.error(eventId,"SmartServiceEngine","GetOrganizationAccountService", "GetAccountInfoRequestHelper: buildResponse",
					"Exception occured during GetOrganizationInfo service call", AmexLogger.Result.failure, ex.getMessage(), ex);
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, eventId));
			throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), ex);
		}
		return map;
	}
}
