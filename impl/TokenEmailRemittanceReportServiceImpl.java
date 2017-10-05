package com.americanexpress.smartserviceengine.service.impl;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.SendMail;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.EmailAttachment;
import com.americanexpress.smartserviceengine.common.vo.EmailRequest;
import com.americanexpress.smartserviceengine.common.vo.RemitReportInvoiceVO;
import com.americanexpress.smartserviceengine.common.vo.RemitReportVO;
import com.americanexpress.smartserviceengine.common.vo.ReportDispalyVO;
import com.americanexpress.smartserviceengine.common.vo.ReportInvoice;
import com.americanexpress.smartserviceengine.gcs.util.GCSConstants;
import com.americanexpress.smartserviceengine.manager.SaveRemittanceDataManager;
import com.americanexpress.smartserviceengine.service.TokenEmailRemittanceReportService;
import com.americanexpress.smartserviceengine.util.SupplierRemittanceReportUtil;

@Service
public class TokenEmailRemittanceReportServiceImpl implements TokenEmailRemittanceReportService {

    private static AmexLogger logger = AmexLogger.create(TokenEmailRemittanceReportServiceImpl.class);
   
    @Autowired
    SaveRemittanceDataManager saveRemittanceDataManager;
    
    @Autowired
    SendMail mail;
    
    @Resource
	private TivoliMonitoring tivoliMonitoring;
    
    @SuppressWarnings("unchecked")
	@Override
    public boolean generateReport(String eventId,  RemitReportVO remitReportVO, EmailRequest email) throws SSEApplicationException {

        logger.info(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "generateReport",
            "Start of generateReport  TokenEmailRemittanceReportServiceImpl", AmexLogger.Result.success, "Start");
        final long startTimeStamp = System.currentTimeMillis();
    	boolean status = false;
    	    	
    	if(remitReportVO != null){
    		logger.info(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "generateReport",
    	            "TokenEmailRemittanceReportServiceImpl: remitReportVO received as input", AmexLogger.Result.success, remitReportVO.toString());
            List<ReportDispalyVO> reportDispalyVoList = convertToReportDisplayVO(remitReportVO, eventId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if(reportDispalyVoList != null && !reportDispalyVoList.isEmpty()){
        		//String gcsTrackingId = gcsSupplierRemittanceUtils.createTrackingId(SchedulerConstants.SRRT);
        		//String refNum = gcsSupplierRemittanceUtils.getReferenceNumber();
        	 	String pdfUtilReturnVal = SupplierRemittanceReportUtil.generateReport(eventId, reportDispalyVoList, outputStream, "", "");
        	 	try{
        	 		HashMap<String,String> tokens = email.getTokens();
        			String icmTemplate = email.getTemplate();
        			String body = email.getHtmlBody();
//        			SendMail mail = SendMail.getInstance();
        			List<EmailAttachment> attachments = null;
        			if(pdfUtilReturnVal != null && outputStream != null && outputStream.size() > 0){
        				attachments = new ArrayList<EmailAttachment>();
        				EmailAttachment attachment = new EmailAttachment();
        				attachment.setFile(outputStream.toByteArray());
        				/*
        				 * Set Supplier Remittance Report PDF filename as "Supplier Remittance Report.pdf"
        				 */
        				if(reportDispalyVoList.get(0) != null && reportDispalyVoList.get(0).getPaymentrefrencenumber() != null) {
        					attachment.setFileName("SUPPLIER_REMITTANCE_REPORT_" + reportDispalyVoList.get(0).getPaymentrefrencenumber() + ".pdf");
        					//attachment.setFileName("Supplier Remittance Report.pdf");

        				} else {
        					attachment.setFileName("SUPPLIER_REMITTANCE_REPORT_" +  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + ".pdf");
        				}
        				attachment.setContentType("application/pdf");
        				attachments.add(attachment);
            			status = mail.sendMail(email.getTo(), email.getCc(), email.getFrom(), email.getSubject(), email.getFormat(), body, 
            					icmTemplate, tokens, null, attachments, email.isVoltage());
            			if(status == true){
            				logger.info(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "generateReport",
	            	                "Email sent successfully", AmexLogger.Result.success, "");
	    					logger.info(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "generateReport",
	            	                "Start executing the SP insertion flow", AmexLogger.Result.success, "");
         	                saveRemittanceDataManager.process(remitReportVO, eventId);

            			}else{
            				logger.info(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "generateReport",
	            	                "Error  in  sending email", AmexLogger.Result.failure, "");
            			}
        			}else{
        				logger.error(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "generateReport",
            	                "Exception  in  generating PDF report", AmexLogger.Result.failure, "");
        			}
        	 	}catch(SSEApplicationException me){
        	 		if(ApiErrorConstants.EMAIL_INTERNAL_SERVER_ERR_CD.equals(me.getResponseCode())){
        	 			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SUPP_REMITTANCE_EMAIL_FAIL_ERR_CD,
 	                           TivoliMonitoring.SUPP_REMITTANCE_EMAIL_FAIL_ERR_MSG, eventId));
        	 		}
    				throw me;
    			}catch(Exception e){
    	            logger.error(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "generateReport",
    	                "Exception  in  generating report or sending email", AmexLogger.Result.failure, "", e, "ErrorMsg", e.getMessage());
    	            throw new SSEApplicationException(
    						"Exception in sending email for supplier remittance", ApiErrorConstants.EMAIL_INTERNAL_SERVER_ERR_CD,
    						e.getMessage(),
    						e);
        	 	}
        	}else{
             	logger.info(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "generateReport",
    		            "Empty reportDispalyVoList", AmexLogger.Result.success, "");
            }	                          
    	}else{
         	logger.info(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "generateReport",
		            "Empty display VO's", AmexLogger.Result.success, "");
        }
        
        final long endTimeStamp = System.currentTimeMillis();
        logger.info(eventId,"SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl","End of generateReport",
            "Time taken to execute generateReport",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");

        logger.info(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "generateReport",
            "END of generateReport  TokenEmailRemittanceReportServiceImpl", AmexLogger.Result.success, "END");
        return status;
    }



    private static List<ReportDispalyVO> convertToReportDisplayVO(RemitReportVO remitReportVO, String eventId) {
    	List<ReportDispalyVO> reportDisplayList = new ArrayList<ReportDispalyVO>();
    	
        List<ReportInvoice> reportInvoiceList =new ArrayList<ReportInvoice>();
        
        ReportDispalyVO objReportDispalyVO = new ReportDispalyVO();
        try{
	        objReportDispalyVO.setReportType(GCSConstants.REMIT_REPORT_TYPE_CARD);	
	        setvendordeatils(objReportDispalyVO, remitReportVO, eventId);
	
	        if(remitReportVO.getRemitReportInvoiceVOList() == null || remitReportVO.getRemitReportInvoiceVOList().isEmpty()) {
	            objReportDispalyVO.setInvoiceExisitng(false);
	        } else {
	            objReportDispalyVO.setInvoiceExisitng(true);
	
	        }
	        if(remitReportVO.getRemitReportInvoiceVOList() != null){
	        	for (RemitReportInvoiceVO remitReportInvoiceVO:remitReportVO.getRemitReportInvoiceVOList()) {	              
	                ReportInvoice objReportInvoice = new ReportInvoice();
	                setinvoiceDetails(remitReportInvoiceVO, objReportInvoice, eventId);
	                reportInvoiceList.add(objReportInvoice);	                
	        	}
	        }	        
	        objReportDispalyVO.setReportInvoiceList(reportInvoiceList);
            reportDisplayList.add(objReportDispalyVO);
	        
        }catch (Exception e) {
	        logger.error(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "convertToReportDisplayVO",
	                "Exception  in  convertToReportDisplayVO - Failure in getPaymentData", AmexLogger.Result.failure, "", e,
	                "ErrorMsg", e.getMessage());
        }
        return reportDisplayList;
    }

    private static void setvendordeatils(ReportDispalyVO objReportDispalyVO, RemitReportVO remitReportVO,
            String eventId) {

        try {

            objReportDispalyVO.setCustomerid(StringUtils.stripToEmpty(remitReportVO.getPrtrcustorgid()));
            objReportDispalyVO.setCustomername(StringUtils.stripToEmpty(remitReportVO.getPrtrcustorgnm()));

            objReportDispalyVO.setCustomeraddress(StringUtils.stripToEmpty(remitReportVO.getPrtrcustadline1tx()) + " "
                    + StringUtils.stripToEmpty(remitReportVO.getPrtrcustadline2tx()) + " " + StringUtils.stripToEmpty(remitReportVO.getPrtrcustadline3tx()) + " "
                    + StringUtils.stripToEmpty(remitReportVO.getPrtrcustadline4tx()) + " " + StringUtils.stripToEmpty(remitReportVO.getPrtrcustadline5tx()) + " "
                    + StringUtils.stripToEmpty(remitReportVO.getPrtrcustadposttownnm()) + " " + StringUtils.stripToEmpty(remitReportVO.getPrtrcustadrgnareacd()) + " "
                    + StringUtils.stripToEmpty(remitReportVO.getPrtrcustadpstlcd()));

            
            objReportDispalyVO.setPrtrcustadline1tx(StringUtils.stripToEmpty(remitReportVO.getPrtrcustadline1tx()));
            
            objReportDispalyVO.setPrtrcustadline2tx(StringUtils.stripToEmpty(remitReportVO.getPrtrcustadline2tx()));
            
            objReportDispalyVO.setPrtrcustadline3tx(StringUtils.stripToEmpty(remitReportVO.getPrtrcustadline3tx()));
            
            objReportDispalyVO.setPrtrcustadline4tx(StringUtils.stripToEmpty(remitReportVO.getPrtrcustadline4tx()));
            
            objReportDispalyVO.setPrtrcustadline5tx(StringUtils.stripToEmpty(remitReportVO.getPrtrcustadline5tx()));
            
            
            objReportDispalyVO.setPrtrcustadposttownnm(StringUtils.stripToEmpty(remitReportVO.getPrtrcustadposttownnm()));
            objReportDispalyVO.setPrtrcustadrgnareacd(StringUtils.stripToEmpty(remitReportVO.getPrtrcustadrgnareacd()));
            objReportDispalyVO.setPrtrcustadpstlcd(StringUtils.stripToEmpty(remitReportVO.getPrtrcustadpstlcd()));
            objReportDispalyVO.setOrgEmailId(StringUtils.stripToEmpty(remitReportVO.getOrgemailadtx()));
            objReportDispalyVO.setVendorid(StringUtils.stripToEmpty(remitReportVO.getPrtrsplyorgid()));
            objReportDispalyVO.setVendorname(StringUtils.stripToEmpty(remitReportVO.getPrtrsplyorgnm()));
            objReportDispalyVO.setPaymentamount(remitReportVO.getPymtusdam());

            objReportDispalyVO.setPaymentdate(DateTimeUtil.convertYYYYMMDDtoMMddyyy(remitReportVO.getPymtdt()));
            objReportDispalyVO.setPymtid(StringUtils.stripToEmpty(remitReportVO.getPymtid()));
           // objReportDispalyVO.setInvid(objReportDataVO.getInvid());
            objReportDispalyVO.setPrtrmacid(remitReportVO.getPrtrmacid());
            objReportDispalyVO.setPaymentrefrencenumber(StringUtils.stripToEmpty(remitReportVO.getEntrprmoneymovementpymtreferno()));
            String splAccNo = remitReportVO.getSplybankacctno();
            if(null != splAccNo && splAccNo.length() > 4){
            	splAccNo = splAccNo.substring(splAccNo.length() - 4);
            }
            objReportDispalyVO.setSplAcctLast4Digs(splAccNo);

        } catch (Exception e) {
            logger.error(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "setvendordeatils",
                "Exception  in  setvendordeatils - Failure in getPaymentData", AmexLogger.Result.failure, "", e,
                "ErrorMsg", e.getMessage());
        }

    }

    private static void setinvoiceDetails(RemitReportInvoiceVO remitReportInvoiceVO, ReportInvoice objReportInvoice, String eventId) {
        try {
                  	
            	StringBuilder desc = new StringBuilder();
            	
            	if(remitReportInvoiceVO.getSplyinvreferno() != null && remitReportInvoiceVO.getSplyinvreferno().trim().length() >0) {
            		desc.append(StringUtils.stripToEmpty(remitReportInvoiceVO.getSplyinvreferno()) );
            		desc.append(" ");
            	} else {
            		desc.append(" ");  
            	}
            	
            	if(remitReportInvoiceVO.getBuyinfo1tx() != null && remitReportInvoiceVO.getBuyinfo1tx().trim().length() >0) {
            		desc.append(StringUtils.stripToEmpty(remitReportInvoiceVO.getBuyinfo1tx()) );
            		desc.append(" ");
            	} else {
            		desc.append(" ");   
            	}
            	
            	if(remitReportInvoiceVO.getBuyinfo2tx() != null && remitReportInvoiceVO.getBuyinfo2tx().trim().length() >0) {
            		desc.append(StringUtils.stripToEmpty(remitReportInvoiceVO.getBuyinfo2tx()) );
            		desc.append(" ");
            	} else {
            		desc.append(" ");   
            	}
            	
            	if(remitReportInvoiceVO.getBuyinfo3tx() != null && remitReportInvoiceVO.getBuyinfo3tx().trim().length() >0) {
            		desc.append(StringUtils.stripToEmpty(remitReportInvoiceVO.getBuyinfo3tx()) );
            		desc.append(" ");
            	} else {
            		desc.append(" ");   
            	}
            	
            	if(remitReportInvoiceVO.getBuyinfo4tx() != null && remitReportInvoiceVO.getBuyinfo4tx().trim().length() >0) {
            		desc.append(StringUtils.stripToEmpty(remitReportInvoiceVO.getBuyinfo4tx()) );
            	} else {
            		desc.append(" ");   
            	}

            objReportInvoice.setDesciption(desc.toString());
           
           /* objReportInvoice.setDesciption(StringUtils.stripToEmpty(remitReportInvoiceVO.getSplyinvreferno()) + " "
                    + StringUtils.stripToEmpty(remitReportInvoiceVO.getBuyinfo1tx()) + " " + StringUtils.stripToEmpty(remitReportInvoiceVO.getBuyinfo2tx()) + " "
                    + StringUtils.stripToEmpty(remitReportInvoiceVO.getBuyinfo3tx()) + " " + StringUtils.stripToEmpty(remitReportInvoiceVO.getBuyinfo4tx()));
            */            
            objReportInvoice.setReferencenumber(StringUtils.stripToEmpty(remitReportInvoiceVO.getBuyinvreferno()));
            objReportInvoice.setInvoiceduedate(DateTimeUtil.convertYYYYMMDDtoMMddyyy(remitReportInvoiceVO.getInvduedt()));
            objReportInvoice.setInvoicedate(DateTimeUtil.convertYYYYMMDDtoMMddyyy(remitReportInvoiceVO.getInvdt()));
            objReportInvoice.setInvoiceamount(remitReportInvoiceVO.getInvgrlocalam());
            objReportInvoice.setPaidamount(remitReportInvoiceVO.getInvnetlocalam());
            objReportInvoice.setAdjustmentamount(remitReportInvoiceVO.getShrtpaylocalam());
            objReportInvoice.setAdjustmentdescription(StringUtils.stripToEmpty(remitReportInvoiceVO.getShrtpaytx()));
            
        } catch (Exception e) {
            logger.error(eventId, "SmartServiceEngine", "TokenEmailRemittanceReportServiceImpl", "setinvoiceDetails",
                "Exception  in  setinvoiceDetails - Failure in getPaymentData", AmexLogger.Result.failure, "", e,
                "ErrorMsg", e.getMessage());
        }
    }
    
    /* dummy method to generate report data 
    to be removed after sprint 33
    */
	private RemitReportVO getRemittanceReportData() {
		RemitReportVO dataVO = new RemitReportVO();
		List<RemitReportInvoiceVO> reportInvoiceVOList = new ArrayList<RemitReportInvoiceVO>();
		dataVO.setPrtrmacid("2324424232-232-2323");

		dataVO.setPymtid("434322431");
		dataVO.setPrtrcustorgnm("XYZ_COMP");
		dataVO.setPrtrsplyorgid("vendorid1");
		dataVO.setBuypymtid("Buypymtid:");
		dataVO.setEntrprmoneymovementpymtreferno("434322431");
		dataVO.setPymtdt("2016-01-01");
		dataVO.setOrgemailadtx("test@gmail.com");
		dataVO.setPrtrcustadline1tx("4322 BELL RD");
		dataVO.setPrtrcustadline2tx("APT# 3232");
	
		dataVO.setPrtrcustadposttownnm("PHOENIX");
		dataVO.setPrtrcustadpstlcd("85432");
		dataVO.setPrtrcustadrgnareacd("AZ");
		dataVO.setPrtrcustorgid("prtrcustorgid");
		dataVO.setPrtrsplyorgnm("ABC_Corp");
		dataVO.setSplybankacctno("990000756");
		dataVO.setPymtusdam("12.00");
        dataVO.setPymtalphacurrcd("$");
        
		for (int i = 1; i <= 5; i++) {
			RemitReportInvoiceVO remitInvoiceVO = new RemitReportInvoiceVO();

			remitInvoiceVO.setBuyinfo1tx("Sample InfoTxt1_" + i);
			remitInvoiceVO.setBuyinfo2tx("Sample InfoTxt2_" + i);
			remitInvoiceVO.setBuyinfo3tx("Sample InfoTxt3_" + i);
			remitInvoiceVO.setBuyinfo4tx("Sample InfoTxt4_" + i);
			remitInvoiceVO.setBuyinvreferno("653234" + i);
			remitInvoiceVO.setInvdt("2016-01-0" + i);
			remitInvoiceVO.setInvduedt("2016-02-0" + i);
			remitInvoiceVO.setInvgrlocalam("100" + i);
			remitInvoiceVO.setInvid("invid:" + i);
			remitInvoiceVO.setInvnetlocalam("200" + i);
			remitInvoiceVO.setShrtpaylocalam("50" + i);
			remitInvoiceVO.setShrtpaytx("shortpaytext_" + i);
			remitInvoiceVO.setSplyinvreferno("64343434343434434" + i);
			reportInvoiceVOList.add(remitInvoiceVO);
		}
		dataVO.setRemitReportInvoiceVOList(reportInvoiceVOList);
		return dataVO;

	}


}
