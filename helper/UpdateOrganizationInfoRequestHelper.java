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
import com.americanexpress.payve.handler.ext.UpdateOrganizationInfoServiceExt;
import com.americanexpress.payve.organizationmanagementservice.v1.IOrganizationManagementService;
import com.americanexpress.payve.organizationmanagementservice.v1.UpdateOrganizationInfoFaultMsg;
import com.americanexpress.payve.organizationmanagementservice.v1.updateorganizationinfo.FaultType;
import com.americanexpress.payve.organizationmanagementservice.v1.updateorganizationinfo.RequestType;
import com.americanexpress.payve.organizationmanagementservice.v1.updateorganizationinfo.ResponseType;
import com.americanexpress.payve.organizationmanagementservice.v1.updateorganizationinfo.UpdateOrganizationInfoType;
import com.americanexpress.smartserviceengine.client.UpdateOrganizationInfoServiceClient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.CommonContext;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentRequestType;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentResponse;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentResponseData;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.SOAPOperationsLoggingHandler;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;

/**
 * This class contains methods (soap client) to call the UpdateOrganizationInfo
 * BIP API.
 *
 */
@Service
public class UpdateOrganizationInfoRequestHelper {

    private static AmexLogger logger = AmexLogger.create(UpdateOrganizationInfoRequestHelper.class);

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    /**
     * This method invokes the service call and populates the success response
     * from service to response beans.
     *
     * @param requestWrapper
     * @param payvePartnerId
     * @param paymentEntityId
     * @param apiMsgId
     * @return ResponseType object with the Map
     * @throws GeneralSecurityException
     * @throws SSEApplicationException
     */
    public Map<String, Object> updateOrganizationInfo(ManageEnrollmentRequestType requestWrapper, String payvePartnerId,
        String paymentEntityId, String apiMsgId) throws GeneralSecurityException, SSEApplicationException {
        Map<String, Object> map = new HashMap<String, Object>();
        logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrganizationInfoRequestHelper: updateOrganizationInfo: starts",
            "Before invoking updateOrganizationInfo service", AmexLogger.Result.success, "");
        ResponseType responseType = null;
        ThreadLocalManager.setBipOrgid(paymentEntityId);//Added PayveOrgId/BipOrgId for Splunk requirement
        map = callUpdateOrganizationInfoService(requestWrapper, payvePartnerId, paymentEntityId,  apiMsgId);// Calling the updateOrganizationInfo soap service
        responseType = (ResponseType) map.get(ApiConstants.RESPONSE_DETAILS);
        //Bypassing the BIP Invocation
        /*ResponseType responseType = new ResponseType();
        UpdateOrganizationInfoRespGrpType updateOrganizationInfoRespGrpType = new UpdateOrganizationInfoRespGrpType();
        StatusType statusType = new StatusType();
        updateOrganizationInfoRespGrpType.setOrgId(requestWrapper.getManageEnrollmentRequest().getData().getOrganizationId());
        updateOrganizationInfoRespGrpType.setStatusCd(ApiConstants.STATUS_BIP_INPROGRESS);
        updateOrganizationInfoRespGrpType.setPaymentEntityId(paymentEntityId);
        responseType.setUpdateOrganizationInfoRespGrp(updateOrganizationInfoRespGrpType);
        statusType.setRespCd(ApiConstants.PAYVE_SUCCESS_RESP_CD);
        responseType.setStatus(statusType);*/
        logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrganizationInfoRequestHelper: updateOrganizationInfo: ends",
            "After invoking updateOrganizationInfo service", AmexLogger.Result.success, "","status", responseType.getStatus().getRespCd());
        map = buildResponse(responseType, requestWrapper, apiMsgId);// set the response attributes to ManageEnrollmentResponse object
        return map;
    }

    /**
     * This method creates request object, calls the service.
     *
     * @param requestWrapper
     * @param payvePartnerId
     * @param paymentEntityId
     * @param apiMsgId
     * @return
     * @throws GeneralSecurityException
     * @throws SSEApplicationException
     */
    private Map<String, Object> callUpdateOrganizationInfoService(ManageEnrollmentRequestType requestWrapper, String payvePartnerId,
        String paymentEntityId,String apiMsgId) throws GeneralSecurityException, SSEApplicationException {
        logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrganizationInfoRequestHelper: callUpdateOrganizationInfoService: starts",
            "Before invoking UpdateOrganiztionInfo service", AmexLogger.Result.success, "");
        Map<String, Object> map = new HashMap<String, Object>();
        final long startTimeStamp = System.currentTimeMillis();
        boolean isError = false;
        try {
            UpdateOrganizationInfoServiceExt addOrgInfoService = new UpdateOrganizationInfoServiceExt();
            IOrganizationManagementService proxy = addOrgInfoService.getOrganizationManagementServicePort();
            BindingProvider bindingProvider = (BindingProvider) proxy;
            bindingProvider.getBinding().getHandlerChain().add(new SOAPOperationsLoggingHandler());
            Map<String, Object> requestContext = bindingProvider.getRequestContext();
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_ORG_SOAP_SERVICE_URL));
            requestContext.put(ApiConstants.REQUEST_TIMEOUT, EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REQUEST_TIMEOUT_VALUE));
            logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrganizationInfoRequestHelper: callUpdateOrganizationInfoService",
                "Before Calling updateOrganizationInfo BIP service",AmexLogger.Result.success, " ", "BIP SOAP Endpoint URL",
                EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_ORG_SOAP_SERVICE_URL));

            UpdateOrganizationInfoServiceClient client = new UpdateOrganizationInfoServiceClient();
            ResponseType response = client.buildResponseType();
            final Holder<ResponseType> responseHolder = new Holder<ResponseType>(response);
            RequestType reqType = client.buildRequestType(requestWrapper, payvePartnerId, paymentEntityId);
            UpdateOrganizationInfoType updateOrgInfoType = client.buildUpdateOrganizationInfoType(reqType);
            final Holder<UpdateOrganizationInfoType> requestHolder = new Holder<UpdateOrganizationInfoType>(updateOrgInfoType);
            proxy.updateOrganizationInfo(reqType, requestHolder, responseHolder);//Call the updateOrganizationInfo Soap service
            ResponseType respType = responseHolder.value;
            if (null == respType) {
                logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrganizationInfoRequestHelper: updateOrganizationInfo",
                    "After invoking updateOrganizationInfo service",AmexLogger.Result.failure,"Null response from updateOrganizationInfo API", "");
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.UPD_ORG_INFO_ERR_CD, TivoliMonitoring.UPD_ORG_INFO_ERR_MSG, apiMsgId));
                throw new SSEApplicationException("Null response from updateOrganizationInfo API",ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                    EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD));
            }
            logger.debug(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "UpdateOrganizationInfoRequestHelper: callUpdateOrganizationInfoService",
                "After calling updateOrganizationInfo BIP service",AmexLogger.Result.success, "", "responseCode", responseHolder.value.getStatus().getRespCd());
            map.put(ApiConstants.RESPONSE_DETAILS, respType);

        } catch (UpdateOrganizationInfoFaultMsg ex) {
            isError = true;
            FaultType faultType = ex.getFaultInfo().getFault();
            String faultDetail = faultType.getFaultDetail();
            String faultCode = faultType.getFaultCode();
            logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrganizationInfoRequestHelper: callUpdateOrganizationInfoService",
                "SOAP Fault Error occured during updateOrganizationInfo service call",AmexLogger.Result.failure, faultDetail, ex, "fault-Actor",
                faultType.getFaultActor(), "fault-Code", faultCode,"fault-String", faultType.getFaultString(), "fault-Detail",faultDetail);
            String sseRespCode = null;
            String sseRespDesc = null;
            // Error response from UpdateOrgInfo SOAP API
            if (faultCode != null) {
                sseRespCode = EnvironmentPropertiesUtil.getProperty(faultCode);
                if (sseRespCode != null) {
                    sseRespDesc = EnvironmentPropertiesUtil.getProperty(sseRespCode);
                } else {
                    if (faultDetail.indexOf(" ") > 0) {
                        faultDetail = faultDetail.substring(faultDetail.indexOf(" ") + 1);
                    }
                    sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD;
                    sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD)+ ": " + faultDetail;
                    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.UPD_ORG_INFO_ERR_CD, TivoliMonitoring.UPD_ORG_INFO_ERR_MSG, apiMsgId));
                }
            } else {
                sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD;
                sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD);
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.UPD_ORG_INFO_ERR_CD, TivoliMonitoring.UPD_ORG_INFO_ERR_MSG, apiMsgId));
            }
            throw new SSEApplicationException("SOAP Fault Error occured during updateOrganizationInfo service call",sseRespCode, sseRespDesc, ex);
        } catch (SSEApplicationException ex) {
            isError = true;
            throw ex;
		} catch (WebServiceException exception) {
			isError = true;
			
			logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "UpdateOrganizationInfoRequestHelper: callUpdateOrganizationInfoService",
	                "Exception occured during updateOrganizationInfo service call", AmexLogger.Result.failure, exception.getMessage(), exception);
			if (exception.getMessage().contains("java.net.SocketTimeoutException")) {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_TIMEOUT_ERR_CD, TivoliMonitoring.BIP_TIMEOUT_ERR_MSG, apiMsgId));
				throw new SSEApplicationException("Exception occured during updateOrganizationInfo service call",ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901,
						EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901),exception);
			} else {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.UPD_ORG_INFO_ERR_CD, TivoliMonitoring.UPD_ORG_INFO_ERR_MSG, apiMsgId));
				throw new SSEApplicationException("Exception occured during updateOrganizationInfo service call",ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
						EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),exception);
			}
		}catch (Exception ex) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.UPD_ORG_INFO_ERR_CD, TivoliMonitoring.UPD_ORG_INFO_ERR_MSG, apiMsgId));
            isError = true;
            logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "UpdateOrganizationInfoRequestHelper: callUpdateOrganizationInfoService",
                "Exception occured during updateOrganizationInfo service call", AmexLogger.Result.failure, ex.getMessage(), ex);
            throw new SSEApplicationException("Exception occured during updateOrganizationInfo service call",ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),ex);
        }  finally {
            final long endTimeStamp = System.currentTimeMillis();
            logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrganizationInfoRequestHelper: callUpdateOrganizationInfoService: ends",
                "After invoking updateOrganizationInfo service", AmexLogger.Result.success, "", "Total Time Taken to get response from updateOrganizationAccounts SOAP service",
                (endTimeStamp-startTimeStamp)+ " milliseconds("+(endTimeStamp-startTimeStamp)/1000.00+" seconds)");
            ThreadLocalManager.getRequestStatistics().put("AddOrganizationInfoService", (isError?"F:":"S:")+(endTimeStamp-startTimeStamp) +" ms");
        }
        return map;
    }

    /**
     * Utility method to populate the java beans from service response
     *
     * @param responseType
     * @param requestWrapper
     * @return Map with response bean in it.
     * @throws SSEApplicationException
     */
    private Map<String, Object> buildResponse(ResponseType responseType, ManageEnrollmentRequestType requestWrapper, String apiMsgId) throws SSEApplicationException {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            logger.info(apiMsgId, "SmartServiceEngine", "Manage Enrollment API", "UpdateOrganizationInfoRequestHelper: buildResponse: starts",
                "Build response of UpdateOrganiztionInfo service", AmexLogger.Result.success, "");
            ManageEnrollmentResponse response = new ManageEnrollmentResponse();
            ManageEnrollmentResponseData responseData = new ManageEnrollmentResponseData();
            // CommonContext Object
            CommonContext commonResponseContext = new CommonContext();
            commonResponseContext.setPartnerId(requestWrapper.getManageEnrollmentRequest().getData().getCommonRequestContext().getPartnerId());
            commonResponseContext.setRequestId(requestWrapper.getManageEnrollmentRequest().getData().getCommonRequestContext().getRequestId());
            commonResponseContext.setPartnerName(requestWrapper.getManageEnrollmentRequest().getData().getCommonRequestContext().getPartnerName());
            String timestamp = null;
            try {
                TimeZone timeZone = DateTimeUtil.getTimeZone(requestWrapper.getManageEnrollmentRequest().getData().getCommonRequestContext().getTimestamp());
                timestamp = DateTimeUtil.getSystemTime(timeZone);
            } catch (Exception e) {
                logger.error(apiMsgId, "SmartServiceEngine","Manage Enrollment API","UpdateOrganizationInfoRequestHelper: buildResponse",
                    "After invoking updateOrganizationInfo service",AmexLogger.Result.failure,"Exception while getting the system current date", "");
            }
            commonResponseContext.setTimestamp(timestamp);
            responseData.setCommonResponseContext(commonResponseContext);
            if( responseType != null && responseType.getUpdateOrganizationInfoRespGrp() != null) {// Server Response
            responseData.setOrganizationId(responseType.getUpdateOrganizationInfoRespGrp().getOrgId());
            }

            String orgStatusCode = null;
            if(responseType != null && responseType.getUpdateOrganizationInfoRespGrp() != null) {
            	orgStatusCode = responseType.getUpdateOrganizationInfoRespGrp().getStatusCd();
            }
            String orgEnrollStatus = null;
            /*
             * Map the BIP Status code to SSE Status code
             * TODO - Map other BIP status codes
             */
            if (orgStatusCode != null) {
                if (orgStatusCode.equalsIgnoreCase(ApiConstants.STATUS_BIP_INPROGRESS)) {
                    orgEnrollStatus = ApiConstants.STATUS_BIP_INPROGRESS;
                } else if (orgStatusCode.equalsIgnoreCase( ApiConstants.STATUS_BIP_INACTIVE)) {
                    orgEnrollStatus =  ApiConstants.STATUS_BIP_INACTIVE;
                }else if (orgStatusCode.equalsIgnoreCase( ApiConstants.STATUS__BIP_ACTIVE)) {
                    orgEnrollStatus =  ApiConstants.STATUS__BIP_ACTIVE;
                }else if (orgStatusCode.equalsIgnoreCase( ApiConstants.STATUS_BIP_VERIFICATION_FAILED)) {
                    orgEnrollStatus =  ApiConstants.STATUS_BIP_VERIFICATION_FAILED;
                }
                responseData.setOrgCheckStatus(orgEnrollStatus);
            }

            String payveRespCd = null;
            if (responseType != null && responseType.getStatus() != null){
            	payveRespCd = responseType.getStatus().getRespCd();
            }

            if (payveRespCd != null && payveRespCd.equals(ApiConstants.PAYVE_SUCCESS_RESP_CD)) {// Success response from UpdateOrgInfo SOAP API
                map.put(ApiConstants.RESP_PAYVE_PYMT_ENTITY_ID, responseType.getUpdateOrganizationInfoRespGrp().getPaymentEntityId());
                map.put(ApiConstants.ERROR_RESPONSE_FLAG, ApiConstants.FALSE);
            } else {// Failure response from UpdateOrgInfo SOAP API
                map.put(ApiConstants.ERROR_RESPONSE_FLAG, ApiConstants.TRUE);
            }
            response.setData(responseData);
            map.put(ApiConstants.RESPONSE_DETAILS, response);
        } catch (Exception ex) {
            logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrganizationInfoRequestHelper: buildResponse",
                "Exception occured during updateOrganizationInfo service call", AmexLogger.Result.failure, ex.getMessage(), ex);
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
            throw new SSEApplicationException("Exception occured during updateOrganizationInfo service call", ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),ex);
        }
        return map;
    }
}
