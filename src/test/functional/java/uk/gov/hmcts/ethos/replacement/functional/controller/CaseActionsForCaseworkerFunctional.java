package uk.gov.hmcts.ethos.replacement.functional.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import net.serenitybdd.junit.runners.SerenityRunner;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ecm.common.model.ccd.CCDRequest;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.CaseDetails;
import uk.gov.hmcts.ecm.common.model.ccd.types.ClaimantIndType;
import uk.gov.hmcts.ethos.replacement.functional.FunctionalTest;

import java.io.IOException;

import static io.restassured.RestAssured.useRelaxedHTTPSValidation;

@RunWith(SerenityRunner.class)
@TestPropertySource(locations = "classpath:application.properties")
public class CaseActionsForCaseworkerFunctional {

    private String AUTH_TOKEN = "";

    private FuncHelper funcHelper;
    private FunctionalCCDReq functionalCcdReq;

    @Before
    public void setUp() throws IOException {
        funcHelper = new FuncHelper();
        useRelaxedHTTPSValidation();
        functionalCcdReq = new FunctionalCCDReq();
    }

    @Test
    @Category(FunctionalTest.class)
    public void createCaseIndividual() throws IOException {
        CCDRequest ccdRequest = functionalCcdReq.CCDRequest();
        int response = funcHelper.createIndividualCase(ccdRequest);
        Assertions.assertEquals(200, response);
    }

    @Test
    @Category(FunctionalTest.class)
    public void createIndividualCaseNoPayload() throws IOException {
        CCDRequest ccdRequest = new CCDRequest();
        AUTH_TOKEN = funcHelper.loadAuthToken();
        RestAssured.given()
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
            .body(ccdRequest)
            .post("/createCase")
            .then()
            .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @Category(FunctionalTest.class)
    public void retrieveCases() throws IOException {
        CCDRequest ccdRequest = functionalCcdReq.CCDRequest();
        AUTH_TOKEN = funcHelper.loadAuthToken();
        RestAssured.given()
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
                .body(ccdRequest)
                .post("/retrieveCases")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    @Category(FunctionalTest.class)
    public void updateCase() throws IOException {
        CCDRequest ccdRequest = new CCDRequest();
        CaseDetails caseDetails = functionalCcdReq.CDDetails();
        CaseData caseData = functionalCcdReq.CDData();
        caseData.setEthosCaseReference("1811113/2021");

        ClaimantIndType claimantIndType = new ClaimantIndType();
        claimantIndType.setClaimantFirstNames("John");
        claimantIndType.setClaimantLastName("Terry");

        caseData.setClaimantIndType(claimantIndType);
        caseData.setClaimant("John Terry");

        caseDetails.setCaseData(caseData);
        ccdRequest.setCaseDetails(caseDetails);

        AUTH_TOKEN = funcHelper.loadAuthToken();
        RestAssured.given()
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
            .body(ccdRequest)
            .post("/updateCase");
    }

    @Test
    @Category(FunctionalTest.class)
    public void postDefaultValues() throws IOException {
        CCDRequest ccdRequest = functionalCcdReq.CCDRequest();
        Response response = funcHelper.getCcdResponse(ccdRequest, "/postDefaultValues");
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
    }

    @Test
    @Category(FunctionalTest.class)
    public void invokePostDefaultAsGet() throws IOException {
        AUTH_TOKEN = funcHelper.loadAuthToken();
        RestAssured.given()
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
                .get("/postDefaultValues")
                .then()
                .statusCode(405);
    }

    @Test
    @Category(FunctionalTest.class)
    public void postDefaultNoPayload() throws IOException {
        CCDRequest ccdRequest = new CCDRequest();
        AUTH_TOKEN = funcHelper.loadAuthToken();
        RestAssured.given()
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
                .body(ccdRequest)
                .post("/postDefaultValues")
                .then()
                .statusCode(500);
    }

    @Test
    @Category(FunctionalTest.class)
    public void preDefaultValues() throws IOException {
        CCDRequest ccdRequest = functionalCcdReq.CCDRequest();
        AUTH_TOKEN = funcHelper.loadAuthToken();
        RestAssured.given()
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
            .body(ccdRequest)
            .post("/preDefaultValues")
            .then()
            .statusCode(200);
    }

    @Test
    @Category(FunctionalTest.class)
    public void preDefaultNoPayload() throws IOException {
    CCDRequest ccdRequest = new CCDRequest();
        AUTH_TOKEN = funcHelper.loadAuthToken();
        RestAssured.given()
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
                .body(ccdRequest)
                .post("/preDefaultValues")
                .then()
                .statusCode(500);
    }

    @Test
    @Category(FunctionalTest.class)
    public void invokePreDefaultAsGet() throws IOException {
        AUTH_TOKEN = funcHelper.loadAuthToken();
        RestAssured.given()
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
                .get("/preDefaultValues")
                .then()
                .statusCode(405);
    }

    @Test
    @Category(FunctionalTest.class)
    public void amendCaseDetails() throws IOException {
        AUTH_TOKEN = funcHelper.loadAuthToken();
        CCDRequest ccdRequest1 = functionalCcdReq.CCDRequest();
        Response response = RestAssured.given()
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
            .body(ccdRequest1)
            .post("/createCase");
        String ethosCaseReference = response.body().jsonPath().getString("data.ethosCaseReference");
        String feeGroupReference = response.body().jsonPath().getString("data.feeGroupReference");
        String caseId = response.body().jsonPath().getString("id");
        ccdRequest1.getCaseDetails().getCaseData().setEthosCaseReference(ethosCaseReference);
        ccdRequest1.getCaseDetails().getCaseData().setFeeGroupReference(feeGroupReference);
        ccdRequest1.getCaseDetails().setCaseId(caseId);

        ccdRequest1.getCaseDetails().getCaseData().setFileLocation("Appeals");
        ccdRequest1.getCaseDetails().getCaseData().setConciliationTrack("No Conciliation");

        Response response1 = RestAssured.given()
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
                .body(ccdRequest1)
                .post("/amendCase");
        System.out.println(response1.getStatusCode());
    }

    @Test
    @Category(FunctionalTest.class)
    public void aboutToStartDisposal() throws IOException {
        AUTH_TOKEN = funcHelper.loadAuthToken();
        CCDRequest ccdRequest = functionalCcdReq.CCDRequest();
        Response response = RestAssured.given()
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
                .body(ccdRequest)
                .post("/aboutToStartDisposal");
        Assertions.assertEquals(200, response.getStatusCode());

    }

}
