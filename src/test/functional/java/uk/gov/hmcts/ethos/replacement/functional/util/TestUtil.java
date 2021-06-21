package uk.gov.hmcts.ethos.replacement.functional.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.json.JSONException;
import org.junit.Assert;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.ecm.common.model.bulk.BulkRequest;
import uk.gov.hmcts.ecm.common.model.ccd.CCDRequest;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBException;

public class TestUtil {

    private static String authToken;
    private String topLevel;
    private String childLevel;

    @Value("${test-url}")
    private String environment;

    private static String downloadedFilePath;

    public TestUtil() {
         environment = System.getProperty("VAULTNAME").replace("ethos-", "");
    }

    //End-point /generateDocument
    public void executeGenerateDocumentTest(String topLevel, String childLevel, String expectedValue)
            throws IOException, JAXBException, Docx4JException {
        executeGenerateDocumentTest(topLevel, childLevel, expectedValue, false);
    }

    public void executeGenerateDocumentTest(String topLevel, String childLevel, String expectedValue,
                                            boolean isScotland)
            throws IOException, JAXBException, Docx4JException {
        if (isScotland) {
            executeGenerateDocumentTest(topLevel, childLevel, expectedValue, true, Constants.TEST_DATA_SCOT_CASE1);
        } else {
            executeGenerateDocumentTest(topLevel, childLevel, expectedValue, false, Constants.TEST_DATA_CASE1);
        }
    }

    public void executeGenerateDocumentTest(String topLevel, String childLevel, String expectedValue,
                                            boolean isScotland, String testData)
            throws IOException, JAXBException, Docx4JException {

        this.topLevel = topLevel;
        this.childLevel = childLevel;

        loadAuthToken();

        CCDRequest ccdRequest;

        ccdRequest = getCcdRequest(topLevel, childLevel, isScotland, new File(testData));

        Response response = getResponse(ccdRequest);

        verifyDocument(topLevel, expectedValue, isScotland, ccdRequest, response);

    }

    public void executeOutstationDocumentTest(String topLevel, String childLevel, String expectedValue,
                                              boolean isScotland, String testData)
            throws IOException, JAXBException, Docx4JException {

        this.topLevel = topLevel;
        this.childLevel = childLevel;

        loadAuthToken();

        CCDRequest ccdRequest;

        ccdRequest = getCcdRequest(topLevel, childLevel, isScotland, new File(testData));

        Response response = getResponse(ccdRequest, Constants.AMEND_CASE_DETAILS_URI);

        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        String caseData = response.body().prettyPrint();
        caseData = caseData.substring(caseData.indexOf('{') + 1);
        caseData = caseData.substring(0, caseData.lastIndexOf('}'));
        caseData = caseData.replace("\"data\"", "\"case_data\"");

        String caseDetails = FileUtils.readFileToString(new File(Constants.TEST_DATA_SCOT_TEMPLATE), "UTF-8");
        caseDetails = caseDetails.replace("#CASE_DATA#", caseData);

        ccdRequest = getCcdRequest(topLevel, childLevel, isScotland, caseDetails);

        response = getResponse(ccdRequest, Constants.DOCGEN_URI);

        verifyDocument(topLevel, expectedValue, isScotland, ccdRequest, response);

    }

    public void verifyDocMosisPayload(String topLevel, String childLevel, boolean isScotland, String testDataFile)
            throws IOException, JSONException {
        this.topLevel = topLevel;
        this.childLevel = childLevel;

        if (authToken == null) {
            authToken = ResponseUtil.getAuthToken(environment);
        }
        if (!authToken.startsWith("Bearer")) {
            authToken = "Bearer " + authToken;
        }

        CCDRequest ccdRequest = getCcdRequest(topLevel, childLevel, isScotland, new File(testDataFile));

        Response response = getResponse(ccdRequest);
        String url = ResponseUtil.getUrlFromResponse(response);
        downloadedFilePath = FileUtil.downloadFileFromUrl(url, authToken);

        String actualPayload = LogUtil.getDocMosisPayload();
        actualPayload = actualPayload.substring(0, actualPayload.lastIndexOf(',')) + "}}";

        String expectedPayload = DocumentUtil.buildDocumentContent(ccdRequest.getCaseDetails(), "");

        JSONAssert.assertEquals(expectedPayload, actualPayload, JSONCompareMode.LENIENT);
    }

    //End-point /preDefaultValues
    public void executePreDefaultValuesTest(String paramName, String paramValue, boolean isScotland, String testData)
            throws IOException {
        CCDRequest ccdRequest;

        loadAuthToken();

        ccdRequest = getCcdRequest("1", "1", isScotland, new File(testData));

        Response response = getResponse(ccdRequest, Constants.PRE_DEFAULT_URI);

        String json = response.body().prettyPrint();

        verifyElementValue(json, paramName, paramValue);
    }

    //End-point /postDefaultValues
    public void executePostDefaultValuesTest(String paramName, String paramValue, boolean isScotland, String testData)
            throws IOException {
        CCDRequest ccdRequest;

        loadAuthToken();

        if (isScotland) {
            ccdRequest = getCcdRequest("1", "1", true, new File(testData));
        } else {
            ccdRequest = getCcdRequest("1", "", false, new File(testData));
        }

        Response response = getResponse(ccdRequest, Constants.POST_DEFAULT_URI);

        String json = response.body().prettyPrint();

        verifyElementValue(json, paramName, paramValue);
    }

    //End-point /createBulk
    public void executeCreateBulkTest(boolean isScotland, String testDataFilePath, List<String> caseList)
            throws IOException {

        String testData = FileUtils.readFileToString(new File(testDataFilePath), "UTF-8");

        loadAuthToken();

        testData = createIndividualCases(isScotland, caseList, testData);

        BulkRequest bulkRequest = getBulkRequest(isScotland, testData);
        Response response = getBulkResponse(bulkRequest, Constants.CREATE_BULK_URI);

        verifyBulkResponse(testData, response);

    }

    //End-point //createSubMultiple
    public void executeCreateSubMultiples(boolean isScotland, String testDataFilePath, int locationRefNo,
                                          String caseType)
            throws IOException {
        String testData = FileUtils.readFileToString(new File(testDataFilePath), "UTF-8");
        loadAuthToken();
        testData = testData.replace("#CASE_TYPE_ID#", caseType);
        String ethosCaseReference1 = getUniqueCaseReference(5);
        String multipleReference = locationRefNo + ethosCaseReference1;
        testData = testData.replace("#MULTIPLEREFERENCE#", multipleReference);
        ethosCaseReference1 = locationRefNo + ethosCaseReference1 + "/19";
        testData = testData.replace("#ETHOS_CASE_REFERENCE_M1#", ethosCaseReference1);
        String ethosCaseReference2 = getUniqueCaseReference(5);
        ethosCaseReference2 = locationRefNo + ethosCaseReference2 + "/19";
        testData = testData.replace("#ETHOS_CASE_REFERENCE_M2#", ethosCaseReference2);
        BulkRequest bulkRequest = getBulkRequest(isScotland, testData);
        Response response = getBulkResponse(bulkRequest, Constants.CREATE_SUB_MULTIPLE_URI);
        String multipleValue = response.body().jsonPath().getString(
                "data.multipleCollection[0].value.subMultipleM");
        String expectedSubMultipleValue = response.body().jsonPath()
                .getString("data.subMultipleCollection[0].value.subMultipleRefT");
        String expectedMultipleValue = multipleValue.split("/")[0].toString();
        String expectedMultipleValue1 = expectedSubMultipleValue.split("/")[0].toString();
        Assert.assertEquals(expectedMultipleValue, multipleReference);
        Assert.assertEquals(expectedMultipleValue1, multipleReference);
    }

    //End-point //updateSubMultiple
    public void executeUpdateSubMultiples(boolean isScotland, String testDataFilePath, int locationRefNo,
                                          String caseType) throws IOException {

        String testData = FileUtils.readFileToString(new File(testDataFilePath), "UTF-8");
        loadAuthToken();
        testData = testData.replace("#CASE_TYPE_ID#", caseType);
        String ethosCaseReference1 = getUniqueCaseReference(5);
        String multipleReference = locationRefNo + ethosCaseReference1;
        testData = testData.replace("#MULTIPLEREFERENCE#", multipleReference);
        ethosCaseReference1 = locationRefNo + ethosCaseReference1 + "/19";
        testData = testData.replace("#ETHOS_CASE_REFERENCE_M1#", ethosCaseReference1);
        String ethosCaseReference2 = getUniqueCaseReference(5);
        ethosCaseReference2 = locationRefNo + ethosCaseReference2 + "/19";
        testData = testData.replace("#ETHOS_CASE_REFERENCE_M2#", ethosCaseReference2);
        BulkRequest bulkRequest = getBulkRequest(isScotland, testData);
        Response response = getBulkResponse(bulkRequest, Constants.UPDATE_SUB_MULTIPLE_URI);

        String actualValue = JsonPath.read(testData, "$.case_details.case_data.subMultipleRef");
        String expectedMultipleValue = response.body().jsonPath()
                .getString("data.multipleCollection[1].value.subMultipleM");
        String expectedSubMultipleValue = response.body().jsonPath()
                .getString("data.subMultipleCollection[0].value.subMultipleRefT");
        Assert.assertEquals(expectedMultipleValue, actualValue);
        Assert.assertEquals(expectedSubMultipleValue, actualValue);
    }

    //deleteSubMultiple
    public void executeDeleteSubMultiples(boolean isScotland, String testDataFilePath, String caseType)
            throws IOException {
        Response response;
        String testData = FileUtils.readFileToString(new File(testDataFilePath), "UTF-8");
        BulkRequest bulkRequest = getBulkRequest(isScotland, testData);
        response = getBulkResponse(bulkRequest, Constants.DELETE_SUB_MULTIPLE_URI);
        String expectedMultipleValue = response.body().jsonPath()
                .getString("data.multipleCollection[1].value.subMultipleM");
        Assert.assertNotNull(expectedMultipleValue);
    }

    //End-point /searchBulk
    public void executeSearchBulkTest(boolean isScotland, String testDataFilePath, List<String> caseList)
            throws IOException {

        String testData = FileUtils.readFileToString(new File(testDataFilePath), "UTF-8");

        loadAuthToken();

        testData = createIndividualCases(isScotland, caseList, testData);

        BulkRequest bulkRequest = getBulkRequest(isScotland, testData);
        Response response = getBulkResponse(bulkRequest, Constants.CREATE_BULK_URI);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        response = getBulkResponse(bulkRequest, Constants.SEARCH_BULK_URI);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        verifyBulkResponse(testData, response);
    }

    //End-point /updateBulk
    public void executeUpdateBulkTest(boolean isScotland, String testDataFilePath, List<String> caseList)
            throws IOException {

        String testData = FileUtils.readFileToString(new File(testDataFilePath), "UTF-8");

        loadAuthToken();

        testData = createIndividualCases(isScotland, caseList, testData);

        BulkRequest bulkRequest = getBulkRequest(isScotland, testData);
        Response response = getBulkResponse(bulkRequest, Constants.CREATE_BULK_URI);

        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        response = getBulkResponse(bulkRequest, Constants.SEARCH_BULK_URI);

        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        response = getBulkResponse(bulkRequest, Constants.UPDATE_BULK_URI);

        verifyBulkResponse(testData, response);
    }

    //End-point //updateBulkCase
    public void executeUpdateBulkCaseTest(boolean isScotland, String testDataFilePath, List<String> caseList)
            throws IOException {

        String testData = FileUtils.readFileToString(new File(testDataFilePath), "UTF-8");

        loadAuthToken();

        testData = createIndividualCases(isScotland, caseList, testData);

        BulkRequest bulkRequest = getBulkRequest(isScotland, testData);
        Response response = getBulkResponse(bulkRequest, Constants.CREATE_BULK_URI);

        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        response = getBulkResponse(bulkRequest, Constants.UPDATE_BULK_CASE_URI);

        verifyBulkResponse(testData, response);
    }

    //End-point /generateBulkLetter
    public void executeGenerateBulkLetterTest(String topLevel, String childLevel, String expectedValue,
                                              boolean isScotland, String testDataFilePath,
                                              List<String> caseList) throws Exception {

        String testData = FileUtils.readFileToString(new File(testDataFilePath), "UTF-8");

        loadAuthToken();

        testData = createIndividualCases(isScotland, caseList, testData);

        BulkRequest bulkRequest = getBulkRequest(isScotland, testData);
        Response response = getBulkResponse(bulkRequest, Constants.CREATE_BULK_URI);

        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        response = getBulkResponse(bulkRequest, Constants.GENERATE_BULK_LETTER_URI);

        verifyDocument(topLevel, expectedValue, isScotland, null, response);

    }

    //General methods
    public String getEnvironment() {
        return this.environment;
    }

    public void deleteTempFile() throws IOException {
        if (downloadedFilePath != null) {
            FileUtils.forceDelete(new File(downloadedFilePath));
            downloadedFilePath = null;
        }
    }

    public Response getResponse(CCDRequest ccdRequest) throws IOException {
        return getResponse(ccdRequest, Constants.DOCGEN_URI);
    }

    public Response getResponse(CCDRequest ccdRequest, String uri) throws IOException {
        return getResponse(ccdRequest, uri, 200);
    }

    public Response getResponse(CCDRequest ccdRequest, String uri, int expectedStatusCode) throws IOException {

        RestAssured.config = RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames());
        RequestSpecification httpRequest = SerenityRest.given().relaxedHTTPSValidation().config(RestAssured.config);
        httpRequest.header("Authorization", authToken);
        httpRequest.header("Content-Type", ContentType.JSON);
        httpRequest.body(ccdRequest);
        Response response = httpRequest.post(ResponseUtil.getProperty(environment.toLowerCase()) + uri);

        Assert.assertEquals(expectedStatusCode, response.getStatusCode());
        return response;
    }

    public Response getBulkResponse(BulkRequest bulkRequest, String uri) throws IOException {
        return getBulkResponse(bulkRequest, uri, 200);
    }

    public Response getBulkResponse(BulkRequest bulkRequest, String uri, int expectedStatusCode)
            throws IOException {

        RestAssured.config = RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames());
        RequestSpecification httpRequest = SerenityRest.given().relaxedHTTPSValidation()
                .config(RestAssured.config);
        httpRequest.header("Authorization", authToken);
        httpRequest.header("Content-Type", ContentType.JSON);
        httpRequest.body(bulkRequest);
        Response response = httpRequest.post(ResponseUtil.getProperty(environment.toLowerCase()) + uri);

        Assert.assertEquals(expectedStatusCode, response.getStatusCode());
        return response;
    }

    public CCDRequest getCcdRequest(String topLevel, String childLevel, boolean isScotland, File testDataFile)
            throws IOException {
        String payLoad = FileUtils.readFileToString(testDataFile, "UTF-8");

        return getCcdRequest(topLevel, childLevel, isScotland, payLoad);
    }

    public CCDRequest getCcdRequest(String topLevel, String childLevel, boolean isScotland, String testData)
            throws IOException {
        return JsonUtil.getCaseDetails(testData, topLevel, childLevel, isScotland);
    }

    public BulkRequest getBulkRequest(boolean isScotland, String testData) throws IOException {
        return JsonUtil.getBulkDetails(isScotland, testData);
    }

    public void setAuthToken(String authToken) {
        TestUtil.authToken = authToken;
    }

    public String loadAuthToken() throws IOException {
        if (authToken == null) {
            authToken = ResponseUtil.getAuthToken(environment);
        }
        if (!authToken.startsWith("Bearer")) {
            authToken = "Bearer " + authToken;
        }

        return authToken;
    }

    public void verifyBulkResponse(String testData, Response response) {

        String caseTitle = JsonPath.read(testData, "$.case_details.case_data.bulkCaseTitle");
        String caseReference = JsonPath.read(testData, "$.case_details.case_data.multipleReference");

        Assert.assertEquals(caseTitle, response.body().jsonPath().getString("data.bulkCaseTitle"));
        Assert.assertEquals(caseReference, response.body().jsonPath().getString("data.multipleReference"));

    }

    //Private methods
    private String createIndividualCases(boolean isScotland, List<String> caseList, String testData)
            throws IOException {
        int count = 1;
        String ethosCaseReference = "";
        for (String caseDataFilePath : caseList) {
            ethosCaseReference = getUniqueCaseReference(10);
            String caseDetails = FileUtils.readFileToString(new File(caseDataFilePath), "UTF-8");
            caseDetails = caseDetails.replace("#ETHOS-CASE-REFERENCE#", ethosCaseReference);
            CCDRequest ccdRequest = getCcdRequest("1", "", isScotland, caseDetails);
            Response response = getResponse(ccdRequest, Constants.CREATE_CASE_URI);

            Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());

            //testData = testData.replace("#ETHOS-CASE-REFERENCE" + count + "#", ethosCaseReference);
            count++;
        }

        return testData.replace("#ETHOS-CASE-REFERENCE" + count + "#", ethosCaseReference);
    }

    private void verifyElementValue(String json, String paramName, String paramValue) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode rootNode = objectMapper.readTree(json);
        JsonNode childNode = rootNode.findValue(paramName);

        if (childNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> elements = childNode.fields();
            for (; elements.hasNext(); ) {
                Map.Entry<String, JsonNode> element = elements.next();
                Assert.assertTrue(paramValue.contains(element.getValue().textValue()));
            }
        } else {
            Assert.assertEquals(paramValue, childNode.textValue());
        }
    }

    private void verifyDocument(String topLevel, String expectedValue, boolean isScotland,
                                CCDRequest ccdRequest,
                                Response response) throws IOException,
            JAXBException, Docx4JException {
        Pattern pattern = Pattern.compile(Constants.URL_PATTERN);
        String url = ResponseUtil.getUrlFromResponse(response);

        Assert.assertNotNull("Null URL returned", url);
        Assert.assertTrue(pattern.matcher(url).matches());

        downloadedFilePath = FileUtil.downloadFileFromUrl(url, authToken);

        existsInDocument(expectedValue, new File(downloadedFilePath), isScotland);
    }

    private void existsInDocument(String expectedValue, File actualDocument, boolean isScotland)
            throws JAXBException, Docx4JException {

        String docVersion;

        if (isScotland) {
            if (StringUtils.isEmpty(this.topLevel)) {
                docVersion = "";
            } else {
                if (StringUtils.isEmpty(this.childLevel)) {
                    docVersion = "Scot_" + this.topLevel;
                } else {
                    docVersion = "Scot_" + this.topLevel + "_" + this.childLevel;
                }
            }
        } else {
            if (StringUtils.isEmpty(this.childLevel)) {
                docVersion = "";
            } else {
                docVersion = this.topLevel + "_" + this.childLevel;
            }
        }

        List<String> actualTextElements = Docx4jUtil.getAllTextElementsFromDocument(actualDocument);

        boolean hasMatched = false;
        for (String key : actualTextElements) {
            Pattern pattern = Pattern.compile(".*" + expectedValue + ".*");
            Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                hasMatched = true;
                break;
            }
        }

        Assert.assertTrue("Expected value \"" + expectedValue
                + "\" doesn't exist in Document with version: "
                + docVersion + " \n", hasMatched);
    }

    public String getUniqueCaseReference(int digit) {
        return RandomStringUtils.randomNumeric(digit);
    }

}
