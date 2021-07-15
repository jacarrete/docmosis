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
import uk.gov.hmcts.ecm.common.model.bulk.BulkData;
import uk.gov.hmcts.ecm.common.model.bulk.BulkDetails;
import uk.gov.hmcts.ecm.common.model.bulk.BulkRequest;
import uk.gov.hmcts.ecm.common.model.bulk.items.MultipleTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.ecm.common.model.ccd.items.RespondentSumTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.types.ClaimantIndType;
import uk.gov.hmcts.ecm.common.model.ccd.types.RespondentSumType;
import uk.gov.hmcts.ecm.common.model.helper.BulkCasesPayload;
import uk.gov.hmcts.ecm.common.model.helper.BulkRequestPayload;
import uk.gov.hmcts.ethos.replacement.docmosis.DocmosisApplication;
import uk.gov.hmcts.ethos.replacement.docmosis.service.BulkCreationService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.BulkSearchService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.BulkUpdateService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.VerifyTokenService;
import uk.gov.hmcts.ethos.replacement.docmosis.utils.InternalException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ethos.replacement.docmosis.utils.InternalException.ERROR_MESSAGE;

@RunWith(SpringRunner.class)
@WebMvcTest(PersistentQueueActionsController.class)
@ContextConfiguration(classes = DocmosisApplication.class)
public class PersistentQueueActionsControllerTest {

    private static final String AUTH_TOKEN = "Bearer eyJhbGJbpjciOiJIUzI1NiJ9";
    private static final String AFTER_SUBMITTED_PQ_BULK_URL = "/afterSubmittedBulkPQ";
    private static final String PRE_ACCEPT_PQ_BULK_URL = "/preAcceptBulkPQ";
    private static final String UPDATE_BULK_CASE_PQ_URL = "/updateBulkCasePQ";

    @Autowired
    private WebApplicationContext applicationContext;

    @MockBean
    private BulkCreationService bulkCreationService;

    @MockBean
    private BulkUpdateService bulkUpdateService;

    @MockBean
    private BulkSearchService bulkSearchService;

    @MockBean
    private VerifyTokenService verifyTokenService;

    private MockMvc mvc;
    private JsonNode requestContent;
    private BulkCasesPayload bulkCasesPayload;
    private BulkRequestPayload bulkRequestPayload;

    private void doRequestSetUp() throws IOException, URISyntaxException {
        ObjectMapper objectMapper = new ObjectMapper();
        requestContent = objectMapper.readTree(new File(getClass()
                .getResource("/exampleBulkV1.json").toURI()));
    }

    @Before
    public void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        doRequestSetUp();
        List<SubmitEvent> submitEvents;
        List<MultipleTypeItem> multipleTypeItems = new ArrayList<>();
        bulkCasesPayload = new BulkCasesPayload();
        submitEvents = getSubmitEvents();
        bulkCasesPayload.setSubmitEvents(submitEvents);
        bulkCasesPayload.setMultipleTypeItems(multipleTypeItems);
        BulkData bulkData = new BulkData();
        BulkDetails bulkDetails = new BulkDetails();
        bulkDetails.setCaseData(bulkData);
        bulkRequestPayload = new BulkRequestPayload();
        bulkRequestPayload.setBulkDetails(bulkDetails);
    }

    private List<SubmitEvent> getSubmitEvents() {
        CaseData caseData = new CaseData();
        caseData.setClerkResponsible("JuanFran");
        ClaimantIndType claimantIndType = new ClaimantIndType();
        claimantIndType.setClaimantLastName("Mike");
        caseData.setClaimantIndType(claimantIndType);
        RespondentSumType respondentSumType = new RespondentSumType();
        respondentSumType.setRespondentName("Andrew Smith");
        RespondentSumTypeItem respondentSumTypeItem = new RespondentSumTypeItem();
        respondentSumTypeItem.setValue(respondentSumType);
        caseData.setRespondentCollection(new ArrayList<>(Collections.singletonList(respondentSumTypeItem)));

        caseData.setFileLocation("Manchester");
        SubmitEvent submitEvent1 = new SubmitEvent();
        submitEvent1.setCaseData(caseData);
        SubmitEvent submitEvent2 = new SubmitEvent();
        submitEvent2.setCaseData(caseData);
        return new ArrayList<>(Arrays.asList(submitEvent1, submitEvent2));
    }

    @Test
    public void afterSubmittedBulkPQ() throws Exception {
        when(bulkSearchService.bulkCasesRetrievalRequestElasticSearch(isA(BulkDetails.class), eq(AUTH_TOKEN), isA(Boolean.class), isA(Boolean.class)))
                .thenReturn(bulkCasesPayload);
        when(bulkCreationService.bulkCreationLogic(isA(BulkDetails.class), isA(BulkCasesPayload.class), eq(AUTH_TOKEN), isA(String.class)))
                .thenReturn(bulkRequestPayload);
        when(verifyTokenService.verifyTokenSignature(AUTH_TOKEN)).thenReturn(true);
        mvc.perform(post(AFTER_SUBMITTED_PQ_BULK_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void preAcceptBulkPq() throws Exception {
        when(bulkSearchService.retrievalCasesForPreAcceptRequest(isA(BulkDetails.class), eq(AUTH_TOKEN))).thenReturn(bulkCasesPayload.getSubmitEvents());
        when(bulkUpdateService.bulkPreAcceptLogic(isA(BulkDetails.class), any(), eq(AUTH_TOKEN), eq(true))).thenReturn(bulkRequestPayload);
        when(verifyTokenService.verifyTokenSignature(AUTH_TOKEN)).thenReturn(true);
        mvc.perform(post(PRE_ACCEPT_PQ_BULK_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void updateBulkCasePq() throws Exception {
        when(bulkCreationService.bulkUpdateCaseIdsLogic(isA(BulkRequest.class), eq(AUTH_TOKEN), isA(Boolean.class))).thenReturn(bulkRequestPayload);
        when(verifyTokenService.verifyTokenSignature(AUTH_TOKEN)).thenReturn(true);
        mvc.perform(post(UPDATE_BULK_CASE_PQ_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.errors", nullValue()))
                .andExpect(jsonPath("$.warnings", nullValue()));
    }

    @Test
    public void preAcceptBulkPqError400() throws Exception {
        mvc.perform(post(PRE_ACCEPT_PQ_BULK_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateBulkCasePqError400() throws Exception {
        mvc.perform(post(UPDATE_BULK_CASE_PQ_URL)
                .content("error")
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void preAcceptBulkPqError500() throws Exception {
        when(bulkSearchService.retrievalCasesForPreAcceptRequest(isA(BulkDetails.class), eq(AUTH_TOKEN))).thenThrow(new InternalException(ERROR_MESSAGE));
        when(verifyTokenService.verifyTokenSignature(AUTH_TOKEN)).thenReturn(true);
        mvc.perform(post(PRE_ACCEPT_PQ_BULK_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void updateBulkCasePqError500() throws Exception {
        when(bulkCreationService.bulkUpdateCaseIdsLogic(isA(BulkRequest.class), eq(AUTH_TOKEN), isA(Boolean.class))).thenThrow(new InternalException(ERROR_MESSAGE));
        when(verifyTokenService.verifyTokenSignature(AUTH_TOKEN)).thenReturn(true);
        mvc.perform(post(UPDATE_BULK_CASE_PQ_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void afterSubmittedPqBulkForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(AUTH_TOKEN)).thenReturn(false);
        mvc.perform(post(AFTER_SUBMITTED_PQ_BULK_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void preAcceptBulkPqForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(AUTH_TOKEN)).thenReturn(false);
        mvc.perform(post(PRE_ACCEPT_PQ_BULK_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updateBulkCasePqForbidden() throws Exception {
        when(verifyTokenService.verifyTokenSignature(AUTH_TOKEN)).thenReturn(false);
        mvc.perform(post(UPDATE_BULK_CASE_PQ_URL)
                .content(requestContent.toString())
                .header("Authorization", AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}