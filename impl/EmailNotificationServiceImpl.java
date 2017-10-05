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
import com.americanexpress.smartserviceengine.common.vo.CardMemberDtlRemainderVO;
import com.americanexpress.smartserviceengine.common.vo.FailureMailVO;
import com.americanexpress.smartserviceengine.common.vo.MicroDebitConfVO;
import com.americanexpress.smartserviceengine.common.vo.RemEmailVO;
import com.americanexpress.smartserviceengine.common.vo.SuccessMailVO;
import com.americanexpress.smartserviceengine.dao.EmailNotificationFetchDAO;
import com.americanexpress.smartserviceengine.dao.GetCardRsvpRemainderEmailDAO;
import com.americanexpress.smartserviceengine.service.EmailNotificationService;
import com.americanexpress.smartserviceengine.util.EmailNotificationUtil;
import com.americanexpress.smartserviceengine.common.vo.ABODtlReminderVO;


public class EmailNotificationServiceImpl implements EmailNotificationService {

    AmexLogger logger = AmexLogger.create(EmailNotificationServiceImpl.class);

    @Autowired
    @Qualifier("emailNotificationFetchDAO")
    private EmailNotificationFetchDAO emailNotificationFetchDAO;
    
    @Autowired
    @Qualifier("getCardRsvpRemainderEmailDAO")
    private GetCardRsvpRemainderEmailDAO getCardRsvpRemainderEmailDAO;
    
    @Autowired
    private EmailNotificationUtil emailNotificationUtil;

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    @Override
    @SuppressWarnings("unchecked")
    public void emailService(String eventId) throws SSEApplicationException {

        logger.info(eventId, "SmartServiceEngine", "EmailNotificationServiceImpl", "emailService",
            "Start of  enrollService", AmexLogger.Result.success, "Start");
        final long startTimeStamp = System.currentTimeMillis();

        HashMap<String, Object> inMap = new HashMap<String, Object>();
        String propPartnerIdMap = EnvironmentPropertiesUtil.getProperty(ApiConstants.SSE_PAYVE_PARTNERID_MAP);
        String[] partnerIdMapList = propPartnerIdMap.split(ApiConstants.CHAR_COLON);
        String intacctPartnerId = partnerIdMapList[0];

        //inMap.put(ApiConstants.IN_PRTR_ID, partnerId);
        try {
            Map<String, Object> outMap = emailNotificationFetchDAO.execute(eventId);
         if(outMap!=null){
            if (outMap.get(ApiConstants.SP_RESP_CD) != null
                    && ApiConstants.SP_SUCCESS_ACESP000.equals(StringUtils.stripToEmpty((String) outMap
                        .get(ApiConstants.SP_RESP_CD)))) {

                List<List<SuccessMailVO>> enrollSuccList = (List<List<SuccessMailVO>>) outMap.get(SchedulerConstants.RESULT_SET1);

                List<List<FailureMailVO>> enrollFailList = (List<List<FailureMailVO>>) outMap.get(SchedulerConstants.RESULT_SET2);

                List<List<MicroDebitConfVO>> microDebitList = (List<List<MicroDebitConfVO>>) outMap.get(SchedulerConstants.RESULT_SET3);

                List<List<RemEmailVO>> reminderMailList = (List<List<RemEmailVO>>) outMap.get(SchedulerConstants.RESULT_SET4);

                if (enrollSuccList != null && !enrollSuccList.isEmpty()) {

                	logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                            "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                            AmexLogger.Result.success, "", "Number of records for Enrollment Success mail ", String.valueOf(enrollSuccList.size()));

                    emailNotificationUtil.enrollSucess(enrollSuccList, SchedulerConstants.ENROLL_SUCCESS, eventId, intacctPartnerId);

                }else{
                	logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                            "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                            AmexLogger.Result.success, "", "Enrollment Success mail Resultset is Empty ", String.valueOf(enrollSuccList.size()));
                }

                if (enrollFailList != null && !enrollFailList.isEmpty()) {

                    logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                            "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                            AmexLogger.Result.success, "", "Number of records for Enrollment Failure mail", String.valueOf(enrollFailList.size()));

                    emailNotificationUtil.enrollFail(enrollFailList, SchedulerConstants.ENROLL_FAILURE, eventId, intacctPartnerId);

                }else{

                    logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                            "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                            AmexLogger.Result.success, "", "Enrollment Failure mail Resultset is empty", String.valueOf(enrollFailList.size()));

                }

                if (microDebitList != null && !microDebitList.isEmpty()) {

                    logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                            "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                            AmexLogger.Result.success, "", "Number of records for Microdebit confirmation mail", String.valueOf(microDebitList.size()));

                    emailNotificationUtil.confirmMicroDebit(microDebitList, SchedulerConstants.CONFIRM_MICRO_DEBIT, eventId, intacctPartnerId);

                }else{

                    logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                            "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                            AmexLogger.Result.success, "", "Microdebit confirmation mail Resultset is empty", String.valueOf(microDebitList.size()));

                }

                if (reminderMailList != null && !reminderMailList.isEmpty()) {

                    logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                            "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                            AmexLogger.Result.success, "", "Number of records for Microdebit reminder mail", String.valueOf(reminderMailList.size()));

                    emailNotificationUtil.reminderMail(reminderMailList, SchedulerConstants.REMINDER_MAIL, eventId, intacctPartnerId);

                }else{

                    logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                            "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                            AmexLogger.Result.success, "", "Microdebit reminder mail Resultset is empty", String.valueOf(reminderMailList.size()));

                }
            } else {
            	logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_142_ERR_CD, TivoliMonitoring.SSE_SP_142_ERR_MSG, eventId));
                logger.error(eventId, "SmartServiceEngine", "Enroll Serivce", "EmailNotificationServiceImpl - emailService", " SP E3GMR142 was NOT successful",
                    AmexLogger.Result.failure, "", ApiConstants.SQLCODE_PARM,
                    (String) outMap.get(ApiConstants.SQLCODE_PARM), "resp_code",
                    (String) outMap.get(ApiConstants.SP_RESP_CODE), "resp_msg",
                    (String) outMap.get(ApiConstants.SP_RESP_MSG));
            }
         }else{
             logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_142_ERR_CD, TivoliMonitoring.SSE_SP_142_ERR_MSG, eventId));
             logger.debug(eventId, "SmartServiceEngine", "Email Notification Service",
                 "EmailNotificationServiceImpl : emailService", "End of Get Organization Details SP TEST0142",
                 AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
         }
         
         /*
          * To process Card accounts for sending Remainder RSVP Email
          */
         inMap = new HashMap<String, Object>();
         //inMap.put(ApiConstants.IN_PRTR_MAC_ID, partnerId);

         outMap = getCardRsvpRemainderEmailDAO.execute(inMap, eventId);

         if(outMap!=null){
             if (outMap.get(ApiConstants.SP_RESP_CD) != null
                     && ApiConstants.SP_SUCCESS_SSEIN000.equals(StringUtils.stripToEmpty((String) outMap
                         .get(ApiConstants.SP_RESP_CD)))) {

                 List<List<CardMemberDtlRemainderVO>> cmRemainderRsvpList = (List<List<CardMemberDtlRemainderVO>>) outMap.get(SchedulerConstants.CM_DTL);

               if (cmRemainderRsvpList != null && !cmRemainderRsvpList.isEmpty()) {

                     logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                             "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                             AmexLogger.Result.success, "", "Number of records for Card RSVP reminder mail", String.valueOf(cmRemainderRsvpList.size())
                             ,"Outmap" , cmRemainderRsvpList.toString());

                     emailNotificationUtil.processCardRsvpRemainderEmail(
                    		 cmRemainderRsvpList, SchedulerConstants.CARD_REMAINDER_RSVP_MAIL, eventId);

                 }else{

                     logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                             "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                             AmexLogger.Result.success, "", "Card RSVP reminder mail Resultset is empty", String.valueOf(cmRemainderRsvpList.size()));

                 }
             } else {
             	logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_156_ERR_CD, TivoliMonitoring.SSE_SP_156_ERR_MSG, eventId));
                 logger.error(eventId, "SmartServiceEngine", "Enroll Serivce", "EmailNotificationServiceImpl - emailService", " SP E3GMR156 was NOT successful",
                     AmexLogger.Result.failure, "", ApiConstants.SQLCODE_PARM,
                     (String) outMap.get(ApiConstants.SQLCODE_PARM), "resp_code",
                     (String) outMap.get(ApiConstants.SP_RESP_CODE), "resp_msg",
                     (String) outMap.get(ApiConstants.SP_RESP_MSG));
             }
          }else{
              logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_156_ERR_CD, TivoliMonitoring.SSE_SP_156_ERR_MSG, eventId));
              logger.debug(eventId, "SmartServiceEngine", "Email Notification Service",
                  "EmailNotificationServiceImpl : emailService", "End of Get Card RSVP Remainder Email SP E3GMR156",
                  AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
          }
         
         /**
          * Sprint41 : process ABO reminder Email - START
          */
         if(outMap!=null) {
        	 if (outMap.get(ApiConstants.SP_RESP_CD) != null
                     && ApiConstants.SP_SUCCESS_SSEIN000.equals(StringUtils.stripToEmpty((String) outMap
                         .get(ApiConstants.SP_RESP_CD)))) {

                 List<ABODtlReminderVO> aboReminderRsvpList = (List<ABODtlReminderVO>) outMap.get(SchedulerConstants.ABO_REMINDER);

               if (aboReminderRsvpList != null && !aboReminderRsvpList.isEmpty()) {

                     logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                             "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                             AmexLogger.Result.success, "", "Number of records for ABO RSVP reminder mail", String.valueOf(aboReminderRsvpList.size())
                             ,"Outmap" , aboReminderRsvpList.toString());

                     emailNotificationUtil.processABORsvpReminderEmail(aboReminderRsvpList, SchedulerConstants.ABO_REMAINDER_RSVP_MAIL, eventId);

                 }else{

                     logger.info(eventId, "SmartServiceEngine", "Email Notification Service",
                             "EmailNotificationServiceImpl : emailService", "Start of email fetch Service",
                             AmexLogger.Result.success, "", "ABO RSVP reminder mail Resultset is empty", "");

                 }
             } else {
             	logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_156_ERR_CD, TivoliMonitoring.SSE_SP_156_ERR_MSG, eventId));
                 logger.error(eventId, "SmartServiceEngine", "Enroll Serivce", "EmailNotificationServiceImpl - emailService", " SP E3GMR156 was NOT successful",
                     AmexLogger.Result.failure, "", ApiConstants.SQLCODE_PARM,
                     (String) outMap.get(ApiConstants.SQLCODE_PARM), "resp_code",
                     (String) outMap.get(ApiConstants.SP_RESP_CODE), "resp_msg",
                     (String) outMap.get(ApiConstants.SP_RESP_MSG));
             }
         }
         else {
        	 logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_156_ERR_CD, TivoliMonitoring.SSE_SP_156_ERR_MSG, eventId));
             logger.debug(eventId, "SmartServiceEngine", "Email Notification Service", "EmailNotificationServiceImpl : emailService", "End of Get ABO RSVP Remainder Email SP E3GMR156",
                 AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
         }
         
         
         // Sprint41 : changes END

        } catch (Exception e) {
            logger.error(eventId, "SmartServiceEngine", "EmailNotificationServiceImpl", "emailService",
                "Exception while generating mails ", AmexLogger.Result.failure, "", e, "ErrorMsg", e.getMessage());
        }
        final long endTimeStamp = System.currentTimeMillis();
        logger.info(eventId,"SmartServiceEngine", "EmailNotificationServiceImpl","emailService",
            "Time taken to execute emailService",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
        ;
        logger.info(eventId, "SmartServiceEngine", "EmailNotificationServiceImpl", "emailService",
            "End of  emailService", AmexLogger.Result.success, "END");
    }
}
