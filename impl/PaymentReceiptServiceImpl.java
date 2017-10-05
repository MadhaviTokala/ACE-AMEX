package com.americanexpress.smartserviceengine.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.PaymentReceiptResultSetVO;
import com.americanexpress.smartserviceengine.common.vo.ReportDispalyVO;
import com.americanexpress.smartserviceengine.dao.CreatePaymentReceiptDAO;
import com.americanexpress.smartserviceengine.dao.UpdatePaymentReceiptDAO;
import com.americanexpress.smartserviceengine.gcs.util.GCSConstants;
import com.americanexpress.smartserviceengine.gcs.util.GCSPaymentUtils;
import com.americanexpress.smartserviceengine.jms.MessageProducer;
import com.americanexpress.smartserviceengine.service.PaymentReceiptService;
import com.americanexpress.smartserviceengine.util.PaymentReceiptHeaderUtil;
import com.itextpdf.text.pdf.BaseFont;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEvent;
import com.lowagie.text.pdf.PdfWriter;

public class PaymentReceiptServiceImpl implements PaymentReceiptService {

    private static AmexLogger logger = AmexLogger.create(PaymentReceiptServiceImpl.class);

    @Autowired
    private CreatePaymentReceiptDAO createPaymentReceiptDAO;

    @Autowired
    private UpdatePaymentReceiptDAO updatePaymentReceiptDAO;

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    @Autowired
    private GCSPaymentUtils gCSPaymentUtils;

    @Resource
    @Qualifier("messageProducer")
    private MessageProducer messageProducer;

    private final String absoultePath;
    private String logoPath;

    private Font labelFont = null;
    private Font valueFont = null;
    private Font ParagraphFontWithBold = null;
    private Font ParagraphFont = null;
    private Font ParagraphFontWithoutBold = null;
    private Font headerFont = null;
    private Font paymentTableHeadingFont = null;
    private Font tableHeadingFont = null;
    private Font bookFont = null;

    public PaymentReceiptServiceImpl(String absoultePath) {
    	this.absoultePath = absoultePath;
    	 setFont(absoultePath);
	}

    @Override
    @SuppressWarnings("unchecked")
    public void createPaymentReceipt(String eventId) throws SSEApplicationException {
        logger.info(eventId, "SmartServiceEngine", "PaymentReceiptServiceImpl", "createPaymentReceipt",
            "Start of  createPaymentReceipt", AmexLogger.Result.success, "Start");
        final long startTimeStamp = System.currentTimeMillis();

        //String resourcePath = absoultePath + EnvironmentPropertiesUtil.getProperty(SchedulerConstants.IMAGE_PATH);
        logoPath = absoultePath + EnvironmentPropertiesUtil.getProperty(SchedulerConstants.LOGO_PATH);
        Document document = null;
        PdfWriter pdfWriter = null;

        try {

            Map<String, Object> inMap = new HashMap<String, Object>();
            Map<String, Object> outMap = createPaymentReceiptDAO.execute(inMap, eventId);
            if (outMap != null && !outMap.isEmpty()) {
                Object object = outMap.get(SchedulerConstants.RESP_CD);
                String responseCode = object != null ? object.toString().trim() : StringUtils.EMPTY;
                if (SchedulerConstants.RESPONSE_CODE_SUCCESS_SSEABO01.equals(responseCode)) {
                    List<PaymentReceiptResultSetVO> paymentDetailsVOs = (List<PaymentReceiptResultSetVO>) outMap.get(SchedulerConstants.RESULT_SET);
                    if(paymentDetailsVOs!=null && !paymentDetailsVOs.isEmpty()){
                    for(PaymentReceiptResultSetVO paymentRecepitResultSetVO:paymentDetailsVOs){
                    	if (paymentRecepitResultSetVO != null) {
		                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		                        document = new Document(PageSize.LETTER, 36, 36, 36, 36);
                                pdfWriter = PdfWriter.getInstance(document, outputStream);//pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(file));
                                pdfWriter.setCompressionLevel(0);
                                PdfPageEvent event=new PaymentReceiptHeaderUtil(logoPath,headerFont,eventId);
                                pdfWriter.setPageEvent(event);
                                document.setMarginMirroring(true);
                                document.setMargins(75f, 70f,10,0);
                                document.open();
                                addChunks(document, 6);
                                Chunk paymentTable= new Chunk("Payment Receipt",paymentTableHeadingFont);
                                Paragraph paymentHeading= new Paragraph(paymentTable);
                                paymentHeading.setAlignment(Element.ALIGN_CENTER);
                                document.add(paymentHeading);
                                addChunks(document,2);
                                document.add(createPaymentTable(paymentRecepitResultSetVO));
                                document.add(createTransFeesPara());
                                addChunks(document,1);
                        		Chunk c4= new Chunk("Sender",tableHeadingFont);
                        		Paragraph sender= new Paragraph(c4);
                        		sender.setAlignment(Element.ALIGN_LEFT);
                        		document.add(sender);
                        		document.add(createSenderTable(paymentRecepitResultSetVO,eventId));
                        		addChunks(document,1);
                        		Chunk c5= new Chunk("Recipient",tableHeadingFont);
                        		Paragraph recipient= new Paragraph(c5);
                        		recipient.setAlignment(Element.ALIGN_LEFT);
                        		document.add(recipient);
                        		document.add(createRecipientTable(paymentRecepitResultSetVO,eventId));
                        		document.newPage();
                        		addChunks(document,6);
                        		createAMEXTandCParagraph(document);
                                		//createParagraph(document);
		                        document.close();
		                        PaymentReceiptResultSetVO paymentReceiptVOreport = paymentRecepitResultSetVO;//.get(0);
		                        ReportDispalyVO reportDisplayVO = new ReportDispalyVO();
		                        setDetailstoReportVO(paymentReceiptVOreport, reportDisplayVO);

		                        String commTrackingId = gCSPaymentUtils.createTrackingId(SchedulerConstants.PYRT);
		                        reportDisplayVO.setReferenceNumber(gCSPaymentUtils.getReferenceNumber());
		                        reportDisplayVO.setTemplateId(EnvironmentPropertiesUtil.getProperty(GCSConstants.GCS_PAYMENT_RECEIPT_TEMPLATE_ID));
		                        reportDisplayVO.setTotalNumberOfPayments(1);
		                        reportDisplayVO.setPaymentReceiptResultSetVOs(paymentRecepitResultSetVO);
		                        try{
		                        String message = gCSPaymentUtils.sendReportThroughEmail(reportDisplayVO, outputStream, commTrackingId);
		                        logger.info(eventId, "SmartServiceEngine", "PaymentReceiptServiceImpl", "generateReport",
		                            "GCS Request xml generated", AmexLogger.Result.success, "", "GCS Request xml", message);
		                        messageProducer.setMessage(message);
		                        messageProducer.sendMessages();
		                        }catch(Exception e){
		                            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SCHED_GCS_FAIL_PAYMENT_RECEIPT_CD, TivoliMonitoring.SCHED_GCS_FAIL_PAYMENT_RECEIPT_MSG, eventId));
		                            logger.error(eventId, "SmartServiceEngine", "PaymentReceiptServiceImpl", "generateReport",
		                                "Exception  in  generating report", AmexLogger.Result.failure, "", e, "ErrorMsg", e.getMessage());
		                        }
		                       udpateGCSStatusOfPYRT(eventId, reportDisplayVO, paymentRecepitResultSetVO, commTrackingId);
	                    }else{
			                    logger.error(eventId, "SmartServiceEngine", "Payment Receipt Scheduler Service",
			                        "PaymentReceiptServiceImpl : createPaymentReceipt", "Check resultset map for SP E3GMR108",
			                        AmexLogger.Result.failure, "Result set is null");
		                }
                    }
                 }else{
	                 logger.error(eventId, "SmartServiceEngine", "Payment Receipt Scheduler Service",
	                     "PaymentReceiptServiceImpl : createPaymentReceipt", "Check resultset map for SP E3GMR108",
	                     AmexLogger.Result.failure, "Result set is null");
                 }
                }else {
                    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_108_ERR_CD, TivoliMonitoring.SSE_SP_108_ERR_MSG, eventId));
                    logger.error(eventId, "SmartServiceEngine", "PaymentReceiptServiceImpl", "createPaymentReceipt",
                        "Check out for the outMap for SP E3GMR108", AmexLogger.Result.failure, "response Code is "
                            + responseCode);
                }
            } else {
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_108_ERR_CD, TivoliMonitoring.SSE_SP_108_ERR_MSG, eventId));
                logger.debug(eventId, "SmartServiceEngine", "Payment Receipt  Scheduler Service",
                    "PaymentReceiptServiceImpl : createPaymentReceipt", "End of Get Organization Details SP E3GMR108",
                    AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
            }
        } catch (Exception e) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, eventId));
            logger.error(eventId, "SmartServiceEngine", "Payment Receipt Scheduler Service", "PaymentReceiptServiceImpl : createPaymentReceipt",
                "Exception while generating Payment Receipt Service call", AmexLogger.Result.failure, "Failed to execute createPaymentReceipt", e, "ErrorMsg:", e.getMessage());
            throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD, EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), e);
        }
        final long endTimeStamp = System.currentTimeMillis();
        logger.info(eventId,"SmartServiceEngine", "PaymentReceiptServiceImpl","createPaymentReceipt",
            "Time taken to execute createPaymentReceipt",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
                logger.info(eventId, "SmartServiceEngine", "PaymentReceiptServiceImpl", "createPaymentReceipt",
            "End of  createPaymentReceipt", AmexLogger.Result.success, "END");
    }

	private void setDetailstoReportVO(PaymentReceiptResultSetVO paymentReceiptVO, ReportDispalyVO reportDispalyVO) {
        reportDispalyVO.setCustomerid(paymentReceiptVO.getCustomerOrganizationId());
        // reportDispalyVO.setCustomeraddress(customeraddress);
        reportDispalyVO.setCustomername(paymentReceiptVO.getCustomerOrganizationName());
        reportDispalyVO.setVendorid(paymentReceiptVO.getSupplierOrganizationId());
        reportDispalyVO.setAboFirstName(paymentReceiptVO.getABOFirstName());
        reportDispalyVO.setAboLastName(paymentReceiptVO.getABOLastName());
        reportDispalyVO.setAboFullName(paymentReceiptVO.getABOFirstName() + " " + paymentReceiptVO.getABOLastName());
        reportDispalyVO.setPaymentrefrencenumber(paymentReceiptVO.getPaymentReferenceNo());
        reportDispalyVO.setPaymentamount(paymentReceiptVO.getPaymentUSDAmount());
        reportDispalyVO.setPaymentdate(paymentReceiptVO.getPaymentCreateDate());
        reportDispalyVO.setPrtrmacid(paymentReceiptVO.getPartnerId());
        reportDispalyVO.setPrtrcustadline1tx(paymentReceiptVO.getCustomerAddressLine1());
        reportDispalyVO.setPrtrcustadline2tx(paymentReceiptVO.getCustomerAddressLine2());
        reportDispalyVO.setPrtrcustadline3tx(paymentReceiptVO.getCustomerAddressLine3());
        reportDispalyVO.setPrtrcustadline4tx(paymentReceiptVO.getCustomerAddressLine4());
        reportDispalyVO.setPrtrcustadline5tx(paymentReceiptVO.getCustomerAddressLine5());
        reportDispalyVO.setPrtrcustadposttownnm(paymentReceiptVO.getCustomerTownName());
        reportDispalyVO.setPrtrcustadrgnareacd(paymentReceiptVO.getCustomerRegionName());
        reportDispalyVO.setPrtrcustadpstlcd(paymentReceiptVO.getCustomerPostalCode());
        reportDispalyVO.setAboEmailId(paymentReceiptVO.getABOemail());
        reportDispalyVO.setTemplateId(EnvironmentPropertiesUtil.getProperty(GCSConstants.GCS_PAYMENT_RECEIPT_TEMPLATE_ID));
        reportDispalyVO.setDocumentName(GCSConstants.PAYMENT_RECEIPT_DOC_NAME);

    }
	
    private void addChunks(Document document, int newLineCount) throws DocumentException {
        for (int i = 1; i <= newLineCount; i++) {
            document.add(new Paragraph(Chunk.NEWLINE));
        }
    }
    
    private PdfPTable createPaymentTable(PaymentReceiptResultSetVO paymentReceiptVO) throws ParseException, DocumentException {
		PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(90);
        int length=paymentReceiptVO.getPaymentUSDAmount().length();
        float[] colWidths={30,8,length*2+7,40-(length*2+7)};
        table.setWidths(colWidths);
        insertCell(table,SchedulerConstants.PAYMENT_SUBMISSION_DATE_TIME,labelFont,Element.ALIGN_RIGHT,1,true);
        String subDate = DateTimeUtil.convertYYYYMMDDtoMMddyyyTS(paymentReceiptVO.getPaymentCreateDate());
        if(StringUtils.isNotBlank(subDate)){
        	subDate = subDate+" MST";
        }
        insertCell(table,subDate,valueFont,Element.ALIGN_LEFT,3,false);
        
        insertCell(table,SchedulerConstants.PAYMENT_ID,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table,paymentReceiptVO.getPaymentId(),valueFont,Element.ALIGN_LEFT,3,false);
        /*insertCell(table,SchedulerConstants.PAYMENT_REF_NUMBER,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table,paymentReceiptVO.getPaymentReferenceNo(),valueFont,Element.ALIGN_LEFT,3,false);*/
        insertCell(table,SchedulerConstants.PYMT_USER_ID,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table, "",valueFont,Element.ALIGN_LEFT,3,false);
        insertCell(table,SchedulerConstants.CURRENCY,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table,"USD",valueFont,Element.ALIGN_LEFT,3,false);
        insertCell(table,SchedulerConstants.EXCHANGE_RATE,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table,"NA",valueFont,Element.ALIGN_LEFT,3,false);
        insertCell(table,SchedulerConstants.PYMT_AMT,labelFont,Element.ALIGN_RIGHT,1,true);
        String paymentAmount=paymentReceiptVO.getPaymentUSDAmount();
        if(StringUtils.isBlank(paymentAmount)){
        	paymentAmount = "0.00";
        }
        insertAmountCell(table,paymentAmount,false);
        insertCell(table,SchedulerConstants.TRANS_FEES,labelFont,Element.ALIGN_RIGHT,1,true);
        String transFee=paymentReceiptVO.getTransactionFeeUSDAmount();
        if(StringUtils.isBlank(transFee)){
        	transFee = "0.00";
        }
        insertAmountCell(table,transFee,false);
        insertCell(table,SchedulerConstants.TOTAL_AMT,labelFont,Element.ALIGN_RIGHT,1,true);
        String cost = paymentReceiptVO.getPaymentUSDAmount();
        BigDecimal roundCost = null;
        if(StringUtils.isNotBlank(cost)){
                roundCost = new BigDecimal(cost.trim()).setScale(2,BigDecimal.ROUND_HALF_UP);
        }else{
                roundCost = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
        }

        String transactionFee =paymentReceiptVO.getTransactionFeeUSDAmount();
        BigDecimal roundedTransactionFee = null;
        if(StringUtils.isNotBlank(transactionFee)){
                roundedTransactionFee = new BigDecimal(transactionFee.trim()).setScale(2,BigDecimal.ROUND_HALF_UP);
        }else{
                roundedTransactionFee = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
        }
        insertAmountCell(table,String.valueOf(new BigDecimal(roundCost.doubleValue() + roundedTransactionFee.doubleValue()).setScale(2,BigDecimal.ROUND_HALF_UP)),true);
        table.setSpacingAfter(5);
        return table;
	}
    
    private Paragraph createTransFeesPara(){
    	Paragraph para=new Paragraph();
		Chunk heading= new Chunk("*Transaction Fees: ",bookFont);
		Chunk body=new Chunk("Additional transaction fees may be charged by the UI Provider (as such term is defined in the American Express Payment Services Client Agreement you entered into with American Express). Refer to your agreement with the UI Provider for details on such additional fees.",ParagraphFontWithoutBold);
		para.add(heading);
		para.add(body);
		para.setIndentationLeft(105f);
		para.setLeading(9);
		return para;
    }
    
    private PdfPTable createSenderTable(PaymentReceiptResultSetVO paymentReceiptVO,String eventId) throws DocumentException {
		PdfPTable table = new PdfPTable(2);
		table.setWidthPercentage(90);
        float[] colWidths={30F,60F};
        table.setWidths(colWidths);
        table.setSpacingBefore(5);
        insertCell(table,SchedulerConstants.CLNT_NAME,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table,paymentReceiptVO.getCustomerOrganizationName(),valueFont,Element.ALIGN_LEFT,1,false);
        insertCell(table,SchedulerConstants.BANK_NAME,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table,paymentReceiptVO.getCustomerBankName(),valueFont,Element.ALIGN_LEFT,1,false);
        insertCell(table,SchedulerConstants.BANK_ACC_NO,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table,maskAccountNumber(paymentReceiptVO.getCustomerAccNum(),eventId),valueFont,Element.ALIGN_LEFT,1,false);
        insertCell(table,SchedulerConstants.PAYMENT_REF_NUMBER,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table,paymentReceiptVO.getPaymentReferenceNo(),valueFont,Element.ALIGN_LEFT,1,false);
		return table;
	}
    
    private PdfPTable createRecipientTable(PaymentReceiptResultSetVO paymentReceiptVO,String eventId) throws DocumentException {
		PdfPTable table = new PdfPTable(2);
		table.setWidthPercentage(90);
        float[] colWidths={30F,60F};
        table.setWidths(colWidths);
        table.setSpacingBefore(5);
        insertCell(table,SchedulerConstants.RCPNT_NAME,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table,paymentReceiptVO.getSupplierName(),valueFont,Element.ALIGN_LEFT,1,false);
        insertCell(table,SchedulerConstants.BANK_NAME,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table,paymentReceiptVO.getBankName(),valueFont,Element.ALIGN_LEFT,1,false);
        insertCell(table,SchedulerConstants.BANK_ACC_NO,labelFont,Element.ALIGN_RIGHT,1,true);
        insertCell(table,maskAccountNumber(paymentReceiptVO.getAccountNumber(),eventId),valueFont,Element.ALIGN_LEFT,1,false);
		return table;
	}
    
    private void createAMEXTandCParagraph(Document document) throws DocumentException {
    	Chunk p1=new Chunk("If you have any questions about this payment receipt confirmation or to report any complaints, please call Customer Service at 1-855-431-2430, email us at b2bPaymentSupport@aexp.com, or contact us by regular mail:",ParagraphFont);
		Paragraph TC1= new Paragraph(p1);
		TC1.setLeading(12);		
		document.add(TC1);
		Chunk new1= new Chunk("American Express Travel Related Services Company, Inc.",ParagraphFont);
		Chunk new2= new Chunk("Attention: Global Corporate Payments",ParagraphFont);
		Chunk new3= new Chunk("200 Vesey Street, Mail Code - 01-29-07 - New York, NY 10285",ParagraphFont);
		Paragraph TCnew1 = new Paragraph(new1);
		TCnew1.setLeading(12);
		Paragraph TCnew2 = new Paragraph(new2);
		TCnew2.setLeading(12);
		Paragraph TCnew3 = new Paragraph(new3);
		TCnew3.setLeading(12);
		document.add(TCnew1);
		document.add(TCnew2);
		document.add(TCnew3);
		document.add(new Paragraph(Chunk.NEWLINE));
		Chunk p2=new Chunk("You must notify us of any errors within one business day of the payment initiation date. To the extent permitted by applicable law,"+
		" American Express will have no obligation or liability to you for any errors in a transaction after one business day of the transaction date, or for any non-delivery"+
		", delays or other failure in payment due to the action or inaction of the payee or any intermediaries. After one business day following the transaction date, American"+
		" Express may, but will not have any obligation to, assist you in attempting to recover funds sent to a payee in error. You will not be entitled to any refund of your"+
		" funds from American Express to the extent not recoverable from the payee. Any funds returned to you may be reduced as a result of our costs of processing your transaction."+
		" Settlement of payments to the payee is provisional until the payee’s bank (RDFI) receives final settlement.  If the RDFI does not receive such settlement, your payee will not"+
		" receive the payment and you will not be deemed to have paid the payee.  See Terms and Conditions for additional information.",ParagraphFont);
		Paragraph TC2= new Paragraph(p2);
		TC2.setLeading(12);
		document.add(TC2);
		document.add(new Paragraph(Chunk.NEWLINE));
		Chunk p3= new Chunk("American Express Travel Related Services Company, Inc. (“American Express”) ",ParagraphFontWithBold);
		Chunk p4= new Chunk("is authorized to engage in the money transmission business, including the offer of ACH and wire transfers.",ParagraphFont);
		Paragraph TC3=new Paragraph();
		TC3.add(p3);
		TC3.add(p4);
		TC3.setLeading(12);
		document.add(TC3);
		document.add(new Paragraph(Chunk.NEWLINE));
		Chunk p5= new Chunk("Notice to California Clients",ParagraphFontWithBold);
		p5.setUnderline(0.1f, -0.5f);
		Chunk p6=new Chunk(": You, the customer, are entitled to a refund of the money to be transmitted as the result of this agreement if American Express does not forward"+
		" the money received from you within 10 days of the date of its receipt, or does not give instructions committing an equivalent amount of money to the person designated"+
				" by you within 10 days of the date of the receipt of the funds from you unless otherwise instructed by you.",ParagraphFontWithBold);
		Paragraph TC4=new Paragraph();
		TC4.add(p5);
		TC4.add(p6);
		TC4.setLeading(12);
		document.add(TC4);
		Chunk p7= new Chunk("If your instructions as to when the moneys shall be forwarded or transmitted are not complied with and the money has not yet been forwarded or"+
		" transmitted, you have a right to a refund of your money. If you want a refund, you must mail or deliver your written request to American Express Travel Related "+
				"Services Company, Inc. at 200 Vesey Street, New York, NY 10285, Attention: Global Corporate Payments. If you do not receive your refund, you may be entitled to "+
		"your money back plus a penalty of up to $1,000 and attorney’s fees pursuant to Section 2102 of the California Financial Code.",ParagraphFontWithBold);
		Paragraph TC5=new Paragraph(p7);
		TC5.setLeading(12);
		document.add(TC5);
		document.add(new Paragraph(Chunk.NEWLINE));
		Chunk p8=new Chunk("Notice to New York Clients",ParagraphFontWithBold);
		p8.setUnderline(0.1f, -0.5f);
		Chunk p9=new Chunk(": American Express is not liable for special, incidental, consequential or punitive damages or indirect losses or damages, even if American Express"+
		" is on notice of the possibility of such losses or damages.  See Terms and Conditions for other limitations on liability.",ParagraphFont);
		Paragraph TC6=new Paragraph();
		TC6.add(p8);
		TC6.add(p9);
		TC6.setLeading(12);
		document.add(TC6);
		document.add(new Paragraph(Chunk.NEWLINE));
		Chunk p10=new Chunk("Notice to Texas Clients",ParagraphFontWithBold);
		p10.setUnderline(0.1f, -0.5f);
		Chunk p11=new Chunk(": If you have a complaint, first contact the customer assistance division of ",ParagraphFont);
		Chunk p12=new Chunk("American Express Travel Related Services Company, Inc. at 866-458-8325.",ParagraphFontWithBold);
		Chunk p13=new Chunk(" If you still have an unresolved complaint regarding the company’s money transmission activity, please direct your complaint to: Texas Department"+
		" of Banking, 2601 North Lamar Boulevard, Austin, Texas 78705, 1-877-276-5554 (toll free), www.banking.state.tx.us.",ParagraphFont);
		Paragraph TC7=new Paragraph();
		TC7.add(p10);
		TC7.add(p11);
		TC7.add(p12);
		TC7.add(p13);
		TC7.setLeading(12);
		document.add(TC7);
	}
    
    private static void insertCell(PdfPTable table, String text, Font font, int align,int colspan,boolean noWrap) {
		PdfPCell cell=new PdfPCell(new Phrase(StringUtils.stripToEmpty(text),font));
		cell.setBorder(0);
		cell.setHorizontalAlignment(align);
		cell.setVerticalAlignment(0);
		cell.setNoWrap(noWrap);
        cell.setPaddingLeft(5);
        cell.setFixedHeight(100f);
        cell.setPaddingRight(3);
       	cell.setColspan(colspan);
        cell.setMinimumHeight(25f);
		table.addCell(cell);
	}
    
    private void insertAmountCell(PdfPTable table, String currValue,boolean topBorder) {
		PdfPCell cell=new PdfPCell();
		cell.setBorder(0);
		cell.setPhrase(new Phrase("USD",valueFont));
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setVerticalAlignment(0);
		cell.setNoWrap(true);
        cell.setPaddingLeft(5);
        cell.setFixedHeight(100f);
        cell.setPaddingRight(3);
        cell.setMinimumHeight(25f);
        PdfPCell cell1=new PdfPCell();
		cell1.setBorder(0);
		cell1.setPhrase(new Phrase(StringUtils.stripToEmpty(currValue),valueFont));
		cell1.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cell1.setVerticalAlignment(0);
		cell1.setNoWrap(true);
        cell1.setPaddingLeft(5);
        cell1.setFixedHeight(100f);
        cell1.setPaddingRight(3);
        cell1.setMinimumHeight(25f);
        PdfPCell cell2=new PdfPCell();
		cell2.setBorder(0);
		cell2.setPhrase(new Phrase("   "));
		cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell2.setVerticalAlignment(0);
		cell2.setNoWrap(true);
        cell2.setPaddingLeft(5);
        cell2.setFixedHeight(100f);
        cell2.setPaddingRight(3);
        cell2.setMinimumHeight(25f);
        if(topBorder){
        	 cell.setBorderWidthTop(1f);
             cell.setBorderColorTop(new Color(75,172,198));
             cell1.setBorderWidthTop(1f);
             cell1.setBorderColorTop(new Color(75,172,198));
        	 cell2.setBorderWidthTop(1f);
             cell2.setBorderColorTop(new Color(75,172,198));
        }
        table.addCell(cell);
        table.addCell(cell1);
        table.addCell(cell2);
	}

    private void setFont(String filePath) {
        FontFactory.register(filePath + EnvironmentPropertiesUtil.getProperty(SchedulerConstants.BENTONSANS_BOOK_FONT_PATH), "book_font");
        FontFactory.register(filePath + EnvironmentPropertiesUtil.getProperty(SchedulerConstants.BENTONSANS_REGULAR_FONT_PATH), "regular_font");

        labelFont = FontFactory.getFont("book_font",BaseFont.CP1252,BaseFont.EMBEDDED);
        labelFont.setSize(12f);
        labelFont.setStyle(Font.NORMAL);
        labelFont.setColor(new Color(128,128,128));

        valueFont =  FontFactory.getFont("book_font",BaseFont.CP1252,BaseFont.EMBEDDED);
        valueFont.setSize(14f);
        valueFont.setStyle(Font.NORMAL);
        valueFont.setColor(new Color(49,132,155));
        
        bookFont =  FontFactory.getFont("book_font",BaseFont.CP1252,BaseFont.EMBEDDED);
        bookFont.setSize(10f);
        bookFont.setStyle(Font.NORMAL);

        ParagraphFontWithBold = FontFactory.getFont("regular_font",BaseFont.IDENTITY_H,BaseFont.EMBEDDED);
        ParagraphFontWithBold.setSize(10f);
        ParagraphFontWithBold.setStyle(Font.BOLD);

        ParagraphFontWithoutBold = FontFactory.getFont("regular_font",BaseFont.CP1252,BaseFont.EMBEDDED);
        ParagraphFontWithoutBold.setSize(7.5f);
        ParagraphFontWithoutBold.setStyle(Font.NORMAL);
        ParagraphFontWithoutBold.setColor(new Color(128,128,128));
        
        ParagraphFont = FontFactory.getFont("regular_font",BaseFont.CP1252,BaseFont.EMBEDDED);
        ParagraphFont.setSize(10f);
        ParagraphFont.setStyle(Font.NORMAL);
        
        headerFont = FontFactory.getFont("book_font",BaseFont.CP1252,BaseFont.EMBEDDED);
        headerFont.setSize(26f);
        headerFont.setStyle(Font.NORMAL);
        headerFont.setColor(new Color(23,54,93));
        
        paymentTableHeadingFont = FontFactory.getFont("book_font",BaseFont.CP1252,BaseFont.EMBEDDED);
        paymentTableHeadingFont.setSize(20f);
        paymentTableHeadingFont.setStyle(Font.NORMAL);
        paymentTableHeadingFont.setColor(new Color(54,95,145));
        
        tableHeadingFont = FontFactory.getFont("book_font",BaseFont.CP1252,BaseFont.EMBEDDED);
        tableHeadingFont.setSize(18f);
        tableHeadingFont.setStyle(Font.NORMAL);
        tableHeadingFont.setColor(new Color(54,95,145));
        
    }

    private Map<String, Object> udpateGCSStatusOfPYRT(String eventId, ReportDispalyVO reportDisplayVO, PaymentReceiptResultSetVO paymentRecepitResultSetVO, String commTrackingId) {
		Map<String, Object> inputMap = new HashMap<String, Object>();
		inputMap.put(SchedulerConstants.IN_ABO_EMAIL_AD_TX, reportDisplayVO.getAboEmailId().toLowerCase());
		inputMap.put(SchedulerConstants.IN_CUST_ORG_ID, reportDisplayVO.getCustomerid());
		inputMap.put(SchedulerConstants.IN_SSE_TRK_ID, reportDisplayVO.getReferenceNumber());
		inputMap.put(SchedulerConstants.IN_GCS_TRANS_ID, commTrackingId);
		inputMap.put(SchedulerConstants.IN_RPT_STA_CD, StringUtils.EMPTY);
		inputMap.put(SchedulerConstants.IN_RPT_STA_DS, StringUtils.EMPTY);
		inputMap.put(SchedulerConstants.IN_UPDT_IN  , SchedulerConstants.CHAR_A);
		inputMap.put(SchedulerConstants.IN_EMM_PYMT_REFER_NO,paymentRecepitResultSetVO.getPaymentReferenceNo());
		inputMap.put(SchedulerConstants.IN_PRTR_MAC_ID,paymentRecepitResultSetVO.getPartnerId());

		Map<String, Object> outMap = updatePaymentReceiptDAO.execute(inputMap, eventId);
		if (outMap != null && !outMap.isEmpty()
                && ApiConstants.SP_SUCCESS_SSABO001.equals(StringUtils.stripToEmpty((String) outMap
                    .get(ApiConstants.SP_RESP_CD)))) {
                logger.info(eventId, "SmartServiceEngine", "Payment Receipt Service", "PaymentReceiptServiceImpl : generateReport",
                		"Update SP E3GMR109 Successfull", AmexLogger.Result.success, "");
            } else  {
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_109_ERR_CD, TivoliMonitoring.SSE_SP_109_ERR_MSG, eventId));
                logger.error(eventId, "SmartServiceEngine", "Payment Receipt Service", "PaymentReceiptServiceImpl -generateReport ", "SP E3GMR109 is NOT successful",
                    AmexLogger.Result.failure, "", ApiConstants.SQLCODE_PARM, (String) outMap.get(ApiConstants.SQLCODE_PARM), "resp_code",
                    (String) outMap.get(ApiConstants.SP_RESP_CODE), "resp_msg", (String) outMap.get(ApiConstants.SP_RESP_MSG));
            } 
		return outMap;
	}
    
    public static String maskAccountNumber(String number, String apiMsgId) {
  	  String maskedNum = null;
  	  int length=4;
  	  try{
  	   if (StringUtils.isNotBlank(number)){
  	    maskedNum = number.trim();
  	    if (maskedNum.length() >= length) {
  	     maskedNum = SchedulerConstants.MASK+number.substring(number.length()-4, number.length());
  	    } else{
  	     maskedNum = number;
  	    }
  	   }
  	  }catch(Exception e){
  		  logger.error(apiMsgId, "SmartServiceEngine Scheduler", "Mask PII Data", "PaymentReceiptServiceImpl:maskAccRoutingNumber",
  	                "Exception while masking ACC/RTR number in logs", AmexLogger.Result.failure, "Exception while masking ACC/RTR number in logs");
  	  }
  	  return maskedNum;
  	 }
    
}
