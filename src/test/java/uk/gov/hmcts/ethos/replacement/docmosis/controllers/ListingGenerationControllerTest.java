package uk.gov.hmcts.ethos.replacement.docmosis.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ecm.common.model.ccd.Address;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.CaseDetails;
import uk.gov.hmcts.ecm.common.model.ccd.DocumentInfo;
import uk.gov.hmcts.ecm.common.model.helper.DefaultValues;
import uk.gov.hmcts.ecm.common.model.listing.ListingData;
import uk.gov.hmcts.ecm.common.model.listing.ListingDetails;
import uk.gov.hmcts.ecm.common.model.listing.ListingRequest;
import uk.gov.hmcts.ecm.common.model.listing.items.ListingTypeItem;
import uk.gov.hmcts.ecm.common.model.listing.types.ListingType;
import uk.gov.hmcts.ethos.replacement.docmosis.DocmosisApplication;
import uk.gov.hmcts.ethos.replacement.docmosis.service.DefaultValuesReaderService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.ListingService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.VerifyTokenService;
import uk.gov.hmcts.ethos.replacement.docmosis.utils.InternalException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SCOTLAND_LISTING_CASE_TYPE_ID;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SINGLE_CASE_TYPE;
import static uk.gov.hmcts.ethos.replacement.docmosis.utils.InternalException.ERROR_MESSAGE;

@RunWith(SpringRunner.class)
@WebMvcTest(ListingGenerationController.class)
@ContextConfiguration(classes = DocmosisApplication.class)
public class ListingGenerationControllerTest {

    private static final String AUTH_TOKEN = "Bearer eyJhbGJbpjciOiJIUzI1NiJ9";
    private static final String LISTING_CASE_CREATION_URL = "/listingCaseCreation";
    private static final String LISTING_HEARINGS_URL = "/listingHearings";
    private static final String GENERATE_HEARING_DOCUMENT_URL = "/generateHearingDocument";
    private static final String GENERATE_HEARING_DOCUMENT_CONFIRMATION_URL = "/generateHearingDocumentConfirmation";
    private static final String LISTING_SINGLE_CASES_URL = "/listingSingleCases";
    private static final String GENERATE_LISTINGS_DOC_SINGLE_CASES_URL = "/generateListingsDocSingleCases";
    private static final String GENERATE_LISTINGS_DOC_SINGLE_CASES_CONFIRMATION_URL = "/generateListingsDocSingleCasesConfirmation";
    private static final String GENERATE_REPORT_URL = "/generateReport";

    @Autowired
    private WebApplicationContext applicationContext;

    @MockBean
    private ListingService listingService;

    @MockBean
    private DefaultValuesReaderService defaultValuesReaderService;

    @MockBean
    private VerifyTokenService verifyTokenService;

    private MockMvc mvc;
    private JsonNode requestContent;
    private JsonNode requestContent1;
    private JsonNode requestContentSingleCase;
    private JsonNode requestContentSingleCase1;
    private JsonNode listingRequestJson;
    private ListingDetails listingDetails;
    private ListingData listingData;
    private CaseData caseData;
    private DocumentInfo documentInfo;
    private DefaultValues defaultValues;
    private ListingRequest singleListingRequest;
    private ListingRequest listingRequest;

    private void doRequestSetUp() throws Exception, IOException, URISyntaxException {
        ObjectMapper objectMapper = new ObjectMapper();
        requestContent = objectMapper.readTree(new File(getClass()
                .getResource("/exampleListingV1.json").toURI()));
        requestContent1 = objectMapper.readTree(new File(getClass()
                .getResource("/exampleListingV2.json").toURI()));
        requestContentSingleCase = objectMapper.readTree(new File(getClass()
                .getResource("/exampleListingSingleV1.json").toURI()));
        requestContentSingleCase1 = objectMapper.readTree(new File(getClass()
                .getResource("/exampleListingSingleV2.json").toURI()));

        listingRequest = generateListingDetails("exampleListingV3.json");
        singleListingRequest = generateListingDetails("exampleListingV2.json");
    }

    private ListingRequest generateListingDetails(String jsonFileName) throws Exception {
        String json = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource(jsonFileName)).toURI())));

        return new ObjectMapper().readValue(json, ListingRequest.class);
    }

    private ListingRequest getListingData() {

        singleListingRequest = new ListingRequest();
        var listingDetails = new ListingDetails();
        var listingData = new ListingData();

        listingData.setDocMarkUp("Test doc markup");
        listingData.setDocumentName("test listing doc name");
        listingData.setListingDate("2021-10-20");
        listingData.setListingDateFrom("2020-11-12");
        listingData.setHearingDateType("Range");
        listingData.setListingDateTo("2021-10-18");

        var listingTypeItem1 = new ListingTypeItem();
        listingTypeItem1.setId("97087d19-795a-4886-8cdb-06489b8b2ef5");

        var listingTypeValues = new ListingType();
        listingTypeValues.setCauseListTime("12 October 2020");
        listingTypeValues.setCauseListTime("00:00");
        listingTypeValues.setCauseListVenue("Manchester");
        listingTypeValues.setElmoCaseReference("1112");
        listingTypeValues.setJurisdictionCodesList("ADG, COM");
        listingTypeValues.setHearingType("Hearing");
        listingTypeValues.setPositionType("Manually Created");
        listingTypeItem1.setValue(listingTypeValues);

        var listingTypeItem2 = new ListingTypeItem();
        listingTypeItem2.setId("97087d19-795a-4886-8cdb-46089b8b27ef");

        var listingTypeValues2 = new ListingType();
        listingTypeValues2.setCauseListTime("12 October 2020");
        listingTypeValues2.setCauseListTime("00:00");
        listingTypeValues2.setCauseListVenue("Manchester");
        listingTypeValues2.setElmoCaseReference("1135");
        listingTypeValues2.setJurisdictionCodesList("ADG, COM");
        listingTypeValues2.setHearingType("Preliminary Hearing (CM)");
        listingTypeValues2.setPositionType("Manually Created");
        listingTypeItem1.setValue(listingTypeValues2);

        List<ListingTypeItem> listingCollection = new ArrayList<>();
        listingCollection.add(listingTypeItem1);
        listingCollection.add(listingTypeItem2);

        listingData.setListingCollection(listingCollection);

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

        var listingTypeItems = new ArrayList<ListingTypeItem>();
        listingTypeItems.add(new ListingTypeItem());

        listingData.setListingCollection(listingTypeItems);
        listingDetails.setCaseData(listingData);
        singleListingRequest.setCaseDetails(listingDetails);

        return singleListingRequest;
    }

    @Before
    public void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        doRequestSetUp();
        listingDetails = new ListingDetails();
        listingDetails.setCaseTypeId(SCOTLAND_LISTING_CASE_TYPE_ID);
        listingDetails.setCaseData(getListingData().getCaseDetails().getCaseData());

        documentInfo = new DocumentInfo();
        documentInfo.setMarkUp("Test doc markup");
        caseData = new CaseData();
        caseData.setPrintHearingDetails(getListingData().getCaseDetails().getCaseData());

        defaultValues = DefaultValues.builder()
                .positionType("Awaiting ET3")
                .claimantTypeOfClaimant("Individual")
                .managingOffice("Glasgow")
                .caseType(SINGLE_CASE_TYPE)
                .tribunalCorrespondenceAddressLine1("")
                .tribunalCorrespondenceAddressLine2("")
                .tribunalCorrespondenceAddressLine3("")
                .tribunalCorrespondenceTown("")
                .tribunalCorrespondencePostCode("")
                .tribunalCorrespondenceTelephone("3577131270")
                .tribunalCorrespondenceFax("7577126570")
                .tribunalCorrespondenceDX("123456")
                .tribunalCorrespondenceEmail("manchester@gmail.com")
                .build();
    }

    @Test
    public void listingCaseCreation() throws Exception {
        when(listingService.listingCaseCreation(isA(ListingDetails.class)))
                .thenReturn(singleListingRequest.getCaseDetails().getCaseData());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(LISTING_CASE_CREATION_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void listingHearings() throws Exception {
        when(listingService.processListingHearingsRequest(isA(ListingDetails.class), eq(AUTH_TOKEN)))
                .thenReturn(listingDetails.getCaseData());
        when(defaultValuesReaderService.getDefaultValues(isA(String.class), isA(String.class)))
                .thenReturn(defaultValues);
        when(defaultValuesReaderService.getListingData(isA(ListingData.class), isA(DefaultValues.class)))
                .thenReturn(singleListingRequest.getCaseDetails().getCaseData());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(LISTING_HEARINGS_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", hasSize(0)))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void generateHearingDocument() throws Exception {
        when(listingService.processHearingDocument(isA(ListingData.class), isA(String.class), eq(AUTH_TOKEN)))
                .thenReturn(documentInfo);
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(GENERATE_HEARING_DOCUMENT_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void generateHearingDocumentConfirmation() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(GENERATE_HEARING_DOCUMENT_CONFIRMATION_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void generateHearingDocumentWithErrors() throws Exception {
        when(listingService.processHearingDocument(isA(ListingData.class), isA(String.class), eq(AUTH_TOKEN)))
                .thenReturn(documentInfo);
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(GENERATE_HEARING_DOCUMENT_URL)
                .content(requestContent1.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void listingSingleCases() throws Exception {
        when(listingService.processListingSingleCasesRequest(isA(CaseDetails.class))).thenReturn(caseData);
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(LISTING_SINGLE_CASES_URL)
                .content(requestContentSingleCase.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", hasSize(0)))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void generateListingsDocSingleCases() throws Exception {
        when(listingService.setCourtAddressFromCaseData(isA(CaseData.class)))
                .thenReturn(singleListingRequest.getCaseDetails().getCaseData());
        when(listingService.processHearingDocument(isA(ListingData.class), isA(String.class), eq(AUTH_TOKEN)))
                .thenReturn(documentInfo);
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(GENERATE_LISTINGS_DOC_SINGLE_CASES_URL)
                .content(requestContentSingleCase.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void generateListingsDocSingleCasesConfirmation() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(GENERATE_LISTINGS_DOC_SINGLE_CASES_CONFIRMATION_URL)
                .content(requestContentSingleCase.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void generateListingsDocSingleCasesWithErrors() throws Exception {
        when(listingService.processHearingDocument(isA(ListingData.class), isA(String.class), eq(AUTH_TOKEN)))
                .thenReturn(documentInfo);
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(GENERATE_LISTINGS_DOC_SINGLE_CASES_URL)
                .content(requestContentSingleCase1.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void generateReportOk() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        when(listingService.generateReportData(isA(ListingDetails.class), eq(AUTH_TOKEN)))
                .thenReturn(listingRequest.getCaseDetails().getCaseData());
        when(listingService.processHearingDocument(isA(ListingData.class),
                isA(String.class), eq(AUTH_TOKEN)))
                .thenReturn(documentInfo);

        mvc.perform(post(GENERATE_REPORT_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void generateReportError400() throws Exception {
        mvc.perform(post(GENERATE_REPORT_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void generateReportError500() throws Exception {
        when(listingService.generateReportData(isA(ListingDetails.class), eq(AUTH_TOKEN)))
                .thenThrow(new InternalException(ERROR_MESSAGE));
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(GENERATE_REPORT_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void generateReportForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(GENERATE_REPORT_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void generateHearingDocumentError400() throws Exception {
        mvc.perform(post(GENERATE_HEARING_DOCUMENT_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void listingCaseCreationError400() throws Exception {
        mvc.perform(post(LISTING_CASE_CREATION_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void listingHearingsError400() throws Exception {
        mvc.perform(post(LISTING_HEARINGS_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void listingSingleCasesError400() throws Exception {
        mvc.perform(post(LISTING_SINGLE_CASES_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void generateListingsDocSingleCasesError400() throws Exception {
        mvc.perform(post(GENERATE_LISTINGS_DOC_SINGLE_CASES_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void listingCaseCreationError500() throws Exception {
        when(listingService.listingCaseCreation(isA(ListingDetails.class)))
                .thenThrow(new InternalException(ERROR_MESSAGE));
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(LISTING_CASE_CREATION_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void listingSingleCasesError500() throws Exception {
        when(listingService.processListingSingleCasesRequest(isA(CaseDetails.class)))
                .thenThrow(new InternalException(ERROR_MESSAGE));
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(LISTING_SINGLE_CASES_URL)
                .content(requestContentSingleCase.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void generateListingsDocSingleCasesError500() throws Exception {
        when(listingService.processHearingDocument(isA(ListingData.class), isA(String.class), eq(AUTH_TOKEN)))
                .thenThrow(new InternalException(ERROR_MESSAGE));

        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        when(listingService.setCourtAddressFromCaseData(isA(CaseData.class)))
                .thenReturn(singleListingRequest.getCaseDetails().getCaseData());

        mvc.perform(post(GENERATE_LISTINGS_DOC_SINGLE_CASES_URL)
                .content(requestContentSingleCase.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void generateHearingDocumentError500() throws Exception {
        when(listingService.processHearingDocument(isA(ListingData.class), isA(String.class), eq(AUTH_TOKEN)))
                .thenThrow(new InternalException(ERROR_MESSAGE));
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(GENERATE_HEARING_DOCUMENT_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void listingHearingsError500() throws Exception {
        when(listingService.processListingHearingsRequest(isA(ListingDetails.class), eq(AUTH_TOKEN)))
                .thenThrow(new InternalException(ERROR_MESSAGE));
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(LISTING_HEARINGS_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void listingCaseCreationForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(LISTING_CASE_CREATION_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void listingHearingsForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(LISTING_HEARINGS_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void generateHearingDocumentConfirmationForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(GENERATE_HEARING_DOCUMENT_CONFIRMATION_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void generateHearingDocumentWithErrorsForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(GENERATE_HEARING_DOCUMENT_URL)
                .content(requestContent1.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void listingSingleCasesForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(LISTING_SINGLE_CASES_URL)
                .content(requestContentSingleCase.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void generateListingsDocSingleCasesForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(GENERATE_LISTINGS_DOC_SINGLE_CASES_URL)
                .content(requestContentSingleCase.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void generateListingsDocSingleCasesConfirmationForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(GENERATE_LISTINGS_DOC_SINGLE_CASES_CONFIRMATION_URL)
                .content(requestContentSingleCase.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

}