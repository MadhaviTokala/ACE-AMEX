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
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.emm.handler.ext.PaymentRelationshipInquiryServiceExt;
import com.americanexpress.smartserviceengine.client.AchPymtInquiryServiceClient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.SOAPOperationsLoggingHandler;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.ValidateAchPymtRespVO;
import com.americanexpress.smartserviceengine.common.vo.ValidateEnrollmentRespVO;
import com.americanexpress.soaui.am.commonresponse.v2.EnrollDetGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.EnrollGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.ErrGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.FIAcctDtlGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.FailGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.PayArngGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.PaymentType;
import com.americanexpress.soaui.am.commonresponse.v2.PymtHstGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.RespGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.ResponseType;
import com.americanexpress.soaui.am.commonresponse.v2.ServRegisGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.StatusType;
import com.americanexpress.soaui.am.commonresponse.v2.SucGrpType;
import com.americanexpress.soaui.am.paymentrelationshipinquiryservice.v4.GetInquiryFaultMsg;
import com.americanexpress.soaui.am.paymentrelationshipinquiryservice.v4.IPaymentRelationshipInquiryService;
import com.americanexpress.soaui.am.paymentrelationshipinquiryservice.v4.getinquiry.FaultType;
import com.americanexpress.soaui.am.paymentrelationshipinquiryservice.v4.getinquiry.RequestType;
import com.americanexpress.soaui.serviceheader.v4.TrackingHdrType;

@Service
public class ProcessAchPymtInqHelper {

    private static AmexLogger logger = AmexLogger.create(ProcessAchPymtInqHelper.class);

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    public Map<String, Object> getInquiry(String requestId, ValidateAchPymtRespVO validateAchPymtRespVO,
        boolean isScheduler, String apiMsgId) throws GeneralSecurityException, SSEApplicationException {

        Map<String, Object> map = new HashMap<String, Object>();
        logger.debug(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
            "ProcessAchPymtInqHelper: getInquiry: starts", "inquiry ACH Payment Helper - Start",
            AmexLogger.Result.success, "");

        ResponseType responseType = null;
        map = callAchPaymentInquiry(requestId, validateAchPymtRespVO, apiMsgId, isScheduler);
        responseType = (ResponseType) map.get(ApiConstants.RESPONSE_DETAILS);
        // set the response data to Ach Payment Inquiry Response object
        map = buildInquiryResponse(responseType, validateAchPymtRespVO.getPayArngSetupCfmNbr(), apiMsgId);
        return map;
    }

    public Map<String, String> getInquiry(String requestId, ValidateEnrollmentRespVO validateEnrollmentRespVO,
        String apiMsgId) throws GeneralSecurityException, SSEApplicationException {

        Map<String, Object> map = new HashMap<String, Object>();
        logger.debug(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
            "ProcessAchPymtInqHelper: getInquiry: starts", "inquiry ACH Payment Helper - Start",
            AmexLogger.Result.success, "");

        ResponseType responseType = null;
        ValidateAchPymtRespVO validateAchPymtRespVO = new ValidateAchPymtRespVO();
        validateAchPymtRespVO.setCardAcctNbr(validateEnrollmentRespVO.getSseAccountNumber());
        validateAchPymtRespVO.setOrgName(validateEnrollmentRespVO.getOrgName());

        map = callAchPaymentInquiry(requestId, validateAchPymtRespVO, apiMsgId, false);
        responseType = (ResponseType) map.get(ApiConstants.RESPONSE_DETAILS);
        logger.debug(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
            "ProcessAchPymtInqHelper: getInquiry: starts", "inquiry ACH Payment Helper - End",
            AmexLogger.Result.success, "");
        return buildInquiryResponse(responseType, apiMsgId);
    }

    private Map<String, Object> callAchPaymentInquiry(String requestId, ValidateAchPymtRespVO validateAchPymtRespVO,
        String apiMsgId, boolean isScheduler) throws GeneralSecurityException, SSEApplicationException {
        logger.info(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
            "ProcessAchPymtInqHelper: callAchPaymentInquiry: starts", "Before invoking getInquiry service",
            AmexLogger.Result.success, "", "isScheduler", Boolean.toString(isScheduler), "emmAccountId",Integer.toString(validateAchPymtRespVO.getSrcEnrollId()),
            "cardAccountNumber",CommonUtils.maskAccRoutingNumber(validateAchPymtRespVO.getCardAcctNbr(),5,apiMsgId),
            "paymentRefrencenumber",validateAchPymtRespVO.getPayArngSetupCfmNbr(), "paymentTransactionType", validateAchPymtRespVO.getPaymentTransactionType());
        Map<String, Object> map = new HashMap<String, Object>();
        final long startTimeStamp = System.currentTimeMillis();
        try {

            PaymentRelationshipInquiryServiceExt pymtInquiryService = new PaymentRelationshipInquiryServiceExt();
            IPaymentRelationshipInquiryService proxy = pymtInquiryService.getPaymentRelationshipInquiryServicePort();

            BindingProvider bindingProvider = (BindingProvider) proxy;

            SOAPOperationsLoggingHandler soapOperationsLoggingHandler = new SOAPOperationsLoggingHandler();
            soapOperationsLoggingHandler.setEventId(apiMsgId);

            bindingProvider.getBinding().getHandlerChain().add(soapOperationsLoggingHandler);

            Map<String, Object> requestContext = bindingProvider.getRequestContext();
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_INQUIRY_SOAP_SERVICE_URL));
            requestContext.put(ApiConstants.REQUEST_TIMEOUT, EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_REQUEST_TIMEOUT_VALUE));
            logger.debug(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
                "ProcessAchPymtInqHelper: callAchPaymentInquiry", "Calling ACH Payment Inquiry EMM service",
                AmexLogger.Result.success, "", "EMM Inquiry SOAP Endpoint URL",
                EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_INQUIRY_SOAP_SERVICE_URL));

            AchPymtInquiryServiceClient client = new AchPymtInquiryServiceClient();

            RespGrpType response = client.buildResponseGroup();
            final Holder<RespGrpType> responseHolder = new Holder<RespGrpType>(response);

            TrackingHdrType trackingHdrType = null;
            logger.info("validateAchPymtRespVO.getPartnerIndicator(): " + validateAchPymtRespVO.getPartnerIndicator());
            // Liberty Specific
            if(("L").equals(validateAchPymtRespVO.getPartnerIndicator())){
            	trackingHdrType = client.buildTrackingHdrLiberty();
            	logger.info("Liberty Specific Record");
            }else{
            	trackingHdrType = client.buildTrackingHdr();
            }


            final Holder<TrackingHdrType> trackingHdr = new Holder<TrackingHdrType>(trackingHdrType);

            StatusType statusType = client.buildStatusType();
            final Holder<StatusType> statusHolder = new Holder<StatusType>(statusType);

            RequestType request = client.buildRequestType(requestId, validateAchPymtRespVO, apiMsgId);

            proxy.getInquiry(request, statusHolder, responseHolder, trackingHdr);

            RespGrpType responseGrpType = responseHolder.value;
            StatusType staType = statusHolder.value;

            String errorCd = null;
            String errorDesc = null;
            if (null == staType || null == responseGrpType) {
                logger.error(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
                    "ProcessAchPymtInqHelper: callAchPaymentInquiry", "After invoking getInquiry Payment service",
                    AmexLogger.Result.failure, "", "Null response from getInquiry Service", "");
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_CD,
                    TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_MSG, apiMsgId));

                throw new SSEApplicationException("Null response from ACH getInquiry Service",
                    ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY,
                    EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY));
            } else if (null != staType.getRespCd() && ApiConstants.FAIL.equalsIgnoreCase(staType.getRespCd())) {
                List<FailGrpType> failGrpList = responseGrpType.getFailGrp();
                if (null != failGrpList && !failGrpList.isEmpty()) {
                    for (FailGrpType failGrpType : failGrpList) {
                        if(null != failGrpType){
                            List<ErrGrpType> errGrpList = failGrpType.getErrGrp();
                            ErrGrpType errGrpType = errGrpList.get(0);
                            // Error response.
                            errorCd = errGrpType.getErrCd();
                            errorDesc = errGrpType.getErrMsgTxt();
                            break;
                        }
                    }
                }
            }

            if (null != errorCd && !StringUtils.EMPTY.equalsIgnoreCase(errorCd)) {

                // ** Get the corresponding SSE response code and response desc from Properties files */
                String sseRespCode = EnvironmentPropertiesUtil.getProperty(errorCd);
                String sseRespDesc = null;
                if (sseRespCode != null) {
                    sseRespDesc = EnvironmentPropertiesUtil.getProperty(sseRespCode);
                }else {
                    sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY;
                    sseRespDesc = EnvironmentPropertiesUtil
                            .getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY);
                    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_CD,
                        TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_MSG, apiMsgId));
                }
                if(isScheduler && sseRespCode==null){
                    throw new SSEApplicationException(
                        "Got Failure Response from ACH Payment createPaymentRelationship SOAP service call", errorCd,
                        errorDesc);
                }else{
                    throw new SSEApplicationException(
                        "Got Failure Response from ACH Payment createPaymentRelationship SOAP service call", sseRespCode,
                        sseRespDesc);
                }
            }

            ResponseType respType = client.buildResponse(responseGrpType, staType);

            logger.info(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
                "ProcessAchPymtInqHelper: callAchPaymentInquiry", "After invoking EMM getInquiry Payment service",
                AmexLogger.Result.success, "", "emmAccountId",Integer.toString(validateAchPymtRespVO.getSrcEnrollId()),
                "cardAccountNumber",CommonUtils.maskAccRoutingNumber(validateAchPymtRespVO.getCardAcctNbr(),5,apiMsgId),
                "paymentRefrencenumber",validateAchPymtRespVO.getPayArngSetupCfmNbr(), "paymentTransactionType", validateAchPymtRespVO.getPaymentTransactionType(), "Response Code: ", staType.getRespCd());

            map.put(ApiConstants.RESPONSE_DETAILS, respType);

        } catch (GetInquiryFaultMsg ex) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_CD, TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_MSG, apiMsgId));

            handleFaultResponse(apiMsgId, ex);

        } catch (SSEApplicationException ex) {
            throw ex;
        }  catch (WebServiceException exception) {
			
			logger.error(apiMsgId,"SmartServiceEngine","ACH Payment Inquiry API","ProcessAchPymtInqHelper: callAchPaymentInquiry",
					"Exception occured during Process ACH payment Inquiry service call",AmexLogger.Result.failure, exception.getMessage(), exception);
			if (exception.getMessage().contains("java.net.SocketTimeoutException")) {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.EMM_TIMEOUT_ERR_CD, TivoliMonitoring.EMM_TIMEOUT_ERR_MSG, apiMsgId));
				throw new SSEApplicationException("Exception occured during Process ACH payment Inquiry service call",ApiErrorConstants.EMM_TIMEOUT_SSEAPIEN902,
						EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.EMM_TIMEOUT_SSEAPIEN902),exception);
			} else {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_CD, TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_MSG, apiMsgId));
				throw new SSEApplicationException("Exception occured during Process ACH Payment Inquiry service call",
						ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),exception);
			}
		} catch (Exception ex) {
            handleException(apiMsgId, isScheduler, ex);
        } finally {
            final long endTimeStamp = System.currentTimeMillis();
            logger.info(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
                "ProcessAchPymtInqHelper: callAchPaymentInquiry", "After calling ACH Payment Inquiry EMM service",
                AmexLogger.Result.success, "", "emmAccountId",Integer.toString(validateAchPymtRespVO.getSrcEnrollId()),
                "cardAccountNumber",CommonUtils.maskAccRoutingNumber(validateAchPymtRespVO.getCardAcctNbr(),5,apiMsgId),
                "paymentRefrencenumber",validateAchPymtRespVO.getPayArngSetupCfmNbr(), "paymentTransactionType", validateAchPymtRespVO.getPaymentTransactionType(), "Total Time Taken to get response from getInquiry SOAP service",
                (endTimeStamp - startTimeStamp) + " milliseconds(" + (endTimeStamp - startTimeStamp) / 1000.00
                + " seconds)");
        }
        return map;
    }

    private void handleException(String apiMsgId, boolean isScheduler, Exception ex) throws SSEApplicationException {
        if (isScheduler) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD,
                TivoliMonitoring.SSE_SCHED_ERR_MSG, apiMsgId));
        } else {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD,
                TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
        }

        logger.error(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
            "ProcessAchPymtInqHelper: callAchPaymentInquiry",
            "Exception occured during ACH Payment Inquiry service call", AmexLogger.Result.failure,
            ex.getMessage(), ex);

        throw new SSEApplicationException("Exception occured during ACH Payment Inquiry service call",
            ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY,
            EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY), ex);
    }

    private void handleFaultResponse(String apiMsgId, GetInquiryFaultMsg ex) throws SSEApplicationException {
        FaultType faultType = ex.getFaultInfo().getFault();

        String sseRespCode = null;
        String sseRespDesc = null;
        String faultDetail = null;
        String faultCode = null;

        if (faultType != null) {
            faultDetail = faultType.getFaultDetail();
            faultCode = faultType.getFaultCode();
            if (faultCode != null) {
                /**
                 * Get the corresponding SSE response code and response desc from Properties files
                 */
                sseRespCode = EnvironmentPropertiesUtil.getProperty(faultCode);
                if (sseRespCode != null) {
                    sseRespDesc = EnvironmentPropertiesUtil.getProperty(sseRespCode);
                } else {
                    sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY;
                    sseRespDesc =
                            EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY)
                            + ": " + faultDetail;
                    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_CD,
                        TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_MSG, apiMsgId));
                }
            } else {
                sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY;
                sseRespDesc =
                        EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY);
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_CD,
                    TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_MSG, apiMsgId));
            }
        } else {
            sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY;
            sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY);
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_CD,
                TivoliMonitoring.ACH_PYMT_INQUIRY_ERR_MSG, apiMsgId));
        }

        logger.error(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
            "ProcessAchPymtInqHelper: callAchPaymentInquiry",
            "SOAP Fault Error occured during ACH Payment getInquiry SOAP service call", AmexLogger.Result.failure,
            faultDetail, ex, "fault-Actor", faultType.getFaultActor(), "fault-Code", faultCode, "fault-String",
            faultType.getFaultString(), "fault-Detail", faultDetail, "SSEResponseCode", sseRespCode,
            "SSEResponseDesc", sseRespDesc);
        throw new SSEApplicationException(
            "SOAP Fault Error occured during ACH Payment getInquiry SOAP service call", sseRespCode, sseRespDesc,
            ex);
    }

    private Map<String, String> buildInquiryResponse(ResponseType responseType, String apiMsgId)
            throws SSEApplicationException {
        Map<String, String> map = new HashMap<String, String>();
        logger.info(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
            "ProcessAchPymtInqHelper:  buildInquiryResponse", "Build response of ACH Payment Inquiry EMM service",
            AmexLogger.Result.success, "Start");
        String emmAcctNumber = StringUtils.EMPTY;
        String emmRtngNbr = StringUtils.EMPTY;
        String nameOnAcct = StringUtils.EMPTY;
        String enrollStatusCode = StringUtils.EMPTY;
        List<SucGrpType> sucGrpList = responseType.getRespGrp().getSucGrp();
        if (null != sucGrpList && !sucGrpList.isEmpty()) {
            SucGrpType sucGrpType = sucGrpList.get(0);
            if (sucGrpType != null) {
                List<EnrollGrpType> enrollGrpTypes = sucGrpType.getEnrollGrp();
                if (enrollGrpTypes != null && !enrollGrpTypes.isEmpty()) {
                    EnrollGrpType enrollGrpType = enrollGrpTypes.get(0);
                    if (enrollGrpType != null) {
                        EnrollDetGrpType enrollDetGrpType = enrollGrpType.getEnrollDetGrp();
                        enrollStatusCode = enrollDetGrpType.getEnrollStaCd();
                        // String enrollId = enrollGrpType.getEnrollId();
                        if (enrollDetGrpType != null) {
                            FIAcctDtlGrpType acctDtlGrpType = enrollDetGrpType.getFIAcctDtlGrp();
                            if (acctDtlGrpType != null) {
                                emmAcctNumber = acctDtlGrpType.getFIAcctNbr();
                                emmRtngNbr = acctDtlGrpType.getFIRteTrnstNbr();
                                nameOnAcct = acctDtlGrpType.getOrgnAcctNm();
                            }
                        }
                    }
                }
            }
        }
        map.put(ApiConstants.EMM_ACCNT_NBR, emmAcctNumber);
        map.put(ApiConstants.EMM_RTNG_NBR, emmRtngNbr);
        map.put(ApiConstants.EMM_NAME_ON_ACCT, nameOnAcct);
        map.put(ApiConstants.EMM_PYMT_RESP_CD, responseType.getStatus().getRespCd());
        map.put(ApiConstants.IN_ACCT_ACT_STA_CD, enrollStatusCode);
        return map;
    }

    private Map<String, Object> buildInquiryResponse(ResponseType responseType, String PayArngSetupCfmNbr,
        String apiMsgId) {

        Map<String, Object> map = new HashMap<String, Object>();
        // ServRegisGrpType PayArngGrpType
        logger.info(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API",
            "ProcessAchPymtInqHelper:  buildInquiryResponseForShedular",
            "Build response of ACH Payment Inquiry EMM service", AmexLogger.Result.success, "Start", "paymentRefrencenumber", PayArngSetupCfmNbr);
        List<SucGrpType> sucGrpList = responseType.getRespGrp().getSucGrp();
        // IN_RTRN_RSN_CD
        map.put(SchedulerConstants.IN_PYMT_RESP_CD, responseType.getStatus().getRespCd());
        map.put(SchedulerConstants.IN_PYMT_RESP_DS, responseType.getStatus().getRespDesc());
        map.put(SchedulerConstants.IN_EXPL_CD, responseType.getStatus().getExplCd());
        map.put(SchedulerConstants.IN_EXPL_DS, responseType.getStatus().getExplDesc());
        map.put(SchedulerConstants.IN_EMM_PYMT_REFER_NO, PayArngSetupCfmNbr);
        String upid = null;

        if (null != sucGrpList && !sucGrpList.isEmpty()) {
            outer : for (SucGrpType sucGrpType : sucGrpList) {
                if(null != sucGrpType){
                    List<ServRegisGrpType> objServRegisGrpType = sucGrpType.getServRegisGrp();
                    if(null != objServRegisGrpType && !objServRegisGrpType.isEmpty()){
                        for (ServRegisGrpType servRegisGrpType : objServRegisGrpType) {
                            if(null != servRegisGrpType){
                                List<PayArngGrpType> objPayArngGrpType = servRegisGrpType.getPayArngGrp();
                                if(null != objPayArngGrpType && !objPayArngGrpType.isEmpty()){
                                    for (PayArngGrpType payArngGrpType : objPayArngGrpType) {
                                        if (null != payArngGrpType && PayArngSetupCfmNbr.equalsIgnoreCase(payArngGrpType.getPayArngSetupCfmNbr())) {
                                            map.put(SchedulerConstants.IN_PYMT_REC_STA_CD, payArngGrpType.getPayArngStaCd());
                                            upid = payArngGrpType.getIntUPID();
                                            map.put(SchedulerConstants.IN_EMM_PYMT_ID, payArngGrpType.getIntUPID());
                                            // map.put(SchedulerConstants.REMIT_STSTUS_CODE_EXTERNAL,payArngGrpType.getArrangementStatusEnum());
                                            map.put(ApiConstants.IN_EMM_PYMT_SET_DT, payArngGrpType.getPayArngSetupTs());
                                            if(payArngGrpType.getLstUpdtGrp()!=null){
                                            	map.put(ApiConstants.IN_EMM_PYMT_UPD_DT, payArngGrpType.getLstUpdtGrp().getLstUpdtTs());
                                            }
                                            break outer;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (null == upid) {
            for (SucGrpType sucGrpType : sucGrpList) {

                PymtHstGrpType objymtHstGrpType = sucGrpType.getPymtHstGrp();
                List<PaymentType> payments = objymtHstGrpType.getPayment();
                for (PaymentType paymentType : payments) {
                    if(null != paymentType){
                        String emmConfNbr = paymentType.getPymtCfmNbr();
                        if(!StringUtils.isEmpty(emmConfNbr) && PayArngSetupCfmNbr.equalsIgnoreCase(emmConfNbr)){
                            map.put(SchedulerConstants.IN_EMM_PYMT_ID, paymentType.getIntUPID());
                            break;
                        }
                    }


                }
            }

        }

        return map;
    }
}
