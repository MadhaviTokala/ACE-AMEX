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
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.payve.handler.ext.GetOrganizationInfoServiceExt;
import com.americanexpress.payve.organizationmanagementservice.v1.GetOrganizationInfoFaultMsg;
import com.americanexpress.payve.organizationmanagementservice.v1.IOrganizationManagementService;
import com.americanexpress.payve.organizationmanagementservice.v1.getorganizationinfo.FaultType;
import com.americanexpress.payve.organizationmanagementservice.v1.getorganizationinfo.GetOrganizationInfoType;
import com.americanexpress.payve.organizationmanagementservice.v1.getorganizationinfo.RequestType;
import com.americanexpress.payve.organizationmanagementservice.v1.getorganizationinfo.ResponseType;
import com.americanexpress.smartserviceengine.client.GetOrganizationInfoServiceClient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.SOAPOperationsLoggingHandler;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.OrgRequestVO;
import com.americanexpress.smartserviceengine.common.vo.OrgResponseVO;

/**
 * This class contains methods (soap client) to call the GetOrganizationInfo BIP
 * API.
 *
 */
@Service
public class GetOrganizationInfoRequestHelper {

	private static AmexLogger logger = AmexLogger.create(GetOrganizationInfoRequestHelper.class);

	@Autowired
	private TivoliMonitoring tivoliMonitoring;

	/**
	 * This method invokes the service call and populates the success response
	 * from service to response beans.
	 *
	 * @param requestWrapper
	 * @param eventId
	 * @return ResponseType object with the Map
	 * @throws GeneralSecurityException
	 * @throws SSEApplicationException
	 */

	public Map<String, Object> getOrganizationInfo(OrgRequestVO requestWrapper, String eventId) throws GeneralSecurityException, SSEApplicationException {
		Map<String, Object> map = new HashMap<String, Object>();
		logger.info(eventId,"SmartServiceEngine","Organization Status Scheduler Service", "Start of GetOrganizationInfoRequestHelper: getOrganizationInfo",
				"Before invoking GetOrganizationInfo service",AmexLogger.Result.success, "",
				"partnerId" ,requestWrapper.getPartnerId(),"organizationId",requestWrapper.getOrganizationId(),
                "partnerName",requestWrapper.getPartnername(),"paymentEntityId",requestWrapper.getPaymentEntityId());

    	ResponseType responseType = null;
		map = callGetOrganizationInfoService(requestWrapper, eventId);// Calling the GetOrganizationInfo service
		responseType = (ResponseType) map.get(SchedulerConstants.RESPONSE_DETAILS);
		logger.info(eventId, "SmartServiceEngine", "Organization Status Scheduler Service",
				"End of GetOrganizationInfoRequestHelper: getOrganizationInfo", "After invoking GetOrganizationInfo service",
				AmexLogger.Result.success,"", "responseCode", responseType.getStatus().getRespCd());
		map = buildResponse(responseType, requestWrapper, eventId);// set the response attributes to OrgResponseVO object
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
	private Map<String, Object> callGetOrganizationInfoService(OrgRequestVO requestWrapper, String eventId) throws GeneralSecurityException, SSEApplicationException {
		logger.info(eventId,"SmartServiceEngine","Organization Status Scheduler Service", "GetOrganizationInfoRequestHelper: callGetOrganizationInfoService: starts",
				"Before invoking getOrganiztionInfo service",AmexLogger.Result.success, "", "partnerId" ,requestWrapper.getPartnerId(),
				"organizationId",requestWrapper.getOrganizationId(), "partnerName",requestWrapper.getPartnername(),"paymentEntityId",requestWrapper.getPaymentEntityId());
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			GetOrganizationInfoServiceExt getOrgInfoService = new GetOrganizationInfoServiceExt();
			IOrganizationManagementService proxy = getOrgInfoService.getOrganizationManagementServicePort();
			BindingProvider bindingProvider = (BindingProvider) proxy;
			bindingProvider.getBinding().getHandlerChain().add(new SOAPOperationsLoggingHandler());
			Map<String, Object> requestContext = bindingProvider.getRequestContext();
			requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,EnvironmentPropertiesUtil .getProperty(SchedulerConstants.BIP_ORG_SOAP_SERVICE_URL));
			requestContext.put(ApiConstants.REQUEST_TIMEOUT, EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REQUEST_TIMEOUT_VALUE));

			logger.info(eventId,"SmartServiceEngine","Organization Status Scheduler Service", "GetOrganizationInfoRequestHelper: callGetOrganizationInfoService",
					"Calling GetOrganizationInfo BIP service", AmexLogger.Result.success, "", "partnerId" ,requestWrapper.getPartnerId(),"organizationId",
					requestWrapper.getOrganizationId(), "partnerName",requestWrapper.getPartnername(),"paymentEntityId",requestWrapper.getPaymentEntityId(),
					"BIP_Endpoint_URL", EnvironmentPropertiesUtil.getProperty(SchedulerConstants.BIP_ORG_SOAP_SERVICE_URL));
			GetOrganizationInfoServiceClient client = new GetOrganizationInfoServiceClient();
			RequestType reqType = client.buildRequestType(requestWrapper,eventId);
			GetOrganizationInfoType getOrgInfoType = client.buildGetOrganizationInfoType(reqType);

			final Holder<GetOrganizationInfoType> requestHolder = new Holder<GetOrganizationInfoType>(getOrgInfoType);
			ResponseType response = client.buildResponseType();

			final Holder<ResponseType> responseHolder = new Holder<ResponseType>(response);
			proxy.getOrganizationInfo(reqType, requestHolder, responseHolder);

			ResponseType respType = responseHolder.value;
			if (null == respType) {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_ORG_INFO_ERR_CD, TivoliMonitoring.GET_ORG_INFO_ERR_MSG, eventId));
				logger.error(eventId,"SmartServiceEngine","Organization Status Scheduler Service","GetOrganizationInfoRequestHelper: callGetOrganizationInfoService",
						"After invoking GetOrganizationInfo service", AmexLogger.Result.failure, "Null response from GetOrganizationInfo Service");
				throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil .getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD));
			}
			logger.info(eventId,"SmartServiceEngine", "Organization Status Scheduler Service", "GetOrganizationInfoRequestHelper: callGetOrganizationInfoService",
					"After calling GetOrganizationInfo BIP service",AmexLogger.Result.success, "", "responseCode", responseHolder.value.getStatus().getRespCd());
			map.put(SchedulerConstants.RESPONSE_DETAILS, respType);
		} catch (GetOrganizationInfoFaultMsg ex) {
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
					logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_ORG_INFO_ERR_CD, TivoliMonitoring.GET_ORG_INFO_ERR_MSG, eventId));
					}
				}
			} else {
				sseRespCode = SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD;
				sseRespDesc = EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD);
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_ORG_INFO_ERR_CD, TivoliMonitoring.GET_ORG_INFO_ERR_MSG, eventId));
			}
			logger.error(eventId,"SmartServiceEngine","Organization Status Scheduler Service","GetOrganizationInfoRequestHelper: callGetOrganizationInfoService",
					"SOAP Fault Error occured during GetOrganizationInfo service call",AmexLogger.Result.failure, faultDetail, ex, "fault-Actor",
					faultType.getFaultActor(), "fault-Code", faultCode,"fault-String", faultType.getFaultString(), "fault-Detail",
					faultDetail, "SSEResponseCode", sseRespCode, "SSEResponseDesc", sseRespDesc);
			throw new SSEApplicationException(sseRespCode, sseRespDesc, ex);
		}catch (WebServiceException exception) {
                    
               logger.error(eventId, "SmartServiceEngine", "GetOrganizationInfoRequestHelper", "GetOrganizationInfoRequestHelper: callGetOrganizationInfoService",
                   "Exception occured during callGetOrganizationInfoService service call", AmexLogger.Result.failure, exception.getMessage(), exception);
                   if(exception.getMessage().contains("java.net.SocketTimeoutException")){
                	   logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_TIMEOUT_ERR_CD, TivoliMonitoring.BIP_TIMEOUT_ERR_MSG, eventId));
                    throw new SSEApplicationException("Exception occured during callGetOrganizationInfoService service call", ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901,
                        EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901),exception);
                   }else{
                	   logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_ORG_INFO_ERR_CD, TivoliMonitoring.GET_ORG_INFO_ERR_MSG, eventId));
                           throw new SSEApplicationException("Exception occured during callGetOrganizationInfoService service call", ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                           EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),exception);
                   }
                   } catch (SSEApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_ORG_INFO_ERR_CD, TivoliMonitoring.GET_ORG_INFO_ERR_MSG, eventId));
			logger.error(eventId,"SmartServiceEngine","Organization Status Scheduler Service","GetOrganizationInfoRequestHelper: callGetOrganizationInfoService",
					"Exception occured during GetOrganizationInfo service call",AmexLogger.Result.failure, ex.getMessage(), ex);
			throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,
					EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD),ex);
		}
		logger.info(eventId,"SmartServiceEngine","Organization Status Scheduler Service","GetOrganizationInfoRequestHelper: callGetOrganizationInfoService: ends",
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
	private Map<String, Object> buildResponse(ResponseType responseType,OrgRequestVO requestWrapper, String eventId) throws SSEApplicationException {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			OrgResponseVO response = new OrgResponseVO();
			if (responseType != null) {
				if (responseType.getGetOrganizationInfoRespGrp() != null) {
					response.setOrgStatusDesc(responseType.getGetOrganizationInfoRespGrp().getStatusDesc());
				}
				response.setOrgStatusDate(DateTimeUtil.getYYYYMMDDDb2DateValue(responseType.getGetOrganizationInfoRespGrp().getStatusDt()));
				response.setOrgStatusCd(responseType.getGetOrganizationInfoRespGrp().getStatusCd());
				if (responseType.getGetOrganizationInfoRespGrp().getOrganizationInfo() != null) {
					response.setOrganizationId(responseType.getGetOrganizationInfoRespGrp().getOrganizationInfo().getOrgId());
					response.setPartnerId(responseType.getGetOrganizationInfoRespGrp().getOrganizationInfo().getPartnerEntityId());
				}
				String payveRespCd = responseType.getStatus().getRespCd();
				response.setOrgRespCd(payveRespCd);
				String payveRespDesc = responseType.getStatus().getRespDesc();
				response.setOrgRespDesc(payveRespDesc);

				if (payveRespCd.equals(SchedulerConstants.SUCCESS_RESP_CD)) {// Success response from GetOrgInfo SOAP API
					map.put(SchedulerConstants.ERROR_RESPONSE_FLAG, SchedulerConstants.FALSE);
				} else {// Error response from GetOrgInfo SOAP API
					map.put(SchedulerConstants.ERROR_RESPONSE_FLAG, SchedulerConstants.TRUE);
				}
			} else {// Error response from GetOrgInfo SOAP API
				map.put(SchedulerConstants.ERROR_RESPONSE_FLAG, SchedulerConstants.TRUE);
			}
			map.put(SchedulerConstants.RESPONSE_DETAILS, response);
		} catch (Exception ex) {
			logger.error(eventId,"SmartServiceEngine","Organization Status Scheduler Service", "GetOrganizationInfoRequestHelper: buildResponse",
					"Exception occured during GetOrganizationInfo service call", AmexLogger.Result.failure, ex.getMessage(), ex);
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, eventId));
			throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), ex);
		}
		return map;
	}
}
