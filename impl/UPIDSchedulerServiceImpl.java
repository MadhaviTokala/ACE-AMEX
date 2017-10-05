package com.americanexpress.smartserviceengine.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.callable.PaymentStatusDetailsCallable;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.dao.GetPaymentDtlsDAO;
import com.americanexpress.smartserviceengine.service.UPIDSchedulerService;

public class UPIDSchedulerServiceImpl implements UPIDSchedulerService {

    AmexLogger logger = AmexLogger.create(UPIDSchedulerServiceImpl.class);

    @Resource
    private GetPaymentDtlsDAO getPaymentDtlsDAO;

    @Resource
    private PaymentStatusDetailsCallable paymentStatusDetailsCallable;

    @Resource
    private TivoliMonitoring tivoliMonitoring;
    


    /*@Autowired
    @Qualifier("upidThreadPoolExecutor")
    private SpringThreadPooledExecutor springThreadPoolExecutor;*/

    @Override
    public void updatePaymentStatus(String eventId) throws SSEApplicationException {

        logger.debug(eventId, "SmartServiceEngine", "UPIDSchedulerServiceImpl", "updatePaymentStatus",
            "Start of  PaymentStatusService", AmexLogger.Result.success, "");

        boolean isContinueLoopMicro = true;
        boolean isContinueLoopActual = true;
        boolean isFirstTime = true;
       long actualTimeToSleep = 0;
        String lastUpdatedTimestamp_mic = StringUtils.EMPTY;
        String lastUpdatedTimestamp_act = StringUtils.EMPTY;

        //Get ExecutorService from SpringThreadPoolExecutor utility class
        //ExecutorService executor = springThreadPoolExecutor.getExecutorService();
        int tpsCount =
                Integer.valueOf(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.EMM_TPS_COUNT));
        
        actualTimeToSleep = 1000/tpsCount;
        
        logger.info("Time to Sleep: " +actualTimeToSleep);
        
       // ExecutorService executor = Executors.newFixedThreadPool(tpsCount);

        int paginationCount = Integer.valueOf(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.PAGINATION_COUNT));

        try {
            while (isContinueLoopMicro || isContinueLoopActual) {

                Map<String, Object> inMap = createInMap(lastUpdatedTimestamp_act, lastUpdatedTimestamp_mic, isFirstTime);
                     isFirstTime = false;

                logger.debug(eventId, "SmartServiceEngine", "UPIDSchedulerServiceImpl", "updatePaymentStatus",
                    "inMap to 77 SP", AmexLogger.Result.success, "", "inMap", inMap.toString());

                Map<String, Object> outMap = getPaymentDtlsDAO.execute(inMap, eventId);

                logger.debug(eventId, "SmartServiceEngine", "UPIDSchedulerServiceImpl", "updatePaymentStatus",
                    "outMap from 77 SP", AmexLogger.Result.success, "", "outMap", outMap.toString());

                if (null != outMap && !outMap.isEmpty()) {
                    logger.info("outmap size:" + outMap.size());

                    String responseCode = StringUtils.stripToEmpty((String)outMap.get(SchedulerConstants.RESP_CD));

                    if (SchedulerConstants.RESPONSE_CODE_SUCCESS_MICR.equals(responseCode)
                            || SchedulerConstants.RESPONSE_CODE_SUCCESS_ACH.equals(responseCode)) {

                        int recordCountMicro = getIntValueFromMap(outMap, SchedulerConstants.OU_MR_COUNT);
                        int recordCountActual = getIntValueFromMap(outMap, SchedulerConstants.OU_ACH_COUNT);

                        logger.info("recordCountMicro:" + recordCountMicro);
                        logger.info("recordCountActual:" + recordCountActual);

                        isContinueLoopMicro = haveMoreRows(recordCountMicro, paginationCount);
                        isContinueLoopActual = haveMoreRows(recordCountActual, paginationCount);

                       
                        if(recordCountMicro > 0 ) {
                        	logger.info("Call EMM Audit Trail service and Update UPID Status for Micro Payment");
                        	lastUpdatedTimestamp_mic = updateUPIDStatus(outMap, eventId, true, actualTimeToSleep);
                       }
                        if(recordCountActual > 0) {
                        	logger.info("Call EMM Audit Trail service and Update  UPID Status for Actual Payment");
                        lastUpdatedTimestamp_act = updateUPIDStatus(outMap, eventId, false, actualTimeToSleep);
                        }

                    } else {
                        isContinueLoopMicro = false;
                        isContinueLoopActual = false;
                        logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_075_ERR_CD,
                            TivoliMonitoring.SSE_SP_075_ERR_MSG, eventId));
                        logger.debug(eventId, "SmartServiceEngine", "UPIDSchedulerServiceImpl",
                            "updatePaymentStatus", "Check resultset map for SP TEST0075", AmexLogger.Result.failure,
                            "responseCode" + responseCode);
                    }
                } else {
                    isContinueLoopMicro = false;
                    isContinueLoopActual = false;
                    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_075_ERR_CD,
                        TivoliMonitoring.SSE_SP_075_ERR_MSG, eventId));
                    logger.debug(eventId, "SmartServiceEngine", "UPIDSchedulerServiceImpl",
                        "updatePaymentStatus", "Check resultset map for SP TEST0075", AmexLogger.Result.failure,
                            "OutMap is null");
                }
            }
        } catch (Exception e) {
            logger.error(eventId, "SmartServiceEngine", "UPIDSchedulerServiceImpl", "updatePaymentStatus",
                "Exception while processing UPID Scheduler Service call", AmexLogger.Result.failure,
                "Failure in UPID Scheduler service", e, "ErrorMsg", e.getMessage());
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, eventId));
            throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), e);
        }/*finally{
            executor.shutdown();
        }*/
        logger.info(eventId, "SmartServiceEngine", "UPIDSchedulerServiceImpl", "updatePaymentStatus",
            "End of UPIDSchedulerServiceImpl", AmexLogger.Result.success, "End");

    }

    private boolean haveMoreRows(int actualCount, int paginationCount){
        boolean haveMoreRows = false;
        if (actualCount > paginationCount) {
            haveMoreRows = true;
        } else {
            haveMoreRows = false;
        }
        return haveMoreRows;
    }

    private Map<String, Object> createInMap(String lastUpdatedTimestamp_act, String lastUpdatedTimestamp_mic, boolean isFirstTime){

        Map<String, Object> inMap = new HashMap<String, Object>();

        if (isFirstTime) {
            inMap.put(SchedulerConstants.IN_LST_UPDT_TS_MP, SchedulerConstants.DEFAULT_LOW_TS);
            inMap.put(SchedulerConstants.IN_LST_UPDT_TS_AC, SchedulerConstants.DEFAULT_LOW_TS);
        } else {
            inMap.put(SchedulerConstants.IN_LST_UPDT_TS_MP, getLastUpdTimestamp(lastUpdatedTimestamp_mic));
            inMap.put(SchedulerConstants.IN_LST_UPDT_TS_AC, getLastUpdTimestamp(lastUpdatedTimestamp_act));
        }
        return inMap;
    }

    private String getLastUpdTimestamp(String lastUpdatedTimestamp){
        String lastUpdTimestamp = StringUtils.EMPTY;
        if (lastUpdatedTimestamp != null && !lastUpdatedTimestamp.isEmpty()) {
            lastUpdTimestamp = lastUpdatedTimestamp;
        } else {
            lastUpdTimestamp = SchedulerConstants.HIGH_TIMESTAMP;
        }
        return lastUpdTimestamp;
    }

    private int getIntValueFromMap(Map<String, Object> outMap, String paramName){
        Object object = outMap.get(paramName);
        int intValue = object != null ? Integer.valueOf(object.toString().trim()) : 0;
        return intValue;
    }

    @SuppressWarnings("unchecked")
    private String updateUPIDStatus(Map<String, Object> outMap, String eventId, boolean isMicro,
    		long actualTimeToSleep) throws Exception{
        String lastUpdatedTimestamp = StringUtils.EMPTY;
        /*List<Future<String>> futureList = new ArrayList<Future<String>>();*/

        logger.debug(eventId, "SmartServiceEngine", "UPIDSchedulerServiceImpl",
            "updateUPIDStatus", "start executing the updateUPIDStatus method ",
            AmexLogger.Result.success, "Retrieve Micro and Actual pament records from Resultset and initiate EMM service call");

        List<Object> paymentList = null;

        if(isMicro){
            paymentList = (List<Object>) outMap.get(SchedulerConstants.RESULT_SET2);
        }else{
            paymentList = (List<Object>) outMap.get(SchedulerConstants.RESULT_SET1);
        }

        if (paymentList != null && !paymentList.isEmpty()) {

            for (Object paymentVO : paymentList) {

                // submit Callable tasks to be executed by thread pool
                /*Future<String> future =executor.submit(new PaymentStatusDetailsCallable(paymentVO,updatePaymentStatusDetailsDAO, processPaymentStatusInqHelper, tivoliMonitoring,
                            eventId));*/
                // add Future to the list, we can get return value using Future
                /*futureList.add(future);*/
            	final long startTimeStamp = System.currentTimeMillis();
            	Date startTime = new Date();
            	lastUpdatedTimestamp = paymentStatusDetailsCallable.call(paymentVO, eventId);
            	Date endTime = new Date();
				final long endTimeStamp = System.currentTimeMillis();
				
				long totalTime = endTimeStamp - startTimeStamp;
				logger.info(eventId, "SmartServiceEngine", "UPIDSchedulerServiceImpl", "updatePaymentStatus", "", 
						AmexLogger.Result.success, "","startTime", startTime.toString(), "endTime", endTime.toString(), "time_Taken_for_NOC_Status", String.valueOf(totalTime)+" milliseconds");
				if(totalTime < actualTimeToSleep){
		        	logger.info("Sleeping for ms: " + (actualTimeToSleep-totalTime));

					Thread.sleep(actualTimeToSleep-totalTime);
				}
            	
            }

           /* if (futureList != null && !futureList.isEmpty()) {
                Future<String> futureObject = futureList.get(futureList.size() - 1);
                lastUpdatedTimestamp = futureObject.get();
            } else {
                lastUpdatedTimestamp = SchedulerConstants.HIGH_TIMESTAMP;
            }*/
        } else {
            lastUpdatedTimestamp = SchedulerConstants.HIGH_TIMESTAMP;
            if(isMicro){
                logger.debug(eventId, "SmartServiceEngine", "UPIDSchedulerServiceImpl",
                    "updatePaymentStatus", "Check outMap for the microPymt cursor for SP TEST0075",
                    AmexLogger.Result.failure, "MicroPymt Result set is Empty");
            }else{
                logger.debug(eventId, "SmartServiceEngine", "UPIDSchedulerServiceImpl",
                    "updatePaymentStatus", "Check outMap for the ActualPymt cursor for SP TEST0075",
                    AmexLogger.Result.failure, "ActualPymt Result set is Empty");
            }
        }

        return lastUpdatedTimestamp;
    }
}
