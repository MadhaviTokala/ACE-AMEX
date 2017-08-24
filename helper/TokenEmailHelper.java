package com.americanexpress.smartserviceengine.helper;

import static com.americanexpress.smartserviceengine.common.constants.ServiceConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.etv.issuetoken.data.IssueTokenResponseOutVO;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.ServiceConstants;
import com.americanexpress.smartserviceengine.common.constants.SplunkConstants;
import com.americanexpress.smartserviceengine.common.constants.TokenPaymentStatus;
import com.americanexpress.smartserviceengine.common.enums.TokenPaymentResponseCodes;
import com.americanexpress.smartserviceengine.common.enums.TokenStatus;
import com.americanexpress.smartserviceengine.common.mail.SendMail;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.EmailRequest;
import com.americanexpress.smartserviceengine.common.vo.InvoiceInformationVO;
import com.americanexpress.smartserviceengine.common.vo.RemitReportInvoiceVO;
import com.americanexpress.smartserviceengine.common.vo.RemitReportVO;
import com.americanexpress.smartserviceengine.common.vo.SupplierInformationVO;
import com.americanexpress.smartserviceengine.common.vo.TokenPaymentRecordVO;
import com.americanexpress.smartserviceengine.dao.InvoiceInformationDAO;
import com.americanexpress.smartserviceengine.dao.SupplierOrgDetailsDAO;
import com.americanexpress.smartserviceengine.service.TokenEmailRemittanceReportService;

public class TokenEmailHelper{

	private static AmexLogger LOGGER = AmexLogger.create(TokenEmailHelper.class);
	private static final String EVENT_NAME = TokenEmailHelper.class.getSimpleName();
	private static final String SUPPLIERNAME="supplierName";
	private static final String TOKENEXPIRATIONDATE="tokenExpirationDate";
	private static final String TOKENNUMBER="tokenNumber";
	private static final String EXPIRYDATE="expiryDate";
	private static final String CURRENTYEAR="currentYear";
	private static final String BUYERNAME="buyerName";
	private static final String TOKENCREATIONDATE="tokenCreationDate";
	private static final String MAILTXTFRMT="text/html; charset=utf-8";
	private static final String PRTR_ETV_SUBJECT = CommonUtils.getParnerIdForAccess(ApiConstants.PRTR_ETV_SUBJECT,"");
	
	@Autowired
	private TivoliMonitoring tivoliMonitoring;
	
	@Autowired
	private SendMail sendMail;
	
	@Autowired
	private SupplierOrgDetailsDAO supplierOrgDetailsDAO;
	
	@Autowired
	private InvoiceInformationDAO invoiceInformationDAO;
	
	@Autowired
	private StatusUpdateHelper statusUpdateHelper;
	@Autowired
	private TokenEmailRemittanceReportService tokenEmailRemittanceReportService; 
	
	
	public boolean sendToken(TokenPaymentRecordVO tokenPaymentRecordVO, IssueTokenResponseOutVO issueTokenResponseOutVO){
		boolean success = false;
		long startTimeStamp = System.currentTimeMillis();
		long endTimeStamp =  0;
		String apiMsgID = ThreadLocalManager.getApiMsgId();
		try{
			
			
			String emailGenPartner = CommonUtils.getParnerIdForAccess(ApiConstants.TOKENGEN_EMAIL_SOLACE_ACE_PRTNR_IDS,apiMsgID);
			
			LOGGER.info(apiMsgID, APPLICATION_NAME, EVENT_NAME, "emailGenPartner", "emailGenPartner", AmexLogger.Result.success,
					"COMPLETED", "emailGenPartnerId",emailGenPartner);
			
			if(emailGenPartner.length()>0){
				
				if(emailGenPartner.toString().contains(tokenPaymentRecordVO.getPrtrMacId())){	
					LOGGER.info(apiMsgID, APPLICATION_NAME, EVENT_NAME, "emailGenPartner", "emailGenPartner", AmexLogger.Result.success, 
							"COMPLETED", "emailGenPartnerId",emailGenPartner);
				
					SupplierInformationVO supplierInformationVO = supplierOrgDetailsDAO.getSupplierDetails(tokenPaymentRecordVO.getPaymentID());
					List<InvoiceInformationVO> invoices = null;
					
					/*if(tokenPaymentRecordVO.getInvoiceInformationVOList()!=null && tokenPaymentRecordVO.getInvoiceInformationVOList().size()>0){
						invoices = tokenPaymentRecordVO.getInvoiceInformationVOList();
					}else{
						invoices = invoiceInformationDAO.getInvoiceDetails(tokenPaymentRecordVO.getPaymentID());
					}*/
					
					if(!tokenPaymentRecordVO.isPaymentReqApiInitCall()){
						invoices = invoiceInformationDAO.getInvoiceDetails(tokenPaymentRecordVO.getPaymentID());
					}else{
						invoices = tokenPaymentRecordVO.getInvoiceInformationVOList();
					}
					
					
					RemitReportVO remitReportVO = formRemitReportVO(tokenPaymentRecordVO,supplierInformationVO,invoices,apiMsgID);
					success = tokenEmailRemittanceReportService.generateReport(apiMsgID, remitReportVO, formEmailRequest(tokenPaymentRecordVO,supplierInformationVO,
							issueTokenResponseOutVO , apiMsgID));
				}else{
					LOGGER.info(apiMsgID, APPLICATION_NAME, EVENT_NAME, "emailGenPartner", "emailGenPartner", AmexLogger.Result.success, 
							"COMPLETED", "emailGenPartnerId",emailGenPartner);
					success = prepareAndSendTokenEmailRequest(tokenPaymentRecordVO, issueTokenResponseOutVO); // TODO - Have to put partner specific logic here
				}
			}
			//System.out.println("mail success "+success);
			if(success){
				//send email token failure - update token and payment status to email sent
				saveEmailSentStatus(tokenPaymentRecordVO, issueTokenResponseOutVO, apiMsgID);
				endTimeStamp = System.currentTimeMillis();
				LOGGER.info(apiMsgID, APPLICATION_NAME, EVENT_NAME, "sendToken", "Token Sent to Supplier Successfully", AmexLogger.Result.success, 
						"COMPLETED", SplunkConstants.PYMT_DURATION_BTW_INPROGRESS_TO_SENT, (endTimeStamp-startTimeStamp)+ " milliseconds");
			}else{
				String paymtRespFailDesc = TokenPaymentResponseCodes.getErrorDescription(TokenPaymentResponseCodes.SEND_EMAIL_FAILED);
				//send email token failure - update retry count, cancel the token and payment in DB and cancel token in ETV.
				statusUpdateHelper.cancelTokenAndFailPayment(tokenPaymentRecordVO.getPaymentID(), 
						TokenPaymentStatus.FAILED.getStatusCode(),TokenPaymentStatus.FAILED.getDbStatusCode(), paymtRespFailDesc, issueTokenResponseOutVO,tokenPaymentRecordVO,false);
			}
		}catch(Exception ex){
			LOGGER.error(apiMsgID, APPLICATION_NAME, EVENT_NAME, "sendToken", "Update response after sending token", AmexLogger.Result.failure, 
					"Unexpected Exception while Updating response after sending token", ex, "Token Sent Status", success? "SUCCESS" : "FAILURE");
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_EMAIL_FAILURE_ERR_CD, TivoliMonitoring.SSE_EMAIL_FAILURE_ERR_MSG, apiMsgID));
		}
		return success;
	}

	
	private EmailRequest formEmailRequest(TokenPaymentRecordVO tokenPaymentRecordVO , 
			SupplierInformationVO supplierInformationVO ,IssueTokenResponseOutVO issueTokenResponseOutVO,String apiMsgId) {
		EmailRequest email = new EmailRequest();
		String message = null; //getMessage(tokenPaymentRecordVO, supplierInformationVO, issueTokenResponseOutVO);
		//String subject = getTokenEmailSubject(); // TODO - Pull the subject based on partner
		String subject = "";// TODO - Pull the subject based on partner
		String buyerName = "";
		
		HashMap<String,String> keyValueMap = CommonUtils.getHashMapKeyValue(PRTR_ETV_SUBJECT, "");		
		if(keyValueMap.containsKey(tokenPaymentRecordVO.getPrtrMacId())){
			String nonFormattedSubject = keyValueMap.get(tokenPaymentRecordVO.getPrtrMacId());
			// Sprint40: changes START
			LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "formEmailRequest", "Org DBA name", AmexLogger.Result.success, "COMPLETED", "orgDBAName",tokenPaymentRecordVO.getOrgDBAName());
			LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "formEmailRequest", "Org name", AmexLogger.Result.success, "COMPLETED", "orgName",tokenPaymentRecordVO.getOrgName());
			buyerName = tokenPaymentRecordVO.getOrgDBAName();
			if(GenericUtilities.isNullOrEmpty(buyerName))
			{
				buyerName = tokenPaymentRecordVO.getOrgName();
			}
			subject = CommonUtils.getFormattedMessage(nonFormattedSubject, new Object[]{buyerName});
			// Sprint40: changes END
		}		
		
		
		LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "formEmailRequest", "Subject formed Successfully", AmexLogger.Result.success, "COMPLETED", "subject1",subject);
	
		String toRecipient = supplierInformationVO.getSupplierOrgEmail();
		/*String maskedMessageBody  = message.replace(getFormattedTokenNumber(issueTokenResponseOutVO.getTokenNumber()),
				issueTokenResponseOutVO.getTokenNumber()==null ? null : GenericUtilities.maskField(issueTokenResponseOutVO.getTokenNumber().trim(),SchedulerConstants.START_MASK_TOKEN_INDEX,
						SchedulerConstants.END_MASK_TOKEN_INDEX,SchedulerConstants.SSE_MASK_CHAR,apiMsgId, false));*/
		email.setHtmlBody(message);
		email.setSubject(subject);
		email.setTo(toRecipient);
		email.setFrom(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.DEFAULT_EMAIL_FROM_ADDRESS));
		email.setVoltage(Boolean.valueOf(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.TOKEN_VOLTAGE_EMAIL)));
		email.setTemplate(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.TOKEN_TEMPLATE_NAME));
		email.setFormat(MAILTXTFRMT);
		
		// Sprint40: changes START
				String propPartnerIdMap = EnvironmentPropertiesUtil.getProperty(ApiConstants.SSE_PAYVE_PARTNERID_MAP);
				String[] partnerIdMapList = propPartnerIdMap.split(ApiConstants.CHAR_COLON);
				String intacctPartnerId = partnerIdMapList[0];
				StringBuilder supplierName = new StringBuilder();
				
				LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "formEmailRequest", "Partner ID", AmexLogger.Result.success, "COMPLETED", "intacctPartnerId",intacctPartnerId);
				LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "formEmailRequest", "Supplier Org Name", AmexLogger.Result.success, "COMPLETED", "supplierOrgName",supplierInformationVO.getSupplierOrgName());
				LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "formEmailRequest", "Supplier First Name", AmexLogger.Result.success, "COMPLETED", "supplierFirstName",supplierInformationVO.getSupplierFirstName());
				LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "formEmailRequest", "Supplier Last Name", AmexLogger.Result.success, "COMPLETED", "supplierLastName",supplierInformationVO.getSupplierLastName());
								
				if(null!= tokenPaymentRecordVO.getPrtrMacId() && tokenPaymentRecordVO.getPrtrMacId().equals(intacctPartnerId)) {
						supplierName.append(supplierInformationVO.getSupplierOrgName());
				}
				else {
					if(!GenericUtilities.isNullOrEmpty(supplierInformationVO.getSupplierFirstName())){
						supplierName.append(supplierInformationVO.getSupplierFirstName()).append(" ");
					}
					if(!GenericUtilities.isNullOrEmpty(supplierInformationVO.getSupplierLastName())){
						supplierName.append(supplierInformationVO.getSupplierLastName());			
					}
					if( (GenericUtilities.isNullOrEmpty(supplierInformationVO.getSupplierFirstName())) && (GenericUtilities.isNullOrEmpty(supplierInformationVO.getSupplierLastName())) ) {
						supplierName.append(supplierInformationVO.getSupplierOrgName());
					}
				}
				LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "formEmailRequest", "Supplier Name", AmexLogger.Result.success, "COMPLETED", "supplierName",supplierName.toString().trim());
		// Sprint40: changes END
		
		HashMap<String, String> tokens = new HashMap<String, String>();
		tokens.put(SUPPLIERNAME,supplierName.toString().trim());
		tokens.put(BUYERNAME,buyerName);		
		tokens.put(TOKENCREATIONDATE,GenericUtilities.convertFromUTCToMST_MMMMDDYYYYFormat(issueTokenResponseOutVO.getTokenStartDate()));		
		tokens.put(TOKENEXPIRATIONDATE,GenericUtilities.convertFromUTCToMST_MMMMDDYYYYFormat(issueTokenResponseOutVO.getTokenExpiry()));		
		tokens.put(TOKENNUMBER,getFormattedTokenNumber(issueTokenResponseOutVO.getTokenNumber()));		
		tokens.put(EXPIRYDATE,GenericUtilities.convertFromUTCToMSTMMDDYYYYFormat(issueTokenResponseOutVO.getTokenExpiry()));		
		tokens.put(CURRENTYEAR,String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
		if(tokens!=null && !tokens.isEmpty()){
			email.setTokens(tokens);	
		}
		
		return email;
	}
	private RemitReportVO formRemitReportVO(TokenPaymentRecordVO tokenPaymentRecordVO,
			SupplierInformationVO supplierInformationVO,List<InvoiceInformationVO> invoices,String apiMsgId) {
		RemitReportVO remitReportVO = new RemitReportVO();
		if(tokenPaymentRecordVO!=null){
			
			
			
			remitReportVO.setPrtrmacid(tokenPaymentRecordVO.getPrtrMacId());
			remitReportVO.setPymtid(tokenPaymentRecordVO.getPaymentID());
			remitReportVO.setBuypymtid(tokenPaymentRecordVO.getBuyerPaymentID());
			remitReportVO.setPymtdt(tokenPaymentRecordVO.getDueDate());
			remitReportVO.setPymtalphacurrcd(tokenPaymentRecordVO.getCurrencyCode());
			remitReportVO.setPymtlocalam(tokenPaymentRecordVO.getLocalAmount()); // TRANS_LOCAL_AM
			remitReportVO.setPymtusdam(tokenPaymentRecordVO.getPaymentAmount());// TRANS_USD_AM
			remitReportVO.setPrtrcustorgid(tokenPaymentRecordVO.getCustomerOrgId());
			remitReportVO.setPrtrsplyorgid(tokenPaymentRecordVO.getSupplierOrgId());
			remitReportVO.setPrtrcustorgnm(ApiUtil.fetchString1OrString2(
					tokenPaymentRecordVO.getOrgDBAName(),tokenPaymentRecordVO.getOrgName()));
					
			// Setting EMM Ref No same as buyer payment reference Id
			remitReportVO.setEntrprmoneymovementpymtreferno(tokenPaymentRecordVO.getBuyerPaymentID());
			
			remitReportVO.setPrtrcustadposttownnm(tokenPaymentRecordVO.getTownOrCityName());
			remitReportVO.setPrtrcustadrgnareacd(ApiUtil.fetchString1OrString2(
					tokenPaymentRecordVO.getStateAreaCode(),tokenPaymentRecordVO.getStateAreaName()));
			remitReportVO.setPrtrcustadpstlcd(tokenPaymentRecordVO.getZipCode());	
			
			if(supplierInformationVO!=null){
				remitReportVO.setPrtrsplyorgnm(supplierInformationVO.getSupplierOrgName()); 
				remitReportVO.setOrgemailadtx(supplierInformationVO.getSupplierOrgEmail());	
			}
			
			
			remitReportVO.setPrtrcustadline1tx(tokenPaymentRecordVO.getAddressLine1()); // FROM 714 SP
			remitReportVO.setPrtrcustadline2tx(tokenPaymentRecordVO.getAddressLine2());
			remitReportVO.setPrtrcustadline3tx(tokenPaymentRecordVO.getAddressLine3());
			remitReportVO.setPrtrcustadline4tx(tokenPaymentRecordVO.getAddressLine4());
			remitReportVO.setPrtrcustadline5tx(tokenPaymentRecordVO.getAddressLine5());
			List<RemitReportInvoiceVO> remitReportInvoiceVOList = new ArrayList<RemitReportInvoiceVO>();
			if(remitReportInvoiceVOList!=null){				
			
				for(InvoiceInformationVO invoiceInformationVO:invoices){
					RemitReportInvoiceVO remitReportInvoiceVO = new RemitReportInvoiceVO();
					
					remitReportInvoiceVO.setInvid(invoiceInformationVO.getInvoiceId());
					remitReportInvoiceVO.setBuyinvreferno(invoiceInformationVO.getBuyerInvoiceRefNo());
					remitReportInvoiceVO.setInvduedt(String.valueOf(invoiceInformationVO.getInvoiceDueDate()));
					remitReportInvoiceVO.setInvdt(String.valueOf(invoiceInformationVO.getInvoiceDate()));
					remitReportInvoiceVO.setSplyinvreferno(invoiceInformationVO.getSupplierInvoiceRefNo());
					remitReportInvoiceVO.setInvgrlocalam(String.valueOf(invoiceInformationVO.getInvoiceGrossAmt()));
					remitReportInvoiceVO.setInvnetlocalam(String.valueOf(invoiceInformationVO.getInvoiceNetAmt()));
					remitReportInvoiceVO.setBuyinfo1tx(invoiceInformationVO.getBuyerInfo1());
					remitReportInvoiceVO.setBuyinfo2tx(invoiceInformationVO.getBuyerInfo2());
					remitReportInvoiceVO.setBuyinfo3tx(invoiceInformationVO.getBuyerInfo3());
					remitReportInvoiceVO.setBuyinfo4tx(invoiceInformationVO.getBuyerInfo4());
					remitReportInvoiceVO.setShrtpaytx(invoiceInformationVO.getShortPayTax());
					remitReportInvoiceVO.setShrtpaylocalam(String.valueOf(invoiceInformationVO.getShortPayLocalAmount()));				
					remitReportInvoiceVOList.add(remitReportInvoiceVO);
				}
			}
			
			remitReportVO.setRemitReportInvoiceVOList(remitReportInvoiceVOList);
			/*private String entrprmoneymovementpymtreferno;
			private List<RemitReportInvoiceVO> remitReportInvoiceVOList;*/
		}
		return remitReportVO;
	}
		
		

	private boolean saveEmailSentStatus(TokenPaymentRecordVO tokenPaymentRecordVO, IssueTokenResponseOutVO issueTokenResponseOutVO,String apiMsgId) {
		boolean success = statusUpdateHelper.updateTokenAndPaymentStatus(tokenPaymentRecordVO.getPaymentID(), TokenPaymentStatus.GENERATED.getStatusCode(),
				TokenPaymentStatus.GENERATED.getDbStatusCode(),TokenPaymentResponseCodes.TOKEN_GENERATED.getResponseDesc(), 0, issueTokenResponseOutVO.getTokenNumber(), TokenStatus.ACTIVE.getStatusCode(),TokenStatus.ACTIVE.name());
		if(!success){
			String paymtRespFailDesc = TokenPaymentResponseCodes.getErrorDescription(TokenPaymentResponseCodes.SAVE_EMAIL_SENT_STATUS_FAILED);
			LOGGER.error(apiMsgId, APPLICATION_NAME, EVENT_NAME, "saveEmailSentStatus", "UNABLE TO UPDATE EMAIL SENT STATUS IN DB.", AmexLogger.Result.failure, 
					"Error while updating Email Sent status in database", paymtRespFailDesc);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_UNABLE_TO_UPDATE_EMAIL_SENT_STATUS_CD, 
					TivoliMonitoring.SSE_UNABLE_TO_UPDATE_EMAIL_SENT_STATUS_MSG, apiMsgId));
			/*statusUpdateHelper.handleTokenPaymentFailure(tokenPaymentRecordVO.getPaymentID(), issueTokenResponseOutVO.getTokenNumber(), 
					issueTokenResponseOutVO.getTokenExpiry(), tokenPaymentRecordVO.getRetryCount() + 1, 
					issueTokenResponseOutVO.getTokenRequestorID(), paymtRespFailDesc, ApiConstants.CHAR_SPACE);*/
		}
		return success;
	}


	private boolean prepareAndSendTokenEmailRequest(TokenPaymentRecordVO tokenPaymentRecordVO, IssueTokenResponseOutVO issueTokenResponseOutVO) {
		String apiMsgID = ThreadLocalManager.getApiMsgId();
		boolean success = false;
		try{
			SupplierInformationVO supplierInformationVO = supplierOrgDetailsDAO.getSupplierDetails(tokenPaymentRecordVO.getPaymentID());
			List<String> inlineImagesPath = getPathOfImages();
			List<String> imagesName = getNameOfImages();
			String message = getMessage(tokenPaymentRecordVO, supplierInformationVO, issueTokenResponseOutVO);
			//String subject = getEmailSubject(tokenPaymentRecordVO);			
			String subject = "";// TODO - Pull the subject based on partner
			
			
			HashMap<String,String> keyValueMap = CommonUtils.getHashMapKeyValue(PRTR_ETV_SUBJECT, "");		
			if(keyValueMap.containsKey(tokenPaymentRecordVO.getPrtrMacId())){
				String nonFormattedSubject = keyValueMap.get(tokenPaymentRecordVO.getPrtrMacId());
				subject = CommonUtils.getFormattedMessage(nonFormattedSubject, new Object[]{StringUtils.isNotBlank(tokenPaymentRecordVO.getOrgName())?tokenPaymentRecordVO.getOrgName():"" });
			}		
			
			LOGGER.info(apiMsgID, APPLICATION_NAME, EVENT_NAME, "prepareAndSendTokenEmailRequest", "Subject formed Successfully", AmexLogger.Result.success, 
					"COMPLETED", "subject2",subject);
			
			String toRecipient = supplierInformationVO.getSupplierOrgEmail();
			/*String maskedMessageBody  = message.replace(getFormattedTokenNumber(issueTokenResponseOutVO.getTokenNumber()),
					issueTokenResponseOutVO.getTokenNumber()==null ? null : GenericUtilities.maskField(issueTokenResponseOutVO.getTokenNumber().trim(),SchedulerConstants.START_MASK_TOKEN_INDEX,
							SchedulerConstants.END_MASK_TOKEN_INDEX,SchedulerConstants.SSE_MASK_CHAR,apiMsgID, false));*/
			LOGGER.info(apiMsgID, APPLICATION_NAME, EVENT_NAME, "sendToken", "SEND TOKEN EMAIL", AmexLogger.Result.success, "CALLING SENDMAIL",
					"STARTED", "Supplier Email Address", supplierInformationVO.getSupplierOrgEmail(), "messageBody",message, "Subject", subject,
					"TO_EMAIL_ADDRESS", toRecipient,"paymentRefID",tokenPaymentRecordVO.getPaymentID());
			success = sendMail.send(toRecipient, subject, message, inlineImagesPath, imagesName,message);
			LOGGER.info(apiMsgID, APPLICATION_NAME, EVENT_NAME, "prepareAndSendTokenEmailRequest", SplunkConstants.TOKEN_EMAIL_SENT_TO_SUPPLIER, 
					success? AmexLogger.Result.success : AmexLogger.Result.failure, "","paymentRefID",tokenPaymentRecordVO.getPaymentID());
		}catch(Exception ex){
			LOGGER.error(apiMsgID, APPLICATION_NAME, EVENT_NAME, "prepareAndSendTokenEmailRequest", SplunkConstants.TOKEN_EMAIL_SENT_TO_SUPPLIER, 
					AmexLogger.Result.failure, "Messaging Exception while Sending Token.", ex);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_EMAIL_FAILURE_ERR_CD, TivoliMonitoring.SSE_EMAIL_FAILURE_ERR_MSG, apiMsgID));
		}
		return success;
	}

	private List<String> getNameOfImages() {
		List<String> imagesName = new ArrayList<String>();
		imagesName.add("AMEX_service_spacer1.gif");
		//imagesName.add("AMEX_service_logo1.jpg");
		imagesName.add("AMEX_service_logo1.png");
		imagesName.add("GCP_MarketingLogo.jpg");
		//imagesName.add("GCP_MarketingSeparator.jpg");
		imagesName.add("GCP_MarketingSeparator.png");
		//imagesName.add("bg_divider.png");
		imagesName.add("spacer.gif");
		return imagesName;
	}

	private List<String> getPathOfImages() {
		List<String> inlineImagesPath = new ArrayList<String>();
		inlineImagesPath.add("/images/AMEX_service_spacer1.gif");
		//inlineImagesPath.add("/images/AMEX_service_logo1.jpg");
		inlineImagesPath.add("/images/AMEX_service_logo1.png");
		inlineImagesPath.add("/images/GCP_MarketingLogo.jpg");
		//inlineImagesPath.add("/images/GCP_MarketingSeparator.jpg");
		inlineImagesPath.add("/images/GCP_MarketingSeparator.png");
		//inlineImagesPath.add("/images/bg_divider.png");
		inlineImagesPath.add("/images/spacer.gif");
		return inlineImagesPath;
	}


	private String getEmailSubject(TokenPaymentRecordVO tokenPaymentRecordVO) {
		String subject = "Payment Received";
		if(!GenericUtilities.isNullOrEmpty(tokenPaymentRecordVO.getOrgName())){
			subject = subject + " from " + tokenPaymentRecordVO.getOrgName();
		}
		return subject;
	}
	
	private String getTokenEmailSubject() {
		String subject = "Action Required: You have an American Express Card token payment is ready for processing";
		return subject;
	}

	private String getInvoiceIDs(TokenPaymentRecordVO tokenPaymentRecordVO) {
		
		List<InvoiceInformationVO> invoices = null;
		
		if(!tokenPaymentRecordVO.isPaymentReqApiInitCall()){
			invoices = invoiceInformationDAO.getInvoiceDetails(tokenPaymentRecordVO.getPaymentID());
		}else{
			invoices = tokenPaymentRecordVO.getInvoiceInformationVOList();
		}
		
		//List<InvoiceInformationVO> invoices = invoiceInformationDAO.getInvoiceDetails(tokenPaymentRecordVO.getPaymentID());
		String invoiceIDs = null;
		if(!GenericUtilities.isNullOrEmpty(invoices)){
			String[] invoiceStr = new String[invoices.size()];
			for(int i=0; i < invoices.size(); i++){
				invoiceStr[i] = invoices.get(i).getBuyerInvoiceRefNo();
			}
			invoiceIDs = GenericUtilities.appendCommaSeperatedValue(invoiceStr);
		}		
		return invoiceIDs;
	}


	private String getMessage(TokenPaymentRecordVO tokenPaymentRecordVO, SupplierInformationVO supplierInformationVO,
			 IssueTokenResponseOutVO issueTokenResponseOutVO){
		String tokenNumber = getFormattedTokenNumber(issueTokenResponseOutVO.getTokenNumber());
		String pin = ServiceConstants.DEFAULT_TOKEN_CID ;
		
		String supplierCompanyName = supplierInformationVO.getSupplierOrgName();
		String supplierEmail = supplierInformationVO.getSupplierOrgEmail();
		String supplierContactName = supplierInformationVO.getSupplierContactName();
		String supplierFirstName = supplierInformationVO.getSupplierFirstName();
		String supplierMiddleName = supplierInformationVO.getSupplierMiddleName();
		String supplierLastName = supplierInformationVO.getSupplierLastName();
		String supplierTitle = supplierInformationVO.getSupplierTitle();
		String buyerFullName = tokenPaymentRecordVO.getEmbossedCMName();
		String buyerCompanyName = tokenPaymentRecordVO.getOrgName();
	
		String tokenExpirationDate = GenericUtilities.convertFromUTCToMSTMMDDYYYYFormat(issueTokenResponseOutVO.getTokenExpiry());
		String tokenCreationDate = GenericUtilities.convertFromUTCToMSTMMDDYYYYFormat(issueTokenResponseOutVO.getTokenStartDate());
		String invoiceDetails = getInvoiceIDs(tokenPaymentRecordVO);
		String amount = "$"+ tokenPaymentRecordVO.getPaymentAmount() + " USD";
		String tokenPaymentId = tokenPaymentRecordVO.getBuyerPaymentID();	
		//String paymentDesc = tokenPaymentRecordVO.getPaymentDesc();	
		String paymentType = ServiceConstants.DEFAULT_CARD_PAYMENT_TYPE;
		//String paymentDate = tokenPaymentRecordVO.getPaymentDate();
		
		String cardBillingAddress = GenericUtilities.appendCommaSeperatedValue(tokenPaymentRecordVO.getAddressLine1(), 
				tokenPaymentRecordVO.getAddressLine2(), tokenPaymentRecordVO.getAddressLine3(), tokenPaymentRecordVO.getAddressLine4(),
				tokenPaymentRecordVO.getAddressLine5(), tokenPaymentRecordVO.getTownOrCityName(), tokenPaymentRecordVO.getStateAreaName(), 
				tokenPaymentRecordVO.getZipCode());
		
		String message = prepareEmailMessageBody(pin, tokenExpirationDate, tokenNumber, supplierCompanyName, supplierEmail, 
				buyerFullName, buyerCompanyName, tokenCreationDate, invoiceDetails, amount, tokenPaymentId,  paymentType, cardBillingAddress,supplierFirstName,supplierMiddleName,supplierLastName,supplierContactName,supplierTitle);	
		
		return message;
	}


	private String getFormattedTokenNumber(String tokenNum) {
		String part1 = tokenNum.substring(0, 4);
		String part2 = tokenNum.substring(4, 10);
		String part3 = tokenNum.substring(10);	
		String tokenNumber = part1 + " " + part2 + " " + part3;
		return tokenNumber;
	}


	private String prepareEmailMessageBody(String pin, String tokenExpirationDate, String tokenNumber, String supplierCompanyName, 
			String supplierEmail, String buyerFullName, String buyerCompanyName, String tokenCreationDate, String invoiceDetails, String amount,
			String tokenPaymentId, String paymentType, String cardBillingAddress,String supplierFirstName,String supplierMiddleName,String supplierLastname,String supplierContactName,String supplierTitle) {
		String message = null;	
		message = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body bgcolor=\"#ffffff\" style=\"margin:0px\"><title>Payment From Amex</title><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" align=\"center\"><tbody><tr><td valign=\"top\" align=\"center\" ><table width=\"550\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr>" +
				  "<td width=\"14\" bgcolor=\"#ffffff\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"14\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td><td width=\"522\" valign=\"top\" bgcolor=\"#FFFFFF\"><table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" bgcolor=\"#f2f3f5\"><tbody><tr><td><img src=\"cid:AMEX_service_spacer1.gif\" width=\"1\" height=\"10\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td></tr></tbody></table>" +
				  "<table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" bgcolor=\"#f2f3f5\"><tbody><tr><td width=\"15\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"15\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td><td valign=\"top\" width=\"270\" ><img src=\"cid:GCP_MarketingLogo.jpg\" alt=\"\" width=\"257\" height=\"45\" hspace=\"0\" vspace=\"0\" border=\"0\" align=\"absmiddle\" style=\"display:block\" ></td>";
	   if(!GenericUtilities.isNullOrEmpty(supplierCompanyName) && !GenericUtilities.isNullOrEmpty(supplierTitle) && 
			   !GenericUtilities.isNullOrEmpty(supplierFirstName) && !GenericUtilities.isNullOrEmpty(supplierMiddleName) && !GenericUtilities.isNullOrEmpty(supplierLastname)){
		  message +="<td align=\"right\" width=\"237\" style=\"font-family:Arial, Helvetica, sans-serif;font-size:13px;font-weight:bold;color:#006890; padding-right:10px\">" +
				   "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" align=\"right\"><tr><td align=\"right\" style=\"font-family:Arial, Helvetica, sans-serif;font-size:12px;font-weight:bold;color:#006890;\">{supplierCompanyName}</td></tr><tr><td align=\"right\" style=\"font-family:Arial, Helvetica, sans-serif;font-size:12px;font-weight:bold;color:#006890;\">{supplierContactName}</td></tr><tr><td align=\"right\" style=\"font-family:Arial, Helvetica, sans-serif;font-size:12px;font-weight:bold;color:#006890;\">{supplierTitle}</td></tr></table></td>" ;
	     }				  
		 message +="</tr></tbody></table><table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" bgcolor=\"#f2f3f5\"><tbody><tr><td><img src=\"cid:AMEX_service_spacer1.gif\" width=\"1\" height=\"10\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td></tr></tbody></table><table border=\"0\" width=\"522\" cellpadding=\"0\" cellspacing=\"0\">" +
				  "<tbody><tr><td><img src=\"cid:GCP_MarketingSeparator.png\" height=\"28\" width=\"520\" hspace=\"0\" vspace=\"0\" border=\"0\" > </td></tr></tbody></table><table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr><td bgcolor=\"#FFFFFF\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"1\" height=\"20\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td></tr></tbody></table><table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr>" +
				  "<td width=\"15\" bgcolor=\"#FFFFFF\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"15\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td><td bgcolor=\"#FFFFFF\" align=\"left\" valign=\"top\" style=\"font-family:Arial, Helvetica, sans-serif; font-size:12px; line-height: 18px; color:#333333; font-weight:bold\">Dear {supplierCompanyName}, <br/><br/></td><td width=\"20\" bgcolor=\"#FFFFFF\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"20\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td></tr><tr>" +
				  "<td width=\"15\" bgcolor=\"#FFFFFF\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"15\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td><td bgcolor=\"#FFFFFF\" align=\"left\" valign=\"top\" style=\"font-family:Arial, Helvetica, sans-serif; font-size:12px; line-height: 18px; color:#333333;\">An American Express Card payment was issued to you by";
	    if(!GenericUtilities.isNullOrEmpty(buyerFullName)){
			 message +=" {buyerFullName} at";
	     }
		 message +=" {buyerCompanyName}. You’ll find details of this payment and information needed for processing below: </td>"+
	     "<td width=\"20\" bgcolor=\"#FFFFFF\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"20\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td></tr></tbody></table><table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr><td bgcolor=\"#FFFFFF\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"1\" height=\"30\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td>"+
	     "</tr></tbody></table><table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr><td width=\"15\" bgcolor=\"#FFFFFF\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"15\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td><td bgcolor=\"#FFFFFF\" align=\"left\" valign=\"top\" ><table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\"><tr>"+
	     "<td style=\"font-family:Arial, Helvetica, sans-serif; font-size:13px;color:#4d4f53; font-weight:bold;\" height=\"30\">Date Payment Issued: {tokenCreationDate}</td></tr><tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:13px;color:#4d4f53; font-weight:bold;\"  height=\"30\">Payment Amount: {amount}</td></tr><tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:13px;color:#4d4f53; font-weight:bold;\"  height=\"30\">Payment Number: {tokenPaymentId}</td></tr>";
	     if(!GenericUtilities.isNullOrEmpty(invoiceDetails)){
	    	 message += "<tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:13px;color:#4d4f53; font-weight:bold;\"  height=\"30\">Invoice-Level Detail: {invoiceDetails} </td></tr>";
	     }
		 if(!GenericUtilities.isNullOrEmpty(" ")){
			 message += "<tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:13px;color:#4d4f53; font-weight:bold;\"  height=\"30\">Payment Description: {paymentDesc}</td></tr>";     
	     }						 
		 message += "<tr><td style=\"font-family:Arial, Helvetica, sans-serif; font-size:13px;color:#4d4f53; font-weight:bold;\"  height=\"30\">Payment Type: {paymentType}</td>"+
	     "</tr><tr><td style=\"padding-left:5px\"><table width=\"62%\" border=\"0\" cellspacing=\"3\" cellpadding=\"0\" style=\"background-color:#cdd1da; border:1px solid #a6a6a6;border-radius:4px;font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height: 18px; color:#000; font-weight:bold;Padding:5px;\"><tr><td>&nbsp;</td><td>&nbsp;</td><td align=\"right\" style=\"padding-top:15px\">&nbsp;</td></tr><tr><td colspan=\"3\" ><strong><pre style=\" font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height: 18px; color:#000; font-weight:bold;font-size:26px;\">  {tokenNumber}</pre></strong></td>"+
	     "</tr><tr><td>VALID THRU </td><td align=\"center\">{expiryDate}</td><td align=\"right\"><img src=\"cid:AMEX_service_logo1.png\" width=\"67\" height=\"60\" hspace=\"0\" vspace=\"0\" border=\"0\"/></td></tr></table></td></tr></table></td><td width=\"20\" bgcolor=\"#FFFFFF\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"20\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td></tr></tbody></table><table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"+
	     "<tbody><tr><td bgcolor=\"#FFFFFF\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"1\" height=\"20\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td></tr></tbody></table><table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr><td width=\"15\" bgcolor=\"#FFFFFF\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"15\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td><td bgcolor=\"#FFFFFF\" align=\"left\" valign=\"top\" style=\"font-family:Arial, Helvetica, sans-serif; font-size:12px; line-height: 18px; color:#333333; border-bottom:1px solid #000000;padding-bottom:25px\">This is a one-time account number only valid for the payment amount above and the following date range:<strong> {tokenCreationDate}</strong> through<strong> {tokenExpirationDate}</strong>. <br/>"+
		 "<br/>Please contact <span style=\"text-transform:uppercase\">{buyerCompanyName} </span>with any questions relating to this payment.</td><td width=\"20\" bgcolor=\"#FFFFFF\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"20\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td></tr></tbody></table><table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr><td bgcolor=\"#ffffff\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"1\" height=\"20\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td>"+
	     "</tr></tbody></table><table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr><td width=\"20\" bgcolor=\"#ffffff\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"20\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td><td bgcolor=\"#ffffff\" valign=\"top\" align=\"left\" style=\"color: #8b8d8e; font-family: Arial, sans-serif; font-size: 12px; line-height:16px\"><table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr><td bgcolor=\"#ffffff\" align=\"center\" valign=\"top\" style=\"color: #818285; font-family: Arial, sans-serif; font-size: 12px\">"+						 
		 "</tr></tbody></table>This is a customer service e-mail from the Global Corporate Payments division of American Express. To learn more about e-mail security or report a suspicious e-mail, please visit us at <a href=\"http://www.americanexpress.com/phishing\" style=\"color: #006890;text-decoration:none\">americanexpress.com/phishing</a>.We kindly ask you not to reply to this e-mail.<br><br>© {year} American Express. All rights reserved.</td><td width=\"10\" bgcolor=\"#ffffff\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"10\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td></tr></tbody></table>"+
		 "<table width=\"522\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr><td width=\"20\" bgcolor=\"#ffffff\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"20\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td><td width=\"20\" bgcolor=\"#ffffff\"><img src=\"cid:AMEX_service_spacer1.gif\" width=\"20\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td></tr></tbody></table></td><td width=\"14\" bgcolor=\"#ffffff\">"+
		 "<img src=\"cid:AMEX_service_spacer1.gif\" width=\"14\" height=\"1\" hspace=\"0\" vspace=\"0\" border=\"0\" style=\"display:block\"></td></tr></tbody></table></td></tr></tbody></table><img src=\"cid:spacer.gif\"></body></html>";		
                        
		int year = Calendar.getInstance().get(Calendar.YEAR);               
		message = message.replace("{expiryDate}",tokenExpirationDate == null ? " " : tokenExpirationDate);
		message = message.replace("{tokenExpirationDate}",tokenExpirationDate ==null ? " ":tokenExpirationDate);
		message = message.replace("{pin}",pin ==null ? " ": pin);
		message = message.replace("{tokenNumber}",tokenNumber  ==null ? " ": tokenNumber );
		message = message.replace("{supplierCompanyName}",supplierCompanyName  ==null ? " ":supplierCompanyName );
		message = message.replace("{buyerFullName}",buyerFullName  ==null ? " ": buyerFullName);		
		//message = message.replace("{paymentDesc}",paymentDesc  ==null ? " ": paymentDesc);
		message = message.replace("{paymentType}",paymentType  ==null ? " ": paymentType);		
		message = message.replace("{buyerCompanyName}",buyerCompanyName  ==null ? " ": buyerCompanyName);
		message = message.replace("{tokenCreationDate}",tokenCreationDate  ==null ? " ": tokenCreationDate);
		message = message.replace("{invoiceDetails}",invoiceDetails  ==null ? " ": invoiceDetails);
		message = message.replace("{amount}",amount  ==null ? " ": amount);
		message = message.replace("{tokenPaymentId}",tokenPaymentId  ==null ? " ": tokenPaymentId);
		message = message.replace("{cardBillingAddress}",cardBillingAddress  ==null ? " ": cardBillingAddress);
		message = message.replace("{supplierContactName}",supplierContactName  ==null ? " ": supplierContactName);
		message = message.replace("{supplierTitle}",supplierTitle  ==null ? " ": supplierTitle);
		message = message.replace("{year}",String.valueOf(year));
		return message;
	}	
	
}