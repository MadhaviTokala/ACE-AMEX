package com.americanexpress.smartserviceengine.service.impl;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.ReportDataVO;
import com.americanexpress.smartserviceengine.common.vo.ReportDispalyVO;
import com.americanexpress.smartserviceengine.common.vo.ReportInvoice;
import com.americanexpress.smartserviceengine.dao.GetReportDataDAO;
import com.americanexpress.smartserviceengine.dao.UpdateReportMailDAO;
import com.americanexpress.smartserviceengine.gcs.util.GCSSupplierRemittanceUtils;
import com.americanexpress.smartserviceengine.jms.MessageProducer;
import com.americanexpress.smartserviceengine.service.SupplierRemittanceReportService;
import com.americanexpress.smartserviceengine.util.SupplierRemittanceReportUtil;


public class SupplierRemittanceReportServiceImpl implements SupplierRemittanceReportService {

    private static AmexLogger logger = AmexLogger.create(SupplierRemittanceReportServiceImpl.class);
    @Resource
    private TivoliMonitoring tivoliMonitoring;

    @Resource
    private GetReportDataDAO getReportDataDAO;

    @Resource
    private UpdateReportMailDAO updateReportMailDAO;

    @Resource
    @Qualifier("messageProducer")
    private MessageProducer messageProducer;

    @Resource
    private GCSSupplierRemittanceUtils gcsSupplierRemittanceUtils;

    @SuppressWarnings("unchecked")
	@Override
    public void generateReport(String eventId) throws SSEApplicationException {

        logger.info(eventId, "SmartServiceEngine", "SupplierRemittanceReportServiceImpl", "generateReport",
            "Start of generateReport  SupplierRemittanceReportServiceImpl", AmexLogger.Result.success, "Start");
        final long startTimeStamp = System.currentTimeMillis();

        Map<String, Object> inMap = new HashMap<String, Object>();
        Map<String, Object> outMap = null;
        List<List<ReportDataVO>> reportDataVOs = new ArrayList<List<ReportDataVO>>();
        try {
            outMap = getReportDataDAO.execute(inMap, eventId);
        } catch (Exception e) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_089_ERR_CD, TivoliMonitoring.SSE_SP_089_ERR_MSG, eventId));
            logger.error(eventId, "SmartServiceEngine", "SupplierRemittanceReportServiceImpl", "generateReport",
                "Exception while Calling TEST0089 - Failure in GetReportDataDAO", AmexLogger.Result.failure, "", e, "ErrorMsg", e.getMessage());
        }

        if(outMap != null ){
        if (outMap.get(SchedulerConstants.RESP_CD).toString().trim()
                .equalsIgnoreCase(SchedulerConstants.RESPONSE_CODE_SUCCESS_SSEABO01)) {

            reportDataVOs = (List<List<ReportDataVO>>) outMap.get(SchedulerConstants.RESULT_SET);
            if(reportDataVOs!=null && !reportDataVOs.isEmpty()){
            for(List<ReportDataVO> reportDataVOList:reportDataVOs){
            try {
            	if(reportDataVOList!=null && !reportDataVOList.isEmpty()){
                List<ReportDispalyVO> reportDispalyVoList = convertToReportDisplayVO(reportDataVOList, eventId);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                if(reportDispalyVoList!=null && !reportDispalyVoList.isEmpty()){
            		String gcsTrackingId = gcsSupplierRemittanceUtils.createTrackingId(SchedulerConstants.SRRT);
            		String refNum=gcsSupplierRemittanceUtils.getReferenceNumber();
            	 	SupplierRemittanceReportUtil.generateReport(eventId, reportDispalyVoList, outputStream,gcsTrackingId,refNum);
            	 	try{
						String message = gcsSupplierRemittanceUtils.sendReportThroughEmail(reportDispalyVoList.get(0), outputStream, gcsTrackingId);
						logger.info(eventId, "SmartServiceEngine", "SupplierRemittanceReportServiceImpl", "generateReport",
					            "GCS Request xml generated", AmexLogger.Result.success, "", "GCS Request xml", message);
						messageProducer.setMessage(message);
						messageProducer.sendMessages();
            	 	}catch(Exception e){
            	 	 logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SCHED_GCS_FAIL_SUPPLIER_REMITTANCE_CD,
            	                TivoliMonitoring.SCHED_GCS_FAIL_SUPPLIER_REMITTANCE_MSG, eventId));

            	            logger.error(eventId, "SmartServiceEngine", "SupplierRemittanceReportServiceImpl", "generateReport",
            	                "Exception  in  generating report", AmexLogger.Result.failure, "", e, "ErrorMsg", e.getMessage());
            	 	}
							updateGCSStatus(eventId, reportDispalyVoList.get(0));
            	}
            	}else{
                 	logger.info(eventId, "SmartServiceEngine", "SupplierRemittanceReportServiceImpl", "generateReport",
     			            "Empty display VO's", AmexLogger.Result.success, "");
                 }
            } catch (Exception e) {
                logger.error(eventId, "SmartServiceEngine", "SupplierRemittanceReportServiceImpl", "generateReport", "Exception in SupplierRemittanceReportServiceImpl",
                		AmexLogger.Result.failure, "", e, "ErrorMsg", e.getMessage());
            }
        }
            }else{
            	logger.info(eventId, "SmartServiceEngine", "SupplierRemittanceReportServiceImpl", "generateReport",
			            "Empty outmap", AmexLogger.Result.success, "");
            }
            }else{
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_089_ERR_CD,
                    TivoliMonitoring.SSE_SP_089_ERR_MSG, eventId));
                logger.error(eventId, "SmartServiceEngine", "SupplierRemittanceReportServiceImpl", "generateReport",
                    "Check out for the outMap for SP TEST0089", AmexLogger.Result.failure, "responseCode"
                        + outMap.get(SchedulerConstants.RESP_CD));
            }
        }else{
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_089_ERR_CD,
                TivoliMonitoring.SSE_SP_089_ERR_MSG, eventId));
            logger.debug(eventId, "SmartServiceEngine", "Supplier Remitance Report  Scheduler Service",
                "SupplierRemittanceReportServiceImpl : generateReport", "End of Get Organization Details SP TEST0089",
                AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
        }
        final long endTimeStamp = System.currentTimeMillis();
        logger.info(eventId,"SmartServiceEngine", "SupplierRemittanceReportServiceImpl","End of generateReport",
            "Time taken to execute generateReport",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");

        logger.info(eventId, "SmartServiceEngine", "SupplierRemittanceReportServiceImpl", "generateReport",
            "END of generateReport  SupplierRemittanceReportServiceImpl", AmexLogger.Result.success, "END");
    }
    
    
    

	private void updateGCSStatus(String eventId, ReportDispalyVO reportDispalyVO) {
		Map<String, Object> inputMap = new HashMap<String, Object>();
		inputMap.put(SchedulerConstants.IN_PRTR_MAC_ID, reportDispalyVO.getPrtrmacid());
		inputMap.put(SchedulerConstants.IN_PYMT_ID, reportDispalyVO.getPymtid());
		inputMap.put(SchedulerConstants.IN_SSE_TRK_ID, reportDispalyVO.getReferenceNumber());
		inputMap.put(SchedulerConstants.IN_GCS_TRANS_ID, reportDispalyVO.getGcsTrackingId());
		inputMap.put(SchedulerConstants.IN_RPT_STA_CD, StringUtils.EMPTY);
		inputMap.put(SchedulerConstants.IN_RPT_STA_DS, StringUtils.EMPTY);
		inputMap.put(SchedulerConstants.IN_UPDT_IN, SchedulerConstants.CHAR_A);
		Map<String, Object> outMap = updateReportMailDAO.execute(inputMap, eventId);
		if (outMap != null && !outMap.isEmpty()
	                && ApiConstants.SP_SUCCESS_SSABO001.equals(StringUtils.stripToEmpty((String) outMap
	                    .get(ApiConstants.SP_RESP_CD)))) {
	                logger.info(eventId, "SmartServiceEngine", "Supplier Remittance Report Generation Service", "SupplierRemittanceReportServiceImpl : generateReport",
	                                "Update SP E3GMR109 Successfull", AmexLogger.Result.success, "");
	            } else  {
	                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_090_ERR_CD, TivoliMonitoring.SSE_SP_090_ERR_MSG, eventId));
	                logger.error(eventId, "SmartServiceEngine", "Supplier Remittance Report Generation Service", "SupplierRemittanceReportServiceImpl -generateReport ", "SP E3GMR090 is NOT successful",
	                    AmexLogger.Result.failure, "", ApiConstants.SQLCODE_PARM, (String) outMap.get(ApiConstants.SQLCODE_PARM), "resp_code",
	                    (String) outMap.get(ApiConstants.SP_RESP_CODE), "resp_msg", (String) outMap.get(ApiConstants.SP_RESP_MSG));
	            } 


	}

    private static List<ReportDispalyVO> convertToReportDisplayVO(List<ReportDataVO> reportDataVOList, String eventId) {
    	List<ReportDispalyVO> reportDisplayList = new ArrayList<ReportDispalyVO>();
        List<ReportInvoice> reportInvoiceList =new ArrayList<ReportInvoice>();
        ReportDispalyVO objReportDispalyVO = new ReportDispalyVO();
        setvendordeatils(objReportDispalyVO, reportDataVOList.get(0), eventId);
        for (ReportDataVO objReportDataVO:reportDataVOList) {
                if ("".equalsIgnoreCase(objReportDispalyVO.getInvid().trim())) {
                    objReportDispalyVO.setInvoiceExisitng(false);
                } else {
                    objReportDispalyVO.setInvoiceExisitng(true);

                }
                ReportInvoice objReportInvoice = new ReportInvoice();
                setinvoiceDetails(objReportDataVO, objReportInvoice, eventId);
                reportInvoiceList.add(objReportInvoice);
                objReportDispalyVO.setReportInvoiceList(reportInvoiceList);
                reportDisplayList.add(objReportDispalyVO);
            }
        return reportDisplayList;
    }

    private static void setvendordeatils(ReportDispalyVO objReportDispalyVO, ReportDataVO objReportDataVO,
            String eventId) {

        try {

            objReportDispalyVO.setCustomerid(objReportDataVO.getPrtrcustorgid());
            objReportDispalyVO.setCustomername(objReportDataVO.getPrtrcustorgnm());

            objReportDispalyVO.setCustomeraddress(objReportDataVO.getPrtrcustadline1tx() + " "
                    + objReportDataVO.getPrtrcustadline2tx() + " " + objReportDataVO.getPrtrcustadline3tx() + " "
                    + objReportDataVO.getPrtrcustadline4tx() + " " + objReportDataVO.getPrtrcustadline5tx() + " "
                    + objReportDataVO.getPrtrcustadposttownnm() + " " + objReportDataVO.getPrtrcustadrgnareacd() + " "
                    + objReportDataVO.getPrtrcustadpstlcd());

            objReportDispalyVO.setPrtrcustadline1tx(objReportDataVO.getPrtrcustadline1tx());
            objReportDispalyVO.setPrtrcustadline2tx(objReportDataVO.getPrtrcustadline2tx());
            objReportDispalyVO.setPrtrcustadline3tx(objReportDataVO.getPrtrcustadline3tx());
            objReportDispalyVO.setPrtrcustadline4tx(objReportDataVO.getPrtrcustadline4tx());
            objReportDispalyVO.setPrtrcustadline5tx(objReportDataVO.getPrtrcustadline5tx());
            objReportDispalyVO.setPrtrcustadposttownnm(objReportDataVO.getPrtrcustadposttownnm());
            objReportDispalyVO.setPrtrcustadrgnareacd(objReportDataVO.getPrtrcustadrgnareacd());
            objReportDispalyVO.setPrtrcustadpstlcd(objReportDataVO.getPrtrcustadpstlcd());
            objReportDispalyVO.setOrgEmailId(objReportDataVO.getOrgemailadtx());
            objReportDispalyVO.setVendorid(objReportDataVO.getPrtrsplyorgid());
            objReportDispalyVO.setVendorname(objReportDataVO.getPrtrsplyorgnm());
            objReportDispalyVO.setPaymentamount(objReportDataVO.getPymtusdam());

            objReportDispalyVO.setPaymentdate(DateTimeUtil.convertYYYYMMDDtoMMddyyy(objReportDataVO.getPymtdt()));
            objReportDispalyVO.setPymtid(objReportDataVO.getPymtid());
            objReportDispalyVO.setInvid(objReportDataVO.getInvid());
            objReportDispalyVO.setPrtrmacid(objReportDataVO.getPrtrmacid());
            objReportDispalyVO.setPaymentrefrencenumber(objReportDataVO.getEntrprmoneymovementpymtreferno());
            String splAccNo = objReportDataVO.getSplybankacctno();
            if(null != splAccNo && splAccNo.length()>4){
            	splAccNo = splAccNo.substring(splAccNo.length()-4);
            }
            objReportDispalyVO.setSplAcctLast4Digs(splAccNo);

        } catch (Exception e) {
            logger.error(eventId, "SmartServiceEngine", "SupplierRemittanceReportServiceImpl", "setvendordeatils",
                "Exception  in  setvendordeatils - Failure in getPaymentData", AmexLogger.Result.failure, "", e,
                "ErrorMsg", e.getMessage());
        }

    }

    private static void setinvoiceDetails(ReportDataVO objReportDataVO, ReportInvoice objReportInvoice, String eventId) {
        try {

            objReportInvoice.setDesciption(objReportDataVO.getSplyinvreferno().trim() + " "
                    + objReportDataVO.getBuyinfo1tx().trim() + " " + objReportDataVO.getBuyinfo2tx().trim() + " "
                    + objReportDataVO.getBuyinfo3tx() + " " + objReportDataVO.getBuyinfo4tx());
            objReportInvoice.setReferencenumber(objReportDataVO.getBuyinvreferno());
            objReportInvoice.setInvoiceduedate(DateTimeUtil.convertYYYYMMDDtoMMddyyy(objReportDataVO.getInvduedt()));
            objReportInvoice.setInvoicedate(DateTimeUtil.convertYYYYMMDDtoMMddyyy(objReportDataVO.getInvdt()));
            objReportInvoice.setInvoiceamount(objReportDataVO.getInvgrlocalam());
            objReportInvoice.setPaidamount(objReportDataVO.getInvnetlocalam());
            objReportInvoice.setAdjustmentamount(objReportDataVO.getShrtpaylocalam());
            objReportInvoice.setAdjustmentdescription(objReportDataVO.getShrtpaytx());
        } catch (Exception e) {
            logger.error(eventId, "SmartServiceEngine", "SupplierRemittanceReportServiceImpl", "setinvoiceDetails",
                "Exception  in  setinvoiceDetails - Failure in getPaymentData", AmexLogger.Result.failure, "", e,
                "ErrorMsg", e.getMessage());
        }
    }
    
   


}
