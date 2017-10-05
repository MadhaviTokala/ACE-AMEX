package com.americanexpress.smartserviceengine.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.ReportDispalyVO;
import com.americanexpress.smartserviceengine.common.vo.TermsAndConditionsResultSetVO;
import com.americanexpress.smartserviceengine.common.vo.TnCResponse;
import com.americanexpress.smartserviceengine.dao.TermsAndConditionsMailDAO;
import com.americanexpress.smartserviceengine.dao.UpdateTermsAndConditionsMailDAO;
import com.americanexpress.smartserviceengine.gcs.util.GCSConstants;
import com.americanexpress.smartserviceengine.gcs.util.GCSTermsAndConditionsUtil;
import com.americanexpress.smartserviceengine.jms.MessageProducer;
import com.americanexpress.smartserviceengine.service.TermsAndConditionService;
import com.itextpdf.text.Document;
import com.itextpdf.text.RectangleReadOnly;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

public class TermsAndConditionServiceImpl implements TermsAndConditionService {

    AmexLogger logger = AmexLogger.create(TermsAndConditionServiceImpl.class);

    @Autowired
    @Qualifier("termsAndConditionsMailDAO")
    private TermsAndConditionsMailDAO termsAndConditionsMailDAO;

    @Autowired
    private GCSTermsAndConditionsUtil gcsTermsAndConditionsUtil;

    @Resource
    @Qualifier("messageProducer")
    private MessageProducer messageProducer;

    @Autowired
    @Qualifier("updateTermsAndConditionsMailDAO")
    private UpdateTermsAndConditionsMailDAO updateTermsAndConditionsMailDAO;

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    private String PROXY_ENV;
    private String PROXY_HOST;
    private int PROXY_PORT;

    @SuppressWarnings("unchecked")
    @Override
    public void termsAndConditionsMailService(String eventId) throws SSEApplicationException {

        logger.info(eventId, "SmartServiceEngine", "TermsAndConditionServiceImpl", "termsAndConditionsMailService",
            "Start of termsAndConditionsMailService  TermsAndConditionServiceImpl", AmexLogger.Result.success, "Start");
        final long startTimeStamp = System.currentTimeMillis();

        HashMap<String, Object> inMap = new HashMap<String, Object>();
        Map<String, Object> outMap = termsAndConditionsMailDAO.execute(inMap, eventId);
        if (outMap != null) {
            if (outMap.get(ApiConstants.SP_RESP_CD) != null
                && ApiConstants.SP_SUCCESS_SSEIN000.equals(StringUtils.stripToEmpty((String) outMap
                    .get(ApiConstants.SP_RESP_CD)))) {

                List<List<TermsAndConditionsResultSetVO>> tandCList =
                    (List<List<TermsAndConditionsResultSetVO>>) outMap.get(SchedulerConstants.TNC_EMAIL_DTL_CSR);

                if (tandCList != null && !tandCList.isEmpty()) {

                    logger.info(eventId, "SmartServiceEngine", "Terms and Conditions Notification Service",
                        "TermsAndConditionServiceImpl : termsAndConditionsMailService", "Start of records fetch",
                        AmexLogger.Result.success, "", "Number of records for Terms and Conditions mail ",
                        String.valueOf(tandCList.size()));

                    for (List<TermsAndConditionsResultSetVO> subList : tandCList) {

                        if (!subList.isEmpty()) {
                            ReportDispalyVO reportDispalyVO = setDetailstoReportVO(subList);
                            String commTrackingId = gcsTermsAndConditionsUtil.createTrackingId(SchedulerConstants.TANC);
                            TnCResponse tncResponse =getContentFromiCM(subList.get(0),eventId);
                            if(tncResponse!=null && tncResponse.getContent()!=null){
	                            ByteArrayOutputStream outputStream = generateAttachment(subList.get(0), eventId,tncResponse );
	                            generateMail(reportDispalyVO, eventId, commTrackingId, outputStream);
                            }
                        }
                    }

                }
            } else {
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_149_ERR_CD,
                    TivoliMonitoring.SSE_SP_149_ERR_MSG, eventId));
                logger.error(eventId, "SmartServiceEngine", "Terms and Conditions Notification  Serivce",
                    "TermsAndConditionServiceImpl - termsAndConditionsMailService", " SP TEST0149 is NOT successful",
                    AmexLogger.Result.failure, "", ApiConstants.SQLCODE_PARM,
                    (String) outMap.get(ApiConstants.SQLCODE_PARM), "resp_code",
                    (String) outMap.get(ApiConstants.SP_RESP_CD), "resp_msg",
                    (String) outMap.get(ApiConstants.SP_RESP_MSG));
            }
        } else {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_149_ERR_CD,
                TivoliMonitoring.SSE_SP_149_ERR_MSG, eventId));
            logger.debug(eventId, "SmartServiceEngine", "Terms and Conditions Notification Service",
                "TermsAndConditionServiceImpl : termsAndConditionsMailService", " SP TEST0149 is NOT successful",
                AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
        }

        final long endTimeStamp = System.currentTimeMillis();
        logger.info(eventId,"SmartServiceEngine", "TermsAndConditionServiceImpl","End of  termsAndConditionsMailService",
            "Time taken to execute termsAndConditionsMailService",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
        
        logger.info(eventId, "SmartServiceEngine", "TermsAndConditionServiceImpl", "termsAndConditionsMailService",
            "END of termsAndConditionsMailService ", AmexLogger.Result.success, "END");

    }

    private TnCResponse getContentFromiCM(TermsAndConditionsResultSetVO termsAndConditionsResultSetVO,
            String eventId) {
        logger.info(eventId, "SmartServiceEngine Scheduler", "TermsAndConditionServiceImpl",
            "TermsAndConditionServiceImpl - getContentFromiCM", "Terms and Condition get content from iCM",
            AmexLogger.Result.success, "Start");

        TnCResponse tncResponse = new TnCResponse();
        RestTemplate rt = new RestTemplate();
       
        PROXY_ENV=EnvironmentPropertiesUtil
                .getProperty(ApiConstants.PROXY_ENV);
        try{
        if (termsAndConditionsResultSetVO.getTAndCURL() != null) {

           if(PROXY_ENV.equals(ApiConstants.TRUE)){
               PROXY_HOST=EnvironmentPropertiesUtil.getProperty(ApiConstants.PROXY_HOST);
               PROXY_PORT=Integer.parseInt(EnvironmentPropertiesUtil.getProperty(ApiConstants.PROXY_PORT));

                SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOST, PROXY_PORT));
                clientHttpRequestFactory.setProxy(proxy);
                 rt.setRequestFactory(clientHttpRequestFactory);
        }

            tncResponse = rt.getForObject(termsAndConditionsResultSetVO.getTAndCURL(), TnCResponse.class);
          }
        }catch(Exception e){
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SCHED_TANDC_INVALID_URL_CD, TivoliMonitoring.SCHED_TANDC_INVALID_URL_MSG, eventId));
            logger.error(eventId, "SmartServiceEngine", "TermsAndConditionServiceImpl", "generateAttachment",
                "Exception  in  generating Attachment", AmexLogger.Result.failure, "", e, "ErrorMsg", e.getMessage());
        
        }
        logger.info(eventId, "SmartServiceEngine Scheduler", "TermsAndConditionServiceImpl",
            "TermsAndConditionServiceImpl - getContentFromiCM", "Terms and Condition get content from iCM",
            AmexLogger.Result.success, "End");
        return tncResponse;
    }

    private ByteArrayOutputStream generateAttachment(TermsAndConditionsResultSetVO termsAndConditionsResultSetVO,
            String eventId,TnCResponse tncResponse ) {

        logger.info(eventId, "SmartServiceEngine Scheduler", "TermsAndConditionServiceImpl",
            "TermsAndConditionServiceImpl - generateAttachment", "Terms and Condition report generation",
            AmexLogger.Result.success, "Start");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

       
        Document document = new Document(new RectangleReadOnly(842, 595), 15f, 15f, 20f, 20f);
        PdfWriter pdfWriter = null;
        String content = null;
        try {
            pdfWriter = PdfWriter.getInstance(document, outputStream);

            document.setMargins(41, 30, 40, 80);
            document.open();
            if (tncResponse.getContent() != null) {
                content = StringEscapeUtils.unescapeHtml(tncResponse.getContent().getBody());
            }

            content = SchedulerConstants.HTML_BODY_TAG_START + content + SchedulerConstants.HTML_BODY_TAG_END;

            XMLWorkerHelper worker = XMLWorkerHelper.getInstance();
            worker.parseXHtml(pdfWriter, document, new StringReader(content));

            logger.info(eventId, "SmartServiceEngine Scheduler", "TermsAndConditionServiceImpl",
                "TermsAndConditionServiceImpl - generateAttachment", "Terms and Condition report generation",
                AmexLogger.Result.success, "End");
            document.close();
        } catch (Exception e) {
            logger.error(eventId, "SmartServiceEngine", "TermsAndConditionServiceImpl", "generateAttachment",
                "Exception in TermsAndConditionServiceImpl ", AmexLogger.Result.failure,
                "Failure in generateAttachment", e, "ErrorMsg", e.getMessage());

        }
        return outputStream;

    }

    private ReportDispalyVO setDetailstoReportVO(List<TermsAndConditionsResultSetVO> subList) {
        ReportDispalyVO reportDispalyVO = new ReportDispalyVO();

        TermsAndConditionsResultSetVO termsAndConditionsResultSetVO = subList.get(0);
        reportDispalyVO.setTermsAndConditionsResultSetVO(subList);
        reportDispalyVO.setPrtrmacid(termsAndConditionsResultSetVO.getPartnerMacId());
        reportDispalyVO.setPartnerAccountId(termsAndConditionsResultSetVO.getPartnerAccountId());
        reportDispalyVO.setTandCTemplateName(termsAndConditionsResultSetVO.getTandCTemplateName());
        reportDispalyVO.setPrtrOrgId(termsAndConditionsResultSetVO.getOrganizationId());
        reportDispalyVO.setOrgName(termsAndConditionsResultSetVO.getOrgName());
        reportDispalyVO.setAboFirstName(termsAndConditionsResultSetVO.getAboFirstName());
        reportDispalyVO.setAboLastName(termsAndConditionsResultSetVO.getAboLastName());
        reportDispalyVO.setBankName(termsAndConditionsResultSetVO.getBankName());
        reportDispalyVO.setAccountNumber(termsAndConditionsResultSetVO.getBankAccountNumber());
        reportDispalyVO.setAboEmailId(termsAndConditionsResultSetVO.getAboEmailAddress());
        reportDispalyVO.settAndCURL(termsAndConditionsResultSetVO.getTAndCURL());
        //reportDispalyVO.setTemplateId(EnvironmentPropertiesUtil
         //   .getProperty(GCSConstants.GCS_TERMS_AND_CONDITIONS_TEMPLATE_ID));
        reportDispalyVO.setDocumentName(GCSConstants.TERMS_AND_CONDITIONS_DOC_NAME);
        Integer serviceId = termsAndConditionsResultSetVO.getServiceId();
        switch(serviceId){
	        case 1: 
	        	reportDispalyVO.setSvcType(GCSConstants.PAYMENT_MTD_CHECK);
	        	break;
	        case 2: 
	        	reportDispalyVO.setSvcType(GCSConstants.PAYMENT_MTD_CARD);
	        	break;
	        case 3: 
	        	reportDispalyVO.setSvcType(GCSConstants.PAYMENT_MTD_ACH);
	        	break;
	        case 4: 
	        	reportDispalyVO.setSvcType(GCSConstants.PAYMENT_MTD_FX);
	        	break;
	    }
        
        String propPartnerIdMap = EnvironmentPropertiesUtil.getProperty(ApiConstants.SSE_PAYVE_PARTNERID_MAP);
        String[] partnerIdMapList = propPartnerIdMap.split(ApiConstants.CHAR_COLON);
        String intacctPartnerId = partnerIdMapList[0];
        
        if(termsAndConditionsResultSetVO.getPartnerMacId() != null && termsAndConditionsResultSetVO.getPartnerMacId().equalsIgnoreCase(intacctPartnerId)
        	&&	termsAndConditionsResultSetVO.getServiceId() != null && termsAndConditionsResultSetVO.getServiceId() == 3){
        	reportDispalyVO.setTemplateId(EnvironmentPropertiesUtil
                    .getProperty(GCSConstants.GCS_TERMS_AND_CONDITIONS_TEMPLATE_ID));
        }else{
        	reportDispalyVO.setTemplateId(EnvironmentPropertiesUtil
                    .getProperty(GCSConstants.GCS_TERMS_AND_CONDITIONS_NEW_TEMPLATE_ID));
        }
        return reportDispalyVO;
    }

    private void generateMail(ReportDispalyVO reportDispalyVO, String eventId, String commTrackingId,
            ByteArrayOutputStream outputStream) {

        reportDispalyVO.setReferenceNumber(gcsTermsAndConditionsUtil.getReferenceNumber());

        String message;
        Map<String, Object> outMap = null;
        try {
            message = gcsTermsAndConditionsUtil.sendReportThroughEmail(reportDispalyVO, outputStream, commTrackingId);

            logger.info(eventId, "SmartServiceEngine", "TermsAndConditionServiceImpl", "generateMail",
                "GCS Request xml generated", AmexLogger.Result.success, "", "GCS Request xml", message);
            messageProducer.setMessage(message);

            messageProducer.sendMessages();
            outMap = updateGCSStatusOfTandCMails(eventId, reportDispalyVO, commTrackingId);

            if (outMap != null
                    && !outMap.isEmpty()){
                    if(ApiConstants.SP_SUCCESS_SSEIN000.equals(StringUtils.stripToEmpty((String) outMap
                        .get(ApiConstants.SP_RESP_CD)))) {
                    logger.info(eventId, "SmartServiceEngine", "Terms And Conditions Notification Service",
                        "TermsAndConditionServiceImpl : updateGCSStatusOfTandCMails", "Update SP TEST0150 Successfull",
                        AmexLogger.Result.success, "outMap" + outMap);
                } else {
                    
                        logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_150_ERR_CD,
                            TivoliMonitoring.SSE_SP_150_ERR_MSG, eventId));                   

                    logger.error(eventId, "SmartServiceEngine", "Terms and Conditions mail Notification Service",
                        "TermsAndConditionServiceImpl -updateGCSStatusOfTandCMails ", " SP TEST0150 is NOT successful",
                        AmexLogger.Result.failure, "", ApiConstants.SQLCODE_PARM,
                        (String) outMap.get(ApiConstants.SQLCODE_PARM), "resp_code",
                        (String) outMap.get(ApiConstants.SP_RESP_CODE), "resp_msg",
                        (String) outMap.get(ApiConstants.SP_RESP_MSG));
                }}else{
                    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_150_ERR_CD,
                        TivoliMonitoring.SSE_SP_150_ERR_MSG, eventId));
                    logger.debug(eventId, "SmartServiceEngine", "Email Notification Service",
                        "TermsAndConditionServiceImpl : updateGCSStatusOfTandCMails", "End of Update GCS Status SP TEST0150",
                        AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
                }

        } catch (Exception e) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SCHED_GCS_FAIL_ICM_PUSH_CD, TivoliMonitoring.SCHED_GCS_FAIL_ICM_PUSH_MSG, eventId));
            logger.error(eventId, "SmartServiceEngine", "TermsAndConditionServiceImpl", "generateMail",
                "Exception  in  generating report", AmexLogger.Result.failure, "", e, "ErrorMsg", e.getMessage());
        }
    }

    private Map<String, Object> updateGCSStatusOfTandCMails(String eventId, ReportDispalyVO reportDispalyVO,
            String communicationTrackingId) {

        Map<String, Object> inputMap = new HashMap<String, Object>();
        inputMap.put(SchedulerConstants.IN_PRTR_MAC_ID, reportDispalyVO.getPrtrmacid());
        inputMap.put(SchedulerConstants.IN_ACCT_ID, reportDispalyVO.getPartnerAccountId());
        inputMap.put(SchedulerConstants.IN_TNC_TMPLT_NM, reportDispalyVO.getTandCTemplateName());
        inputMap.put(SchedulerConstants.IN_ABO_EMAIL_AD_TX, reportDispalyVO.getAboEmailId().toLowerCase());
        inputMap.put(SchedulerConstants.IN_GCS_TRANS_ID, communicationTrackingId);
        inputMap.put(SchedulerConstants.IN_SSE_TRACK_ID, StringUtils.EMPTY);
        inputMap.put(SchedulerConstants.IN_RPT_STA_CD, StringUtils.EMPTY);
        inputMap.put(SchedulerConstants.IN_RPT_STA_DS, StringUtils.EMPTY);
        inputMap.put(SchedulerConstants.IN_UPDT_IN, SchedulerConstants.CHAR_A);
        if((GCSConstants.PAYMENT_MTD_CHECK).equals(reportDispalyVO.getSvcType())){
        	 inputMap.put(SchedulerConstants.IN_SRVC_ID, 1);
    	}
    	else if((GCSConstants.PAYMENT_MTD_CARD).equals(reportDispalyVO.getSvcType())){
       	 inputMap.put(SchedulerConstants.IN_SRVC_ID, 2);
    	}
    	else if((GCSConstants.PAYMENT_MTD_ACH).equals(reportDispalyVO.getSvcType())){
       	 inputMap.put(SchedulerConstants.IN_SRVC_ID, 3);
    	}
    	else if((GCSConstants.PAYMENT_MTD_FX).equals(reportDispalyVO.getSvcType())){
       	 inputMap.put(SchedulerConstants.IN_SRVC_ID, 4);
    	}
       

        Map<String, Object> outMap = updateTermsAndConditionsMailDAO.execute(inputMap, eventId);

        return outMap;
    }

}
