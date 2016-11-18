package com.axelor.apps.account.service.bankorder.file.transfer;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.account.db.BankOrder;
import com.axelor.apps.account.db.BankOrderLine;
import com.axelor.apps.account.service.bankorder.file.BankOrderFileService;
import com.axelor.apps.account.xsd.pain_001_001_02.AccountIdentification3Choice;
import com.axelor.apps.account.xsd.pain_001_001_02.AmountType2Choice;
import com.axelor.apps.account.xsd.pain_001_001_02.BranchAndFinancialInstitutionIdentification3;
import com.axelor.apps.account.xsd.pain_001_001_02.CashAccount7;
import com.axelor.apps.account.xsd.pain_001_001_02.CreditTransferTransactionInformation1;
import com.axelor.apps.account.xsd.pain_001_001_02.CurrencyAndAmount;
import com.axelor.apps.account.xsd.pain_001_001_02.Document;
import com.axelor.apps.account.xsd.pain_001_001_02.FinancialInstitutionIdentification5Choice;
import com.axelor.apps.account.xsd.pain_001_001_02.GroupHeader1;
import com.axelor.apps.account.xsd.pain_001_001_02.Grouping1Code;
import com.axelor.apps.account.xsd.pain_001_001_02.ObjectFactory;
import com.axelor.apps.account.xsd.pain_001_001_02.Pain00100102;
import com.axelor.apps.account.xsd.pain_001_001_02.PartyIdentification8;
import com.axelor.apps.account.xsd.pain_001_001_02.PaymentIdentification1;
import com.axelor.apps.account.xsd.pain_001_001_02.PaymentInstructionInformation1;
import com.axelor.apps.account.xsd.pain_001_001_02.PaymentMethod3Code;
import com.axelor.apps.account.xsd.pain_001_001_02.PaymentTypeInformation1;
import com.axelor.apps.account.xsd.pain_001_001_02.RemittanceInformation1;
import com.axelor.apps.account.xsd.pain_001_001_02.ServiceLevel1Code;
import com.axelor.apps.account.xsd.pain_001_001_02.ServiceLevel2Choice;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;

public class BankOrderFile00100102Service extends BankOrderFileService  {

	private final Logger log = LoggerFactory.getLogger( getClass() );

	
	@Inject
	public BankOrderFile00100102Service(BankOrder bankOrder)  {
		
		super(bankOrder);
		
		context = "com.axelor.apps.account.xsd.pain_001_001_02";
		fileExtension = FILE_EXTENSION_XML;
	}
	
	
	/**
	 * Method to create an XML file for SEPA transfer pain.001.001.02
	 * 
	 * @throws AxelorException
	 * @throws DatatypeConfigurationException
	 * @throws JAXBException
	 * @throws IOException
	 */
	@Override
	public File generateFile() throws JAXBException, IOException, AxelorException, DatatypeConfigurationException  {

		DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();

		ObjectFactory factory = new ObjectFactory();

		ServiceLevel2Choice svcLvl = factory.createServiceLevel2Choice();
		svcLvl.setCd(ServiceLevel1Code.SEPA);

		PaymentTypeInformation1 pmtTpInf = factory.createPaymentTypeInformation1();
		pmtTpInf.setSvcLvl(svcLvl);

		// Payer
		PartyIdentification8 dbtr = factory.createPartyIdentification8();
		dbtr.setNm(senderBankDetails.getOwnerName());

		// IBAN
		AccountIdentification3Choice iban = factory.createAccountIdentification3Choice();
		iban.setIBAN(senderBankDetails.getIban());

		CashAccount7 dbtrAcct = factory.createCashAccount7();
		dbtrAcct.setId(iban);

		// BIC
		FinancialInstitutionIdentification5Choice finInstnId = factory.createFinancialInstitutionIdentification5Choice();
		finInstnId.setBIC(senderBankDetails.getBic());

		BranchAndFinancialInstitutionIdentification3 dbtrAgt = factory.createBranchAndFinancialInstitutionIdentification3();
		dbtrAgt.setFinInstnId(finInstnId);

		PaymentInstructionInformation1 pmtInf = factory.createPaymentInstructionInformation1();
		pmtInf.setPmtMtd(PaymentMethod3Code.TRF);
		pmtInf.setPmtTpInf(pmtTpInf);
		
		/**
		 * RequestedExecutionDate
		 * Definition : Date at which the initiating party asks the Debtor's Bank to process the payment. This is the
		 * date on which the debtor's account(s) is (are) to be debited.
		 * XML Tag : <ReqdExctnDt>
		 * Occurrences : [1..1]
		 * Format : YYYY-MM-DD
		 * Rules : date is limited to maximum one year in the future. 
		 */
		pmtInf.setReqdExctnDt(datatypeFactory.newXMLGregorianCalendar(bankOrderDate.toString("yyyy-MM-dd")));
		pmtInf.setDbtr(dbtr);
		pmtInf.setDbtrAcct(dbtrAcct);
		pmtInf.setDbtrAgt(dbtrAgt);

		CreditTransferTransactionInformation1 cdtTrfTxInf = null; PaymentIdentification1 pmtId = null;
		AmountType2Choice amt = null; CurrencyAndAmount instdAmt = null;
		PartyIdentification8 cbtr = null; CashAccount7 cbtrAcct = null;
		BranchAndFinancialInstitutionIdentification3 cbtrAgt = null;
		RemittanceInformation1 rmtInf = null;
		
		for (BankOrderLine bankOrderLine : bankOrderLineList)  { 

			BankDetails receiverBankDetails = bankOrderLine.getReceiverBankDetails();

			// Reference
			pmtId = factory.createPaymentIdentification1();
			pmtId.setEndToEndId(bankOrderLine.getReceiverReference());

			// Amount
			instdAmt = factory.createCurrencyAndAmount();
			instdAmt.setCcy(bankOrderCurrency.getCode());
			instdAmt.setValue(bankOrderLine.getBankOrderAmount());

			amt = factory.createAmountType2Choice();
			amt.setInstdAmt(instdAmt);

			// Receiver
			cbtr = factory.createPartyIdentification8();
			cbtr.setNm(receiverBankDetails.getOwnerName());

			// IBAN
			iban = factory.createAccountIdentification3Choice();
			iban.setIBAN(receiverBankDetails.getIban());

			cbtrAcct = factory.createCashAccount7();
			cbtrAcct.setId(iban);

			// BIC
			finInstnId = factory.createFinancialInstitutionIdentification5Choice();
			finInstnId.setBIC(receiverBankDetails.getBic());

			cbtrAgt = factory.createBranchAndFinancialInstitutionIdentification3();
			cbtrAgt.setFinInstnId(finInstnId);

			rmtInf = factory.createRemittanceInformation1();

			rmtInf.getUstrd().add(bankOrderLine.getReceiverLabel());

			// Transaction
			cdtTrfTxInf = factory.createCreditTransferTransactionInformation1();
			cdtTrfTxInf.setPmtId(pmtId);
			cdtTrfTxInf.setAmt(amt);
			cdtTrfTxInf.setCdtr(cbtr);
			cdtTrfTxInf.setCdtrAcct(cbtrAcct);
			cdtTrfTxInf.setCdtrAgt(cbtrAgt);
			cdtTrfTxInf.setRmtInf(rmtInf);

			pmtInf.getCdtTrfTxInf().add(cdtTrfTxInf);
		}

		// Header
		GroupHeader1 grpHdr = factory.createGroupHeader1();
		
		/**
		 * CreationDateTime
		 * Definition : Date and Time at which a (group of) payment instruction(s) was created by the instructing party.
		 * XML Tag : <CreDtTm>
		 * Occurrences : [1..1]
		 * Format : YYYY-MM-DDThh:mm:ss 
		 */
		grpHdr.setCreDtTm(datatypeFactory.newXMLGregorianCalendar(validationDateTime.toString("yyyy-MM-dd'T'HH:mm:ss")));
		grpHdr.setNbOfTxs(Integer.toString(nbOfLines));
		grpHdr.setCtrlSum(arithmeticTotal);
		grpHdr.setGrpg(Grouping1Code.MIXD);
		grpHdr.setInitgPty(dbtr);

		// Parent
		Pain00100102 pain00100102 = factory.createPain00100102();
		pain00100102.setGrpHdr(grpHdr);
		pain00100102.getPmtInf().add(pmtInf);

		// Document
		Document xml = factory.createDocument();
		xml.setPain00100102(pain00100102);

		fileToCreate = factory.createDocument(xml);
		
		return super.generateFile();
	}
	
	
}
