package uk.gov.hmcts.ethos.replacement.functional.controller;

import com.jayway.jsonpath.JsonPath;
import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.ecm.common.model.bulk.BulkRequest;
import uk.gov.hmcts.ethos.replacement.functional.FunctionalTest;
import uk.gov.hmcts.ethos.replacement.functional.util.Constants;
import uk.gov.hmcts.ethos.replacement.functional.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static uk.gov.hmcts.ethos.replacement.functional.util.ResponseUtil.getProperty;

public class BulkActionsFunctionalTest {
    @Value("${test-url}")
    private String testUrl;

    private String AUTH_TOKEN;
    private List<String> caseList = new ArrayList<>();

    private FuncHelper funcHelper;

    @Before
    public void setUp() throws IOException {
        funcHelper = new FuncHelper();
//        baseURI = getProperty("demo.docmosis.api.url");
        useRelaxedHTTPSValidation();
    }

    @Test
    @Category(FunctionalTest.class)
    public void createBulkNoCasesEng() throws IOException {
        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_BULK6), "UTF-8");
        BulkRequest bulkRequest = JsonUtil.getBulkDetails(true, testData);
        Response response = funcHelper.getBulkResponse(bulkRequest, "/createBulk");
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
    }

    @Test
    @Category(FunctionalTest.class)
    public void createBulkNoPayload() throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        Response response = funcHelper.getBulkResponse(bulkRequest, "/createBulk");
        Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    @Category(FunctionalTest.class)
    public void searchBulkTest() throws IOException {
//        caseList.clear();
//        caseList.add(Constants.TEST_DATA_ENG_BULK1_CASE1);
//        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_BULK1));
//        testData = funcHelper.createIndividualCase(caseList, false, testData);
//        BulkRequest bulkRequest = JsonUtil.getBulkDetails(false, testData);
//        Response response = funcHelper.getBulkResponse(bulkRequest, "/createBulk");
        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_BULK6), "UTF-8");
        BulkRequest bulkRequest = JsonUtil.getBulkDetails(true, testData);
        Response response = funcHelper.getBulkResponse(bulkRequest, "/createBulk");
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        Response searchResponse = funcHelper.getBulkResponse(bulkRequest, "/searchBulk");
        Assertions.assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
    }

    @Test
    @Category(FunctionalTest.class)
    public void updateBulk() throws IOException {
        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_BULK6), "UTF-8");
        BulkRequest bulkRequest = JsonUtil.getBulkDetails(true, testData);
        Response response = funcHelper.getBulkResponse(bulkRequest, "/createBulk");
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
//        caseList.clear();
//        caseList.add(Constants.TEST_DATA_ENG_BULK1_CASE1);
//        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_BULK1));
//        testData = funcHelper.createIndividualCase(caseList, false, testData);
//        BulkRequest bulkRequest = JsonUtil.getBulkDetails(false, testData);
//        Response response = funcHelper.getBulkResponse(bulkRequest, "/createBulk");
//        Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        response = funcHelper.getBulkResponse(bulkRequest, "/searchBulk");
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        response = funcHelper.getBulkResponse(bulkRequest, "/updateBulk");
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
//        verifyBulkResponse(testData, response);
    }

    @Test
    @Category(FunctionalTest.class)
    public void updateBulkCase() throws IOException {
        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_BULK6), "UTF-8");
        BulkRequest bulkRequest = JsonUtil.getBulkDetails(true, testData);
        Response response = funcHelper.getBulkResponse(bulkRequest, "/createBulk");
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
//        caseList.clear();
//        caseList.add(Constants.TEST_DATA_ENG_BULK1_CASE1);
//        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_BULK1));
//        testData = funcHelper.createIndividualCase(caseList, false, testData);
//        BulkRequest bulkRequest = JsonUtil.getBulkDetails(false, testData);
//        Response response = funcHelper.getBulkResponse(bulkRequest, "/createBulk");
//        Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        response = funcHelper.getBulkResponse(bulkRequest, "/updateBulkCase");
//        verifyBulkResponse(testData, response);
    }

    @Test
    @Category(FunctionalTest.class)
    public void generateBulkLetter() throws IOException {
        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_BULK6), "UTF-8");
        BulkRequest bulkRequest = JsonUtil.getBulkDetails(true, testData);
        Response response = funcHelper.getBulkResponse(bulkRequest, "/createBulk");
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
//        caseList.clear();
//        caseList.add(Constants.TEST_DATA_ENG_BULK1_CASE1);
//        caseList.add(Constants.TEST_DATA_ENG_BULK1_CASE2);
//        caseList.add(Constants.TEST_DATA_ENG_BULK1_CASE3);
//        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_BULK1));
//        testData = funcHelper.createIndividualCase(caseList, false, testData);
//        BulkRequest bulkRequest = JsonUtil.getBulkDetails(false, testData);
//        Response response = funcHelper.getBulkResponse(bulkRequest, "/createBulk");
//        Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        response = funcHelper.getBulkResponse(bulkRequest, "/generateBulkLetter");
    }

    @Test
    @Category(FunctionalTest.class)
    public void createSubMultiple() throws IOException {
        String locationRefNo = "24";
        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_CREATE_SUB_MULTIPLE));
        testData = testData.replace("#CASE_TYPE_ID#", "Manchester_Multiples_Dev");
        String ethosCaseRef1 = RandomStringUtils.randomNumeric(5);
        String multipleReference = locationRefNo + ethosCaseRef1;
        testData = testData.replace("#MULTIPLEREFERENCE#", multipleReference);
        ethosCaseRef1 = locationRefNo + ethosCaseRef1 + "/19";
        testData = testData.replace("#ETHOS_CASE_REFERENCE_M1#", ethosCaseRef1);
        String ethosCaseRef2 = RandomStringUtils.randomNumeric(5);
        ethosCaseRef2 = locationRefNo + ethosCaseRef2 + "/19";
        testData = testData.replace("#ETHOS_CASE_REFERENCE_M2#", ethosCaseRef2);
        BulkRequest bulkRequest = JsonUtil.getBulkDetails(false, testData);
        Response response = funcHelper.getBulkResponse(bulkRequest, "/createSubMultiple");
        String multipleValue = response.body().jsonPath().getString("data.multipleCollection[0].value.subMultipleM");
        String expectedValue = response.body().jsonPath()
                .getString("data.subMultipleCollection[0].value.subMultipleRefT");
        String expectedMultipleValue = multipleValue.split("/")[0].toString();
        String expectedMultipleValue1 = expectedValue.split("/")[0].toString();
        Assert.assertEquals(expectedMultipleValue, multipleReference);
        Assert.assertEquals(expectedMultipleValue1, multipleReference);
    }

    @Test
    @Category(FunctionalTest.class)
    public void deleteSubMultiples() throws IOException {
        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_DELETE_SUB_MULTIPLE));
        BulkRequest bulkRequest = JsonUtil.getBulkDetails(false, testData);
        Response response = funcHelper.getBulkResponse(bulkRequest, "/deleteSubMultiple");
        String expectedMultipleValue = response.body().jsonPath()
                .getString("data.multipleCollection[1].value.subMultipleM");
        Assert.assertNotNull(expectedMultipleValue);
    }

    public void verifyBulkResponse(String testData, Response response) {
        String caseTitle = JsonPath.read(testData, "$.case_details.case_data.bulkCaseTitle");
        String caseReference = JsonPath.read(testData, "$.case_details.case_data.multipleReference");
        Assert.assertEquals(caseTitle, response.body().jsonPath().getString("data.bulkCaseTitle"));
        Assert.assertEquals(caseReference, response.body().jsonPath().getString("data.multipleReference"));
    }
}
