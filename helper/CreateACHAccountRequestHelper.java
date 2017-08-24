package com.americanexpress.smartserviceengine.helper;

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.emm.handler.ext.PaymentRelationshipServiceExt;
import com.americanexpress.smartserviceengine.client.CreateACHAccountServiceClient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentRequestData;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.SOAPOperationsLoggingHandler;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.ValidateEnrollmentRespVO;
import com.americanexpress.soaui.am.commonresponse.v2.RespGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.ResponseType;
import com.americanexpress.soaui.am.commonresponse.v2.StatusType;
import com.americanexpress.soaui.am.paymentrelationshipservice.v2.CreatePaymentRelationshipFaultMsg;
import com.americanexpress.soaui.am.paymentrelationshipservice.v2.IPaymentRelationshipService;
import com.americanexpress.soaui.am.paymentrelationshipservice.v4.createpaymentrelationship.FaultType;
import com.americanexpress.soaui.am.paymentrelationshipservice.v4.createpaymentrelationship.RequestType;
import com.americanexpress.soaui.serviceheader.v4.TrackingHdrType;

@Service
public class CreateACHAccountRequestHelper {

    private static AmexLogger logger = AmexLogger.create(CreateACHAccountRequestHelper.class);

    @Autowired
    private TivoliMonitoring tivoliMonitoring;


    public Map<String, Object> createEnrollEMMCall(ManageEnrollmentRequestData requestData, ValidateEnrollmentRespVO validateEnrollmentRespVO,
        boolean isCustomer, String apiMsgId) throws SSEApplicationException{
        logger.info(apiMsgId, "SmartServiceEngine", "Manage Enrollment API","Create SOAP Service Request - Start",
            "Create SOAP Service Request for Add Account EMM Call", AmexLogger.Result.success, "","partnerId", requestData.getCommonRequestContext().getPartnerId(),
			"organizationId", SplunkLogger.organizationId(requestData), "associatedOrgId", SplunkLogger.associatedOrgId(requestData),
			"SSEAccountNumber", validateEnrollmentRespVO.getSseAccountNumber());
        Map<String, Object> map = new HashMap<String, Object>();
        final long startTimeStamp = System.currentTimeMillis();
        boolean isError = false;
        try {
            PaymentRelationshipServiceExt paymentRelationshipServiceExt = new PaymentRelationshipServiceExt();
            IPaymentRelationshipService  proxy= paymentRelationshipServiceExt.getPaymentRelationshipServicePort();
            BindingProvider bindingProvider = (BindingProvider) proxy;
            bindingProvider.getBinding().getHandlerChain().add(new SOAPOperationsLoggingHandler());

            Map<String, Object> requestContext = bindingProvider.getRequestContext();
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_PAYMENT_SOAP_SERVICE_URL));
            requestContext.put(ApiConstants.REQUEST_TIMEOUT, EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_REQUEST_TIMEOUT_VALUE));
            logger.debug(apiMsgId,"SmartServiceEngine","Manage Enrollment API","CreateACHAccountRequestHelper: createEnrollEMMCall",
                "",AmexLogger.Result.success, "", "organizationId", SplunkLogger.organizationId(requestData),
				"associatedOrgId", SplunkLogger.associatedOrgId(requestData), "SSEAccountNumber", validateEnrollmentRespVO.getSseAccountNumber(),
				"EMM SOAP Endpoint URL",EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_PAYMENT_SOAP_SERVICE_URL));

            CreateACHAccountServiceClient client = new CreateACHAccountServiceClient();
            RespGrpType response = client.buildResponseGroup();
            final Holder<RespGrpType> responseHolder = new Holder<RespGrpType>(response);

            StatusType statusType = client.buildStatusType();
            final Holder<StatusType> statusHolder = new Holder<StatusType>(statusType);

            TrackingHdrType trackingHdrType = client.buildTrackingHdr();
            final Holder<TrackingHdrType> trackingHdr = new Holder<TrackingHdrType>(trackingHdrType);

            RequestType request = client.buildRequestType(requestData, validateEnrollmentRespVO, isCustomer, apiMsgId);
            logger.debug(apiMsgId,"SmartServiceEngine","Manage Enrollment API","Invoke SOAP Service Request",
                "Invoke Add Account EMM Service",AmexLogger.Result.success, "", "organizationId", SplunkLogger.organizationId(requestData),
				"associatedOrgId", SplunkLogger.associatedOrgId(requestData), "SSEAccountNumber", validateEnrollmentRespVO.getSseAccountNumber());
            proxy.createPaymentRelationship(request, statusHolder, responseHolder, trackingHdr);
            logger.debug(apiMsgId,"SmartServiceEngine","Manage Enrollment API","SOAP Service Request Completed",
                "",AmexLogger.Result.success, "","organizationId", SplunkLogger.organizationId(requestData),"associatedOrgId", SplunkLogger.associatedOrgId(requestData),
				"SSEAccountNumber", validateEnrollmentRespVO.getSseAccountNumber());
            RespGrpType responseGrpType = responseHolder.value;
            StatusType staType = statusHolder.value;

            if(staType == null || responseGrpType == null){
                logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API",
                    "CreateACHAccountRequestHelper: createEnrollEMMCall","After invoking createACHAccount service",
                    AmexLogger.Result.failure,"", "Null response from createACHAccount Service", "");
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD, TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG, apiMsgId));
                throw new SSEApplicationException("Null response from Create Payment Relationship SOAP service",ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                    EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD));
            }else{
                ResponseType respType = client.buildResponse(responseGrpType, staType);
                logger.debug(apiMsgId,"SmartServiceEngine","Manage Enrollment API","CreateACHAccountRequestHelper: createEnrollEMMCall",
                    "After invoking createACHAccount service",AmexLogger.Result.success, "", "responseCode: ",staType.getRespCd());
                map.put(ApiConstants.RESPONSE_DETAILS, respType);
            }
        } catch (CreatePaymentRelationshipFaultMsg exception) {
            isError = true;
            FaultType faultType = exception.getFaultInfo().getFault();
            String sseRespCode = null;
            String sseRespDesc = null;
            String faultDetail = null;
			String faultCode = null;
			if (faultType != null) {
				faultDetail = faultType.getFaultDetail();
				faultCode = faultType.getFaultCode();
				if (StringUtils.isNotBlank(faultCode)) {
					sseRespCode = EnvironmentPropertiesUtil.getProperty(faultCode);//Get the corresponding SSE response code and response desc from Properties files
					if (sseRespCode != null) {
						sseRespDesc = EnvironmentPropertiesUtil.getProperty(sseRespCode);
					} else {
						if (faultDetail.indexOf(" ") > 0) {// Get the response description from FaultDetail element in soap fault
							faultDetail = faultDetail.substring(faultDetail.indexOf(" ") + 1);
						}
						sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD;
						sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD)+ ": " + faultDetail;
						logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD, TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG, apiMsgId));
					}
				} else {
					sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD;
					sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD);
					logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD, TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG, apiMsgId));
				}
			} else {
                sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD;
                sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD);
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD, TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG, apiMsgId));
            }
            logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "CreateACHAccountRequestHelper: createEnrollEMMCall",
                "SOAP Fault Error occured during Create Payment Relationship SOAP service call", AmexLogger.Result.failure, faultDetail, exception, "fault-Actor",
                faultType.getFaultActor(), "fault-Code", faultCode, "fault-String", faultType.getFaultString(), "fault-Detail",
                faultDetail, "SSEResponseCode", sseRespCode, "SSEResponseDesc", sseRespDesc);
            throw new SSEApplicationException("SOAP Fault Error occured during Create Payment Relationship SOAP service call", sseRespCode, sseRespDesc, exception);
        } catch (SSEApplicationException ex) {
            isError = true;
            throw ex;
		} catch (WebServiceException exception) {
			isError = true;
			
			logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "CreateACHAccountRequestHelper: createEnrollEMMCall",
					"Exception occured during ACH Create Payment Relationship SOAP service",AmexLogger.Result.failure, exception.getMessage(), exception);
			if (exception.getMessage().contains("java.net.SocketTimeoutException")) {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.EMM_TIMEOUT_ERR_CD, TivoliMonitoring.EMM_TIMEOUT_ERR_MSG, apiMsgId));
				throw new SSEApplicationException("Exception occured during Create Payment Relationship service call",ApiErrorConstants.EMM_TIMEOUT_SSEAPIEN902,
						EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.EMM_TIMEOUT_SSEAPIEN902),exception);
			} else {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD, TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG, apiMsgId));
				throw new SSEApplicationException("Exception occured during Create Payment Relationship service call",
						ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),exception);
			}
		}catch(Exception e){
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD, TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG, apiMsgId));
            isError = true;
            logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API", "CreateACHAccountRequestHelper: createEnrollEMMCall",
                "Exception occured during ACH Create Payment Relationship SOAP service", AmexLogger.Result.failure, e.getMessage(), e);
            throw new SSEApplicationException("Exception occured during Create Payment Relationship service call", ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),e);
        }finally {
            final long endTimeStamp = System.currentTimeMillis();
            ThreadLocalManager.getRequestStatistics().put("ACH Create Payment Relationship service", (isError?"F:":"S:")+(endTimeStamp-startTimeStamp) +" ms");
            logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API",
                "CreateACHAccountRequestHelper: createEnrollEMMCall", "After calling Create Payment Relationship EMM SOAP",
                AmexLogger.Result.success, "", "ACH_CreatePaymentTimeTaken",
                (endTimeStamp-startTimeStamp)+ " milliseconds("+(endTimeStamp-startTimeStamp)/1000.00+" seconds)");
        }
        logger.info(apiMsgId, "SmartServiceEngine", "Manage Enrollment API","Create SOAP Service Request - End",
                "Create SOAP Service Request for Add Account EMM Call", AmexLogger.Result.success, "","partnerId", requestData.getCommonRequestContext().getPartnerId());
        return map;
    }
}
