package com.americanexpress.smartserviceengine.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.callable.ScreenDataCallable;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.ScreenDataVO;
import com.americanexpress.smartserviceengine.dao.ScreedataDAO;
import com.americanexpress.smartserviceengine.dao.UpdateScreedataDAO;
import com.americanexpress.smartserviceengine.helper.ScreenDataHelper;
import com.americanexpress.smartserviceengine.service.ScreenDataService;

public class ScreenDataServiceImpl implements ScreenDataService {

    private static AmexLogger logger = AmexLogger.create(ScreenDataServiceImpl.class);

    @Autowired
    private ScreenDataHelper screenDataHelper;

    @Autowired
    private ScreedataDAO screedataDAO;

    @Autowired
    private UpdateScreedataDAO updateScreedataDAO;

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    @Override
	public void getScreenData(String eventId) throws SSEApplicationException {

        logger.info(eventId, "SmartServiceEngine", "ScreenDataServiceImpl", "getScreenData",
            "End of ScreenDataServiceImpl", AmexLogger.Result.success, "End");

        String timeStamp = new String(SchedulerConstants.EMPTY_TIMESTAMP);

        try {
            logger.info(eventId, "SmartServiceEngine", "AchPaymentStatusService", "getAchPaymentData",
                "Getting Payemnt details form DB ", AmexLogger.Result.success, "Start");

            fetchAndProcessRecords(timeStamp, eventId);

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchAndProcessRecords(String timeStamp, String eventId) {

        logger.info(eventId, "SmartServiceEngine", "ScreenDataServiceImpl", "fetchAndProcessRecords",
            "Start of Fetch and Process Records", AmexLogger.Result.success, "Start", "timeStamp " + timeStamp);

        List<ScreenDataVO> screenDataVOResultSet = null;

        Map<String, Object> inMap = new HashMap<String, Object>();
        inMap.put(SchedulerConstants.IN_LST_UPDT_TS, timeStamp);
        Map<String, Object> outMap = new HashMap<String, Object>();

        try {

            outMap = screedataDAO.execute(inMap, eventId);

        } catch (Exception e) {

            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_097_ERR_CD, TivoliMonitoring.SSE_SP_097_ERR_MSG, eventId));
            logger.error(eventId, "SmartServiceEngine", "ScreenDataServiceImpl", "screedataDAO",
                "Exception while Calling TEST0097 ", AmexLogger.Result.failure, "Failure in screedataDAO", e,
                "ErrorMsg", e.getMessage());

        }

        if (outMap != null && null != outMap.get(SchedulerConstants.RESP_CD)
            && outMap.get(SchedulerConstants.RESP_CD).toString().trim().equalsIgnoreCase(SchedulerConstants.SSEBR000)) {

            screenDataVOResultSet = (List<ScreenDataVO>) outMap.get(SchedulerConstants.RESULT_SET);

            Object object = outMap.get(SchedulerConstants.OUT_COUNT);
            int recCount = object != null ? Integer.valueOf(object.toString().trim()) : 0;

            int zero = 0;
            if (null != screenDataVOResultSet) {
                // String reqId = eventId.substring(0, 20);
                callScreenDataAndUpdateDAO(screenDataVOResultSet, eventId);

                ScreenDataVO screenDataVO = null;
                if (recCount > 20) {
                    screenDataVO = screenDataVOResultSet.get(screenDataVOResultSet.size() - 1);
                    timeStamp = screenDataVO.getLastUpdatedTimeStamp();
                } else {
                    timeStamp = SchedulerConstants.HIGH_TIMESTAMP;
                }
            }

            if ((null != screenDataVOResultSet && zero != screenDataVOResultSet.size())) {
                fetchAndProcessRecords(timeStamp, eventId);
            }

        } else {
            logger.debug(eventId, "SmartServiceEngine", "ScreenDataServiceImpl", "getPaymentData",
                "Check out for the outMap for SP TEST0097", AmexLogger.Result.failure,
                "outMap is null or check for Error RESPONSE_CODE");
        }

        logger.info(eventId, "SmartServiceEngine", "ScreenDataServiceImpl", "fetchAndProcessRecords",
            "End of Fetch and Process Records No more records form DB", AmexLogger.Result.success, "End", "timeStamp ="
                + timeStamp);

        return outMap;

    }

    private void callScreenDataAndUpdateDAO(List<ScreenDataVO> screenDataVOResultSet, String eventId) {

        if (screenDataVOResultSet != null && !screenDataVOResultSet.isEmpty()){

            logger.info(eventId, "SmartServiceEngine", "ScreenDataServiceImpl", "callScreenDataAndUpdateDAO",
                "Start Checking Screendata And Updating DB ", AmexLogger.Result.success, "Start", "ResultSet= ",
                screenDataVOResultSet.toString());

            int tpsCount = Integer.valueOf(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.BRIDGER_TPS_COUNT));
            ExecutorService executor = Executors.newFixedThreadPool(tpsCount);

          //  Iterator<ScreenDataVO> iterator = screenDataVOResultSet.iterator();
            List<Future<String>> futureList = new ArrayList<Future<String>>();

            for (ScreenDataVO objScreenDataVO : screenDataVOResultSet) {
                // }
                // while (iterator.hasNext()) {
                // ScreenDataVO objScreenDataVO = iterator.next();

                // submit Callable tasks to be executed by thread pool
                Future<String> future =
                    executor.submit(new ScreenDataCallable(objScreenDataVO, updateScreedataDAO, screenDataHelper, tivoliMonitoring, eventId));
                // add Future to the list, we can get return value using Future
                futureList.add(future);

            }

        } else {

            logger.debug(eventId, "SmartServiceEngine", "ScreenDataServiceImpl", "callScreenDataAndUpdateDAO",
                "End of  callScreenDataAndUpdateDAO ", AmexLogger.Result.failure, "Result set could be null");
        }

        logger.info(eventId, "SmartServiceEngine", "ScreenDataServiceImpl", "callScreenDataAndUpdateDAO",
            "END of callScreenDataAndUpdateDAO ", AmexLogger.Result.success, "END", "ResultSet= ");

    }

}
