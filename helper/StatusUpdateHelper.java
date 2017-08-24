package com.americanexpress.smartserviceengine.helper;

import static com.americanexpress.smartserviceengine.common.constants.ServiceConstants.APPLICATION_NAME;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.etv.issuetoken.data.IssueTokenResponseOutVO;
import com.americanexpress.etv.issuetoken.service.ManageTokenService;
import com.americanexpress.etv.managetoken.data.ManageTokenRequestInputVO;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SplunkConstants;
import com.americanexpress.smartserviceengine.common.enums.TokenStatus;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.TokenPaymentRecordVO;
import com.americanexpress.smartserviceengine.dao.CardTokenPaymentDAO;


/**
 * Helper class to update token, payment or both statuses.
 * Additionally it allows to update retryCount of payment
 * @author dkakka
 */
public class StatusUpdateHelper{

	private static AmexLogger LOGGER = AmexLogger.create(StatusUpdateHelper.class);
	private static final String EVENT_NAME = StatusUpdateHelper.class.getSimpleName();

	@Autowired
	private TivoliMonitoring tivoliMonitoring;

	@Autowired
	private CardTokenPaymentDAO cardTokenPaymentDao;

	/*@Resource
	private CardCancelTokenService cardCancelTokenService;*/

	@Autowired
	@Qualifier("manageTokenService")
	private ManageTokenService manageTokenService;


	public boolean updateTokenStatus(String tokenNumber, String tokenStatus,int cancelRetryCount){
		Map<String, Object> inMap = getInputMap(null, null, null,null, 0, tokenNumber, tokenStatus, "updateTokenStatus",cancelRetryCount,null);
		return cardTokenPaymentDao.updateTknPayCountStatus(inMap);
	}

	public boolean updatePaymentRetryCount(int retryCount, String paymentID){
		Map<String, Object> inMap = getInputMap(paymentID, null, null, null, retryCount, null, null, "updatePaymentRetryCount",0,null);
		return cardTokenPaymentDao.updateTknPayCountStatus(inMap);
	}

	public boolean updatePaymentRetryCountWithPymtStatus(int retryCount, String paymentID, String paymentStatus){
		Map<String, Object> inMap = getInputMap(paymentID, null, paymentStatus, null, retryCount, null, null, "updatePaymentRetryCount",0,null);
		return cardTokenPaymentDao.updateTknPayCountStatus(inMap);
	}	
	public boolean updatePaymentStatus(String paymentID,String paymentCardStatus,String paymentStatus, String paymtRespDesc){
		Map<String, Object> inMap = getInputMap(paymentID, paymentCardStatus,paymentStatus, paymtRespDesc, 0, null, null, "updatePaymentStatus",0,null);
		return cardTokenPaymentDao.updateTknPayCountStatus(inMap);
	}

	public boolean updatePaymentStatusAndRetryCount(String paymentID,String paymentCardStatus, String paymentStatus, String paymtRespDesc, int retryCount){
		Map<String, Object> inMap = getInputMap(paymentID, paymentCardStatus,paymentStatus, paymtRespDesc, retryCount, null, null, "updatePaymentStatusAndRetryCount",0,null);
		return cardTokenPaymentDao.updateTknPayCountStatus(inMap);
	}

	public boolean updateTokenAndPaymentStatus(String paymentID,String paymentCardStatus, String paymentStatus, String paymtRespDesc, int retryCount, 
			String tokenNumber, String tokenStatus,String tokenStatusDesc){
		Map<String, Object> inMap = getInputMap(paymentID,paymentCardStatus, paymentStatus, paymtRespDesc, retryCount, tokenNumber, tokenStatus, "updateTokenAndPaymentStatus",0,tokenStatusDesc);
		return cardTokenPaymentDao.updateTknPayCountStatus(inMap);
	}

	private Map<String, Object> getInputMap(String paymentID,String paymentCardStatus, String paymentStatus, String paymtRespDesc, int retryCount, 
			String tokenNumber, String tokenStatus, String methodName,int cancelRetryCount,String tokenStatusDesc) {
		String apiMsgId = ThreadLocalManager.getApiMsgId();
		LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "getInputMap", methodName, AmexLogger.Result.success, 
				"INPUT PARAMETERS TO PAYMENT STATUSUPDATEHELPER",  "paymentRefID", paymentID,"paymentCardStatus", paymentCardStatus, "paymentStatus", paymentStatus, "paymentRespDesc",paymtRespDesc,
				"retryCount", String.valueOf(retryCount), "tokenNumber",tokenNumber ==null ? null : GenericUtilities.maskField(tokenNumber,
				 SchedulerConstants.START_MASK_TOKEN_INDEX,SchedulerConstants.END_MASK_TOKEN_INDEX,SchedulerConstants.C3_MASK_CHAR,apiMsgId,false), "tokenStatus", tokenStatus);

		Map<String, Object> inMap = new HashMap<String, Object>();
		inMap.put(SchedulerConstants.IN_TOKEN_NUM, tokenNumber == null? ApiConstants.CHAR_SPACE : tokenNumber);
		inMap.put(SchedulerConstants.IN_PAYMENT_ID, paymentID == null? ApiConstants.CHAR_SPACE : paymentID);
		inMap.put(SchedulerConstants.IN_PYMT_STATUS_CARD, paymentCardStatus == null? ApiConstants.CHAR_SPACE : paymentCardStatus);
		inMap.put(SchedulerConstants.IN_PYMT_STATUS_PYMT, paymentStatus == null? ApiConstants.CHAR_SPACE : paymentStatus);
		inMap.put(SchedulerConstants.IN_TOKEN_STATUS, tokenStatus == null? ApiConstants.CHAR_SPACE : tokenStatus);
		inMap.put(SchedulerConstants.IN_TOKEN_RESP_STA_CD_DS, tokenStatusDesc == null? ApiConstants.CHAR_SPACE : tokenStatusDesc);
		inMap.put(SchedulerConstants.IN_RETRY_CT, retryCount);
		inMap.put(SchedulerConstants.IN_PYMT_RESP_DS, paymtRespDesc == null? ApiConstants.CHAR_SPACE : paymtRespDesc);
		inMap.put(SchedulerConstants.IN_TOKEN_CF_RETRY_CT, cancelRetryCount);		
		return inMap;
	}

	public void cancelTokenAndUpdateRetryCount(String paymentID, int retryCount, IssueTokenResponseOutVO issueTokenResponseOutVO,TokenPaymentRecordVO tokenPaymentRecordVO,boolean isErrorDueToDBFailure) {
		String apiMsgId = ThreadLocalManager.getApiMsgId();
		LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "cancelTokenAndUpdateRetryCount", "STARTED", AmexLogger.Result.success, 
				"cancelTokenAndUpdateRetryCount", "paymentRefID", paymentID, "retryCount", String.valueOf(retryCount),
				"issueTokenResponseOutVO", issueTokenResponseOutVO.toString());
		handleTokenPaymentFailure(paymentID, retryCount, null,null, null, issueTokenResponseOutVO,tokenPaymentRecordVO,isErrorDueToDBFailure);
	}
	
	public void cancelTokenAndFailPayment(String paymentID, String paymentCardStatus,String paymentStatus, String paymtRespDesc, IssueTokenResponseOutVO issueTokenResponseOutVO,TokenPaymentRecordVO tokenPaymentRecordVO,boolean isErrorDueToDBFailure) {
		String apiMsgId = ThreadLocalManager.getApiMsgId();
		LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "handleTokenPaymentFailure", "Preparing cancelToken Request", AmexLogger.Result.success, 
				"CancelToken after Token Payment Failure", "paymentRefID", paymentID, "paymentStatus", paymentStatus, "paymentRespDesc", paymtRespDesc,
				"issueTokenResponseOutVO", issueTokenResponseOutVO.toString());
		handleTokenPaymentFailure(paymentID, 0, paymtRespDesc,paymentCardStatus ,paymentStatus, issueTokenResponseOutVO,tokenPaymentRecordVO,isErrorDueToDBFailure);
	}
	
	private void handleTokenPaymentFailure(String paymentID, int retryCount, String paymtRespDesc,String paymentCardStatus, String paymentStatus, 
				IssueTokenResponseOutVO issueTokenResponseOutVO,TokenPaymentRecordVO tokenPaymentRecordVO,boolean isErrorDueToDBFailure) {
		String apiMsgId = ThreadLocalManager.getApiMsgId();
		try{
			ManageTokenRequestInputVO tokenToCancel = prepareCancelTokenRequest(paymentID, issueTokenResponseOutVO);
			boolean success = manageTokenService.cancelToken(tokenToCancel);
			String newTokenStatus = null;
			String newTokenStatusDesc =null;
			if(tokenToCancel.isValidationError()){
				newTokenStatus = TokenStatus.CANCEL_FAILED.getStatusCode();
				newTokenStatusDesc = TokenStatus.CANCEL_FAILED.name();
			}else if(success){
				newTokenStatus = TokenStatus.CANCELLED.getStatusCode();
				LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "handleTokenPaymentFailure", SplunkConstants.ETV_CANCEL_TOKEN_BECOZ_PAYMT_FAILED, 
						 AmexLogger.Result.success,"", "paymentRespDesc", paymtRespDesc, "tokenNumber",tokenToCancel.getTokenNumber() ==null ? null : GenericUtilities.maskField(tokenToCancel.getTokenNumber(),
								 SchedulerConstants.START_MASK_TOKEN_INDEX,SchedulerConstants.END_MASK_TOKEN_INDEX,SchedulerConstants.C3_MASK_CHAR,apiMsgId,false), 
						 "tokenExpiry", tokenToCancel.getTokenExpiry(), "tokenRequestorId", tokenToCancel.getTokenRequestorID(),"paymentRefID", paymentID);
			}
			LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "handleTokenPaymentFailure", "Preparing updatePaymentStatusAndRetryCount Request", 
					AmexLogger.Result.success, "updatePaymentStatusAndRetryCount request started after Token Payment Failure", "paymentRefID", paymentID,
					"newTokenStatus", newTokenStatus);
			
			
			
			LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "handleTokenPaymentFailure", "Preparing updatePaymentStatusAndRetryCount Request", 
					AmexLogger.Result.success, "updatePaymentStatusAndRetryCount request started after Token Payment Failure", "PaymentID", paymentID,
					"newUpdTokenStatus", newTokenStatus,"isErrorDueToDBFailure",String.valueOf(isErrorDueToDBFailure));
			
			if(newTokenStatus == null){
				updatePaymentStatusAndRetryCount(paymentID,paymentCardStatus, paymentStatus, paymtRespDesc, retryCount);
			}else{
				
				/* this flag will be set to true only if there is any DB failure while saving token details , hence no record
				   will be available in the alias_card_details table , so we reset the token status to null , else the update token status sp
				   will be failed srivathsan s 7/28/2016 */
				if(isErrorDueToDBFailure){
					newTokenStatus = null;
					newTokenStatusDesc = null;
				}
				if(BooleanUtils.isFalse(tokenPaymentRecordVO.isPaymentReqApiInitCall())){
					updateTokenAndPaymentStatus(paymentID, paymentCardStatus, paymentStatus, paymtRespDesc, retryCount, issueTokenResponseOutVO.getTokenNumber(), newTokenStatus,newTokenStatusDesc);	
				}else{
					updateTokenAndPaymentStatus(paymentID, null, ApiConstants.PYMT_STAT_INPR, null, retryCount, issueTokenResponseOutVO.getTokenNumber(), newTokenStatus,newTokenStatusDesc);					
				}				
			}
		}catch(Exception ex){
			LOGGER.error(apiMsgId, APPLICATION_NAME, EVENT_NAME, "handleTokenPaymentFailure", "Exception", AmexLogger.Result.success, 
					"Unexpected Exception while handleTokenPaymentFailure", ex, "paymentRefID", paymentID);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_TKN_PAY_PROCESS_ERR_CD, TivoliMonitoring.SSE_TKN_PAY_PROCESS_ERR_MSG, apiMsgId));
		}
	}

	private ManageTokenRequestInputVO prepareCancelTokenRequest(String paymentID, IssueTokenResponseOutVO issueTokenResponseOutVO) {
		String apiMsgId = ThreadLocalManager.getApiMsgId();
		ManageTokenRequestInputVO tokenToCancel = new ManageTokenRequestInputVO();
		String uniqueRequestID = String.valueOf(UUID.randomUUID().getMostSignificantBits());
		tokenToCancel.setRequestID(uniqueRequestID);
		tokenToCancel.setConversationID(apiMsgId);
		tokenToCancel.setTokenNumber(issueTokenResponseOutVO.getTokenNumber());
		Date expiryDate = GenericUtilities.parseFromStdTime(issueTokenResponseOutVO.getTokenExpiry());
		tokenToCancel.setTokenExpiry(GenericUtilities.convertToMMYYFormat(expiryDate));
		tokenToCancel.setTokenRequestorID(issueTokenResponseOutVO.getTokenRequestorID());
		tokenToCancel.setTokenStatus(issueTokenResponseOutVO.getTokenStatus());
		LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "handleTokenPaymentFailure.prepareCancelTokenRequest", "Cancelling Token", 
				AmexLogger.Result.success, "STARTED", "reasonDesc","Cancelling Token after Token Payment Failure", "paymentRefID", paymentID,
				"Token Number",issueTokenResponseOutVO.getTokenNumber() ==null ? null : GenericUtilities.maskField(issueTokenResponseOutVO.getTokenNumber(),
				 SchedulerConstants.START_MASK_TOKEN_INDEX,SchedulerConstants.END_MASK_TOKEN_INDEX,SchedulerConstants.C3_MASK_CHAR,apiMsgId,false)
				, "tokenExpiry", issueTokenResponseOutVO.getTokenExpiry());
		return tokenToCancel;
	}

}