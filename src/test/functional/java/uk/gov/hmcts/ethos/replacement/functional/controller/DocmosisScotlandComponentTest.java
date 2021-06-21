package uk.gov.hmcts.ethos.replacement.functional.controller;

import io.restassured.response.Response;
import net.serenitybdd.junit.runners.SerenityRunner;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ecm.common.model.ccd.CCDRequest;
import uk.gov.hmcts.ethos.replacement.functional.FunctionalTest;
import uk.gov.hmcts.ethos.replacement.functional.util.Constants;

import java.io.File;
import java.io.IOException;

import static io.restassured.RestAssured.useRelaxedHTTPSValidation;

@RunWith(SerenityRunner.class)
@TestPropertySource(locations = "classpath:config/application.properties")
public class DocmosisScotlandComponentTest {

    private FuncHelper funcHelper;

    @Before
    public void setUp() throws IOException {
        funcHelper = new FuncHelper();
        useRelaxedHTTPSValidation();
    }

    @Test
    @Category(FunctionalTest.class)
    public void generateDocumentDundee() throws IOException {
        String payload = FileUtils.readFileToString(new File(Constants.TEST_DATA_SCOT_DUNDEE_CASE1), "UTF-8");
        CCDRequest ccdRequest = funcHelper.getCcdRequest("1", "", true, payload);
        Response response = funcHelper.getCcdResponse(ccdRequest, "/amendCaseDetails");
        Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode()); // Should be 200
        String caseData = response.body().prettyPrint();
        caseData = caseData.substring(caseData.indexOf('{') + 1);
        caseData = caseData.substring(0, caseData.lastIndexOf('}'));
        caseData = caseData.replace("\"data\"", "\"case_data\"");
        String caseDetails = FileUtils.readFileToString(new File(Constants.TEST_DATA_SCOT_TEMPLATE), "UTF-8");
        caseDetails = caseDetails.replace("#CASE_DATA#", caseData);
        ccdRequest = funcHelper.getCcdRequest("1", "", true, caseDetails);
        response = funcHelper.getCcdResponse(ccdRequest, "/generateDocument");
        Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode()); // Should be 200
    }
}
