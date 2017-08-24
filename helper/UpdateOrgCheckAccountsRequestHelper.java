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
import com.americanexpress.payve.handler.ext.UpdateOrgAccountsServiceExt;
import com.americanexpress.payve.organizationaccountservice.v1.IOrganizationAccountService;
import com.americanexpress.payve.organizationaccountservice.v1.UpdateOrganizationAccountsFaultMsg;
import com.americanexpress.payve.organizationaccountservice.v1.updateorganizationaccounts.FaultType;
import com.americanexpress.payve.organizationaccountservice.v1.updateorganizationaccounts.RequestType;
import com.americanexpress.payve.organizationaccountservice.v1.updateorganizationaccounts.ResponseType;
import com.americanexpress.payve.organizationaccountservice.v1.updateorganizationaccounts.UpdateOrganizationAccountsType;
import com.americanexpress.smartserviceengine.client.UpdateOrgCheckAccountsServiceClient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.CheckAccountDetailsType;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.SOAPOperationsLoggingHandler;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.ValidateEnrollmentRespVO;

/**
 *
 */
@Service
public class UpdateOrgCheckAccountsRequestHelper {

    private static AmexLogger logger = AmexLogger.create(UpdateOrgCheckAccountsRequestHelper.class);
    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    /**
     * This method invokes the service call and populates the success response
     * from service to response beans.
     *
     * @param chkAccDetail
     * @param partnerOrgid
     * @param partnerEntityId
     * @param paymentEntityId
     * @param requestId
     * @param apiMsgId
     * @return
     * @throws GeneralSecurityException
     * @throws SSEApplicationException
     */
    public Map<String, Object> updateOrganizationCheckAccounts(CheckAccountDetailsType chkAccDetail, String partnerOrgid, String partnerEntityId, ValidateEnrollmentRespVO validateEnrollmentRespVO,
    		String requestId,String enrollActionType, String apiMsgId) throws GeneralSecurityException, SSEApplicationException {
        Map<String, Object> map = new HashMap<String, Object>();
        logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrgCheckAccountsRequestHelper: updateOrganizationCheckAccounts: starts",
            "Before invoking updateOrganizationAccounts service", AmexLogger.Result.success, "");
        map = callUpdateOrgCheckAccountsService(chkAccDetail, partnerOrgid, partnerEntityId, validateEnrollmentRespVO, requestId, enrollActionType, apiMsgId);// Calling the updateOrganizationAccounts service
        logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrgCheckAccountsRequestHelper: updateOrganizationCheckAccounts: ends",
            "After invoking updateOrganizationAccounts service",AmexLogger.Result.success, "");
        return map;
    }

    /**
     * This method creates request object, calls the service.
     *
     * @param requestWrapper
     * @return
     * @throws GeneralSecurityException
     * @throws SSEApplicationException
     */
    private Map<String, Object> callUpdateOrgCheckAccountsService(CheckAccountDetailsType chkAccDetail, String partnerOrgid,
        String partnerEntityId, ValidateEnrollmentRespVO validateEnrollmentRespVO, String requestId,
        String enrollActionType, String apiMsgId) throws GeneralSecurityException, SSEApplicationException {
        logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrgCheckAccountsRequestHelper: callUpdateOrgCheckAccountsService: starts",
            "Before invoking UpdateOrganiztionInfo service", AmexLogger.Result.success, "");
        Map<String, Object> map = new HashMap<String, Object>();
        final long startTimeStamp = System.currentTimeMillis();
        boolean isError = false;
        try {
            UpdateOrgAccountsServiceExt updateOrgAccountsService = new UpdateOrgAccountsServiceExt();
            IOrganizationAccountService proxy = updateOrgAccountsService.getOrganizationAccountServicePort();
            BindingProvider bindingProvider = (BindingProvider) proxy;
            bindingProvider.getBinding().getHandlerChain().add(new SOAPOperationsLoggingHandler());
            Map<String, Object> requestContext = bindingProvider.getRequestContext();
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_ACC_SOAP_SERVICE_URL));
            requestContext.put(ApiConstants.REQUEST_TIMEOUT, EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REQUEST_TIMEOUT_VALUE));
            logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrgCheckAccountsRequestHelper: callUpdateOrgCheckAccountsService",
                "Calling updateOrganizationAccounts BIP service",AmexLogger.Result.success, "BIP SOAP Endpoint URL",EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_ACC_SOAP_SERVICE_URL));
            UpdateOrgCheckAccountsServiceClient client = new UpdateOrgCheckAccountsServiceClient();
            ResponseType response = client.buildResponseType();
            final Holder<ResponseType> responseHolder = new Holder<ResponseType>(response);
            RequestType reqType = client.buildRequestType(chkAccDetail, partnerOrgid, partnerEntityId, validateEnrollmentRespVO, enrollActionType, requestId);
            UpdateOrganizationAccountsType updateOrgAccountsType = client.buildUpdateOrganizationAccountsType(reqType);
            final Holder<UpdateOrganizationAccountsType> requestHolder = new Holder<UpdateOrganizationAccountsType>(updateOrgAccountsType);
            proxy.updateOrganizationAccounts(reqType, requestHolder,responseHolder);// Call the updateOrganizationAccounts soap service
            ResponseType respType = responseHolder.value;
            if (null == respType) {
                logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrgCheckAccountsRequestHelper: updateOrganizationCheckAccounts",
                    "After invoking updateOrganizationAccounts service",AmexLogger.Result.failure, "Null response from updateOrganizationAccounts API", "");
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.UPD_ORG_ACCNT_ERR_CD, TivoliMonitoring.UPD_ORG_ACCNT_ERR_MSG, apiMsgId));
                throw new SSEApplicationException("Null response from updateOrganizationAccounts API",ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                    EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD));
            }
            logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrgCheckAccountsRequestHelper: callUpdateOrgCheckAccountsService",
                "After calling updateOrganizationAccounts BIP service",AmexLogger.Result.success, "", "responseCode",responseHolder.value.getStatus().getRespCd());
            map.put(ApiConstants.RESPONSE_DETAILS, respType);

        } catch (UpdateOrganizationAccountsFaultMsg ex) {
            isError = true;
            FaultType faultType = ex.getFaultInfo().getFault();
            String faultDetail = faultType.getFaultDetail();
            String faultCode = faultType.getFaultCode();
            String sseRespCode = null;
            String sseRespDesc = null;
            if (faultCode != null) {// Error response from AddOrgAccounts SOAP API
                sseRespCode = EnvironmentPropertiesUtil.getProperty(faultCode);//Get the corresponding SSE response code and response desc from Properties files
                if (sseRespCode != null) {
                    sseRespDesc = EnvironmentPropertiesUtil.getProperty(sseRespCode);
                } else {
                    if (faultDetail.indexOf(" ") > 0) {//Get the response description from FaultDetail element in soap fault
                        faultDetail = faultDetail.substring(faultDetail.indexOf(" ") + 1);
                    }
                    sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD;
                    sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD)+ ": " + faultDetail;
                    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.UPD_ORG_ACCNT_ERR_CD, TivoliMonitoring.UPD_ORG_ACCNT_ERR_MSG, apiMsgId));
                }
            } else {
                sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD;
                sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD);
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.UPD_ORG_ACCNT_ERR_CD, TivoliMonitoring.UPD_ORG_ACCNT_ERR_MSG, apiMsgId));
            }
            logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API","UpdateOrgCheckAccountsRequestHelper: callUpdateOrgCheckAccountsService",
                "SOAP Fault Error occured during updateOrganizationAccounts service call",AmexLogger.Result.failure, faultDetail, ex, "fault-Actor",
                faultType.getFaultActor(), "fault-Code", faultCode,"fault-String", faultType.getFaultString(), "fault-Detail",
                faultDetail, "SSEResponseCode", sseRespCode, "SSEResponseDesc", sseRespDesc);
            throw new SSEApplicationException("SOAP Fault Error occured during updateOrganizationAccounts service call",sseRespCode, sseRespDesc, ex);
        } catch (SSEApplicationException ex) {
            isError = true;
            throw ex;
        }  catch (WebServiceException exception) {
        	isError = true;
        	
        	logger.error(apiMsgId, "SmartServiceEngine", "Manage Enrollment API", "UpdateOrgCheckAccountsRequestHelper: callUpdateOrgCheckAccountsService",
        			"Exception occured during updateOrganizationAccounts service call", AmexLogger.Result.failure, exception.getMessage(), exception);
        	if(exception.getMessage().contains("java.net.SocketTimeoutException")){
        		logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_TIMEOUT_ERR_CD, TivoliMonitoring.BIP_TIMEOUT_ERR_MSG, apiMsgId));
        		throw new SSEApplicationException("Exception occured during updateOrganizationAccounts service call", ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901,
        				EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901),exception);
        	}else{
        		logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.UPD_ORG_ACCNT_ERR_CD, TivoliMonitoring.UPD_ORG_ACCNT_ERR_MSG, apiMsgId));
        		throw new SSEApplicationException("Exception occured during updateOrganizationAccounts service call", ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
        				EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),exception);
        	}
		}catch (Exception ex) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.UPD_ORG_ACCNT_ERR_CD, TivoliMonitoring.UPD_ORG_ACCNT_ERR_MSG, apiMsgId));
            isError = true;
            logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "UpdateOrgCheckAccountsRequestHelper: callUpdateOrgCheckAccountsService",
                "Exception occured during updateOrganizationAccounts service call", AmexLogger.Result.failure, ex.getMessage(), ex);
            throw new SSEApplicationException("Exception occured during updateOrganizationAccounts service call",ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD), ex);
        }
        finally {
            final long endTimeStamp = System.currentTimeMillis();
            logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "UpdateOrgCheckAccountsRequestHelper: callUpdateOrgCheckAccountsService: ends",
                "After invoking updateOrganizationAccounts service", AmexLogger.Result.success, "", "Total Time Taken to get response from updateOrganizationAccounts SOAP service",
                (endTimeStamp-startTimeStamp)+ " milliseconds("+(endTimeStamp-startTimeStamp)/1000.00+" seconds)");
            ThreadLocalManager.getRequestStatistics().put("UpdateOrganizationAccountsService", (isError?"F:":"S:")+(endTimeStamp-startTimeStamp) +" ms");
        }
        return map;
    }
}
