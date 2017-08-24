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
import java.util.TimeZone;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.payve.handler.ext.AddOrganizationInfoServiceExt;
import com.americanexpress.payve.organizationmanagementservice.v1.AddOrganizationInfoFaultMsg;
import com.americanexpress.payve.organizationmanagementservice.v1.IOrganizationManagementService;
import com.americanexpress.payve.organizationmanagementservice.v1.addorganizationinfo.AddOrganizationInfoType;
import com.americanexpress.payve.organizationmanagementservice.v1.addorganizationinfo.FaultType;
import com.americanexpress.payve.organizationmanagementservice.v1.addorganizationinfo.RequestType;
import com.americanexpress.payve.organizationmanagementservice.v1.addorganizationinfo.ResponseType;
import com.americanexpress.smartserviceengine.client.AddOrganizationInfoServiceClient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.CommonContext;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentRequestType;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentResponse;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentResponseData;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.SOAPOperationsLoggingHandler;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;

/**
 * This class contains methods (soap client) to call the AddOrganizationInfo BIP
 * API.
 *
 */
@Service
public class AddOrganizationInfoRequestHelper {

    private static AmexLogger logger = AmexLogger.create(AddOrganizationInfoRequestHelper.class);

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    /**
     * This method invokes the service call and populates the success response
     * from service to response beans.
     *
     * @param requestWrapper
     * @param payvePartnerId
     * @param apiMsgId
     * @return ResponseType object with the Map
     * @throws GeneralSecurityException
     * @throws SSEApplicationException
     */
    public Map<String, Object> addOrganizationInfo(ManageEnrollmentRequestType requestWrapper, String payvePartnerId,
        String apiMsgId) throws GeneralSecurityException,SSEApplicationException {

        Map<String, Object> map = new HashMap<String, Object>();
        logger.debug(apiMsgId,"SmartServiceEngine","Manage Enrollment API","AddOrganizationInfoRequestHelper: addOrganizationInfo: starts",
            "Before invoking AddOrganizationInfo service",AmexLogger.Result.success, "",
            "organizationId", SplunkLogger.organizationId(requestWrapper.getManageEnrollmentRequest().getData()));

        ResponseType responseType = null;
        map = callAddOrganizationInfoService(requestWrapper, payvePartnerId,apiMsgId);// Calling the AddOrganizationInfo service
        responseType = (ResponseType) map.get(ApiConstants.RESPONSE_DETAILS);
       /*ResponseType responseTypestubbed = new ResponseType();
        AddOrganizationInfoRespGrpType addOrganizationInfoRespGrpType = new AddOrganizationInfoRespGrpType();
        StatusType statusType = new StatusType();
        addOrganizationInfoRespGrpType.setOrgId(requestWrapper.getManageEnrollmentRequest().getData().getOrganizationId());
        addOrganizationInfoRespGrpType.setStatusCd(ApiConstants.STATUS_BIP_INPROGRESS);
        addOrganizationInfoRespGrpType.setPaymentEntityId("DR466BPFB8");
        responseTypestubbed.setAddOrganizationInfoRespGrp(addOrganizationInfoRespGrpType);
        statusType.setRespCd(ApiConstants.PAYVE_SUCCESS_RESP_CD);
        responseTypestubbed.setStatus(statusType);  */
        logger.info(apiMsgId, "SmartServiceEngine", "Manage Enrollment API",
            "AddOrganizationInfoRequestHelper: addOrganizationInfo: ends",
            "After invoking AddOrganizationInfo service",
            AmexLogger.Result.success,"",
            "organizationId", SplunkLogger.organizationId(requestWrapper.getManageEnrollmentRequest().getData()));
        map = buildResponse(responseType, requestWrapper, apiMsgId); // set the response attributes to ManageEnrollmentResponse object
        return map;
    }

    /**
     * This method creates request object, calls the soap service.
     *
     * @param requestWrapper
     * @param payvePartnerId
     * @param apiMsgId
     * @return
     * @throws GeneralSecurityException
     * @throws SSEApplicationException
     */
    private Map<String, Object> callAddOrganizationInfoService(ManageEnrollmentRequestType requestWrapper, String payvePartnerId,
        String apiMsgId) throws GeneralSecurityException, SSEApplicationException {
        logger.debug(apiMsgId,"SmartServiceEngine","Manage Enrollment API","AddOrganizationInfoRequestHelper: callAddOrganizationInfoService: starts",
            "Before invoking AddOrganiztionInfo service", AmexLogger.Result.success, "");
        Map<String, Object> map = new HashMap<String, Object>();
        final long startTimeStamp = System.currentTimeMillis();
        boolean isError = false;
        try {
            AddOrganizationInfoServiceExt addOrgInfoService = new AddOrganizationInfoServiceExt();
            IOrganizationManagementService proxy = addOrgInfoService.getOrganizationManagementServicePort();
            BindingProvider bindingProvider = (BindingProvider) proxy;
            bindingProvider.getBinding().getHandlerChain().add(new SOAPOperationsLoggingHandler());
            Map<String, Object> requestContext = bindingProvider.getRequestContext();
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_ORG_SOAP_SERVICE_URL));
            requestContext.put(ApiConstants.REQUEST_TIMEOUT, EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REQUEST_TIMEOUT_VALUE));
            logger.debug(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "AddOrganizationInfoRequestHelper: callAddOrganizationInfoService",
                "Calling AddOrganizationInfo BIP service",AmexLogger.Result.success, "BIP SOAP Endpoint URL",EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_ORG_SOAP_SERVICE_URL));

            AddOrganizationInfoServiceClient client = new AddOrganizationInfoServiceClient();
            ResponseType response = client.buildResponseType();
            final Holder<ResponseType> responseHolder = new Holder<ResponseType>(response);
            RequestType reqType = client.buildRequestType(requestWrapper,payvePartnerId);
            AddOrganizationInfoType addOrgInfoType = client.buildAddOrganizationInfoType(reqType);
            final Holder<AddOrganizationInfoType> requestHolder = new Holder<AddOrganizationInfoType>(addOrgInfoType);
            proxy.addOrganizationInfo(reqType, requestHolder, responseHolder);// Calling addOrganizationInfo SOAP service
            ResponseType respType = responseHolder.value;
            if (null == respType) {
                logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API","AddOrganizationInfoRequestHelper: addOrganizationInfo",
                    "After invoking AddOrganizationInfo service",AmexLogger.Result.failure,"Null response from AddOrganizationInfo API", "");
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ADD_ORG_INFO_ERR_CD, TivoliMonitoring.ADD_ORG_INFO_ERR_MSG, apiMsgId));
                throw new SSEApplicationException("Null response from AddOrganizationInfo API", ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                    EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD));
            }
            logger.debug(apiMsgId,"SmartServiceEngine","Manage Enrollment API","AddOrganizationInfoRequestHelper: callAddOrganizationInfoService",
                "After calling AddOrganizationInfo BIP service",AmexLogger.Result.success, "", "responseCode",responseHolder.value.getStatus().getRespCd());
            map.put(ApiConstants.RESPONSE_DETAILS, respType);
        } catch (AddOrganizationInfoFaultMsg ex) {
            isError = true;
            FaultType faultType = ex.getFaultInfo().getFault();
            String faultDetail = faultType.getFaultDetail();
            String faultCode = faultType.getFaultCode();
            String sseRespCode = null;
            String sseRespDesc = null;
            if (faultCode != null) { // Error response from AddOrgInfo SOAP API
                sseRespCode = EnvironmentPropertiesUtil.getProperty(faultCode);//Get the corresponding SSE response code and response desc from Properties files
                if (sseRespCode != null) {
                    sseRespDesc = EnvironmentPropertiesUtil.getProperty(sseRespCode);
                } else {
                    if (faultDetail.indexOf(" ") > 0) {//Get the response description from FaultDetail element in soap fault
                        faultDetail = faultDetail.substring(faultDetail.indexOf(" ") + 1);
                    }
                    sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD;
                    sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD)+ ": " + faultDetail;
                    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ADD_ORG_INFO_ERR_CD, TivoliMonitoring.ADD_ORG_INFO_ERR_MSG, apiMsgId));
                }
            } else {
                sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD;
                sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD);
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ADD_ORG_INFO_ERR_CD, TivoliMonitoring.ADD_ORG_INFO_ERR_MSG, apiMsgId));
            }
            logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "AddOrganizationInfoRequestHelper: callAddOrganizationInfoService",
                "SOAP Fault Error occured during AddOrganizationInfo service call", AmexLogger.Result.failure, faultDetail, ex, "fault-Actor",
                faultType.getFaultActor(), "fault-Code", faultCode, "fault-String", faultType.getFaultString(), "fault-Detail",
                faultDetail, "SSEResponseCode", sseRespCode, "SSEResponseDesc", sseRespDesc);
            throw new SSEApplicationException("SOAP Fault Error occured during AddOrganizationInfo service call", sseRespCode, sseRespDesc, ex);
        } catch (SSEApplicationException ex) {
            isError = true;
            throw ex;
        }catch (WebServiceException exception) {
        	 isError = true;
        	 
            logger.error(apiMsgId, "SmartServiceEngine", "Manage Enrollment API", "AddOrganizationInfoRequestHelper: callAddOrganizationInfoService",
                "Exception occured during AddOrganizationInfo service call", AmexLogger.Result.failure, exception.getMessage(), exception);
        	if(exception.getMessage().contains("java.net.SocketTimeoutException")){
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_TIMEOUT_ERR_CD, TivoliMonitoring.BIP_TIMEOUT_ERR_MSG, apiMsgId));                
			throw new SSEApplicationException("Exception occured during AddOrganizationInfo service call", ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901,
                     EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901),exception);
        	}else{
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ADD_ORG_INFO_ERR_CD, TivoliMonitoring.ADD_ORG_INFO_ERR_MSG, apiMsgId));
        		throw new SSEApplicationException("Exception occured during AddOrganizationInfo service call", ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                        EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),exception);
        	}
		}catch (Exception ex) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ADD_ORG_INFO_ERR_CD, TivoliMonitoring.ADD_ORG_INFO_ERR_MSG, apiMsgId));
            isError = true;
            logger.error(apiMsgId, "SmartServiceEngine", "Manage Enrollment API", "AddOrganizationInfoRequestHelper: callAddOrganizationInfoService",
                "Exception occured during AddOrganizationInfo service call", AmexLogger.Result.failure, ex.getMessage(), ex);
            throw new SSEApplicationException("Exception occured during AddOrganizationInfo service call", ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),ex);
        } finally {
            final long endTimeStamp = System.currentTimeMillis();
            ThreadLocalManager.getRequestStatistics().put("AddOrganizationInfoService", (isError?"F:":"S:")+(endTimeStamp-startTimeStamp) +" ms");
            logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","AddOrganizationInfoRequestHelper: callAddOrganizationInfoService",
                "After calling AddOrganizationInfo BIP service", AmexLogger.Result.success, "", "addOrganizationInfoTimeTaken",
                (endTimeStamp-startTimeStamp)+ " milliseconds("+(endTimeStamp-startTimeStamp)/1000.00+" seconds)");
        }
        return map;
    }

    /**
     * Utility method to populate the java beans from service response
     *
     * @param responseType
     * @param requestWrapper
     * @param apiMsgId
     * @return Map with response bean in it.
     * @throws SSEApplicationException
     */
    private Map<String, Object> buildResponse(ResponseType responseType, ManageEnrollmentRequestType requestWrapper, String apiMsgId) throws SSEApplicationException {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            logger.debug(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "AddOrganizationInfoRequestHelper: buildSuccessResponse: starts",
                "Build response of AddOrganiztionInfo service",AmexLogger.Result.success, "");
            ManageEnrollmentResponse response = new ManageEnrollmentResponse();
            ManageEnrollmentResponseData responseData = new ManageEnrollmentResponseData();
            CommonContext commonResponseContext = new CommonContext();
            commonResponseContext.setPartnerId(requestWrapper.getManageEnrollmentRequest().getData().getCommonRequestContext().getPartnerId());
            commonResponseContext.setRequestId(requestWrapper.getManageEnrollmentRequest().getData().getCommonRequestContext().getRequestId());
            commonResponseContext.setPartnerName(requestWrapper.getManageEnrollmentRequest().getData().getCommonRequestContext().getPartnerName());
            String timestamp = null;
            try {
                TimeZone timeZone = DateTimeUtil.getTimeZone(requestWrapper.getManageEnrollmentRequest().getData().getCommonRequestContext().getTimestamp());
                timestamp = DateTimeUtil.getSystemTime(timeZone);
            } catch (Exception e) {
                logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API","AddOrganizationInfoRequestHelper: buildSuccessResponse",
                    "After invoking AddOrganizationInfo service",AmexLogger.Result.failure,"Exception while getting the system current date", "");
            }
            commonResponseContext.setTimestamp(timestamp);
            // Service Response
            responseData.setCommonResponseContext(commonResponseContext);
            if(responseType != null && responseType.getAddOrganizationInfoRespGrp() != null) {
            responseData.setOrganizationId(responseType.getAddOrganizationInfoRespGrp().getOrgId());
            }
            String orgStatusCode = null;
            if(responseType != null && responseType.getAddOrganizationInfoRespGrp() != null) {
             orgStatusCode = responseType.getAddOrganizationInfoRespGrp().getStatusCd();
            }
            String orgEnrollStatus = null;
            /*
             * Map the BIP Status code to SSE Status code
             * TODO - Map other BIP status codes
             */

            if (orgStatusCode != null) {
                if (orgStatusCode.equals(ApiConstants.STATUS_BIP_INPROGRESS)) {
                    orgEnrollStatus = ApiConstants.STATUS_BIP_INPROGRESS;
                } else if (orgStatusCode.equals( ApiConstants.STATUS_BIP_INACTIVE)) {
                    orgEnrollStatus = ApiConstants.STATUS_BIP_INACTIVE;
                }else if (orgStatusCode.equals( ApiConstants.STATUS__BIP_ACTIVE)) {
                    orgEnrollStatus = ApiConstants.STATUS__BIP_ACTIVE;
                }else if (orgStatusCode.equals( ApiConstants.STATUS_BIP_VERIFICATION_FAILED)) {
                    orgEnrollStatus = ApiConstants.STATUS_BIP_VERIFICATION_FAILED;
                }
                responseData.setOrgCheckStatus(orgEnrollStatus);
            }

            String payveRespCd = null;
            if(responseType != null && responseType.getStatus() != null) {
             payveRespCd = responseType.getStatus().getRespCd();
            }
            if (payveRespCd != null && payveRespCd.equals(ApiConstants.PAYVE_SUCCESS_RESP_CD)) {
                // Success response from AddOrgInfo SOAP API
                map.put(ApiConstants.RESP_PAYVE_PYMT_ENTITY_ID, responseType.getAddOrganizationInfoRespGrp().getPaymentEntityId());
                ThreadLocalManager.setBipOrgid(responseType.getAddOrganizationInfoRespGrp().getPaymentEntityId());//Added PayveOrgId/BipOrgId for Splunk requirement
                map.put(ApiConstants.ERROR_RESPONSE_FLAG, ApiConstants.FALSE);
            } else {
                map.put(ApiConstants.ERROR_RESPONSE_FLAG, ApiConstants.TRUE);// Error response from AddOrgInfo SOAP API
            }
            response.setData(responseData);
            map.put(ApiConstants.RESPONSE_DETAILS, response);
        } catch (Exception ex) {
            logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API","AddOrganizationInfoRequestHelper: buildSuccessResponse",
                "Exception occured during AddOrganizationInfo service call", AmexLogger.Result.failure, ex.getMessage(), ex);
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
            throw new SSEApplicationException("Exception occured during AddOrganizationInfo service call",ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),ex);
        }
        return map;
    }
}
