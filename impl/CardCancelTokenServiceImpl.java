package com.americanexpress.smartserviceengine.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;

import com.americanexpress.ace.vng.enrollment.helper.VNGTokenEnrollmentHelper;
import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.etv.issuetoken.service.ManageTokenService;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ServiceConstants;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.ExecutorServiceWrapper;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.CancelTokenDetailsVO;
import com.americanexpress.smartserviceengine.common.vo.PaginationDataVO;
import com.americanexpress.smartserviceengine.dao.CardTokenDetailsDAO;
import com.americanexpress.smartserviceengine.handler.CancelTokenHandler;
import com.americanexpress.smartserviceengine.helper.StatusUpdateHelper;
import com.americanexpress.smartserviceengine.service.CardCancelTokenService;

/**
 * This class is for periodically fetching expired or paid tokens from the database and
 * Canceling them in ETV as well as in the ACE database. This job marks the completion of 
 * Token Life cycle.
 */
public class CardCancelTokenServiceImpl implements CardCancelTokenService {

	private static AmexLogger LOGGER = AmexLogger.create(CardCancelTokenService.class);
	private static final String EVENT_NAME = CardCancelTokenService.class.getSimpleName();

	@Resource
	private ManageTokenService manageTokenService;

	@Resource
	private TivoliMonitoring tivoliMonitoring;

	@Resource
	private CardTokenDetailsDAO cardTokenDetailsDAO;

	@Resource
	private StatusUpdateHelper statusUpdateHelper;
	
	@Resource
	private VNGTokenEnrollmentHelper vngTokenEnrollmentHelper;
	
	@Value("${PAGINATION_COUNT}")
	private String pagination;
	
	public void cancelTokens(){
		String vngPymtIndicatorFlag =EnvironmentPropertiesUtil.getOtherPropertyValues(ApiConstants.ISVNGPYMTROUTEINDICATOR.toUpperCase());
		String apiMsgId = ThreadLocalManager.getApiMsgId();
		int paginationCount = Integer.valueOf(pagination);
		ExecutorService executorService = null;
		try{
			LOGGER.info(apiMsgId, "SmartServiceEngine", EVENT_NAME, "cancelTokens", "Initiating CancelTokens Job.", AmexLogger.Result.success, "START");
			PaginationDataVO<CancelTokenDetailsVO> paginationDataVO = new PaginationDataVO<CancelTokenDetailsVO>();
			paginationDataVO.setLastUpdateTime(ServiceConstants.MIN_DEFAULT_TIMESTAMP);
			executorService = ExecutorServiceWrapper.requestExecutor(ServiceConstants.CANCEL_TOKEN_PROCESS_TPS, EVENT_NAME);

			boolean isFirstAttempt = true;
			boolean isContinueLoop = true;
			while(isContinueLoop){
				isContinueLoop = false; // Making it false in the beginning to avoid deadlock issues. will set to true if needed later.
				cardTokenDetailsDAO.getTokensToBeCancelled(paginationDataVO); //fetching tokens to be cancelled data from the ACE database
				if(isFirstAttempt){
					isFirstAttempt = false;
					LOGGER.info(apiMsgId, "SmartServiceEngine", EVENT_NAME, "cancelTokens", "DAILY TOKEN CANCELLATION PROCESS", AmexLogger.Result.success, 
						 "START", "tokenCancellationDate" + GenericUtilities.getTodaysDateMMDDYYYY(),"totalTokensToBeCancelled",
						 String.valueOf(paginationDataVO.getTotalRecordCount()));
				}
				List<CancelTokenDetailsVO> results = paginationDataVO.getResults();
				LOGGER.info(apiMsgId, "SmartServiceEngine", EVENT_NAME, "cancelTokens", "Initiating token Cancellation Process", AmexLogger.Result.success, "START");
				if(!GenericUtilities.isNullOrEmpty(results)){
					String lastUpdateTime = triggerCancelTokenProcess(executorService, results, vngPymtIndicatorFlag);
					isContinueLoop = hasMoreRecords(apiMsgId, paginationCount, lastUpdateTime, paginationDataVO.getTotalRecordCount());
					if(isContinueLoop && lastUpdateTime != null){
						paginationDataVO = new PaginationDataVO<CancelTokenDetailsVO>();
						paginationDataVO.setLastUpdateTime(lastUpdateTime);
					}
				}
			}
		}catch(Exception ex){
			LOGGER.error(apiMsgId, "SmartServiceEngine", EVENT_NAME, "cancelTokens", "Token Cancellation Process Failed", 
					AmexLogger.Result.failure, "Unexpected Exception while Cancelling Tokens", ex);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.CANCEL_TKN_PROCESS_ERR_CD, TivoliMonitoring.CANCEL_TKN_PROCESS_ERR_MSG, apiMsgId));
		}
	}

	private String triggerCancelTokenProcess(ExecutorService executorService, List<CancelTokenDetailsVO> results, String vngPymtRouteIndicator) {
		String apiMsgId = ThreadLocalManager.getApiMsgId();
		String lastUpdateTime = null;
		LOGGER.info(apiMsgId, "SmartServiceEngine", EVENT_NAME, "cancelTokens", "totalTokensToBeCancelled" + results.size(),
				AmexLogger.Result.success, "START");
		List<CancelTokenDetailsVO> tokensToCancel = new ArrayList<CancelTokenDetailsVO>();
		for(CancelTokenDetailsVO cancelTokenDetailsVO : results){
			tokensToCancel.add(cancelTokenDetailsVO);
			if(tokensToCancel.size() >= ServiceConstants.CANCEL_TOKENS_PER_THREAD_COUNT){
				executorService.submit(new CancelTokenHandler(apiMsgId, tivoliMonitoring, manageTokenService, statusUpdateHelper, tokensToCancel, vngTokenEnrollmentHelper, vngPymtRouteIndicator));
				tokensToCancel = new ArrayList<CancelTokenDetailsVO>();
			}
			lastUpdateTime = cancelTokenDetailsVO.getLastUpdateTime(); //resetting lastUpdateTime for fetching next set of records
		}
		if(tokensToCancel.size() > 0){
			executorService.submit(new CancelTokenHandler(apiMsgId, tivoliMonitoring, manageTokenService, statusUpdateHelper, tokensToCancel, vngTokenEnrollmentHelper, vngPymtRouteIndicator));
		}
		return lastUpdateTime;
	}

	private boolean hasMoreRecords(String apiMsgId, int paginationCount, String lastUpdateTime, int totalRecordCount){
		boolean isContinueLoop = false;
		if(totalRecordCount > paginationCount && !GenericUtilities.isNullOrEmpty(lastUpdateTime)){
			isContinueLoop = true;
		}
		return isContinueLoop;
	}

}