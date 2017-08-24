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
import com.americanexpress.emm.handler.ext.PaymentRelationshipServiceExt;
import com.americanexpress.smartserviceengine.client.CreateACHAccountServiceClient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.SOAPOperationsLoggingHandler;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.ValidateAchPymtRespVO;
import com.americanexpress.soaui.am.commonresponse.v2.ErrGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.FailGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.PayArngGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.RespGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.ResponseType;
import com.americanexpress.soaui.am.commonresponse.v2.ServRegisGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.StatusType;
import com.americanexpress.soaui.am.commonresponse.v2.SucGrpType;
import com.americanexpress.soaui.am.commonresponse.v2.WarnGrpType;
import com.americanexpress.soaui.am.paymentrelationshipservice.v2.CreatePaymentRelationshipFaultMsg;
import com.americanexpress.soaui.am.paymentrelationshipservice.v2.IPaymentRelationshipService;
import com.americanexpress.soaui.am.paymentrelationshipservice.v4.createpaymentrelationship.FaultType;
import com.americanexpress.soaui.am.paymentrelationshipservice.v4.createpaymentrelationship.RequestType;
import com.americanexpress.soaui.serviceheader.v4.TrackingHdrType;

@Service
public class ProcessAchPaymentHelper {

    private static AmexLogger logger = AmexLogger.create(ProcessAchPaymentHelper.class);

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    public Map<String, Object> processAchPayment(String requestId, ValidateAchPymtRespVO validateAchPymtRespVO, String apiMsgId,boolean isScheduler, String pymtTransType)
    		throws GeneralSecurityException, SSEApplicationException {
        Map<String, Object> map = new HashMap<String, Object>();
        logger.info(apiMsgId, "SmartServiceEngine", "ACH Payment Inquiry API", "ProcessAchPaymentHelper: getInquiry: starts",
            "Process ACH Payment Helper - Start", AmexLogger.Result.success, "");
        ResponseType responseType = null;
        map = callAchProcessPayment(requestId, validateAchPymtRespVO, apiMsgId, pymtTransType,isScheduler);
        responseType = (ResponseType) map.get(ApiConstants.RESPONSE_DETAILS);
        String errorCode = StringUtils.stripToEmpty((String) map.get(ApiConstants.ERROR_CODE));
        if(isScheduler) {
            map = buildPaymentResponseSchdular(responseType, validateAchPymtRespVO.getPayArngSetupCfmNbr(), errorCode, apiMsgId);
        } else{
            // set the response data to Ach Payment Inquiry Response object
            map = buildPaymentResponse(responseType, validateAchPymtRespVO.getPayArngSetupCfmNbr(), errorCode, apiMsgId);
        }
        return map;
    }

    private Map<String, Object> buildPaymentResponseSchdular(ResponseType responseType, String ssePymtCnfrmNbr, String errorCode, String apiMsgId) throws SSEApplicationException {
        Map<String, Object> map = new HashMap<String, Object>();
        String emmPymtConfNbr = StringUtils.EMPTY;
        String upId = StringUtils.EMPTY;
        String pymtStatusCd = "0";
        String setUpTs = "0001-01-01-00.00.00.000000";
        String lstUpTs = "0001-01-01-00.00.00.000000";
        if(StringUtils.isBlank(errorCode)) {
        	 List<SucGrpType> sucGrpList = responseType.getRespGrp().getSucGrp();
             if(null != sucGrpList && !sucGrpList.isEmpty()){
                outer : for (SucGrpType sucGrpType : sucGrpList){
                    if(null != sucGrpType){
                     List<ServRegisGrpType> ServRegisGrpReqList = sucGrpType.getServRegisGrp();
                        if(null != ServRegisGrpReqList && !ServRegisGrpReqList.isEmpty()){
                     for(ServRegisGrpType servRegisGrpType : ServRegisGrpReqList){
                                if(null != servRegisGrpType){
                         List<PayArngGrpType> payArngGrpList = servRegisGrpType.getPayArngGrp();
                                    if(null != payArngGrpList && ! payArngGrpList.isEmpty()){
                         for(PayArngGrpType payArngGrpType : payArngGrpList){
                         if(null != payArngGrpType){
                             emmPymtConfNbr = payArngGrpType.getPayArngSetupCfmNbr();
                             if(null != ssePymtCnfrmNbr && null != emmPymtConfNbr && emmPymtConfNbr.equals(ssePymtCnfrmNbr)){
                                 upId =  payArngGrpType.getIntUPID();
                                 pymtStatusCd = payArngGrpType.getPayArngStaCd();
                                 setUpTs = payArngGrpType.getPayArngSetupTs();
                                 lstUpTs = payArngGrpType.getLstUpdtGrp().getLstUpdtTs();
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
             }
         }
        if(responseType == null){
        	map.put(ApiConstants.IN_PYMT_RESP_CD, ApiConstants.FAIL);
            map.put(ApiConstants.IN_PYMT_RESP_DS, ApiConstants.FAIL);
            map.put(ApiConstants.IN_EXPL_CD, ApiConstants.FAIL);
            map.put(ApiConstants.IN_EXPL_DS, ApiConstants.FAIL);
            map.put(ApiConstants.ERROR_CODE, errorCode);
       }else{
       	StatusType statusType = responseType.getStatus();
       	if(statusType == null){
               map.put(ApiConstants.IN_PYMT_RESP_CD, ApiConstants.FAIL);
               map.put(ApiConstants.IN_PYMT_RESP_DS, ApiConstants.FAIL);
               map.put(ApiConstants.IN_EXPL_CD, ApiConstants.FAIL);
               map.put(ApiConstants.IN_EXPL_DS, ApiConstants.FAIL);
               map.put(ApiConstants.ERROR_CODE, errorCode);
       	}else{
       		map.put(ApiConstants.IN_PYMT_RESP_CD, statusType.getRespCd());
            map.put(ApiConstants.IN_PYMT_RESP_DS, statusType.getRespDesc());
            map.put(ApiConstants.IN_EXPL_CD, statusType.getExplCd());
            map.put(ApiConstants.IN_EXPL_DS, statusType.getExplDesc());
            map.put(ApiConstants.ERROR_CODE, errorCode);
       	}
       }
        map.put(ApiConstants.IN_PYMT_REC_STA_CD, pymtStatusCd);
        map.put(ApiConstants.IN_EMM_PYMT_REFER_NO, ssePymtCnfrmNbr);
        map.put(ApiConstants.IN_EMM_PYMT_ID, upId);
        map.put(ApiConstants.IN_EMM_PYMT_SET_DT, setUpTs);
        map.put(ApiConstants.IN_EMM_PYMT_UPD_DT, lstUpTs);
        return map;
    }


    private Map<String, Object> callAchProcessPayment(String requestId, ValidateAchPymtRespVO validateAchPymtRespVO, String apiMsgId, String pymtTransType,boolean isScheduler)
    		throws GeneralSecurityException, SSEApplicationException {
        logger.info(
            apiMsgId,
            "SmartServiceEngine",
            "ACH Process Payment API",
            "ProcessAchPaymentHelper: callAchProcessPayment: starts",
            "Start of invoking EMM Process Payment service",
            AmexLogger.Result.success, "", "isScheduler", Boolean.toString(isScheduler),
            "emmAccountId",Integer.toString(validateAchPymtRespVO.getSrcEnrollId()),
            "cardAccountNumber",CommonUtils.maskAccRoutingNumber(validateAchPymtRespVO.getCardAcctNbr(),5,apiMsgId),
            "paymentRefrencenumber",validateAchPymtRespVO.getPayArngSetupCfmNbr(),
            "paymentTransactionType", pymtTransType);
        Map<String, Object> map = new HashMap<String, Object>();
        final long startTimeStamp = System.currentTimeMillis();
        try {
            PaymentRelationshipServiceExt pymtService = new PaymentRelationshipServiceExt();
            IPaymentRelationshipService proxy = pymtService.getPaymentRelationshipServicePort();
            BindingProvider bindingProvider = (BindingProvider) proxy;
            bindingProvider.getBinding().getHandlerChain().add(new SOAPOperationsLoggingHandler());

            Map<String, Object> requestContext = bindingProvider.getRequestContext();
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_PAYMENT_SOAP_SERVICE_URL));
            requestContext.put(ApiConstants.REQUEST_TIMEOUT, EnvironmentPropertiesUtil.getProperty(ApiConstants.REQUEST_TIMEOUT_VALUE));
            logger.debug(apiMsgId, "SmartServiceEngine", "ACH Process Payment API", "ProcessAchPaymentHelper: callAchProcessPayment",
                "Calling ACH Process Payment EMM service", AmexLogger.Result.success, "EMM Process Payment SOAP Endpoint URL",
                EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_PAYMENT_SOAP_SERVICE_URL));
            CreateACHAccountServiceClient client = new CreateACHAccountServiceClient();
            RespGrpType response = client.buildResponseGroup();
            final Holder<RespGrpType> responseHolder = new Holder<RespGrpType>(response);
            TrackingHdrType trackingHdrType = null;
            Holder<TrackingHdrType> trackingHdr = null;
            //Liberty specific changes
            logger.info("validateAchPymtRespVO.getPartnerIndicator(): " +validateAchPymtRespVO.getPartnerIndicator());
            if((ApiConstants.CHAR_L).equals(validateAchPymtRespVO.getPartnerIndicator())){
            	trackingHdrType = client.buildTrackingHdrLiberty();
                trackingHdr = new Holder<TrackingHdrType>(trackingHdrType);
                logger.info("Liberty Specific Tracking");
            } else{
            	trackingHdrType = client.buildTrackingHdr();
                trackingHdr = new Holder<TrackingHdrType>(trackingHdrType);
            }
            StatusType statusType = client.buildStatusType();
            final Holder<StatusType> statusHolder = new Holder<StatusType>(statusType);
            RequestType request = client.buildRequestType(requestId, validateAchPymtRespVO, apiMsgId, pymtTransType);
            proxy.createPaymentRelationship(request, statusHolder, responseHolder, trackingHdr);
            RespGrpType responseGrpType = responseHolder.value;
            StatusType staType = statusHolder.value;

            String errorCd = null;
            if(null == staType || null == responseGrpType){
                logger.error(apiMsgId, "SmartServiceEngine", "ACH Process Payment API", "ProcessAchPaymentHelper: callAchProcessPayment",
                    "After invoking createPaymentRelationship EMM service Call", AmexLogger.Result.failure,"", "Null response from createPaymentRelationship Service", "");
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD, TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG, apiMsgId));
                throw new SSEApplicationException("Null response from ACH createPaymentRelationship EMM Service", ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY,
                    EnvironmentPropertiesUtil .getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY));
            }else if(null != staType.getRespCd() && ApiConstants.FAIL.equalsIgnoreCase(staType.getRespCd())){
                List<FailGrpType> failGrpList = responseGrpType.getFailGrp();
                if(null != failGrpList && !failGrpList.isEmpty()){
                    for(FailGrpType failGrpType : failGrpList){
                        if(null != failGrpType){
                            List<ErrGrpType> errGrpList = failGrpType.getErrGrp();
                            ErrGrpType errGrpType = errGrpList.get(0); //TODO getting only first error code from EMM Error response.
                            errorCd = errGrpType.getErrCd();
                            //errorMsg = errGrpType.getErrMsgTxt();
                            map.put(ApiConstants.ERROR_CODE, errorCd);
                            break;
                        }
                    }
                }
            }

            ResponseType respType = client.buildResponse(responseGrpType, staType);

            logger.info(
                apiMsgId,
                "SmartServiceEngine",
                "ACH Process Payment API",
                "ProcessAchPaymentHelper: callAchProcessPayment",
                "After invoking EMM createPaymentRelationship EMM service",
                AmexLogger.Result.success, "","isScheduler", Boolean.toString(isScheduler),
                "emmAccountId",Integer.toString(validateAchPymtRespVO.getSrcEnrollId()),
                "cardAccountNumber",CommonUtils.maskAccRoutingNumber(validateAchPymtRespVO.getCardAcctNbr(),5,apiMsgId),
                "paymentRefrencenumber",validateAchPymtRespVO.getPayArngSetupCfmNbr(),
                "paymentTransactionType", pymtTransType, "responseCode",
                staType.getRespCd());
            map.put(ApiConstants.RESPONSE_DETAILS, respType);
        }catch(CreatePaymentRelationshipFaultMsg ex){
            handleFaultResponse(apiMsgId, ex, map);
        }catch(WebServiceException exception){
            /*if(isScheduler){
                
                logger.error(apiMsgId,"SmartServiceEngine","ACH Process Payment API ","ProcessAchPaymentHelper: callAchProcessPayment",
                                "Exception occured during ACH createPaymentRelationship EMM service call",AmexLogger.Result.failure, exception.getMessage(), exception);
                if (exception.getMessage().contains("java.net.SocketTimeoutException")) {
                	logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.EMM_TIMEOUT_ERR_CD, TivoliMonitoring.EMM_TIMEOUT_ERR_MSG, apiMsgId));
                        throw new SSEApplicationException("Exception occured during ACH createPaymentRelationship EMM service call",ApiErrorConstants.EMM_TIMEOUT_SSEAPIEN902,
                                        EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.EMM_TIMEOUT_SSEAPIEN902),exception);
                } else {
                	logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, apiMsgId));
                        throw new SSEApplicationException("Exception occured during ACH createPaymentRelationship EMM service call",
                                        ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),exception);
                }
            }else{*/
            map.put(ApiConstants.ERROR_CODE, ApiConstants.TIMEOUT_EXCEPTION);
            map.put(ApiConstants.RESPONSE_DETAILS, new ResponseType());
           // }
            //logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD, TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG, apiMsgId));
        }catch (SSEApplicationException ex) {
            throw ex;
        }catch (Exception ex) {
            if(isScheduler){
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, apiMsgId));
            }else {
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
            }
            logger.error(apiMsgId, "SmartServiceEngine", "ACH Process Payment API", "ProcessAchPaymentHelper: callAchProcessPayment",
                "Exception occured during ACH createPaymentRelationship EMM service call", AmexLogger.Result.failure, ex.getMessage(), ex);
            throw new SSEApplicationException("Exception occured during ACH createPaymentRelationship EMM service call", ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY,
                EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY), ex);
        } finally {
            final long endTimeStamp = System.currentTimeMillis();

            logger.info(
                apiMsgId,
                "SmartServiceEngine",
                "ACH Process Payment API",
                "ProcessAchPaymentHelper: callAchProcessPayment",
                "After calling ACH Process Payment EMM service",
                AmexLogger.Result.success, "", "isScheduler", Boolean.toString(isScheduler),
                "emmAccountId",Integer.toString(validateAchPymtRespVO.getSrcEnrollId()),
                "cardAccountNumber",CommonUtils.maskAccRoutingNumber(validateAchPymtRespVO.getCardAcctNbr(),5,apiMsgId),
                "paymentRefrencenumber",validateAchPymtRespVO.getPayArngSetupCfmNbr(),
                "paymentTransactionType", pymtTransType,
                "Total Time Taken to get response from createPaymentRelationship EMM SOAP service",
                (endTimeStamp-startTimeStamp)+ " milliseconds("+(endTimeStamp-startTimeStamp)/1000.00+" seconds)");
        }
        return map;
    }


    private void handleFaultResponse(String apiMsgId, CreatePaymentRelationshipFaultMsg ex, Map<String, Object> map)
            throws SSEApplicationException {
    	boolean isError = true;
            FaultType faultType = ex.getFaultInfo().getFault();
            String sseRespCode = null;
            String sseRespDesc = null;
            String faultDetail = null;
			String faultCode = null;
			if (faultType != null) {
				faultDetail = faultType.getFaultDetail();
				faultCode = faultType.getFaultCode();
				if (faultCode != null) {
					if(ApiErrorConstants.GENERAL_APPLICAITON_ERROR_CODE.equals(faultCode)){
						isError = false;
						map.put(ApiConstants.ERROR_CODE, ApiConstants.GENERAL_APP_ERROR);
						map.put(ApiConstants.RESPONSE_DETAILS, new ResponseType());
						//logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD, TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG, apiMsgId));
					}else{
						// ** Get the corresponding SSE response code and response desc from Properties files *//
						sseRespCode = EnvironmentPropertiesUtil.getProperty(faultCode);
						if (sseRespCode != null) {
							sseRespDesc = EnvironmentPropertiesUtil.getProperty(sseRespCode);
						} else {
							sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY;
							sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY)+ ": " + faultDetail;
							logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD, TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG, apiMsgId));
						}
					}
				} else{
					sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY;
					sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY);
					logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD,TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG,apiMsgId));
				}
			} else{
                sseRespCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY;
                sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY);
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_CD, TivoliMonitoring.ACH_PROCESS_PAYMENT_ERR_MSG, apiMsgId));
            }
			if(isError){
				logger.error(apiMsgId,"SmartServiceEngine","ACH Process Payment API","ProcessAchPaymentHelper: callAchProcessPayment",
						"SOAP Fault Error occured during ACH createPaymentRelationship SOAP service call", AmexLogger.Result.failure, faultDetail, ex, "fault-Actor",
						faultType.getFaultActor(), "fault-Code", faultCode, "fault-String", faultType.getFaultString(), "fault-Detail",faultDetail, "SSEResponseCode",
						sseRespCode, "SSEResponseDesc", sseRespDesc);
				throw new SSEApplicationException("SOAP Fault Error occured during ACH Payment createPaymentRelationship SOAP service call", sseRespCode, sseRespDesc, ex);
			}
    }

    private Map<String, Object> buildPaymentResponse(ResponseType responseType, String ssePymtCnfrmNbr,
        String errorCode, String apiMsgId) throws SSEApplicationException {
        Map<String, Object> map = new HashMap<String, Object>();
      //  logger.debug(apiMsgId,"SmartServiceEngine","ACH Process Payment API","ProcessAchPaymentHelper:  buildPaymentResponse",
        //    "Build response of ACH Process Payment EMM service",AmexLogger.Result.success, "Start");
        String emmPymtConfNbr = StringUtils.EMPTY;
        String upId = StringUtils.EMPTY;
        String pymtStatusCd = "0";
        String warningCd = StringUtils.EMPTY;

        if(StringUtils.isBlank(errorCode)){
            List<SucGrpType> sucGrpList = responseType.getRespGrp().getSucGrp();
            if(null != sucGrpList && !sucGrpList.isEmpty()){
                for (SucGrpType sucGrpType : sucGrpList){
                    if(null != sucGrpType){
                    //Warning Group
                    List<WarnGrpType> warnGrpTypeList = sucGrpType.getWarnGrp();
                    if(null != warnGrpTypeList && !warnGrpTypeList.isEmpty()){
                        WarnGrpType warnGrpType = warnGrpTypeList.get(0);
                        if(null != warnGrpType){
                            warningCd = warnGrpType.getWarnCd();
                                break;
                        }
                        }
                    }
                }
                    //Service Group
                outer: for (SucGrpType sucGrpType : sucGrpList){
                    if(null != sucGrpType){
                    List<ServRegisGrpType> ServRegisGrpReqList = sucGrpType.getServRegisGrp();
                        if(null != ServRegisGrpReqList && !ServRegisGrpReqList.isEmpty()){
                    for(ServRegisGrpType servRegisGrpType : ServRegisGrpReqList){
                                if(null != servRegisGrpType){
                        List<PayArngGrpType> payArngGrpList = servRegisGrpType.getPayArngGrp();
                                    if(null != payArngGrpList && !payArngGrpList.isEmpty()){
                        for(PayArngGrpType payArngGrpType : payArngGrpList){
                                            if(null != payArngGrpType){
                            emmPymtConfNbr = payArngGrpType.getPayArngSetupCfmNbr();
                            if(null != ssePymtCnfrmNbr && null != emmPymtConfNbr && emmPymtConfNbr.equals(ssePymtCnfrmNbr)){
                                upId =  payArngGrpType.getIntUPID();
                                pymtStatusCd = payArngGrpType.getPayArngStaCd();
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
            }
        }
        if(responseType == null){
            map.put(ApiConstants.EMM_PYMT_RESP_CD, ApiConstants.FAIL);
            map.put(ApiConstants.EMM_PYMT_RESP_DESC, ApiConstants.FAIL);
            map.put(ApiConstants.EMM_PYMT_EXPL_CD, ApiConstants.FAIL);
            map.put(ApiConstants.EMM_PYMT_EXPL_DESC, ApiConstants.FAIL);
            map.put(ApiConstants.EMM_PYMT_CNFM_NBR, emmPymtConfNbr);
            map.put(ApiConstants.EMM_PYMT_UPID, upId);
            map.put(ApiConstants.EMM_PYMT_STATUS_CD, pymtStatusCd);
            map.put(ApiConstants.ERROR_CODE, errorCode);
            map.put(ApiConstants.WARNING_CD, warningCd);
        }else{
            StatusType statusType = responseType.getStatus();
            if(statusType == null){
                map.put(ApiConstants.EMM_PYMT_RESP_CD, ApiConstants.FAIL);
                map.put(ApiConstants.EMM_PYMT_RESP_DESC, ApiConstants.FAIL);
                map.put(ApiConstants.EMM_PYMT_EXPL_CD, ApiConstants.FAIL);
                map.put(ApiConstants.EMM_PYMT_EXPL_DESC, ApiConstants.FAIL);
                map.put(ApiConstants.ERROR_CODE, errorCode);
            }else{
                map.put(ApiConstants.EMM_PYMT_RESP_CD, statusType.getRespCd());
                map.put(ApiConstants.EMM_PYMT_RESP_DESC, statusType.getRespDesc());
                map.put(ApiConstants.EMM_PYMT_EXPL_CD, statusType.getExplCd());
                map.put(ApiConstants.EMM_PYMT_EXPL_DESC, statusType.getExplDesc());
            }
            map.put(ApiConstants.EMM_PYMT_CNFM_NBR, emmPymtConfNbr);
            map.put(ApiConstants.EMM_PYMT_UPID, upId);
            map.put(ApiConstants.EMM_PYMT_STATUS_CD, pymtStatusCd);
            if(StringUtils.isBlank(errorCode)){
            	map.put(ApiConstants.ERROR_CODE, getEMMErrorCode(responseType));
            }else{
            	map.put(ApiConstants.ERROR_CODE, errorCode);
            }
            map.put(ApiConstants.WARNING_CD, warningCd);
            map.put(ApiConstants.WARNING_CD, warningCd);
        }
        return map;
    }

    private String getEMMErrorCode(ResponseType responseType){
    	String errorCode = null;
    	if(responseType != null){
    		RespGrpType respGrpType = responseType.getRespGrp();
    		if(respGrpType != null){
    			List<FailGrpType> failGrpTypes = respGrpType.getFailGrp();
    			if(failGrpTypes != null && !failGrpTypes.isEmpty()){
    				List<ErrGrpType> errGrpTypes = failGrpTypes.get(0).getErrGrp();
    				if(errGrpTypes != null && !errGrpTypes.isEmpty()){
    					errorCode = errGrpTypes.get(0).getErrCd();
    				}
    			}
    		}
    	}
    	return errorCode;
    }
}
