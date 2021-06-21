package uk.gov.hmcts.ethos.replacement.functional.controller;

import com.jayway.jsonpath.JsonPath;
import io.restassured.response.Response;
import net.serenitybdd.junit.runners.SerenityRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.groovy.json.internal.JsonFastParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ecm.common.model.bulk.BulkRequest;
import uk.gov.hmcts.ethos.replacement.functional.FunctionalTest;
import uk.gov.hmcts.ethos.replacement.functional.util.Constants;
import uk.gov.hmcts.ethos.replacement.functional.util.JsonUtil;

import java.io.File;
import java.io.IOException;

@RunWith(SerenityRunner.class)
@TestPropertySource(locations = "classpath:application.properties")
public class ExcelActionsControllerFunctional {
    private FuncHelper funcHelper;
    private String AUTH_TOKEN = "";

    @Before
    public void setUp() throws IOException {
        funcHelper = new FuncHelper();
    }

    @Test
    @Category(FunctionalTest.class)
    public void updateSubMultiple() throws IOException {
        String locationRef = "24";
        String testData = FileUtils.readFileToString(new File(Constants.TEST_DATA_ENG_UPDATE_SUB_MULTIPLE), "UTF-8");
        testData = testData.replace("#CASE_TYPE_ID#", "Manchester_Multiples_Dev");
        String ethosCaseRef1 = RandomStringUtils.randomNumeric(5);
        String multipleReference = locationRef + ethosCaseRef1;
        testData = testData.replace("#MULTIPLEREFERENCE#", multipleReference);
        ethosCaseRef1 = locationRef + ethosCaseRef1 + "/19";
        testData = testData.replace("#ETHOS_CASE_REFERENCE_M1#", ethosCaseRef1);
        String ethosCaseRef2 = RandomStringUtils.randomNumeric(5);
        ethosCaseRef2 = locationRef + ethosCaseRef2 + "/19";
        testData = testData.replace("#ETHOS_CASE_REFERENCE_M2#", ethosCaseRef2);
        BulkRequest bulkRequest = JsonUtil.getBulkDetails(false, testData);
        Response response = funcHelper.getBulkResponse(bulkRequest, "/updateSubMultiple");
        System.out.println(response.getStatusCode());
        String actualValue = JsonPath.read(testData, "$.case_details.case_data.subMultipleRef");
//        String expectedMultipleValue = response.body().jsonPath()
//                .getString("data.multipleCollection[1].value.subMultipleM");
//        String expectedSubMultipleValue = response.body().jsonPath()
//                .getString("data.subMultipleCollection[0].value.subMultipleRefT");
//        Assert.assertEquals(expectedMultipleValue, actualValue);
//        Assert.assertEquals(expectedSubMultipleValue, actualValue);
    }
}
