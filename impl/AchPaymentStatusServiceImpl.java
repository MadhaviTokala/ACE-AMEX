package com.americanexpress.smartserviceengine.service.impl;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.enums.ACHBankNameValues;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.AchPayRequestVO;
import com.americanexpress.smartserviceengine.common.vo.ValidateAchPymtRespVO;
import com.americanexpress.smartserviceengine.dao.AchPaymentDAO;
import com.americanexpress.smartserviceengine.dao.MicroDebitAchUpdateDAO;
import com.americanexpress.smartserviceengine.dao.UpdateAchPaymentDAO;
import com.americanexpress.smartserviceengine.helper.CreateAchPaymentRestClientHelper;
import com.americanexpress.smartserviceengine.helper.ProcessAchPymtInqHelper;
import com.americanexpress.smartserviceengine.service.AchPaymentStatusService;

public class AchPaymentStatusServiceImpl implements AchPaymentStatusService {

    private static AmexLogger logger = AmexLogger.create(AchPaymentStatusServiceImpl.class);
    @Autowired
    private AchPaymentDAO achPaymentDAO;

    @Autowired
    private UpdateAchPaymentDAO updateAchPaymentDAO;

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    @Autowired
    private ProcessAchPymtInqHelper processAchPymtInqHelper;

    @Resource
    CreateAchPaymentRestClientHelper createAchPaymentRestClientHelper;
    
    @Autowired
    private MicroDebitAchUpdateDAO microDebitAchUpdateDAO;
    
    @Resource
    private CacheManager cacheManager;

    @Override
    public void getAchPaymentData(String eventId) throws SSEApplicationException {
        logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "getAchPaymentData",
            "Start of  AchPaymentStatusService", AmexLogger.Result.success, "Start");

        String paymentTimeStamp = new String(SchedulerConstants.EMPTY_TIMESTAMP);
        String microPaymentTimeStamp = new String(SchedulerConstants.EMPTY_TIMESTAMP);
        try {

            fetchAndProcessRecords(paymentTimeStamp, microPaymentTimeStamp, eventId);

        } catch (Exception e) {
            logger.error(eventId, "SmartServiceEngine", "AchPaymentStatusServiceImpl", "getPaymentData",
                "Exception in AchPaymentStatusServiceImpl for SP ", AmexLogger.Result.failure,
                "Failure in getPaymentData", e, "ErrorMsg", e.getMessage());
            throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), e);
        }

        logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "getAchPaymentData",
            "End of  AchPaymentStatusService", AmexLogger.Result.success, "End");
    }

    private void checkAchPaymentStatusAndInitiate(List<AchPayRequestVO> achPayDataResultSet, String eventId,
            String typeOfPayment) throws SSEApplicationException, ParseException{
        if (achPayDataResultSet != null) {
            logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatusAndInitiate",
                "Start Checking payment initiated or not if not initiating payment", AmexLogger.Result.success,
                "Start", "typeOfPayment", typeOfPayment);
            Iterator<AchPayRequestVO> iterator = achPayDataResultSet.iterator();
            while (iterator.hasNext()) {
                AchPayRequestVO achPayDataDetails = iterator.next();
                String respCdFromInquiry = "";
                String rtrnRsnCd = StringUtils.EMPTY;
                int itrCount = 0;
                Map<String, Object> statusMap = new HashMap<String, Object>();
                String requestId = UUID.randomUUID().toString();
                //Generating EMM_PYMT_REF_NO for credit records
                String paymentRefNum = "";
                if (StringUtils.isBlank(achPayDataDetails.getPaymentRefrencenumber())){
                	paymentRefNum = generatePymtRefNo(achPayDataDetails.getPaymentTransactionType());
                	achPayDataDetails.setPaymentRefrencenumber(paymentRefNum);
                }
                
                ValidateAchPymtRespVO objValidateAchPymtRespVO = buildValidateAchPymtRespVO(achPayDataDetails);
                try {
                    requestId = requestId.substring(0, 34);
                    statusMap = processAchPymtInqHelper.getInquiry(requestId, objValidateAchPymtRespVO, true, eventId);

                } catch (SSEApplicationException e) {                	
                    logger.error(eventId, "SmartServiceEngine", "AchPaymentStatusService", "getAchPaymentData",
                        "Exception while Calling Payment Status Service - EMM CALL", AmexLogger.Result.failure,
                        "Failure in getPaymentData", e, "ErrorMsg", e.getMessage());
                } catch (Exception e) {
                    logger.error(eventId, "SmartServiceEngine", "AchPaymentStatusService", "getAchPaymentData",
                        "Exception while Calling Payment Status Service - EMM CALL", AmexLogger.Result.failure,
                        "Failure in getPaymentData", e, "ErrorMsg", e.getMessage());
                }

                if (null != statusMap && SchedulerConstants.SUCCESS.equalsIgnoreCase(StringUtils.stripToEmpty((String) statusMap.get(SchedulerConstants.IN_PYMT_RESP_CD)))) {
                    respCdFromInquiry = StringUtils.stripToEmpty((String) statusMap.get(SchedulerConstants.IN_PYMT_RESP_CD));
                    String intUpid = StringUtils.stripToEmpty((String) statusMap.get(SchedulerConstants.IN_EMM_PYMT_ID));

                    logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService",
                        "checkAchPaymentStatus", "intUPID value from payment inquiry service call", AmexLogger.Result.success, "",
                        "paymentRefrencenumber",objValidateAchPymtRespVO.getPayArngSetupCfmNbr(),
                        "paymentTransactionType", objValidateAchPymtRespVO.getPaymentTransactionType(),
                        "intUPID", intUpid, "eventId", eventId);

                    if (!StringUtils.isEmpty(intUpid)) {
                        statusMap.put(SchedulerConstants.RTRN_RSN_CD, rtrnRsnCd);

                        if (typeOfPayment.equalsIgnoreCase(SchedulerConstants.PAYMENT)) {
                            logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService",
                                "checkAchPaymentStatus", "", AmexLogger.Result.success, "Type of Payment:Payment",
                                "Payment Type Code", SchedulerConstants.PAYMENT, "eventId", eventId);

                            callAchUpdateDAO(achPayDataDetails, statusMap, eventId, itrCount);

                        }
                        if (typeOfPayment.equalsIgnoreCase(SchedulerConstants.MICROPAYMENT)) {
                            logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService",
                                "checkAchPaymentStatus", "Type of Payment:Micro Payment", AmexLogger.Result.success,
                                "", "Payment Type Code", SchedulerConstants.MICROPAYMENT, "eventId", eventId);
                            callMicroAchupdateDAO(achPayDataDetails, statusMap, eventId, itrCount);
                        }
                    } else {
                        logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatus",
                            "Initiating Credit Payment", AmexLogger.Result.success, "",
                            "paymentRefrencenumber",objValidateAchPymtRespVO.getPayArngSetupCfmNbr(),
                            "TypeofPayment", SchedulerConstants.PAYMENT,
                            "TransactionType", achPayDataDetails.getPaymentTransactionType());
                        
                        //calculating eff date
                        String scenario = "";
                    	String bankName = StringUtils.stripToEmpty(achPayDataDetails.getBankName());
                    	logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatus",
             		            "Start of Calculating effective Date", AmexLogger.Result.success, "","BankName",bankName);
                    	boolean onUsBank = ACHBankNameValues.findBankName(bankName);
                    	if(onUsBank){
                    		scenario = SchedulerConstants.ON_US;
                    		logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatus",
                 		            "Calculating effective Date", AmexLogger.Result.success, "","Transaction Type",SchedulerConstants.ON_US);
                    	} else {
                    		scenario = SchedulerConstants.OFF_US;
                    		//considering wells Fargo as default bank for OFF_US cut off time 
                    		bankName = SchedulerConstants.WELLS_FARGO_BANK;
                    		logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatus",
                 		            "Calculating effective Date", AmexLogger.Result.success, "","Transaction Type",SchedulerConstants.OFF_US);
                    	}
                        Date datenow = new Date();
                        String eff_Date = CommonUtils.calculateEffectiveDate(datenow,scenario,cacheManager,bankName,achPayDataDetails.getPaymentAmount(),eventId,achPayDataDetails.getPaymentTransactionType());
                        objValidateAchPymtRespVO.setPaymentEffectiveDate(eff_Date);
                        objValidateAchPymtRespVO.setCompanyId(StringUtils.isBlank(ACHBankNameValues.getCompIdForBank(bankName))?ACHBankNameValues.getCompIdForBank(ApiConstants.WELLS_FARGO_BANK):ACHBankNameValues.getCompIdForBank(bankName));
                        achPayDataDetails.setEffectiveDate(eff_Date);
                        logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatus",
             		            "End of Calculating effective Date", AmexLogger.Result.success, "","Effective Date",eff_Date);
                        
                        Map<String, Object> resultMap = null;
                        try {
                            String reqId = eventId.substring(0, 20);
                            
                            resultMap= createAchPaymentRestClientHelper.generatePaymentRequest(reqId, 
                            		objValidateAchPymtRespVO, eventId,true, achPayDataDetails.getPaymentTransactionType());            	

                          
                        }  catch (SSEApplicationException e) {
                            logger.error(eventId, "SmartServiceEngine", "AchPaymentStatusService", "getAchPaymentData",
                                "Exception while Calling Payment Status Service - EMM CALL", AmexLogger.Result.failure,
                                "Failure in getPaymentData", e, "ErrorMsg", e.getMessage());
                        }

                        if (null != resultMap && SchedulerConstants.SUCCESS.equalsIgnoreCase(StringUtils.stripToEmpty((String) resultMap.get(SchedulerConstants.IN_PYMT_RESP_CD)))) {

                            logger.debug(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatus",
                                "Process ACH Payment service call successful", AmexLogger.Result.success, "");
                            resultMap.put(SchedulerConstants.RTRN_RSN_CD, rtrnRsnCd);

                            if (typeOfPayment.equalsIgnoreCase(SchedulerConstants.PAYMENT)) {
                                callAchUpdateDAO(achPayDataDetails, resultMap, eventId, itrCount);
                            }
                            if (typeOfPayment.equalsIgnoreCase(SchedulerConstants.MICROPAYMENT)) {
                                callMicroAchupdateDAO(achPayDataDetails, resultMap, eventId, itrCount);
                            }
                        } else {

                            logger.debug(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatus",
                                "Process ACH Payment service call Failed", AmexLogger.Result.success, "");

                            String errorCode = (String) resultMap.get(SchedulerConstants.ERROR_CODE);

                            if ((SchedulerConstants.SYSTEM_EXCEPTION.equals(errorCode)
                                    || SchedulerConstants.MMTHDB_DOWN.equals(errorCode) || SchedulerConstants.TIMEOUT_EXCEPTION
                                    .equals(errorCode) || ApiConstants.GENERAL_APP_ERROR.equals(errorCode))) {
                                itrCount = 1;
                                String respCode = tivoliMonitoringAlert(achPayDataDetails, eventId);
                                resultMap.put(SchedulerConstants.IN_PYMT_RESP_CD, respCode);
                                resultMap.put(SchedulerConstants.IN_EXPL_CD, respCode);
                                resultMap.put(SchedulerConstants.IN_PYMT_RESP_DS, respCode);
                                resultMap.put(SchedulerConstants.IN_EXPL_DS, respCode);
                                logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatus",
                                    "Got Timeout Exception/System Exception/GHDB down", AmexLogger.Result.success, "", "errorCode", errorCode,
                                    "Card_Account_Number", CommonUtils.maskAccRoutingNumber(achPayDataDetails.getCardAccountNumber(),5,eventId));
                            }else{
                                String sseErrCode = EnvironmentPropertiesUtil.getProperty(errorCode);
                                logger.debug(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatus",
                                    "Process ACH Payment service call Failed", AmexLogger.Result.success, "", "EMMErrorCode", errorCode,
                                    "SSEEMMErrorCode", sseErrCode);
                                rtrnRsnCd = CommonUtils.getRtrnRsnCd(sseErrCode);
                                resultMap.put(SchedulerConstants.RTRN_RSN_CD, rtrnRsnCd);
                                logger.debug(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatus",
                                    "Process ACH Payment service call Failed", AmexLogger.Result.success, "", "EMMErrorCode", errorCode,
                                    "SSEEMMErrorCode", sseErrCode, "SP_ReturnReasonCode", rtrnRsnCd);
                                //Raise Tivoli alert when ACH Credit fails.
                                if(null != achPayDataDetails.getPaymentTransactionType() && ApiConstants.CHAR_ONLY_CHK.equalsIgnoreCase(achPayDataDetails.getPaymentTransactionType())
                                        && typeOfPayment.equalsIgnoreCase(SchedulerConstants.PAYMENT)){
                                    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SCHED_PYMT_INIT_ACHCREDIT_FAIL_CD,
                                        TivoliMonitoring.SCHED_PYMT_INIT_ACHCREDIT_FAIL_MSG, eventId));
                                    //Raise Tivoli alert when Micro Credit Payment Failed
                                }else if(null != achPayDataDetails.getPaymentTransactionType() && ApiConstants.CHAR_ONLY_CHK.equalsIgnoreCase(achPayDataDetails.getPaymentTransactionType())
                                        && typeOfPayment.equalsIgnoreCase(SchedulerConstants.MICROPAYMENT))   {
                                    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SCHED_PYMT_INIT_MICROCREDIT_FAIL_CD,
                                        TivoliMonitoring.SCHED_PYMT_INIT_MICROCREDIT_FAIL_MSG, eventId));
                                }
                                resultMap.put(SchedulerConstants.IN_PYMT_RESP_CD, SchedulerConstants.FAIL);
                                resultMap.put(SchedulerConstants.IN_EXPL_CD, SchedulerConstants.FAIL);
                            }

                            if (typeOfPayment.equalsIgnoreCase(SchedulerConstants.PAYMENT)) {
                                callAchUpdateDAO(achPayDataDetails, resultMap, eventId, itrCount);
                            }else{
                                callMicroAchupdateDAO(achPayDataDetails, resultMap, eventId, itrCount);
                            }
                            logger.error(eventId,
                                "SmartServiceEngine",
                                "AchPaymentStatusServiceImpl",
                                "getPaymentData",
                                "Failure Response from Payment Status Service. Update Payment SP was not invoked. Response code from Payment Status Service is:"
                                        + respCdFromInquiry, AmexLogger.Result.failure,
                                    "Got Failure Response from Payment Status Service");
                        }
                    }

                } else {
                    logger.error(eventId,
                        "SmartServiceEngine",
                        "AchPaymentStatusServiceImpl",
                        "getPaymentData",
                        "Failure Response from Payment Status Service. Update Payment SP was not invoked. Response code from Payment Status Service is:"
                                + respCdFromInquiry, AmexLogger.Result.failure,
                            "Received Failure Response from Payment Status Service");
                }
            }

            logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "checkAchPaymentStatusAndInitiate",
                "End Checking payemnt initiated or not if not initiating payemnt ", AmexLogger.Result.success, "End",
                "ResultSet= ", achPayDataResultSet.toString(), "typeOfPayment", typeOfPayment);

        } else {
            logger.debug(eventId, "SmartServiceEngine", "AchPaymentStatusServiceImpl", "getPaymentData",
                "End of check checkAchPaymentStatus ", AmexLogger.Result.failure, "Result set could be null");
        }
    }

    private String tivoliMonitoringAlert(AchPayRequestVO achPayDataDetails, String eventId) {
    	String respCode = "";
        if (Integer.parseInt(achPayDataDetails.getRetryCount()) == 2) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SCHEDULAR_PAYMENT_INITIATION_EXEDS_CD,
                TivoliMonitoring.SCHEDULAR_PAYMENT_INITIATION_EXEDS_MSG, eventId));
            respCode =  SchedulerConstants.FAIL;
        }
        return respCode;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchAndProcessRecords(String paymenttimestamp, String microPaymenttimestamp,
        String eventId) throws SSEApplicationException {
        logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "fetchAndProcessRecords",
            "Start of Fetch and Process Records", AmexLogger.Result.success, "Start", "paymenttimestamp ="
                    + paymenttimestamp, "microPaymenttimestamp =" + microPaymenttimestamp);
        List<AchPayRequestVO> achPayDataResultSet = null;
        List<AchPayRequestVO> achmicroPayDataResultSet = null;

        Map<String, Object> inMap = createInputMap(paymenttimestamp, microPaymenttimestamp);
        Map<String, Object> outMap = new HashMap<String, Object>();
        try {
            outMap = achPaymentDAO.execute(inMap, eventId);
            logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "getAchPaymentData",
                "after fetching records from  77 SP", AmexLogger.Result.success, "");

            if (StringUtils.stripToEmpty((String) outMap.get(SchedulerConstants.RESP_CD)).equalsIgnoreCase(
                SchedulerConstants.RESPONSE_CODE_SUCCESS_SSEPY000)) {
                achPayDataResultSet = (List<AchPayRequestVO>) outMap.get(SchedulerConstants.RESULT_SET_1);
                achmicroPayDataResultSet = (List<AchPayRequestVO>) outMap.get(SchedulerConstants.RESULT_SET_2);

                int achRecCount = getIntValueFromMap(outMap, SchedulerConstants.OUT_AC_COUNT);
                int micrRecCount = getIntValueFromMap(outMap, SchedulerConstants.OUT_MD_COUNT);

                String reqId = eventId.substring(0, 20);

                paymenttimestamp = checkPaymentStatus(paymenttimestamp, achPayDataResultSet, achRecCount, reqId, SchedulerConstants.PAYMENT);
                microPaymenttimestamp = checkPaymentStatus(microPaymenttimestamp, achmicroPayDataResultSet, micrRecCount, reqId, SchedulerConstants.MICROPAYMENT);

                if ((null != achmicroPayDataResultSet && achmicroPayDataResultSet.size() != 0)
                        || (null != achPayDataResultSet && achPayDataResultSet.size() != 0)) {
                    fetchAndProcessRecords(paymenttimestamp, microPaymenttimestamp, eventId);
                }
            } else {
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_077_ERR_CD, TivoliMonitoring.SSE_SP_077_ERR_MSG, eventId));
                logger.debug(eventId, "SmartServiceEngine", "AchAchPaymentStatusServiceImplImpl", "getPaymentData",
                    "Check out for the outMap for SP TEST0077", AmexLogger.Result.failure,
                        "outMap is null or check for Error RESPONSE_CODE");
            }
        } catch (Exception e) {
            logger.error(eventId, "SmartServiceEngine", "AchPaymentStatusServiceImpl", "fetchAndProcessRecords",
                "Exception while processing Payment Initiation scheduler ", AmexLogger.Result.failure,
                "Failure in Payment Initiation Scheduler service", e, "ErrorMsg", e.getMessage());
            throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), e);
        }

        logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "fetchAndProcessRecords",
            "End of Fetch and Process Records", AmexLogger.Result.success, "End", "paymenttimestamp ="
                    + paymenttimestamp, "microPaymenttimestamp =" + microPaymenttimestamp);

        return outMap;
    }

    private String checkPaymentStatus(String paymenttimestamp, List<AchPayRequestVO> payDataResultSet, int recCount,
            String reqId, String paymentType) throws SSEApplicationException, ParseException {

        int paginationCount =
                Integer.valueOf(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.PAGINATION_COUNT));

        if (null != payDataResultSet) {
        	if(SchedulerConstants.PAYMENT.equals(paymentType)) {
        		logger.info("Normal Payment");
        		checkAchPaymentStatusAndInitiate(payDataResultSet, reqId, SchedulerConstants.PAYMENT);
        	} else if(SchedulerConstants.MICROPAYMENT.equals(paymentType)) {
        		logger.info("Micro Payment");

                checkAchPaymentStatusAndInitiate(payDataResultSet, reqId, SchedulerConstants.MICROPAYMENT);
            	}
            AchPayRequestVO achPayRequestVO = null;
            if (recCount > paginationCount) {
                achPayRequestVO = payDataResultSet.get(payDataResultSet.size() - 1);
                paymenttimestamp = achPayRequestVO.getTimeStamp();
            } else {
                paymenttimestamp = SchedulerConstants.HIGH_TIMESTAMP;
            }
        }
        return paymenttimestamp;
    }

    private Map<String, Object> createInputMap(String paymenttimestamp, String microPaymenttimestamp) {
        Map<String, Object> inMap = new HashMap<String, Object>();
        inMap.put(SchedulerConstants.IN_LST_UPDT_TS_MD, microPaymenttimestamp);
        inMap.put(SchedulerConstants.IN_LST_UPDT_TS_AC, paymenttimestamp);
        return inMap;
    }

    private int getIntValueFromMap(Map<String, Object> outMap, String paramName) {
        Object object = outMap.get(paramName);
        int intValue = object != null ? Integer.valueOf(object.toString().trim()) : 0;
        return intValue;
    }

    private void callAchUpdateDAO(AchPayRequestVO achPayDataDetails, Map<String, Object> statusMap,
            String eventId, int itrCount) throws ParseException {
        Map<String, Object> inputMap = buildInMapforUpdate(achPayDataDetails, statusMap, itrCount);
        try {
        	Map<String, Object> outMap = updateAchPaymentDAO.execute(inputMap, eventId);
        	if(outMap != null && !outMap.isEmpty()){
        		String respCd = StringUtils.stripToEmpty((String) outMap.get(SchedulerConstants.SP_RESP_CODE));
                if(SchedulerConstants.SUC_SSEIN000.equals(respCd)){
                	logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusServiceImpl", "callAchupdateDAO",
            				"Successfully executed E3GMR059 SP", AmexLogger.Result.failure, "");
                }else{
                	logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusServiceImpl", "callAchupdateDAO",
            				"Empty OutMap from E3GMR059 SP", AmexLogger.Result.failure, "");
                }
        	}else{
        		logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_059_ERR_CD, TivoliMonitoring.SSE_SP_059_ERR_MSG, eventId));
        		logger.error(eventId, "SmartServiceEngine", "AchPaymentStatusServiceImpl", "callAchupdateDAO",
        				"Empty OutMap from E3GMR059 SP", AmexLogger.Result.failure, "");
        	}
        } catch (Exception e) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_059_ERR_CD, TivoliMonitoring.SSE_SP_059_ERR_MSG, eventId));
            logger.error(eventId, "SmartServiceEngine", "AchPaymentStatusServiceImpl", "callAchupdateDAO",
                "Exception while Calling E3GMR059 ", AmexLogger.Result.failure, "Failure in getPaymentData", e, "ErrorMsg", e.getMessage());

        }
    }

    private void callMicroAchupdateDAO(AchPayRequestVO achPayDataDetails, Map<String, Object> statusMap,
            String eventId, int itrCount) {
        try {
            Map<String, Object> inputMap = buildInMapforUpdate(achPayDataDetails, statusMap, itrCount);

        	Map<String, Object> outMap = microDebitAchUpdateDAO.execute(inputMap, eventId);
            if(outMap != null && !outMap.isEmpty()){
            	String respCd = StringUtils.stripToEmpty((String) outMap.get(SchedulerConstants.SP_RESP_CODE));
                if(SchedulerConstants.SUC_SSEIN000.equals(respCd)){
                	logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusServiceImpl", "callMicroAchupdateDAO",
            				"Successfully executed E3GMR054 SP", AmexLogger.Result.failure, "");
                }else{
                	logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusServiceImpl", "callMicroAchupdateDAO",
            				"Empty OutMap from E3GMR054 SP", AmexLogger.Result.failure, "");
                }
        	}else{
        		logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_054_ERR_CD, TivoliMonitoring.SSE_SP_054_ERR_CD, eventId));
        		logger.error(eventId, "SmartServiceEngine", "AchPaymentStatusServiceImpl", "callMicroAchupdateDAO",
        				"Empty OutMap from E3GMR054 SP", AmexLogger.Result.failure, "");
        	}
        }
        catch (Exception e) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_054_ERR_CD, TivoliMonitoring.SSE_SP_054_ERR_CD, eventId));
            logger.error(eventId, "SmartServiceEngine", "AchPaymentStatusServiceImpl", "callMicroAchupdateDAO",
                "Exception while Calling E3GMR054 ", AmexLogger.Result.failure, "Failure in callMicroAchupdateDAO", e,
                "ErrorMsg", e.getMessage());

        }
    }

    private Map<String, Object> buildInMapforUpdate(AchPayRequestVO achPayDataDetails, Map<String, Object> statusMap, int itrCount) throws ParseException {
        Map<String, Object> inputMap = new HashMap<String, Object>();

        inputMap.put(SchedulerConstants.IN_EMM_PYMT_REFER_NO, StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_EMM_PYMT_REFER_NO)));

        inputMap.put(SchedulerConstants.IN_EMM_PYMT_ID, StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_EMM_PYMT_ID)));

        String strRespCd = StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_PYMT_RESP_CD));
        String respCd = (StringUtils.isBlank(strRespCd) || strRespCd.equalsIgnoreCase(SchedulerConstants.SUCCESS)) ? "00" : "07";
        inputMap.put(SchedulerConstants.IN_PYMT_RESP_CD, Integer.valueOf(respCd));

        inputMap.put(SchedulerConstants.IN_PYMT_RESP_DS, StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_PYMT_RESP_DS)));

        String strExplCd = StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_EXPL_CD));
        strExplCd = (strExplCd.length() > 4) ? strExplCd.substring(0, 4) : strExplCd;
        inputMap.put(SchedulerConstants.IN_EXPL_CD, strExplCd);

        inputMap.put(SchedulerConstants.IN_EXPL_DS, StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_EXPL_DS)));

        inputMap.put(SchedulerConstants.IN_RTRN_RSN_CD, StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.RTRN_RSN_CD)));

        Object object = statusMap.get(SchedulerConstants.IN_PYMT_REC_STA_CD);
        String statusCode = StringUtils.isNumeric((String) object) ? object.toString().trim() : "0";
        inputMap.put(SchedulerConstants.IN_PYMT_REC_STA_CD, Integer.valueOf(statusCode));
        inputMap.put(SchedulerConstants.IN_RETRY_CNT, Integer.parseInt(achPayDataDetails.getRetryCount()) + itrCount);
        inputMap.put(SchedulerConstants.IN_EMM_PYMT_REFER_NO1, StringUtils.EMPTY);
        String setTs=StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_EMM_PYMT_SET_DT));
        if(setTs.contains(ApiConstants.CHAR_T)){
        inputMap.put(SchedulerConstants.IN_EMM_PYMT_SET_DT,DateTimeUtil.convertDb2DateValue(StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_EMM_PYMT_SET_DT))));
        }else{
            inputMap.put(SchedulerConstants.IN_EMM_PYMT_SET_DT,StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_EMM_PYMT_SET_DT)));

        }
        String lastUpdTs=StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_EMM_PYMT_UPD_DT));
        if(lastUpdTs.contains(ApiConstants.CHAR_T)){
        inputMap.put(SchedulerConstants.IN_EMM_PYMT_UPD_DT, DateTimeUtil.convertDb2DateValue(StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_EMM_PYMT_UPD_DT))));
        }else{
            inputMap.put(SchedulerConstants.IN_EMM_PYMT_UPD_DT,StringUtils.stripToEmpty((String)statusMap.get(SchedulerConstants.IN_EMM_PYMT_UPD_DT)));
        }
        inputMap.put(SchedulerConstants.IN_PYMT_PRCS_DT, StringUtils.stripToEmpty(achPayDataDetails.getEffectiveDate()));
        inputMap.put(SchedulerConstants.IN_PYMT_PRCS_IN, SchedulerConstants.CHAR_N);
        inputMap.put(SchedulerConstants.IN_SSE_PYMT_ID, StringUtils.stripToEmpty(achPayDataDetails.getSsePymntId()));
        return inputMap;
    }

    private ValidateAchPymtRespVO buildValidateAchPymtRespVO(AchPayRequestVO achPayDataDetails) {
        ValidateAchPymtRespVO objValidateAchPymtRespVO = new ValidateAchPymtRespVO();
        objValidateAchPymtRespVO.setCardAcctNbr(achPayDataDetails.getCardAccountNumber());
        objValidateAchPymtRespVO.setPayArngSetupCfmNbr(achPayDataDetails.getPaymentRefrencenumber());
        objValidateAchPymtRespVO.setOrgName(achPayDataDetails.getOrgName());
        objValidateAchPymtRespVO.setPaymentValue(achPayDataDetails.getPaymentAmount());
        objValidateAchPymtRespVO.setPaymentTransactionType(achPayDataDetails.getPaymentTransactionType());
        if (null != achPayDataDetails.getEmmAccountId()) {
            objValidateAchPymtRespVO.setSrcEnrollId(Integer.parseInt(achPayDataDetails.getEmmAccountId()));
        }
        // Liberty Specific
        if(null != achPayDataDetails.getPartnerIndicator()){
        	objValidateAchPymtRespVO.setPartnerIndicator(achPayDataDetails.getPartnerIndicator());
        }
        return objValidateAchPymtRespVO;
    }
    
    private String generatePymtRefNo(String pymtType){
        String paymtRefNum = StringUtils.EMPTY;
        if (null != pymtType && pymtType.equals(ApiConstants.PAYMENT_TYPE_CODE_C)){
            paymtRefNum = ApiConstants.SSE_CNFM_NBR_PFRX_SC+RandomStringUtils.random(8, 0, 10, false, true, "0123456789".toCharArray());
        }else if (null != pymtType && pymtType.equals(ApiConstants.PAYMENT_TYPE_CODE_D)){
            paymtRefNum = ApiConstants.SSE_CNFM_NBR_PFRX_SD+RandomStringUtils.random(8, 0, 10, false, true, "0123456789".toCharArray());
        }
        return paymtRefNum;
    }
}
