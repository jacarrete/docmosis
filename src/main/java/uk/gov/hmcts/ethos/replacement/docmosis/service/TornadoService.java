package uk.gov.hmcts.ethos.replacement.docmosis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.idam.models.UserDetails;
import uk.gov.hmcts.ecm.common.model.bulk.BulkData;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.DocumentInfo;
import uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.ecm.common.model.listing.ListingData;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleData;
import uk.gov.hmcts.ethos.replacement.docmosis.config.TornadoConfiguration;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.*;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TreeMap;

import static java.net.HttpURLConnection.HTTP_OK;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.OUTPUT_FILE_NAME;
import static uk.gov.hmcts.ethos.replacement.docmosis.service.DocumentManagementService.APPLICATION_DOCX_VALUE;

@Slf4j
@Service("tornadoService")
@RequiredArgsConstructor
public class TornadoService {

    private final TornadoConfiguration tornadoConfiguration;
    private final DocumentManagementService documentManagementService;
    @Value("${ccd_gateway_base_url}")
    private String ccdGatewayBaseUrl;
    private final UserService userService;

    DocumentInfo documentGeneration(String authToken, CaseData caseData) throws IOException {
        HttpURLConnection conn = null;
        DocumentInfo documentInfo = new DocumentInfo();
        try {
            conn = createConnection();
            log.info("Connected");
            UserDetails userDetails = userService.getUserDetails(authToken);
            buildInstruction(conn, caseData, userDetails);
            int status = conn.getResponseCode();
            if (status == HTTP_OK) {
                log.info("HTTP_OK");
                String documentName = Helper.getDocumentName(caseData);
                documentInfo = createDocument(authToken, conn, documentName);
            } else {
                log.error("Our call failed: status = " + status);
                log.error("message:" + conn.getResponseMessage());
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String msg;
                while ((msg = errorReader.readLine()) != null) {
                    log.error(msg);
                }
            }
        } catch (ConnectException e) {
            log.error("Unable to connect to Docmosis: " + e.getMessage());
            log.error("If you have a proxy, you will need the Proxy aware example code.");
            System.exit(2);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return documentInfo;
    }

    private HttpURLConnection createConnection() throws IOException {
        String tornadoURL = tornadoConfiguration.getUrl();
        log.info("TORNADO URL: " + tornadoURL);
        HttpURLConnection conn = (HttpURLConnection) new URL(tornadoURL).openConnection();
        log.info("Connecting [directly] to " + tornadoURL);
        conn.setRequestMethod("POST");
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.connect();
        return conn;
    }

    private void buildInstruction(HttpURLConnection conn, CaseData caseData, UserDetails userDetails) throws IOException {
        StringBuilder sb = Helper.buildDocumentContent(caseData, tornadoConfiguration.getAccessKey(), userDetails);
        //log.info("Sending request: " + sb.toString());
        // send the instruction in UTF-8 encoding so that most character sets are available
        OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
        os.write(sb.toString());
        os.flush();
    }

    private byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    private DocumentInfo createDocument(String authToken, HttpURLConnection conn, String documentName) throws IOException {
        URI documentSelfPath = documentManagementService.uploadDocument(authToken, getBytesFromInputStream(conn.getInputStream()),
                OUTPUT_FILE_NAME, APPLICATION_DOCX_VALUE);
        log.info("URI documentSelfPath uploaded and created: " + documentSelfPath.toString());
        return generateDocumentInfo(documentName,
                documentSelfPath,
                documentManagementService.generateMarkupDocument(documentManagementService.generateDownloadableURL(documentSelfPath)));
    }

    private DocumentInfo generateDocumentInfo(String documentName, URI documentSelfPath, String markupURL) {
        log.info("MarkupURL: "+markupURL);
        return DocumentInfo.builder()
                .type(SignificantItemType.DOCUMENT.name())
                .description(documentName)
                .markUp(markupURL)
                .url(ccdGatewayBaseUrl + documentSelfPath.getRawPath() + "/binary")
                .build();
    }

    DocumentInfo listingGeneration(String authToken, ListingData listingData, String caseType) throws IOException {
        HttpURLConnection conn = null;
        DocumentInfo documentInfo = new DocumentInfo();
        try {
            conn = createConnection();
            log.info("Connected");
            UserDetails userDetails = userService.getUserDetails(authToken);
            String documentName = ListingHelper.getListingDocName(listingData);
            buildListingInstruction(conn, listingData, documentName, userDetails, caseType);
            int status = conn.getResponseCode();
            if (status == HTTP_OK) {
                documentInfo = createDocument(authToken, conn, documentName);
            } else {
                log.error("message:" + conn.getResponseMessage());
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String msg;
                while ((msg = errorReader.readLine()) != null) {
                    log.error(msg);
                }
            }
        } catch (ConnectException e) {
            log.error("Unable to connect to Docmosis: " + e.getMessage());
            log.error("If you have a proxy, you will need the Proxy aware example code.");
            System.exit(2);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return documentInfo;
    }

    private void buildListingInstruction(HttpURLConnection conn, ListingData listingData, String documentName, UserDetails userDetails, String caseType) throws IOException {
        StringBuilder sb = ListingHelper.buildListingDocumentContent(listingData, tornadoConfiguration.getAccessKey(), documentName, userDetails, caseType);
        //log.info("Sending request: " + sb.toString());
        // send the instruction in UTF-8 encoding so that most character sets are available
        OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
        os.write(sb.toString());
        os.flush();
    }

    DocumentInfo scheduleGeneration(String authToken, BulkData bulkData) throws IOException {
        HttpURLConnection conn = null;
        DocumentInfo documentInfo = new DocumentInfo();
        try {
            conn = createConnection();
            log.info("Connected");
            buildScheduleInstruction(conn, bulkData);
            int status = conn.getResponseCode();
            if (status == HTTP_OK) {
                log.info("HTTP_OK");
                documentInfo = createDocument(authToken, conn, BulkHelper.getScheduleDocName(bulkData.getScheduleDocName()));
            } else {
                log.error("message:" + conn.getResponseMessage());
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String msg;
                while ((msg = errorReader.readLine()) != null) {
                    log.error(msg);
                }
            }
        } catch (ConnectException e) {
            log.error("Unable to connect to Docmosis: " + e.getMessage());
            log.error("If you have a proxy, you will need the Proxy aware example code.");
            System.exit(2);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return documentInfo;
    }

    private void buildScheduleInstruction(HttpURLConnection conn, BulkData bulkData) throws IOException {
        StringBuilder sb = BulkHelper.buildScheduleDocumentContent(bulkData, tornadoConfiguration.getAccessKey());
        //log.info("Sending request: " + sb.toString());
        // send the instruction in UTF-8 encoding so that most character sets are available
        OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
        os.write(sb.toString());
        os.flush();
    }

    public DocumentInfo scheduleMultipleGeneration(String authToken, MultipleData multipleData, TreeMap<String, Object> multipleObjectsFiltered,
                                            List<SubmitEvent> submitEventList) throws IOException {
        HttpURLConnection conn = null;
        DocumentInfo documentInfo = new DocumentInfo();
        try {
            conn = createConnection();
            log.info("Connected");
            buildScheduleMultipleInstruction(conn, multipleData, multipleObjectsFiltered, submitEventList);
            int status = conn.getResponseCode();
            if (status == HTTP_OK) {
                log.info("HTTP_OK");
                documentInfo = createDocument(authToken, conn, BulkHelper.getScheduleDocName(multipleData.getScheduleDocName()));
            } else {
                log.error("message:" + conn.getResponseMessage());
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String msg;
                while ((msg = errorReader.readLine()) != null) {
                    log.error(msg);
                }
            }
        } catch (ConnectException e) {
            log.error("Unable to connect to Docmosis: " + e.getMessage());
            log.error("If you have a proxy, you will need the Proxy aware example code.");
            System.exit(2);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return documentInfo;
    }

    private void buildScheduleMultipleInstruction(HttpURLConnection conn, MultipleData multipleData,
                                                  TreeMap<String, Object> multipleObjectsFiltered,
                                                  List<SubmitEvent> submitEventList) throws IOException {
        StringBuilder sb = MultiplesScheduleHelper.buildScheduleDocumentContent(multipleData,
                tornadoConfiguration.getAccessKey(), multipleObjectsFiltered, submitEventList);
        //log.info("Sending request: " + sb.toString());
        OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
        os.write(sb.toString());
        os.flush();
    }

}
