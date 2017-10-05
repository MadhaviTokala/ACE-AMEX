package com.americanexpress.smartserviceengine.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.CardMemberDtlVO;
import com.americanexpress.smartserviceengine.common.vo.FailureMailVO;
import com.americanexpress.smartserviceengine.common.vo.MicroDebitConfVO;
import com.americanexpress.smartserviceengine.common.vo.RemEmailVO;
import com.americanexpress.smartserviceengine.common.vo.SuccessMailVO;
import com.americanexpress.smartserviceengine.dao.EmailNotificationFetchDAO;
import com.americanexpress.smartserviceengine.dao.GetCardOnboardingEmailDAO;
import com.americanexpress.smartserviceengine.service.CardOnboardingEmailService;
import com.americanexpress.smartserviceengine.util.EmailNotificationUtil;

public class CardOnboardingEmailServiceImpl implements
		CardOnboardingEmailService {

	
    AmexLogger logger = AmexLogger.create(CardOnboardingEmailServiceImpl.class);

    @Autowired
    @Qualifier("getCardOnboardingEmailDAO")
    private GetCardOnboardingEmailDAO getCardOnboardingEmailDAO;

    @Autowired
    private EmailNotificationUtil emailNotificationUtil;

    @Autowired
    private TivoliMonitoring tivoliMonitoring;
    
    
	@Override
	public void emailService(String eventId) throws SSEApplicationException {
        logger.info(eventId, "SmartServiceEngine", "Card Onboarding Email Service", "CardOnboardingEmailServiceImpl : emailService",
                "Start of  CardOnboardingEmailService", AmexLogger.Result.success, "Start");
            final long startTimeStamp = System.currentTimeMillis();

            HashMap<String, Object> inMap = new HashMap<String, Object>();
            /* String propPartnerIdMap = EnvironmentPropertiesUtil.getProperty(ApiConstants.SSE_PAYVE_PARTNERID_MAP);
            String[] partnerIdMapList = propPartnerIdMap.split(ApiConstants.CHAR_COLON);
            String partnerId=partnerIdMapList[0];*/

           // inMap.put(ApiConstants.IN_PRTR_MAC_ID, partnerId);
            try {
                Map<String, Object> outMap = getCardOnboardingEmailDAO.execute(inMap, eventId);
             if(outMap!=null){
                if (outMap.get(ApiConstants.SP_RESP_CD) != null
                        && ApiConstants.SP_SUCCESS_SSEIN000.equals(StringUtils.stripToEmpty((String) outMap
                            .get(ApiConstants.SP_RESP_CD)))) {

                    List<List<CardMemberDtlVO>> cmDetailList = (List<List<CardMemberDtlVO>>) outMap.get(SchedulerConstants.RESULT_SET);

                    if (cmDetailList != null && !cmDetailList.isEmpty()) {

                    	logger.info(eventId, "SmartServiceEngine", "Card Onboarding Email Notification Service",
                                "CardOnboardingEmailServiceImpl : emailService", "Start of email fetch Service",
                                AmexLogger.Result.success, "", "Number of CM records for email notification", String.valueOf(cmDetailList.size()));

                        emailNotificationUtil.cardOnboardingEmail(cmDetailList, SchedulerConstants.WELCOME_RSVP, eventId);

                    }else{
                    	logger.info(eventId, "SmartServiceEngine", "Card Onboarding Email Service",
                                "CardOnboardingEmailServiceImpl : emailService", "Start of email fetch Service",
                                AmexLogger.Result.success, "", "Card Onboarding email Resultset is Empty ", String.valueOf(cmDetailList.size()));
                    }

                  
                } else {
                	logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_151_ERR_CD, TivoliMonitoring.SSE_SP_151_ERR_MSG, eventId));
                    logger.error(eventId, "SmartServiceEngine", "Card Onboarding Email Service", "CardOnboardingEmailServiceImpl - emailService", " SP E3GMR151 was NOT successful",
                        AmexLogger.Result.failure, "", ApiConstants.SQLCODE_PARM,
                        (String) outMap.get(ApiConstants.SQLCODE_PARM), "resp_code",
                        (String) outMap.get(ApiConstants.SP_RESP_CODE), "resp_msg",
                        (String) outMap.get(ApiConstants.SP_RESP_MSG));
                }
             }else{
                 logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_151_ERR_CD, TivoliMonitoring.SSE_SP_151_ERR_MSG, eventId));
                 logger.debug(eventId, "SmartServiceEngine", "Card Onboarding Email Service",
                     "CardOnboardingEmailServiceImpl : emailService", "End of Get Organization Details SP E3GMR151",
                     AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
             }


            } catch (Exception e) {
                logger.error(eventId, "SmartServiceEngine", "Card Onboarding Email Service", "CardOnboardingEmailServiceImpl : emailService",
                    "Exception while generating mails ", AmexLogger.Result.failure, "", e, "ErrorMsg", e.getMessage());
            }
            final long endTimeStamp = System.currentTimeMillis();
            logger.info(eventId,"SmartServiceEngine", "Card Onboarding Email Service", "CardOnboardingEmailServiceImpl : emailService",
                "Time taken to execute emailService",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
            ;
            logger.info(eventId, "SmartServiceEngine", "Card Onboarding Email Service", "CardOnboardingEmailServiceImpl : emailService",
                "End of  emailService", AmexLogger.Result.success, "END");
        }


}
