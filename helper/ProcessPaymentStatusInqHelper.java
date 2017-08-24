package com.americanexpress.smartserviceengine.helper;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.emm.handler.SOAPOperationsLoggingHandler;
import com.americanexpress.emm.handler.ext.GetPaymentDetailsServiceExt;
import com.americanexpress.emm.remittanceaudittrailservice.v1.GetPaymentDetailFaultMsg;
import com.americanexpress.emm.remittanceaudittrailservice.v1.IRemittanceAuditTrailService;
import com.americanexpress.emm.remittanceaudittrailservice.v1.getpaymentdetail.ErrGrpType;
import com.americanexpress.emm.remittanceaudittrailservice.v1.getpaymentdetail.FailGrpType;
import com.americanexpress.emm.remittanceaudittrailservice.v1.getpaymentdetail.FaultType;
import com.americanexpress.emm.remittanceaudittrailservice.v1.getpaymentdetail.GetPaymentDetailType;
import com.americanexpress.emm.remittanceaudittrailservice.v1.getpaymentdetail.RequestType;
import com.americanexpress.emm.remittanceaudittrailservice.v1.getpaymentdetail.ResponseType;
import com.americanexpress.emm.remittanceaudittrailservice.v1.getpaymentdetail.StatusType;
import com.americanexpress.emm.serviceheader.v4.TrackingHdrType;
import com.americanexpress.smartserviceengine.client.ProcessPaymentDetailsServiceClient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.ActualPymtVO;

public class ProcessPaymentStatusInqHelper {

    private static AmexLogger logger = AmexLogger.create(ProcessPaymentStatusInqHelper.class);

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    @Autowired
    private GetPaymentDetailsServiceExt getPaymentDetailsExt;

    public Map<String, Object> getPaymentDetail(Object requestWrapper, String eventId) throws GeneralSecurityException,
    SSEApplicationException {

        logger.debug(eventId, "SmartServiceEngine", "ProcessPaymentStatusInqHelper",
            "ProcessPaymentStatusInqHelper: getPaymentDetail: starts", "Before invoking getPaymentDetail service",
            AmexLogger.Result.success, "");

        Map<String, Object> map = new HashMap<String, Object>();

        ResponseType responseType = null;
        // Calling the GetOrganizationInfo service
        map = callGetPaymentDetailsService(requestWrapper, eventId);

        responseType = (ResponseType) map.get(SchedulerConstants.RESPONSE_DETAILS);

        logger.debug(eventId, "SmartServiceEngine", "ProcessPaymentStatusInqHelper",
            "ProcessPaymentStatusInqHelper: getPaymentDetail: ends", "After invoking getPaymentDetail service",
            AmexLogger.Result.success, "", "responseCode", responseType.getStatus().getRespCd());

        return map;

    }

    private Map<String, Object> callGetPaymentDetailsService(Object pymtVO, String apiMsgId)
            throws GeneralSecurityException, SSEApplicationException {

    	 if(pymtVO !=null && pymtVO instanceof  ActualPymtVO){
    		 ActualPymtVO objActualPymtVO = (ActualPymtVO)pymtVO;
    		 if (!("L").equals(objActualPymtVO.getPartnerIdIndicator())){
    		    	logger.info(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
    		                "ProcessPaymentStatusInqHelper: callGetPaymentDetailsService: starts",
    		                "Before invoking getPaymentDetail service", AmexLogger.Result.success, "", "intUPID", objActualPymtVO.getEmmUPID(), "paymentTransactionType", objActualPymtVO.getPymtType(),
    		                "partnerIDIndicator", objActualPymtVO.getPartnerIdIndicator());
    		    	}else{
    		        logger.debug(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
    		            "ProcessPaymentStatusInqHelper: callGetPaymentDetailsService: starts",
    		            "Before invoking getPaymentDetail service", AmexLogger.Result.success, "");
    		    	}
    	 }else{
    		 logger.info(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
 		            "ProcessPaymentStatusInqHelper: callGetPaymentDetailsService: starts",
 		            "Before invoking getPaymentDetail service", AmexLogger.Result.success, "");
    	 }


        Map<String, Object> map = new HashMap<String, Object>();
        final long startTimeStamp = System.currentTimeMillis();

        try {

            IRemittanceAuditTrailService proxy = getPaymentDetailsExt.getRemittanceAuditTrailServicePort();

            BindingProvider bindingProvider = (BindingProvider) proxy;

            String esbServerName = null;
            if(pymtVO !=null && pymtVO instanceof  ActualPymtVO){
            	logger.info("Payment Ind: "+ ((ActualPymtVO)pymtVO).getPartnerIdIndicator());
                if(("L").equals(((ActualPymtVO)pymtVO).getPartnerIdIndicator())){
                	esbServerName = EnvironmentPropertiesUtil.getProperty(SchedulerConstants.EMM_PYMT_ESB_SERVER_LIBERTY_NAME);
                }else {
                	esbServerName = EnvironmentPropertiesUtil.getProperty(SchedulerConstants.EMM_PYMT_ESB_SERVER_NAME);
                }
            }else {
            	esbServerName = EnvironmentPropertiesUtil.getProperty(SchedulerConstants.EMM_PYMT_ESB_SERVER_NAME);
            }

            logger.info("esbServerName: " +esbServerName);
            ThreadLocalManager.setEmmEsbServerName(esbServerName);

            SOAPOperationsLoggingHandler soapOperationsLoggingHandler = new SOAPOperationsLoggingHandler();
            soapOperationsLoggingHandler.setEventId(apiMsgId);

            bindingProvider.getBinding().getHandlerChain().add(soapOperationsLoggingHandler);

            Map<String, Object> requestContext = bindingProvider.getRequestContext();
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,EnvironmentPropertiesUtil.getProperty(SchedulerConstants.EMM_PAY_STATUS_SERVICE_URL));
            requestContext.put(ApiConstants.REQUEST_TIMEOUT, EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_REQUEST_TIMEOUT_VALUE));

            requestContext.put("PMT_STATUS_API_MSGID", apiMsgId);
            logger.debug(apiMsgId, "SmartServiceEngine", "ProcessPaymentStatusInqHelper",
                "ProcessPaymentStatusInqHelper: callGetPaymentDetailsService", "Calling getPaymentDetail EMM service",
                AmexLogger.Result.success, "", "EMM SOAP Endpoint URL",
                EnvironmentPropertiesUtil.getProperty(SchedulerConstants.EMM_PAY_STATUS_SERVICE_URL));

            ProcessPaymentDetailsServiceClient client = new ProcessPaymentDetailsServiceClient();

            ResponseType responseType = new ResponseType();

            StatusType staType = client.buildStatusType();

            ResponseType responseHolder = client.buildResponse(responseType, staType);

            final Holder<ResponseType> response = new Holder<ResponseType>(responseHolder);

            RequestType request = client.buildRequestType(pymtVO, apiMsgId);

            GetPaymentDetailType getPymtDetail = client.buildGetPaymentDetails(pymtVO, apiMsgId);
            final Holder<GetPaymentDetailType> getPaymentDetail = new Holder<GetPaymentDetailType>(getPymtDetail);

            TrackingHdrType trackingHdr = null;
            if(pymtVO !=null && pymtVO instanceof  ActualPymtVO){
            	logger.info("Payment Ind: "+ ((ActualPymtVO)pymtVO).getPartnerIdIndicator());
                if(("L").equals(((ActualPymtVO)pymtVO).getPartnerIdIndicator())){
                	trackingHdr = client.buildLibertyTrackingHdr();
                }
                else {
                	trackingHdr = client.buildTrackingHdr();
                }
            }else {
            	trackingHdr = client.buildTrackingHdr();
			}

            final Holder<TrackingHdrType> trackingHdrHolder = new Holder<TrackingHdrType>(trackingHdr);

            proxy.getPaymentDetail(request, getPaymentDetail, response);

            ResponseType respType = response.value;

            handleResponse(pymtVO, apiMsgId, respType);

            /*logger.debug(apiMsgId, "SmartServiceEngine", "ProcessPaymentStatusInqHelper",
                "ProcessPaymentStatusInqHelper: callGetPaymentDetailsService",
                "After invoking getPaymentDetails EMM service", AmexLogger.Result.success, "",
                "intUPID", objActualPymtVO.getEmmUPID(), "paymentTransactionType", objActualPymtVO.getPymtType(),
                "partnerIDIndicator", objActualPymtVO.getPartnerIdIndicator(), "Response Code: ",
                staType.getRespCd());*/

            map.put(SchedulerConstants.RESPONSE_DETAILS, respType);

        }catch (WebServiceException exception) {
            
            logger.error(apiMsgId,"SmartServiceEngine","ACH Payment Status Inquiry ","ProcessPaymentStatusInqHelper: callGetPaymentDetailsService",
                            "Exception occured during EMM getPaymentDetail SOAP call",AmexLogger.Result.failure, exception.getMessage(), exception);
            if (exception.getMessage().contains("java.net.SocketTimeoutException")) {
            		logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.EMM_TIMEOUT_ERR_CD, TivoliMonitoring.EMM_TIMEOUT_ERR_MSG, apiMsgId));
                    throw new SSEApplicationException("Exception occured during EMM getPaymentDetail SOAP call",ApiErrorConstants.EMM_TIMEOUT_SSEAPIEN902,
                                    EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.EMM_TIMEOUT_SSEAPIEN902),exception);
            } else {
            		logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_STATUS_ERR_CD, TivoliMonitoring.GET_PYMT_STATUS_ERR_MSG, apiMsgId));
                    throw new SSEApplicationException("Exception occured during EMM getPaymentDetail SOAP call",
                                    ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),exception);
            }
    } catch (GetPaymentDetailFaultMsg ex) {

            handleFaultResponse(apiMsgId, ex);

        } catch (SSEApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_STATUS_ERR_CD,
                TivoliMonitoring.GET_PYMT_STATUS_ERR_MSG, apiMsgId));
            logger.error(apiMsgId, "SmartServiceEngine", "ProcessPaymentStatusInqHelper",
                "ProcessPaymentStatusInqHelper: callGetPaymentDetailsService",
                "Exception occured during EMM getPaymentDetail SOAP call", AmexLogger.Result.failure, ex.getMessage(),
                ex);

            throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD));
        } finally {
            final long endTimeStamp = System.currentTimeMillis();

            logger.info(apiMsgId, "SmartServiceEngine", "ProcessPaymentStatusInqHelper",
                "ProcessPaymentStatusInqHelper: callGetPaymentDetailsService",
                "After calling Get Payment Details EMM service", AmexLogger.Result.success, "",
                "total_Time_Taken_By_getPaymentDetail_service", (endTimeStamp - startTimeStamp)
                + " milliseconds(" + (endTimeStamp - startTimeStamp) / 1000.00 + " seconds)");
        }
        return map;

    }

    private void handleResponse(Object pymtVO, String apiMsgId, ResponseType respType) throws SSEApplicationException {
        String errorCd = null;
        String errorMsg = null;

        if (respType == null) {
            logger.error(apiMsgId, "SmartServiceEngine", "ProcessPaymentStatusInqHelper",
                "ProcessPaymentStatusInqHelper: callGetPaymentDetailsService",
                "After invoking RemittanceAuditTrail EMM service", AmexLogger.Result.failure, "",
                "Null response from getPaymentDetail Service", "");
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_STATUS_ERR_CD,
                TivoliMonitoring.GET_PYMT_STATUS_ERR_MSG, apiMsgId));

            throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD));

        } else if (respType.getStatus() != null) {

            StatusType statusType = respType.getStatus();
            if (null != statusType.getRespCd()
                    && SchedulerConstants.FAIL.equalsIgnoreCase(statusType.getRespCd())) {
                List<FailGrpType> failGrpList = respType.getGetPaymentDetailRespGrp().getFailGrp();

                if (null != failGrpList && !failGrpList.isEmpty()) {
                    for (FailGrpType failGrpType : failGrpList) {
                        List<ErrGrpType> errGrpList = failGrpType.getErrGrp();
                        ErrGrpType errGrpType = errGrpList.get(0);
                        errorCd = errGrpType.getErrCd();
                        errorMsg = errGrpType.getErrMsg();
                        break;
                    }
                }
            }
        }

        if (null != errorCd && !StringUtils.EMPTY.equalsIgnoreCase(errorCd)) {

            /*String sseRespCode = EnvironmentPropertiesUtil.getProperty(errorCd);
            String sseRespDesc = null;
            if (sseRespCode != null) {
                sseRespDesc = EnvironmentPropertiesUtil.getProperty(sseRespCode);
            }*/

            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_STATUS_ERR_CD,
                TivoliMonitoring.GET_PYMT_STATUS_ERR_MSG, apiMsgId));
            throw new SSEApplicationException("Got Failure Response from Remittance Audit Trial SOAP service call", errorCd, errorMsg);
        }
    }

    private void handleFaultResponse(String apiMsgId, GetPaymentDetailFaultMsg ex) throws SSEApplicationException {
        FaultType faultType = ex.getFaultInfo().getFault();

        String faultDetail = null;
        String faultCode = null;

        if (faultType != null) {
            faultDetail = faultType.getFaultDetail();
            faultCode = faultType.getFaultCode();

            if (faultCode != null) {
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_STATUS_ERR_CD,
                    TivoliMonitoring.GET_PYMT_STATUS_ERR_MSG, apiMsgId));
            } else {
                faultCode = SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD;
                faultDetail = EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD);
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_STATUS_ERR_CD,
                    TivoliMonitoring.GET_PYMT_STATUS_ERR_MSG, apiMsgId));
            }
        } else {
            faultCode = SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD;
            faultDetail = EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD);
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_STATUS_ERR_CD,
                TivoliMonitoring.GET_PYMT_STATUS_ERR_MSG, apiMsgId));
        }

        logger.error(apiMsgId, "SmartServiceEngine", "ProcessPaymentStatusInqHelper",
            "ProcessPaymentStatusInqHelper: callGetPaymentDetailsService",
            "SOAP Fault Error occured during EMM RemittanceAuditDetails SOAP service call",
            AmexLogger.Result.failure, faultDetail, ex, "fault-Actor", faultType.getFaultActor(), "fault-Code",
            faultCode, "fault-String", faultType.getFaultString(), "fault-Detail", faultDetail,
            "EMMResponseCode", faultCode, "EMMResponseDesc", faultDetail);
        throw new SSEApplicationException(
            "SOAP Fault Error occured during ACH Payment getPaymentDetail SOAP service call", faultCode,
            faultDetail, ex);
    }

}
