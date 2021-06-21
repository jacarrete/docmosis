package uk.gov.hmcts.ethos.replacement.functional.controller;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ecm.common.model.bulk.BulkRequest;
import uk.gov.hmcts.ecm.common.model.ccd.CCDRequest;
import uk.gov.hmcts.ethos.replacement.functional.util.JsonUtil;

import java.io.File;
import java.io.IOException;

import static io.restassured.RestAssured.baseURI;
import static uk.gov.hmcts.ethos.replacement.functional.util.ResponseUtil.getProperty;

@TestPropertySource(locations = "classpath:config.properties")
public class FuncHelper {

    public String environment = "demo";

    private String AUTH_TOKEN;

    public FuncHelper() throws IOException {
        baseURI = getProperty(environment + ".docmosis.api.url");
        FunctionalCCDReq functionalCcdReq = new FunctionalCCDReq();
    }


    public Integer createIndividualCase(CCDRequest ccdRequest) throws IOException {
        loadAuthToken();
            Response response = RestAssured.given()
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
                .body(ccdRequest)
                .post("/createCase");
        return response.getStatusCode();
    }

    public CCDRequest getCcdRequest(String topLevel, String childLevel, boolean isScotland, String testData)
            throws IOException {
        return JsonUtil.getCaseDetails(testData, topLevel, childLevel, isScotland);
    }

    public CCDRequest getCcdRequest(String topLevel, String childLevel, boolean isScotland, File testDataFile)
            throws IOException {
        String payload = FileUtils.readFileToString(testDataFile, "UTF-8");
        return getCcdRequest(topLevel, childLevel, isScotland, payload);
    }

    public Response getBulkResponse(BulkRequest bulkRequest, String uri) throws IOException {
        loadAuthToken();
        return RestAssured.given()
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
                .body(bulkRequest)
                .post(uri);
    }

    public Response getCcdResponse(CCDRequest ccdRequest, String uri) throws IOException {
        loadAuthToken();
        return RestAssured.given()
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.JSON)
                .body(ccdRequest)
                .post(uri);
    }

    public String loadAuthToken() throws IOException {
        if(AUTH_TOKEN == null) {
            AUTH_TOKEN = getAuthToken();
        }
        if(!AUTH_TOKEN.startsWith("Bearer")) {
            AUTH_TOKEN = "Bearer " + AUTH_TOKEN;
        }
        return AUTH_TOKEN;
    }

    public String getAuthToken() throws IOException {
        RestAssured.config = RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames());
        RequestSpecification httpRequest = RestAssured.given();
        httpRequest.header("Accept", "*/*");
        httpRequest.header("Content-Type", "application/x-www-form-urlencoded");
        httpRequest.formParam("username", getProperty(environment + ".ccd.username"));
        httpRequest.formParam("password",  getProperty(environment + ".ccd.password"));
        Response response = httpRequest.post(getProperty(environment + ".idam.auth.url"));
        Assertions.assertEquals(HttpStatus.SC_OK, response.statusCode());
        return response.body().jsonPath().getString("access_token");
    }
}
