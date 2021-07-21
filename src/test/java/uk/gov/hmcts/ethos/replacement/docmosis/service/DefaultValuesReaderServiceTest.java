package uk.gov.hmcts.ethos.replacement.docmosis.service;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.ecm.common.model.bulk.types.DynamicFixedListType;
import uk.gov.hmcts.ecm.common.model.bulk.types.DynamicValueType;
import uk.gov.hmcts.ecm.common.model.ccd.Address;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.CaseDetails;
import uk.gov.hmcts.ecm.common.model.ccd.items.RespondentSumTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.types.RespondentSumType;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.*;
import uk.gov.hmcts.ecm.common.model.helper.DefaultValues;
import uk.gov.hmcts.ecm.common.model.listing.ListingData;
import uk.gov.hmcts.ethos.replacement.docmosis.config.CaseDefaultValuesConfiguration;
import uk.gov.hmcts.ethos.replacement.docmosis.config.TribunalOfficesConfiguration;

import java.util.ArrayList;
import java.util.Collections;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        DefaultValuesReaderService.class,
        TribunalOfficesService.class,
})
@EnableConfigurationProperties({CaseDefaultValuesConfiguration.class, TribunalOfficesConfiguration.class})
public class DefaultValuesReaderServiceTest {

    @Autowired
    DefaultValuesReaderService defaultValuesReaderService;

    private String defaultClaimantTypeOfClaimant;
    private DefaultValues postDefaultValuesManchester;
    private DefaultValues postDefaultValuesGlasgow;
    private DefaultValues postDefaultValuesAberdeen;
    private DefaultValues postDefaultValuesDundee;
    private DefaultValues postDefaultValuesEdinburgh;
    private DefaultValues postDefaultValuesBristol;
    private DefaultValues postDefaultValuesLeeds;
    private DefaultValues postDefaultValuesLondonCentral;
    private DefaultValues postDefaultValuesLondonEast;
    private DefaultValues postDefaultValuesLondonSouth;
    private DefaultValues postDefaultValuesMidlandsEast;
    private DefaultValues postDefaultValuesMidlandsWest;
    private DefaultValues postDefaultValuesNewcastle;
    private DefaultValues postDefaultValuesWales;
    private DefaultValues postDefaultValuesWatford;
    private CaseData caseData;
    private CaseDetails manchesterCaseDetails;
    private CaseDetails glasgowCaseDetails;
    private CaseDetails bristolCaseDetails;
    private CaseDetails leedsCaseDetails;
    private CaseDetails londonCentralCaseDetails;
    private CaseDetails londonEastCaseDetails;
    private CaseDetails londonSouthCaseDetails;
    private CaseDetails midlandsEastCaseDetails;
    private CaseDetails midlandsWestCaseDetails;
    private CaseDetails newcastleCaseDetails;
    private CaseDetails walesCaseDetails;
    private CaseDetails watfordCaseDetails;
    private ListingData listingData;

    private CaseDetails getCaseDetails(String caseTypeId) {
        CaseDetails caseDetails = new CaseDetails();
        CaseData caseData = new CaseData();
        caseDetails.setCaseData(caseData);
        caseDetails.setCaseId("123456");
        caseDetails.setCaseTypeId(caseTypeId);
        caseDetails.setJurisdiction("TRIBUNALS");
        return caseDetails;
    }

    private ListingData getListingDataSetUp() {
        listingData = new ListingData();
        listingData.setTribunalCorrespondenceDX("DX");
        listingData.setTribunalCorrespondenceEmail("m@m.com");
        listingData.setTribunalCorrespondenceFax("100300200");
        listingData.setTribunalCorrespondenceTelephone("077123123");
        Address address = new Address();
        address.setAddressLine1("AddressLine1");
        address.setAddressLine2("AddressLine2");
        address.setAddressLine3("AddressLine3");
        address.setPostTown("Manchester");
        address.setCountry("UK");
        address.setPostCode("L1 122");
        listingData.setTribunalCorrespondenceAddress(address);
        return listingData;
    }

    @Before
    public void setUp() {
        defaultClaimantTypeOfClaimant = INDIVIDUAL_TYPE_CLAIMANT;

        listingData = getListingDataSetUp();
        manchesterCaseDetails = getCaseDetails(MANCHESTER_DEV_CASE_TYPE_ID);
        glasgowCaseDetails = getCaseDetails(SCOTLAND_DEV_CASE_TYPE_ID);
        bristolCaseDetails = getCaseDetails(BRISTOL_CASE_TYPE_ID);
        leedsCaseDetails = getCaseDetails(LEEDS_CASE_TYPE_ID);
        londonCentralCaseDetails = getCaseDetails(LONDON_CENTRAL_CASE_TYPE_ID);
        londonEastCaseDetails = getCaseDetails(LONDON_EAST_CASE_TYPE_ID);
        londonSouthCaseDetails = getCaseDetails(LONDON_SOUTH_CASE_TYPE_ID);
        midlandsEastCaseDetails = getCaseDetails(MIDLANDS_EAST_CASE_TYPE_ID);
        midlandsWestCaseDetails = getCaseDetails(MIDLANDS_WEST_CASE_TYPE_ID);
        newcastleCaseDetails = getCaseDetails(NEWCASTLE_CASE_TYPE_ID);
        walesCaseDetails = getCaseDetails(WALES_CASE_TYPE_ID);
        watfordCaseDetails = getCaseDetails(WATFORD_CASE_TYPE_ID);

        postDefaultValuesManchester = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("Manchester Employment Tribunal")
                .tribunalCorrespondenceAddressLine2("Alexandra House")
                .tribunalCorrespondenceAddressLine3("14-22 The Parsonage")
                .tribunalCorrespondenceTown("Manchester")
                .tribunalCorrespondencePostCode("M3 2JA")
                .tribunalCorrespondenceTelephone("0161 833 6100")
                .tribunalCorrespondenceFax("0870 739 4433")
                .tribunalCorrespondenceDX("DX 743570")
                .tribunalCorrespondenceEmail("Manchesteret@justice.gov.uk")
                .build();
        postDefaultValuesBristol = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("Bristol Civil and Family Justice Centre")
                .tribunalCorrespondenceAddressLine2("2 Redcliff Street")
                .tribunalCorrespondenceTown("Bristol")
                .tribunalCorrespondencePostCode("BS1 6GR")
                .tribunalCorrespondenceTelephone("0117 929 8261")
                .tribunalCorrespondenceFax("0870 739 4009")
                .tribunalCorrespondenceDX("DX 95903 Bristol 3")
                .tribunalCorrespondenceEmail("bristolet@justice.gov.uk")
                .build();
        postDefaultValuesLeeds = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("4th Floor")
                .tribunalCorrespondenceAddressLine2("City Exchange")
                .tribunalCorrespondenceAddressLine3("11 Albion Street")
                .tribunalCorrespondenceTown("Leeds")
                .tribunalCorrespondencePostCode("LS1 5ES")
                .tribunalCorrespondenceTelephone("0113 245 9741")
                .tribunalCorrespondenceFax("01264 785136")
                .tribunalCorrespondenceDX("DX 742940 Leeds 75")
                .tribunalCorrespondenceEmail("LeedsET@justice.gov.uk")
                .build();
        postDefaultValuesLondonCentral = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("Ground Floor")
                .tribunalCorrespondenceAddressLine2("Victory House")
                .tribunalCorrespondenceAddressLine3("30-34 Kingsway")
                .tribunalCorrespondenceTown("London")
                .tribunalCorrespondencePostCode("WC2B 6EX")
                .tribunalCorrespondenceTelephone("0207 273 8603")
                .tribunalCorrespondenceFax("01264 785 100")
                .tribunalCorrespondenceDX("DX 141420 Bloomsbury")
                .tribunalCorrespondenceEmail("londoncentralet@justice.gov.uk")
                .build();
        postDefaultValuesLondonEast = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("2nd Floor")
                .tribunalCorrespondenceAddressLine2("Import Building")
                .tribunalCorrespondenceAddressLine3("2 Clove Crescent")
                .tribunalCorrespondenceTown("London")
                .tribunalCorrespondencePostCode("E14 2BE")
                .tribunalCorrespondenceTelephone("0207 538 6161")
                .tribunalCorrespondenceFax("08703 240 200")
                .tribunalCorrespondenceEmail("eastlondon@justice.gov.uk")
                .build();
        postDefaultValuesLondonSouth = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("Montague Court")
                .tribunalCorrespondenceAddressLine2("101 London Road")
                .tribunalCorrespondenceAddressLine3("West Croydon")
                .tribunalCorrespondenceTown("London")
                .tribunalCorrespondencePostCode("CR0 2RF")
                .tribunalCorrespondenceTelephone("0208 667 9131")
                .tribunalCorrespondenceFax("0870 324 0174")
                .tribunalCorrespondenceDX("DX 155061 Croydon 39")
                .tribunalCorrespondenceEmail("londonsouthet@Justice.gov.uk")
                .build();
        postDefaultValuesMidlandsEast = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("Nottingham Justice Centre")
                .tribunalCorrespondenceAddressLine2("Carrington Street")
                .tribunalCorrespondenceTown("Nottingham")
                .tribunalCorrespondencePostCode("NG2 1EE")
                .tribunalCorrespondenceTelephone("0115 947 5701")
                .tribunalCorrespondenceDX("DX 719030 Nottingham 32")
                .tribunalCorrespondenceEmail("MidlandsEastET@justice.gov.uk")
                .build();
        postDefaultValuesMidlandsWest = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("13th Floor")
                .tribunalCorrespondenceAddressLine2("Centre City Tower")
                .tribunalCorrespondenceAddressLine3("5-7 Hill Street")
                .tribunalCorrespondenceTown("Birmingham")
                .tribunalCorrespondencePostCode("B5 4UU")
                .tribunalCorrespondenceTelephone("0121 600 7780")
                .tribunalCorrespondenceFax("01264 347 999")
                .tribunalCorrespondenceEmail("MidlandsWestET@justice.gov.uk")
                .build();
        postDefaultValuesNewcastle = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("Employment Tribunal")
                .tribunalCorrespondenceAddressLine2("Newcastle Civil Family Courts and Tribunal Centre")
                .tribunalCorrespondenceAddressLine3("Barras Bridge")
                .tribunalCorrespondenceTown("Newcastle upon Tyne")
                .tribunalCorrespondencePostCode("NE1 8QF")
                .tribunalCorrespondenceTelephone("0191 205 8750")
                .tribunalCorrespondenceFax("0870 739 4206")
                .tribunalCorrespondenceDX("DX 336901, Newcastle upon Tyne 55")
                .tribunalCorrespondenceEmail("newcastleet@justice.gov.uk")
                .build();
        postDefaultValuesWales = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("Wales Employment Tribunal")
                .tribunalCorrespondenceAddressLine2("3rd Floor, Cardiff Magistrates' Court")
                .tribunalCorrespondenceAddressLine3("Fitzalan Place")
                .tribunalCorrespondenceTown("Cardiff")
                .tribunalCorrespondencePostCode("CF24 0RZ")
                .tribunalCorrespondenceTelephone("0292 067 8100")
                .tribunalCorrespondenceFax("0870 761 7635")
                .tribunalCorrespondenceDX("317501 Caerdydd/Cardiff 45")
                .tribunalCorrespondenceEmail("waleset@justice.gov.uk")
                .build();
        postDefaultValuesWatford = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("Watford Tribunal Hearing Centre")
                .tribunalCorrespondenceAddressLine2("Radius House")
                .tribunalCorrespondenceAddressLine3("51 Clarendon Road")
                .tribunalCorrespondenceTown("Watford")
                .tribunalCorrespondencePostCode("WD17 1HP")
                .tribunalCorrespondenceTelephone("0192 328 1750")
                .tribunalCorrespondenceFax("01264 887 302")
                .tribunalCorrespondenceDX("DX 155650 Watford 3")
                .tribunalCorrespondenceEmail("watfordet@justice.gov.uk")
                .build();
        postDefaultValuesGlasgow = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("Glasgow Tribunals Centre")
                .tribunalCorrespondenceAddressLine2("3 Atlantic Quay, 20 York Street")
                .tribunalCorrespondenceTown("Glasgow")
                .tribunalCorrespondencePostCode("G2 8GT")
                .tribunalCorrespondenceTelephone("0141 204 0730")
                .tribunalCorrespondenceFax("01264 785 177")
                .tribunalCorrespondenceDX("DX 580003")
                .tribunalCorrespondenceEmail("glasgowet@justice.gov.uk")
                .managingOffice(GLASGOW_OFFICE)
                .build();
        postDefaultValuesAberdeen = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("Ground Floor")
                .tribunalCorrespondenceAddressLine2("AB1, 48 Huntly Street")
                .tribunalCorrespondenceTown("Aberdeen")
                .tribunalCorrespondencePostCode("AB10 1SH")
                .tribunalCorrespondenceTelephone("01224 593 137")
                .tribunalCorrespondenceFax("0870 761 7766")
                .tribunalCorrespondenceDX("DX AB77")
                .tribunalCorrespondenceEmail("aberdeenet@justice.gov.uk")
                .build();
        postDefaultValuesDundee = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("Ground Floor")
                .tribunalCorrespondenceAddressLine2("Endeavour House")
                .tribunalCorrespondenceAddressLine3("1 Greenmarket")
                .tribunalCorrespondenceTown("Dundee")
                .tribunalCorrespondencePostCode("DD1 4QB")
                .tribunalCorrespondenceTelephone("01382 221 578")
                .tribunalCorrespondenceFax("01382 227 136")
                .tribunalCorrespondenceDX("DX DD51")
                .tribunalCorrespondenceEmail("dundeeet@justice.gov.uk")
                .build();
        postDefaultValuesEdinburgh = DefaultValues.builder()
                .positionType(MANUALLY_CREATED_POSITION)
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("54-56 Melville Street")
                .tribunalCorrespondenceTown("Edinburgh")
                .tribunalCorrespondencePostCode("EH3 7HF")
                .tribunalCorrespondenceTelephone("0131 226 5584")
                .tribunalCorrespondenceFax("0131 220 6847")
                .tribunalCorrespondenceDX("DX ED147")
                .tribunalCorrespondenceEmail("edinburghet@justice.gov.uk")
                .build();
        caseData = new CaseData();
    }

    @Test
    public void getPreDefaultValues() {
        assertEquals(defaultClaimantTypeOfClaimant, defaultValuesReaderService.getClaimantTypeOfClaimant());
    }

    @Test
    public void getManchesterPostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( "", manchesterCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesManchester, postDefaultValues1);
    }

    @Test
    public void getBristolPostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( "", bristolCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesBristol, postDefaultValues1);
    }

    @Test
    public void getLeedsPostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( "", leedsCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesLeeds, postDefaultValues1);
    }

    @Test
    public void getLondonCentralPostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( "", londonCentralCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesLondonCentral, postDefaultValues1);
    }

    @Test
    public void getLondonEastPostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues("", londonEastCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesLondonEast, postDefaultValues1);
    }

    @Test
    public void getLondonSouthPostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( "", londonSouthCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesLondonSouth, postDefaultValues1);
    }

    @Test
    public void getMidlandsEastPostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( "", midlandsEastCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesMidlandsEast, postDefaultValues1);
    }

    @Test
    public void getMidlandsWestPostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( "", midlandsWestCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesMidlandsWest, postDefaultValues1);
    }

    @Test
    public void getNewcastlePostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( "", newcastleCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesNewcastle, postDefaultValues1);
    }

    @Test
    public void getWalesPostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( "", walesCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesWales, postDefaultValues1);
    }

    @Test
    public void getWatfordPostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( "", watfordCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesWatford, postDefaultValues1);
    }

    @Test
    public void getGlasgowDefaultPostDefaultValues() {
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( "", glasgowCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesGlasgow, postDefaultValues1);
    }

    @Test
    public void getCaseData() {
        String caseDataExpected = "CaseData(tribunalCorrespondenceAddress=Glasgow Tribunals Centre, 3 Atlantic Quay, "
                + "20 York Street, Glasgow, G2 8GT, tribunalCorrespondenceTelephone=0141 204 0730, "
                + "tribunalCorrespondenceFax=01264 785 177, tribunalCorrespondenceDX=DX 580003, "
                + "tribunalCorrespondenceEmail=glasgowet@justice.gov.uk, ethosCaseReference=null, caseType=Single, "
                + "multipleReference=null, subMultipleName=null, leadClaimant=null, multipleFlag=null, "
                + "claimantTypeOfClaimant=null, claimantCompany=null, claimantIndType=null, claimantType=null, "
                + "claimantOtherType=null, preAcceptCase=null, receiptDate=null, feeGroupReference=null, "
                + "claimantWorkAddressQuestion=null, claimantWorkAddressQRespondent=null, "
                + "representativeClaimantType=null, respondentCollection=null, repCollection=null, "
                + "positionType=Manually Created, dateToPosition=null, currentPosition=null, fileLocation=null, "
                + "fileLocationGlasgow=null, fileLocationAberdeen=null, fileLocationDundee=null, "
                + "fileLocationEdinburgh=null, hearingCollection=null, depositCollection=null, judgementCollection=null, "
                + "jurCodesCollection=null, bfActions=null, clerkResponsible=null, userLocation=null, "
                + "documentCollection=null, additionalCaseInfoType=null, correspondenceScotType=null, "
                + "correspondenceType=null, addressLabelsSelectionType=null, addressLabelCollection=null, "
                + "addressLabelsAttributesType=null, caseNotes=null, claimantWorkAddress=null, "
                + "claimantRepresentedQuestion=null, managingOffice=Glasgow, allocatedOffice=null, "
                + "caseSource=Manually Created, conciliationTrack=null, counterClaim=null, eccCases=null, "
                + "restrictedReporting=null, printHearingDetails=null, printHearingCollection=null, "
                + "targetHearingDate=null, claimant=null, respondent=null, EQP=null, flag1=null, flag2=null, "
                + "docMarkUp=null, caseRefNumberCount=null, startCaseRefNumber=null, multipleRefNumber=null, "
                + "caseRefECC=null, respondentECC=null, ccdID=null, flagsImageFileName=null, flagsImageAltText=null, "
                + "hearingNumbers=null, hearingTypes=null, hearingPublicPrivate=null, hearingVenue=null, "
                + "hearingEstLengthNum=null, hearingEstLengthNumType=null, hearingSitAlone=null, hearingStage=null, "
                + "listedDate=null, hearingNotes=null, hearingSelection=null, hearingActions=null, "
                + "hearingERMember=null, hearingEEMember=null, hearingDatesRequireAmending=null, "
                + "hearingDateSelection=null, hearingDateActions=null, hearingStatus=null, Postponed_by=null, "
                + "hearingRoom=null, hearingClerk=null, hearingJudge=null, hearingCaseDisposed=null, "
                + "hearingPartHeard=null, hearingReservedJudgement=null, attendeeClaimant=null, "
                + "attendeeNonAttendees=null, attendeeRespNoRep=null, attendeeRespAndRep=null, attendeeRepOnly=null, "
                + "hearingTimingStart=null, hearingTimingBreak=null, hearingTimingResume=null, "
                + "hearingTimingFinish=null, hearingTimingDuration=null, companyPremises=null, officeCT=null, "
                + "reasonForCT=null, relatedCaseCT=null, positionTypeCT=null, linkedCaseCT=null, stateAPI=null)";
        defaultValuesReaderService.getCaseData(caseData, postDefaultValuesGlasgow);
        assertEquals(caseDataExpected, caseData.toString());
    }

    @Test
    public void getCaseDataWithPositionTypeAndCaseSource() {
        caseData.setPositionType(MANUALLY_CREATED_POSITION);
        caseData.setCaseSource(ET1_ONLINE_CASE_SOURCE);
        defaultValuesReaderService.getCaseData(caseData, postDefaultValuesGlasgow);
        assertEquals(MANUALLY_CREATED_POSITION, caseData.getPositionType());
        assertEquals(ET1_ONLINE_CASE_SOURCE, caseData.getCaseSource());
    }

    private CaseData getCaseDataWithClaimantWorkAddress(CaseData caseData) {
        Address address = new Address();
        address.setAddressLine1("Line1");
        address.setPostCode("PostCode");
        RespondentSumType respondentSumType = new RespondentSumType();
        respondentSumType.setRespondentName("Andrew Smith");
        respondentSumType.setRespondentAddress(address);
        RespondentSumTypeItem respondentSumTypeItem = new RespondentSumTypeItem();
        respondentSumTypeItem.setValue(respondentSumType);
        caseData.setRespondentCollection(new ArrayList<>(Collections.singletonList(respondentSumTypeItem)));
        caseData.setClaimantWorkAddressQuestion(YES);
        DynamicFixedListType dynamicFixedListType = new DynamicFixedListType();
        DynamicValueType dynamicValueType = new DynamicValueType();
        dynamicValueType.setLabel("PostCode");
        dynamicValueType.setCode("PostCode");
        dynamicFixedListType.setValue(dynamicValueType);
        caseData.setClaimantWorkAddressQRespondent(dynamicFixedListType);
        return caseData;
    }
    @Test
    public void getCaseDataWithClaimantWorkAddress() {
        String caseDataExpected = "CaseData(tribunalCorrespondenceAddress=Glasgow Tribunals Centre, 3 Atlantic Quay, "
                + "20 York Street, Glasgow, G2 8GT, tribunalCorrespondenceTelephone=0141 204 0730, "
                + "tribunalCorrespondenceFax=01264 785 177, tribunalCorrespondenceDX=DX 580003, "
                + "tribunalCorrespondenceEmail=glasgowet@justice.gov.uk, ethosCaseReference=null, caseType=Single, "
                + "multipleReference=null, subMultipleName=null, leadClaimant=null, multipleFlag=null, "
                + "claimantTypeOfClaimant=null, claimantCompany=null, claimantIndType=null, claimantType=null, "
                + "claimantOtherType=null, preAcceptCase=null, receiptDate=null, feeGroupReference=null, "
                + "claimantWorkAddressQuestion=Yes, claimantWorkAddressQRespondent=null, "
                + "representativeClaimantType=null, respondentCollection=[RespondentSumTypeItem(id=null, "
                + "value=RespondentSumType(responseStatus=null, responseToClaim=null, rejectionReason=null, "
                + "rejectionReasonOther=null, responseOutOfTime=null, responseNotOnPrescribedForm=null, "
                + "responseRequiredInfoAbsent=null, responseNotes=null, response_ReferredToJudge=null, "
                + "responseReturnedFromJudge=null, respondentName=Andrew Smith, respondentACASQuestion=null, "
                + "respondentACAS=null, respondentACASNo=null, respondentAddress=Line1, PostCode, "
                + "respondentPhone1=null, respondentPhone2=null, respondentEmail=null, "
                + "respondentContactPreference=null, responseStruckOut=null, responseStruckOutDate=null, "
                + "responseStruckOutChairman=null, responseStruckOutReason=null, responseRespondentAddress=null, "
                + "responseRespondentPhone1=null, responseRespondentPhone2=null, responseRespondentEmail=null, "
                + "responseRespondentContactPreference=null, responseReceived=null, responseReceivedDate=null, "
                + "responseRespondentNameQuestion=null, responseRespondentName=null, responseContinue=null, "
                + "responseCounterClaim=null, responseReference=null))], repCollection=null, "
                + "positionType=Manually Created, dateToPosition=null, currentPosition=null, fileLocation=null, "
                + "fileLocationGlasgow=null, fileLocationAberdeen=null, fileLocationDundee=null, "
                + "fileLocationEdinburgh=null, hearingCollection=null, depositCollection=null, "
                + "judgementCollection=null, jurCodesCollection=null, bfActions=null, clerkResponsible=null, "
                + "userLocation=null, documentCollection=null, additionalCaseInfoType=null, "
                + "correspondenceScotType=null, correspondenceType=null, addressLabelsSelectionType=null, "
                + "addressLabelCollection=null, addressLabelsAttributesType=null, caseNotes=null, "
                + "claimantWorkAddress=ClaimantWorkAddressType(claimantWorkAddress=null, "
                + "claimantWorkPhoneNumber=null), claimantRepresentedQuestion=null, managingOffice=Glasgow, "
                + "allocatedOffice=null, caseSource=Manually Created, conciliationTrack=null, "
                + "counterClaim=null, eccCases=null, restrictedReporting=null, printHearingDetails=null, "
                + "printHearingCollection=null, targetHearingDate=null, claimant=null, respondent=null, EQP=null, "
                + "flag1=null, flag2=null, docMarkUp=null, caseRefNumberCount=null, startCaseRefNumber=null, "
                + "multipleRefNumber=null, caseRefECC=null, respondentECC=null, ccdID=null, flagsImageFileName=null, "
                + "flagsImageAltText=null, hearingNumbers=null, hearingTypes=null, hearingPublicPrivate=null, "
                + "hearingVenue=null, hearingEstLengthNum=null, hearingEstLengthNumType=null, hearingSitAlone=null, "
                + "hearingStage=null, listedDate=null, hearingNotes=null, hearingSelection=null, hearingActions=null, "
                + "hearingERMember=null, hearingEEMember=null, hearingDatesRequireAmending=null, "
                + "hearingDateSelection=null, hearingDateActions=null, hearingStatus=null, Postponed_by=null, "
                + "hearingRoom=null, hearingClerk=null, hearingJudge=null, hearingCaseDisposed=null, "
                + "hearingPartHeard=null, hearingReservedJudgement=null, attendeeClaimant=null, "
                + "attendeeNonAttendees=null, attendeeRespNoRep=null, attendeeRespAndRep=null, attendeeRepOnly=null, "
                + "hearingTimingStart=null, hearingTimingBreak=null, hearingTimingResume=null, "
                + "hearingTimingFinish=null, hearingTimingDuration=null, companyPremises=null, officeCT=null, "
                + "reasonForCT=null, relatedCaseCT=null, positionTypeCT=null, linkedCaseCT=null, stateAPI=null)";
        defaultValuesReaderService.getCaseData(getCaseDataWithClaimantWorkAddress(caseData), postDefaultValuesGlasgow);
        assertEquals(caseDataExpected, caseData.toString());
    }

    @Test
    public void getGlasgowOfficePostDefaultValues() {
        glasgowCaseDetails.getCaseData().setManagingOffice(GLASGOW_OFFICE);
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( GLASGOW_OFFICE, glasgowCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesGlasgow.toString(), postDefaultValues1.toString());
    }

    @Test
    public void getAberdeenOfficePostDefaultValues() {
        glasgowCaseDetails.getCaseData().setManagingOffice(ABERDEEN_OFFICE);
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( ABERDEEN_OFFICE, glasgowCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesAberdeen.toString(), postDefaultValues1.toString());
    }

    @Test
    public void getDundeeOfficePostDefaultValues() {
        glasgowCaseDetails.getCaseData().setManagingOffice(DUNDEE_OFFICE);
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( DUNDEE_OFFICE, glasgowCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesDundee, postDefaultValues1);
    }

    @Test
    public void getEdinburghOfficePostDefaultValues() {
        glasgowCaseDetails.getCaseData().setManagingOffice(EDINBURGH_OFFICE);
        DefaultValues postDefaultValues1 = defaultValuesReaderService.getDefaultValues( EDINBURGH_OFFICE, glasgowCaseDetails.getCaseTypeId());
        assertEquals(postDefaultValuesEdinburgh, postDefaultValues1);
    }

    @Test
    public void getListingData() {
        String listingDataExpected = "ListingData(tribunalCorrespondenceAddress=Glasgow Tribunals Centre, 3 "
                + "Atlantic Quay, 20 York Street, Glasgow, G2 8GT, tribunalCorrespondenceTelephone=0141 204 0730, "
                + "tribunalCorrespondenceFax=01264 785 177, tribunalCorrespondenceDX=DX 580003, "
                + "tribunalCorrespondenceEmail=glasgowet@justice.gov.uk, hearingDateType=null, listingDate=null, "
                + "listingDateFrom=null, listingDateTo=null, listingVenue=null, listingCollection=null, "
                + "listingVenueOfficeGlas=null, listingVenueOfficeAber=null, venueGlasgow=null, venueAberdeen=null, "
                + "venueDundee=null, venueEdinburgh=null, hearingDocType=null, hearingDocETCL=null, roomOrNoRoom=null, "
                + "docMarkUp=null, bfDateCollection=null, clerkResponsible=null, reportType=null, documentName=null, "
                + "showAll=null, localReportsSummaryHdr=null, localReportsSummary=null, localReportsSummaryHdr2=null, "
                + "localReportsSummary2=null, localReportsDetailHdr=null, localReportsDetail=null)";
        assertEquals(listingDataExpected, defaultValuesReaderService.getListingData(listingData, postDefaultValuesGlasgow).toString());
    }
}