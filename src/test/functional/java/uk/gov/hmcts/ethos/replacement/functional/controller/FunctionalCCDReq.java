package uk.gov.hmcts.ethos.replacement.functional.controller;

import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.hmcts.ecm.common.model.ccd.Address;
import uk.gov.hmcts.ecm.common.model.ccd.CCDRequest;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.CaseDetails;
import uk.gov.hmcts.ecm.common.model.ccd.items.RespondentSumTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.types.ClaimantIndType;
import uk.gov.hmcts.ecm.common.model.ccd.types.ClaimantType;
import uk.gov.hmcts.ecm.common.model.ccd.types.ClaimantWorkAddressType;
import uk.gov.hmcts.ecm.common.model.ccd.types.RespondentSumType;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;

public class FunctionalCCDReq {

    public uk.gov.hmcts.ecm.common.model.ccd.CCDRequest CCDRequest() {
        CCDRequest ccdRequest = new CCDRequest();
        ccdRequest.setCaseDetails(CDDetails());
        return ccdRequest;
    }

    public CaseDetails CDDetails() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId("1234567812345678");
        caseDetails.setJurisdiction("EMPLOYMENT");
        caseDetails.setState("Submitted");
        caseDetails.setCaseTypeId("LondonSouth");
        caseDetails.setCreatedDate(LocalDateTime.now());
        caseDetails.setLastModified(LocalDateTime.now());
        caseDetails.setCaseData(CDData());
        caseDetails.setDataClassification(null);
        return caseDetails;
    }

    public CaseData CDData () {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String stringDate = dateFormat.format(date);

        CaseData cd = new CaseData();
        cd.setStateAPI("Submitted");
        cd.setPositionType("Manually Created");
        cd.setClaimantRepresentedQuestion("Yes");
        cd.setRespondentCollection(Collections.singletonList(respondentSumTypeItem()));
        cd.setTribunalCorrespondenceDX("DX 155061 Croydon 39");
        cd.setRespondent("Green Rain");
        cd.setTribunalCorrespondenceFax("0870 324 0174");
        cd.setCaseType("Single");
        cd.setFeeGroupReference(RandomStringUtils.randomNumeric(12));
        //cd.setEthosCaseReference("1811111/2021");
        cd.setClaimantIndType(claimantIndType());
        cd.setClaimantTypeOfClaimant("Individual");
        cd.setTribunalCorrespondenceAddress(address(
                "Montague Court",
                "101 London Road",
                "West Croydon",
                "London",
                "CR0 2RF"));
        cd.setTribunalCorrespondenceEmail("londonsouthet@Justice.gov.uk");
        cd.setClaimant("Harry Mee");
        cd.setDateToPosition(stringDate);
        cd.setClaimantWorkAddress(claimantWorkAddressType());
        cd.setMultipleFlag("No");
        cd.setCurrentPosition("Manually Created");
        cd.setReceiptDate(stringDate);
        cd.setCaseSource("Manually Created");
        cd.setTribunalCorrespondenceTelephone("0113 245 9741");
        cd.setClaimantWorkAddressQuestion("Yes");
        cd.setClaimantType(claimantType());
        cd.setFlagsImageFileName("EMP-TRIB-00000000.jpg");

        return cd;
    }

    public ClaimantType claimantType() {
        ClaimantType claimantType = new ClaimantType();
        claimantType.setClaimantAddressUK(address("12 The Grove","", "New Ridlet", "Stocksfield", "NE43 7RD"));
        return claimantType;
    }

    public ClaimantWorkAddressType claimantWorkAddressType() {
        ClaimantWorkAddressType claimantWorkAddressType = new ClaimantWorkAddressType();
        claimantWorkAddressType.setClaimantWorkAddress(address(
                "The Gables",
                "Prune Hill",
                "Englefield Green",
                "Egham",
                "TW20 9TR"
        ));
        return claimantWorkAddressType;
    }

    public ClaimantIndType claimantIndType() {
        ClaimantIndType claimantIndType = new ClaimantIndType();
        claimantIndType.setClaimantFirstNames("Harry");
        claimantIndType.setClaimantLastName("Mee");
        claimantIndType.setClaimantDateOfBirth("1891-01-01");
        claimantIndType.setClaimantGender("Male");
        return claimantIndType;
    }

    public RespondentSumTypeItem respondentSumTypeItem() {
        RespondentSumTypeItem respondentSumTypeItem = new RespondentSumTypeItem();
        respondentSumTypeItem.setId("9cc5921e-355f-4388-86de-f6aac404261b");
        respondentSumTypeItem.setValue(respondentSumType());
        return respondentSumTypeItem;
    }

    public RespondentSumType respondentSumType() {
        RespondentSumType respondentSumType = new RespondentSumType();
        respondentSumType.setRespondentName("Green Rain");
        respondentSumType.setRespondentACASQuestion("No");
        respondentSumType.setRespondentACASNo("Employer already in touch");
        respondentSumType.setRespondentAddress(address("12 The Grove","", "New Ridlet", "Stocksfield", "NE43 7RD"));
        respondentSumType.setResponseStruckOut("No");
        respondentSumType.setResponseReceived("Yes");
        return respondentSumType;
    }

    public Address address(String addressLine1, String addressLine2,String addressLine3, String postTown, String postCode) {
        Address address = new Address();
        address.setAddressLine1(addressLine1);
        address.setAddressLine2(addressLine2);
        address.setAddressLine3(addressLine3);
        address.setPostTown(postTown);
        address.setPostCode(postCode);
        address.setCountry("United Kingdom");
        return address;
    }
}
