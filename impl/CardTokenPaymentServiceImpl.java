package com.americanexpress.smartserviceengine.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;

import com.americanexpress.smartserviceengine.common.util.PartnerIdMapping;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.springframework.beans.factory.annotation.Autowired;

import com.americanexpress.ace.vng.dao.VngCardTokenDetailsDAO;
import com.americanexpress.ace.vng.enrollment.helper.VNGTokenEnrollmentHelper;
import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.etv.issuetoken.service.IssueTokenService;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.ServiceConstants;
import com.americanexpress.smartserviceengine.common.util.ExecutorServiceWrapper;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.PaginationDataVO;
import com.americanexpress.smartserviceengine.common.vo.TokenPaymentRecordVO;
import com.americanexpress.smartserviceengine.dao.CardTokenDetailsDAO;
import com.americanexpress.smartserviceengine.dao.CardTokenPaymentDAO;
import com.americanexpress.smartserviceengine.handler.TokenPaymentHandler;
import com.americanexpress.smartserviceengine.handler.VNGTokenHandler;
import com.americanexpress.smartserviceengine.helper.StatusUpdateHelper;
import com.americanexpress.smartserviceengine.helper.TokenDomainRestrictionsHelper;
import com.americanexpress.smartserviceengine.helper.TokenEmailHelper;
import com.americanexpress.smartserviceengine.helper.VNGStatusUpdateHelper;
import com.americanexpress.smartserviceengine.helper.VNGTokenEmailHelper;
import com.americanexpress.smartserviceengine.service.CardTokenPaymentService;

public class CardTokenPaymentServiceImpl implements CardTokenPaymentService {
	
	private AmexLogger LOGGER=AmexLogger.create(CardTokenPaymentServiceImpl.class);
	
	@Resource
	private TivoliMonitoring tivoliMonitoring;
	
	@Resource
	private TokenDomainRestrictionsHelper tokenDomainRestrictionsHelper;
	
	@Resource
	private VNGTokenEnrollmentHelper vngTokenEnrollmentHelper;
	
	@Autowired
	private CardTokenPaymentDAO cardTokenPaymentDao;
	
	@Resource
	private VngCardTokenDetailsDAO vngCardTokenDetailsDAO;
	
	@Autowired
	private IssueTokenService issueTokenService;
	
	@Resource
	private TokenEmailHelper tokenEmailHelper;
	
	@Autowired
	private CardTokenDetailsDAO cardTokenDetailsDAO;
	
	@Autowired
	private StatusUpdateHelper statusUpdateHelper;
	
	@Autowired
	private VNGStatusUpdateHelper vngStatusUpdateHelper;
	
	@Resource
	private VNGTokenEmailHelper vngTokenEmailHelper;

    @Autowired
    private PartnerIdMapping partnerIdMapping;
    
    @Autowired
  	private CacheManager cacheManager;

	@Override
	public void processTokenPayments(String partnerID,String apiMsgId) {
		ExecutorService executorService = null;
		try{
			LOGGER.info(apiMsgId,"SmartServiceEngine Scheduler", "Card Token Payment Service", "processTokenPayments", "Initiating Payments Processing", AmexLogger.Result.success, "START");
			executorService = ExecutorServiceWrapper.requestExecutor(SchedulerConstants.TOKEN_PAYMENT_PROCESS_TPS, CardTokenPaymentService.class.getSimpleName());
			PaginationDataVO<TokenPaymentRecordVO> paginationDataVO = new PaginationDataVO<TokenPaymentRecordVO>();
			paginationDataVO.setLastUpdateTime(ServiceConstants.MIN_DEFAULT_TIMESTAMP);
			//Map<String,Object> tokenDomainControlVOMap = tokenDomainRestrictionsHelper.read(partnerID);
			Map<String,Object> tokenDomainControlVOMap = new HashMap<String,Object>();
			Cache cache= cacheManager.getCache(ApiConstants.PARTNER_ID_CACHE);
			Element element=cache.get(ApiConstants.DEFAULTDOMAINCONTROL);
			if(element!=null){
				tokenDomainControlVOMap = (Map<String, Object>) element.getObjectValue();
			}			
			
			
			LOGGER.info(apiMsgId, "SmartServiceEngine", "Card Token Payment Service","processTokenPayments", "Initiating Payments Processing",
					AmexLogger.Result.success, "START","tokenDomainControlVOMapData",tokenDomainControlVOMap.toString());
			
			if(tokenDomainControlVOMap != null && tokenDomainControlVOMap.size()>0){
				boolean isFirstAttempt = true;
				boolean isContinueLoop = true;
				while(isContinueLoop){
					isContinueLoop = false; // Making it false in the beginning to avoid deadlock issues. will set to true if needed later.
					//paginationDataVO.setPartnerID(partnerID);
					cardTokenPaymentDao.getTokenPayments(paginationDataVO); //fetching payment data from the ACE database				
					if(isFirstAttempt){
						isFirstAttempt = false;
						LOGGER.info(apiMsgId, "SmartServiceEngine Scheduler", "Card Token Payment Service", "processTokenPayments", "TOKEN PAYMENT PROCESSING", AmexLogger.Result.success, 
							 "START", "TOTAL NUMBER OF PAYMENTS TO BE PROCESSED ON" + GenericUtilities.getCurrentTimeAS_YYYY_MM_DD_T_HH_MM_SS(), 
							 String.valueOf(paginationDataVO.getTotalRecordCount()));
					}
					List<TokenPaymentRecordVO> results = paginationDataVO.getResults();
					if(!GenericUtilities.isNullOrEmpty(results)){
						String lastUpdateTime = triggerPaymentProcess(executorService, tokenDomainControlVOMap, results, ApiConstants.FALSEBOOLEAN);
						isContinueLoop = hasMoreRecords(apiMsgId, lastUpdateTime, paginationDataVO.getTotalRecordCount());
						if(isContinueLoop && lastUpdateTime != null){
							paginationDataVO = new PaginationDataVO<TokenPaymentRecordVO>();
							paginationDataVO.setLastUpdateTime(lastUpdateTime);
						}
					}
				}
			}else{
				LOGGER.error(apiMsgId, "SmartServiceEngine Scheduler", "Card Token Payment Service", "processTokenPayments", "Token Payment Processing Stopped", 
						AmexLogger.Result.failure, "No Domain Controls Found");
			}
		}catch(Exception ex){
			LOGGER.error(apiMsgId, "SmartServiceEngine Scheduler", "Card Token Payment Service", "processTokenPayments", "Token Payment Processing Failed", 
					AmexLogger.Result.failure, "Unexpected Exception while processing Token Payments", ex);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_TKN_PAY_PROCESS_ERR_CD, TivoliMonitoring.SSE_TKN_PAY_PROCESS_ERR_MSG, apiMsgId));
		}finally{
			ExecutorServiceWrapper.shutdownThreadPool(executorService, CardTokenPaymentService.class.getSimpleName());
		}
	}
	
	@Override
	public void processVNGTokenPayments(String apiMsgId) {
		ExecutorService executorService = null;
		try{
			LOGGER.info(apiMsgId,"SmartServiceEngine Scheduler", "Card Token Payment Service", "processTokenPayments", "Initiating Payments Processing", AmexLogger.Result.success, "START");
			executorService = ExecutorServiceWrapper.requestExecutor(SchedulerConstants.TOKEN_PAYMENT_PROCESS_TPS, CardTokenPaymentService.class.getSimpleName());
			PaginationDataVO<TokenPaymentRecordVO> paginationDataVO = new PaginationDataVO<TokenPaymentRecordVO>();
			paginationDataVO.setLastUpdateTime(ServiceConstants.MIN_DEFAULT_TIMESTAMP);
					
			LOGGER.info(apiMsgId, "SmartServiceEngine", "Card Token Payment Service","processTokenPayments", "Initiating Payments Processing",
					AmexLogger.Result.success, "START");
			
			boolean isFirstAttempt = true;
			boolean isContinueLoop = true;
			while(isContinueLoop){
				isContinueLoop = false; // Making it false in the beginning to avoid deadlock issues. will set to true if needed later.
				cardTokenPaymentDao.getTokenPayments(paginationDataVO); //fetching payment data from the ACE database				
				if(isFirstAttempt){
					isFirstAttempt = false;
					LOGGER.info(apiMsgId, "SmartServiceEngine Scheduler", "Card Token Payment Service", "processTokenPayments", "TOKEN PAYMENT PROCESSING", AmexLogger.Result.success, 
						 "START", "TOTAL NUMBER OF PAYMENTS TO BE PROCESSED ON" + GenericUtilities.getCurrentTimeAS_YYYY_MM_DD_T_HH_MM_SS(), 
						 String.valueOf(paginationDataVO.getTotalRecordCount()));
				}
				List<TokenPaymentRecordVO> results = paginationDataVO.getResults();
				if(!GenericUtilities.isNullOrEmpty(results)){
					String lastUpdateTime = triggerPaymentProcess(executorService, null, results, ApiConstants.TRUEBOOLEAN);
					isContinueLoop = hasMoreRecords(apiMsgId, lastUpdateTime, paginationDataVO.getTotalRecordCount());
					if(isContinueLoop && lastUpdateTime != null){
						paginationDataVO = new PaginationDataVO<TokenPaymentRecordVO>();
						paginationDataVO.setLastUpdateTime(lastUpdateTime);
					}
				}
			}
			
		}catch(Exception ex){
			LOGGER.error(apiMsgId, "SmartServiceEngine Scheduler", "Card Token Payment Service", "processTokenPayments", "Token Payment Processing Failed", 
					AmexLogger.Result.failure, "Unexpected Exception while processing Token Payments", ex);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_TKN_PAY_PROCESS_ERR_CD, TivoliMonitoring.SSE_TKN_PAY_PROCESS_ERR_MSG, apiMsgId));
		}finally{
			ExecutorServiceWrapper.shutdownThreadPool(executorService, CardTokenPaymentService.class.getSimpleName());
		}
	}
	
	@Override
	public void processIndividualTokenPayment(String partnerID,String apiMsgId,List<TokenPaymentRecordVO> tokenPaymentRecordVOList,ExecutorService executorService) {
		//ExecutorService executorService = null;
		try{
			LOGGER.info(apiMsgId,"SmartServiceEngine Scheduler", "Card Token Payment Service", "processIndividualTokenPayment", "Initiating Payments Processing", AmexLogger.Result.success, "START");
			
			//Map<String,Object> tokenDomainControlVOMap = tokenDomainRestrictionsHelper.read(partnerID);
			Map<String,Object> tokenDomainControlVOMap = new HashMap<String,Object>();
			
			Cache cache= cacheManager.getCache(ApiConstants.PARTNER_ID_CACHE);
			Element element=cache.get(ApiConstants.DEFAULTDOMAINCONTROL);
			if(element!=null){
				tokenDomainControlVOMap = (Map<String, Object>) element.getObjectValue();
			}
			
			LOGGER.info(apiMsgId, "SmartServiceEngine", "ProcessPaymentManager","processPayment", "END of  processPayment",
					AmexLogger.Result.success, "END","tokenPaymentRecordValue" ,tokenPaymentRecordVOList.get(0).toString(),
					"tokenDomainControlVOMapIndividualData",tokenDomainControlVOMap.toString());
			
			if(tokenDomainControlVOMap != null && tokenDomainControlVOMap.size()>0){			
					if(!GenericUtilities.isNullOrEmpty(tokenPaymentRecordVOList)){
						triggerPaymentProcess(executorService, tokenDomainControlVOMap, tokenPaymentRecordVOList, ApiConstants.FALSEBOOLEAN);					
					}
			}else{
				LOGGER.error(apiMsgId, "SmartServiceEngine Scheduler", "Card Token Payment Service", "processIndividualTokenPayment", "Token Payment Processing Stopped", 
						AmexLogger.Result.failure, "No Domain Controls Found");
			}
		}catch(Exception ex){
			LOGGER.error(apiMsgId, "SmartServiceEngine Scheduler", "Card Token Payment Service", "processIndividualTokenPayment", "Token Payment Processing Failed", 
					AmexLogger.Result.failure, "Unexpected Exception while processing Token Payments", ex);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_TKN_PAY_PROCESS_ERR_CD, TivoliMonitoring.SSE_TKN_PAY_PROCESS_ERR_MSG, apiMsgId));
		}finally{
			//ExecutorServiceWrapper.shutdownThreadPool(executorService, CardTokenPaymentService.class.getSimpleName());
		}
	}
	
	@Override
	public void processVNGIndividualTokenPayment(String apiMsgId, List<TokenPaymentRecordVO> tokenPaymentRecordVOList,ExecutorService executorService) {
		try{
			LOGGER.info(apiMsgId,"SmartServiceEngine Scheduler", "Card Token Payment Service", "processIndividualTokenPayment", "Initiating Payments Processing", AmexLogger.Result.success, "START");
	
			if(!GenericUtilities.isNullOrEmpty(tokenPaymentRecordVOList)){
				triggerPaymentProcess(executorService, null, tokenPaymentRecordVOList, ApiConstants.TRUEBOOLEAN);					
			}
		}catch(Exception ex){
			LOGGER.error(apiMsgId, "SmartServiceEngine Scheduler", "Card Token Payment Service", "processIndividualTokenPayment", "Token Payment Processing Failed", 
					AmexLogger.Result.failure, "Unexpected Exception while processing Token Payments", ex);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_TKN_PAY_PROCESS_ERR_CD, TivoliMonitoring.SSE_TKN_PAY_PROCESS_ERR_MSG, apiMsgId));
		}finally{
			//ExecutorServiceWrapper.shutdownThreadPool(executorService, CardTokenPaymentService.class.getSimpleName());
		}
	}
	
	private String triggerPaymentProcess(ExecutorService executorService, Map<String,Object> tokenDomainControlVOMap, List<TokenPaymentRecordVO> results, boolean vngFlag) {
		String apiMsgId = ThreadLocalManager.getApiMsgId();
		LOGGER.info(apiMsgId, "SmartServiceEngine Scheduler", "Card Token Payment Service", "processTokenPayments", "Initiating processPayments", AmexLogger.Result.success, "START",
				"Results to process", String.valueOf(results.size()) , "tokenDomainControlVOMap",tokenDomainControlVOMap != null ? tokenDomainControlVOMap.toString() : "[]");
		
		String lastUpdateTime = null;
		List<TokenPaymentRecordVO> payments = new ArrayList<TokenPaymentRecordVO>();
		
		//TokenDomainControlVO tokenDomainControlVO = null;
		for(TokenPaymentRecordVO tokenPaymentRecordVO : results){
			payments.add(tokenPaymentRecordVO);
			LOGGER.info(apiMsgId, "SmartServiceEngine Scheduler", "Card Token Payment Service", "processTokenPayments", "Initiating processPayments", 
					AmexLogger.Result.success, "START","partnerId",tokenPaymentRecordVO.getPrtrMacId(),"partnerName", partnerIdMapping.getPartnerName(tokenPaymentRecordVO.getPrtrMacId().trim(), apiMsgId));
			
			//tokenDomainControlVO = (TokenDomainControlVO)tokenDomainControlVOMap.get(tokenPaymentRecordVO.getPrtrMacId());
			
			if(payments.size() >= ServiceConstants.TOKEN_PAYMENTS_PER_THREAD_COUNT){
				if(!vngFlag)
				{
					executorService.submit(new TokenPaymentHandler(apiMsgId, tivoliMonitoring, issueTokenService, tokenEmailHelper, 
						cardTokenDetailsDAO, statusUpdateHelper, tokenDomainControlVOMap, cardTokenPaymentDao, payments));
				}
				else
				{
					executorService.submit(new VNGTokenHandler(apiMsgId, tivoliMonitoring, vngTokenEnrollmentHelper,  
							vngCardTokenDetailsDAO, ApiConstants.TRUEBOOLEAN, vngStatusUpdateHelper, payments, vngTokenEmailHelper));
					
				}
				payments = new ArrayList<TokenPaymentRecordVO>();
			}
			lastUpdateTime = tokenPaymentRecordVO.getLastUpdateTime(); //resetting lastUpdateTime for fetching next set of records
		}
		if(payments.size() > 0){
			if(!vngFlag)
			{
				executorService.submit(new TokenPaymentHandler(apiMsgId, tivoliMonitoring, issueTokenService, tokenEmailHelper, 
					cardTokenDetailsDAO, statusUpdateHelper, tokenDomainControlVOMap, cardTokenPaymentDao, payments));
			}
			else
			{
				executorService.submit(new VNGTokenHandler(apiMsgId, tivoliMonitoring, vngTokenEnrollmentHelper,  
						vngCardTokenDetailsDAO, ApiConstants.TRUEBOOLEAN, vngStatusUpdateHelper, payments, vngTokenEmailHelper));
				
			}
		}
		return lastUpdateTime;
	}

	private boolean hasMoreRecords(String apiMsgId, String lastUpdateTime, int totalRecordCount){
		boolean isContinueLoop = false;
		if(totalRecordCount > SchedulerConstants.DEFAULT_PAGINATION_COUNT && !GenericUtilities.isNullOrEmpty(lastUpdateTime)){
			isContinueLoop = true;
		}
		return isContinueLoop;
	}
	
}