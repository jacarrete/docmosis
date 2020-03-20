package uk.gov.hmcts.ethos.replacement.docmosis.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ethos.replacement.docmosis.DocmosisApplication;
import uk.gov.hmcts.ethos.replacement.docmosis.model.ccd.CCDRequest;
import uk.gov.hmcts.ethos.replacement.docmosis.model.ccd.CaseData;
import uk.gov.hmcts.ethos.replacement.docmosis.model.ccd.SubmitEvent;
import uk.gov.hmcts.ethos.replacement.docmosis.model.helper.DefaultValues;
import uk.gov.hmcts.ethos.replacement.docmosis.service.*;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ethos.replacement.docmosis.model.helper.Constants.*;
import static uk.gov.hmcts.ethos.replacement.docmosis.utils.SetUpUtils.feignError;

@RunWith(SpringRunner.class)
@WebMvcTest(CaseActionsForCaseWorkerController.class)
@ContextConfiguration(classes = DocmosisApplication.class)
public class CaseActionsForCaseWorkerControllerTest {

    private static final String AUTH_TOKEN = "Bearer eyJhbGJbpjciOiJIUzI1NiJ9";
    private static final String CREATION_CASE_URL = "/createCase";
    private static final String RETRIEVE_CASE_URL = "/retrieveCase";
    private static final String RETRIEVE_CASES_URL = "/retrieveCases";
    private static final String UPDATE_CASE_URL = "/updateCase";
    private static final String PRE_DEFAULT_VALUES_URL = "/preDefaultValues";
    private static final String POST_DEFAULT_VALUES_URL = "/postDefaultValues";
    private static final String PRE_ACCEPT_CASE_URL = "/preAcceptCase";
    private static final String AMEND_CASE_DETAILS_URL = "/amendCaseDetails";
    private static final String ADD_AMEND_ET3_URL = "/addAmendET3";
    private static final String AMEND_CASE_STATE_URL = "/amendCaseState";
    private static final String MID_RESPONDENT_ADDRESS_URL = "/midRespondentAddress";

    @Autowired
    private WebApplicationContext applicationContext;

    @MockBean
    private CaseCreationForCaseWorkerService caseCreationForCaseWorkerService;

    @MockBean
    private CaseRetrievalForCaseWorkerService caseRetrievalForCaseWorkerService;

    @MockBean
    private CaseUpdateForCaseWorkerService caseUpdateForCaseWorkerService;

    @MockBean
    private DefaultValuesReaderService defaultValuesReaderService;

    @MockBean
    private CaseManagementForCaseWorkerService caseManagementForCaseWorkerService;

    @MockBean
    private SingleReferenceService singleReferenceService;

    @MockBean
    private VerifyTokenService verifyTokenService;

    @MockBean
    private  EventValidationService eventValidationService;

    private MockMvc mvc;
    private JsonNode requestContent;
    private JsonNode requestContent2;
    private JsonNode requestContent3;
    private SubmitEvent submitEvent;
    private DefaultValues defaultValues;

    private void doRequestSetUp() throws IOException, URISyntaxException {
        ObjectMapper objectMapper = new ObjectMapper();
        requestContent = objectMapper.readTree(new File(getClass()
                .getResource("/exampleV1.json").toURI()));
        requestContent2 = objectMapper.readTree(new File(getClass()
                .getResource("/exampleV2.json").toURI()));
        requestContent3 = objectMapper.readTree(new File(getClass()
                .getResource("/exampleV3.json").toURI()));
    }

    @Before
    public void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        doRequestSetUp();
        submitEvent = new SubmitEvent();
        submitEvent.setCaseData(new CaseData());
        defaultValues = DefaultValues.builder()
                .positionType("Awaiting ET3")
                .claimantTypeOfClaimant(INDIVIDUAL_TYPE_CLAIMANT)
                .managingOffice(GLASGOW_OFFICE)
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
    public void createCase() throws Exception {
        when(caseCreationForCaseWorkerService.caseCreationRequest(isA(CCDRequest.class), eq(AUTH_TOKEN))).thenReturn(submitEvent);
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(CREATION_CASE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void retrieveCase() throws Exception {
        when(caseRetrievalForCaseWorkerService.caseRetrievalRequest(isA(CCDRequest.class), eq(AUTH_TOKEN))).thenReturn(submitEvent);
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(RETRIEVE_CASE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void retrieveCases() throws Exception {
        List<SubmitEvent> submitEventList = Collections.singletonList(submitEvent);
        when(caseRetrievalForCaseWorkerService.casesRetrievalRequest(isA(CCDRequest.class), eq(AUTH_TOKEN))).thenReturn(submitEventList);
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(RETRIEVE_CASES_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void updateCase() throws Exception {
        when(caseUpdateForCaseWorkerService.caseUpdateRequest(isA(CCDRequest.class), eq(AUTH_TOKEN))).thenReturn(submitEvent);
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(UPDATE_CASE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void preDefaultValues() throws Exception {
        when(defaultValuesReaderService.getDefaultValues(isA(String.class), isA(String.class), isA(String.class))).thenReturn(defaultValues);
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(PRE_DEFAULT_VALUES_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void postDefaultValuesFromET1WithPositionTypeDefined() throws Exception {
        when(defaultValuesReaderService.getDefaultValues(isA(String.class), isA(String.class), isA(String.class))).thenReturn(defaultValues);
        when(defaultValuesReaderService.getCaseData(isA(CaseData.class), isA(DefaultValues.class), (isA(Boolean.class)))).thenReturn(submitEvent.getCaseData());
        when(singleReferenceService.createReference(isA(String.class), isA(String.class))).thenReturn("5100001/2019");
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(POST_DEFAULT_VALUES_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", hasSize(0)))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void postDefaultValues() throws Exception {
        when(defaultValuesReaderService.getDefaultValues(isA(String.class), isA(String.class), isA(String.class))).thenReturn(defaultValues);
        when(defaultValuesReaderService.getCaseData(isA(CaseData.class), isA(DefaultValues.class), (isA(Boolean.class)))).thenReturn(submitEvent.getCaseData());
        when(singleReferenceService.createReference(isA(String.class), isA(String.class))).thenReturn("5100001/2019");
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(POST_DEFAULT_VALUES_URL)
                .content(requestContent2.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", hasSize(0)))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void postDefaultValuesWithErrors() throws Exception {
        when(defaultValuesReaderService.getDefaultValues(isA(String.class), isA(String.class), isA(String.class))).thenReturn(defaultValues);
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(POST_DEFAULT_VALUES_URL)
                .content(requestContent3.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void preAcceptCase() throws Exception {
        when(caseManagementForCaseWorkerService.preAcceptCase(isA(CCDRequest.class))).thenReturn(submitEvent.getCaseData());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(PRE_ACCEPT_CASE_URL)
                .content(requestContent2.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void amendCaseDetails() throws Exception {
        when(defaultValuesReaderService.getDefaultValues(isA(String.class), isA(String.class), isA(String.class))).thenReturn(defaultValues);
        when(defaultValuesReaderService.getCaseData(isA(CaseData.class), isA(DefaultValues.class), (isA(Boolean.class)))).thenReturn(submitEvent.getCaseData());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(AMEND_CASE_DETAILS_URL)
                .content(requestContent2.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", notNullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void addAmendET3() throws Exception {
        when(caseManagementForCaseWorkerService.struckOutRespondents(isA(CCDRequest.class))).thenReturn(submitEvent.getCaseData());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(ADD_AMEND_ET3_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", notNullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void amendCaseState() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(AMEND_CASE_STATE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", notNullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void midRespondentAddress() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(MID_RESPONDENT_ADDRESS_URL)
                .content(requestContent2.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void midRespondentAddressPopulated() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(MID_RESPONDENT_ADDRESS_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void createCaseError400() throws Exception {
        mvc.perform(post(CREATION_CASE_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void retrieveCaseError400() throws Exception {
        mvc.perform(post(RETRIEVE_CASE_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void retrieveCasesError400() throws Exception {
        mvc.perform(post(RETRIEVE_CASES_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateCaseError400() throws Exception {
        mvc.perform(post(UPDATE_CASE_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void preDefaultValuesError400() throws Exception {
        mvc.perform(post(PRE_DEFAULT_VALUES_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void postDefaultValuesError400() throws Exception {
        mvc.perform(post(POST_DEFAULT_VALUES_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void preAcceptCaseError400() throws Exception {
        mvc.perform(post(PRE_ACCEPT_CASE_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void amendCaseDetailsError400() throws Exception {
        mvc.perform(post(AMEND_CASE_DETAILS_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void addAmendET3Error400() throws Exception {
        mvc.perform(post(ADD_AMEND_ET3_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void amendCaseStateError400() throws Exception {
        mvc.perform(post(AMEND_CASE_STATE_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void midRespondentAddressError400() throws Exception {
        mvc.perform(post(MID_RESPONDENT_ADDRESS_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createCaseError500() throws Exception {
        when(caseCreationForCaseWorkerService.caseCreationRequest(isA(CCDRequest.class), eq(AUTH_TOKEN))).thenThrow(feignError());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(CREATION_CASE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void retrieveCaseError500() throws Exception {
        when(caseRetrievalForCaseWorkerService.caseRetrievalRequest(isA(CCDRequest.class), eq(AUTH_TOKEN))).thenThrow(feignError());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(RETRIEVE_CASE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void retrieveCasesError500() throws Exception {
        when(caseRetrievalForCaseWorkerService.casesRetrievalRequest(isA(CCDRequest.class), eq(AUTH_TOKEN))).thenThrow(feignError());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(RETRIEVE_CASES_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void updateCaseError500() throws Exception {
        when(caseUpdateForCaseWorkerService.caseUpdateRequest(isA(CCDRequest.class), eq(AUTH_TOKEN))).thenThrow(feignError());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(UPDATE_CASE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void preDefaultValuesError500() throws Exception {
        when(defaultValuesReaderService.getDefaultValues(isA(String.class), isA(String.class), isA(String.class))).thenThrow(feignError());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(PRE_DEFAULT_VALUES_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void postDefaultValuesError500() throws Exception {
        when(defaultValuesReaderService.getDefaultValues(isA(String.class), isA(String.class), isA(String.class))).thenThrow(feignError());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(POST_DEFAULT_VALUES_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void preAcceptCaseError500() throws Exception {
        when(caseManagementForCaseWorkerService.preAcceptCase(isA(CCDRequest.class))).thenThrow(feignError());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(PRE_ACCEPT_CASE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void amendCaseDetailsError500() throws Exception {
        when(defaultValuesReaderService.getDefaultValues(isA(String.class), isA(String.class), isA(String.class))).thenThrow(feignError());
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(true);
        mvc.perform(post(AMEND_CASE_DETAILS_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }



    @Test
    public void createCaseErrorForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(CREATION_CASE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void retrieveCaseForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(RETRIEVE_CASE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void retrieveCasesForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(RETRIEVE_CASES_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updateCaseForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(UPDATE_CASE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void preDefaultValuesForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(PRE_DEFAULT_VALUES_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void postDefaultValuesForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(POST_DEFAULT_VALUES_URL)
                .content(requestContent2.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void preAcceptCaseForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(PRE_ACCEPT_CASE_URL)
                .content(requestContent2.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void amendCaseDetailsForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(AMEND_CASE_DETAILS_URL)
                .content(requestContent2.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void addAmendET3Forbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(ADD_AMEND_ET3_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void amendCaseStateForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(AMEND_CASE_STATE_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void midRespondentAddressForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(MID_RESPONDENT_ADDRESS_URL)
                .content(requestContent2.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void midRespondentAddressPopulatedForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(eq(AUTH_TOKEN))).thenReturn(false);
        mvc.perform(post(MID_RESPONDENT_ADDRESS_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

}