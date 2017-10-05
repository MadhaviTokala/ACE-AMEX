package com.americanexpress.smartserviceengine.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.callable.NocStatusCallable;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.AccountDetailsVO;
import com.americanexpress.smartserviceengine.dao.PaginationDAO;
import com.americanexpress.smartserviceengine.service.NocStatusService;

public class NocStatusServiceImpl implements NocStatusService{

	private static final AmexLogger logger = AmexLogger.create(NocStatusServiceImpl.class);

	@Autowired
	private PaginationDAO paginationDAO;

	@Autowired
	private NocStatusCallable nocStatusCallable;

	@Autowired
    private TivoliMonitoring tivoliMonitoring;


    /*@Autowired
    @Qualifier("nocStatusThreadPoolExecutor")
    private SpringThreadPooledExecutor springThreadPoolExecutor;*/

    @Override
	@SuppressWarnings("unchecked")
	public void getNocData(String eventId) throws SSEApplicationException{
    	logger.info(eventId, "SmartServiceEngine", "NocStatusServiceImpl", "getNocData", "Start of  NocStatusServiceImpl", AmexLogger.Result.success, "Start");
    	Map<String, Object> inMap = new HashMap<String, Object>();
    	boolean isContinueLoop = true;
    	boolean isFirstTime = true;
    	String lastUpdatedTimestamp = StringUtils.EMPTY;
    	int recordCount = 0;
        long actualTimeToSleep = 0;

       // String intacctPartnerId = getIntacctPartnerId(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.SSE_PAYVE_PARTNERID_MAP));
        int paginationCount = Integer.valueOf(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.PAGINATION_COUNT));

        //Get ExecutorService from SpringThreadPoolExecutor utility class
        //ExecutorService executor = springThreadPoolExecutor.getExecutorService();
        int tpsCount =
                Integer.valueOf(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.EMM_TPS_COUNT));

        actualTimeToSleep = 1000/tpsCount;

        logger.info("Time to Sleep: " +actualTimeToSleep);

        //ExecutorService executor = Executors.newFixedThreadPool(tpsCount);

        //create a list to hold the Future object associated with Callable
        //List<Future<String>> futureList = new ArrayList<Future<String>>();
    	try{
        	while(isContinueLoop){
        		if(isFirstTime){
        			inMap.put(SchedulerConstants.IN_LST_UPDT_TS, SchedulerConstants.DEFAULT_LOW_TS);
        			isFirstTime = false;
        		}else{
        			inMap.put(SchedulerConstants.IN_LST_UPDT_TS, lastUpdatedTimestamp);
        		}

            	Map<String, Object> outMap = paginationDAO.execute(inMap, eventId);
        		if(outMap != null && !outMap.isEmpty()){
            		Object object = outMap.get(SchedulerConstants.RESP_CD);
            		String responseCode = object != null ? object.toString().trim() : StringUtils.EMPTY;
            		if(SchedulerConstants.RESPONSE_CODE_SUCCESS.equals(responseCode)){
            			List<AccountDetailsVO> accountDetailsVOs = (List<AccountDetailsVO>) outMap.get(SchedulerConstants.RESULT_SET);
            			object = outMap.get(SchedulerConstants.OUT_COUNT);
            			recordCount = object != null ? Integer.valueOf(object.toString().trim()) : 0;
                        if(recordCount > paginationCount){
                            isContinueLoop = true;
                        }else{
                            isContinueLoop = false;
                        }
                        if(accountDetailsVOs != null && !accountDetailsVOs.isEmpty()){
                            //isContinueLoop = true;
            				for (AccountDetailsVO accountDetailsVO : accountDetailsVOs) {
            					//if(intacctPartnerId.equals(accountDetailsVO.getPartnerMacId())){//Commented it as part of sage release as we don't need to restrict it only to Intacct
            						//submit Callable tasks to be executed by thread pool
                					/*Future<String> future = executor.submit(new NocStatusCallable(accountDetailsVO, updateNOCStatusDAO, processAchPymtInqHelper, tivoliMonitoring, eventId));*/
                					 //add Future to the list, we can get return value using Future
                					/*futureList.add(future);*/
                					long startTimeStamp = System.currentTimeMillis();
                					Date startTime = new Date();
                					lastUpdatedTimestamp = nocStatusCallable.call(accountDetailsVO, eventId);
                					long endTimeStamp = System.currentTimeMillis();
                					Date endTime = new Date();
                					long totalTime = endTimeStamp - startTimeStamp;
                					logger.info(eventId, "SmartServiceEngine", "NocStatusServiceImpl", "getNocData", "To upadte NOC Status",
                							AmexLogger.Result.success, "","Start Time", startTime.toString(), "End Time", endTime.toString(), "Total time taken to update NOC Status", String.valueOf(totalTime)+" milliseconds");
                					if(totalTime < actualTimeToSleep){
                						Thread.sleep(actualTimeToSleep-totalTime);
                					}
                					/*if(futureList != null && !futureList.isEmpty()){
                					Future<String> futureObject = futureList.get(futureList.size()-1);
                					lastUpdatedTimestamp = futureObject.get();
                					}else{
                					lastUpdatedTimestamp = SchedulerConstants.DEFAULT_LOW_TS;
                					}*/
            					//}
            				}
            			}else{
            				logger.debug(eventId, "SmartServiceEngine", "NocStatusServiceImpl", "getNocData",
                					"Check out for the outMap for SP E3GMR079", AmexLogger.Result.failure, "Result set is Empty");
            			}
            		}else{
            			isContinueLoop = false;
            			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_079_ERR_CD, TivoliMonitoring.SSE_SP_079_ERR_MSG, eventId));
            			logger.debug(eventId, "SmartServiceEngine", "NocStatusServiceImpl", "getNocData",
            					"Check out for the outMap for SP E3GMR079", AmexLogger.Result.failure, "responseCode"+responseCode);
            		}
            	}else{
            		isContinueLoop = false;
            		logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_079_ERR_CD, TivoliMonitoring.SSE_SP_079_ERR_MSG, eventId));
        			logger.debug(eventId, "SmartServiceEngine", "NocStatusServiceImpl", "getNocData",
        					"Check out for the outMap for SP E3GMR079", AmexLogger.Result.failure, "outMap is null");
            	}
        	}
    	}catch(Exception exception){
			logger.error(eventId, "SmartServiceEngine", "NocStatusServiceImpl", "getNocData",
					"Exception while processing NOC Status Service", AmexLogger.Result.failure, "Failed to execute getNocData",exception, "ErrorMsg:", exception.getMessage());
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, eventId));
			throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), exception);
        }/*finally{
            executor.shutdown();
        }*/
        logger.info(eventId, "SmartServiceEngine", "NocStatusServiceImpl", "getNocData", "End of  NocStatusServiceImpl", AmexLogger.Result.success, "End");
    }

    private String getIntacctPartnerId(String partnerId){
    	return partnerId.substring(0, partnerId.indexOf(ApiConstants.CHAR_COLON));
    }
}
