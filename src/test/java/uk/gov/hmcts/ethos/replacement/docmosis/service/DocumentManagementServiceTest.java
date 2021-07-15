package uk.gov.hmcts.ethos.replacement.docmosis.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ecm.common.exceptions.DocumentManagementException;
import uk.gov.hmcts.ecm.common.idam.models.UserDetails;
import uk.gov.hmcts.ecm.common.model.ccd.UploadedDocument;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.OUTPUT_FILE_NAME;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.HelperTest;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.MultipleUtil;
import static uk.gov.hmcts.ethos.replacement.docmosis.service.DocumentManagementService.APPLICATION_DOCX_VALUE;
import static uk.gov.hmcts.ethos.replacement.docmosis.utils.ResourceLoader.successfulDocumentManagementUploadResponse;
import static uk.gov.hmcts.ethos.replacement.docmosis.utils.ResourceLoader.unsuccessfulDocumentManagementUploadResponse;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentDownloadClientApi;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;

@RunWith(SpringJUnit4ClassRunner.class)
public class DocumentManagementServiceTest {

    @Mock
    private DocumentUploadClientApi documentUploadClient;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private UserService userService;
    @Mock
    private DocumentDownloadClientApi documentDownloadClientApi;
    @InjectMocks
    private DocumentManagementService documentManagementService;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private File file;
    private String markup;
    private ResponseEntity<Resource> responseEntity;

    @Before
    public void setUp() {
        file = createTestFile();
        markup = "<a target=\"_blank\" href=\"null/documents/85d97996-22a5-40d7-882e-3a382c8ae1b4/binary\">Document</a>";
        when(authTokenGenerator.generate()).thenReturn("authString");
        responseEntity = MultipleUtil.getResponseOK();
        UserDetails userDetails = HelperTest.getUserDetails();
        when(userService.getUserDetails(anyString())).thenReturn(userDetails);
        ReflectionTestUtils.setField(documentManagementService, "ccdDMStoreBaseUrl", "http://dm-store:8080");
    }

    @Test
    public void shouldUploadToDocumentManagement() throws IOException, URISyntaxException {
        when(documentUploadClient.upload(anyString(), anyString(), anyString(), anyList(), any(), anyList()))
                .thenReturn(successfulDocumentManagementUploadResponse());
        URI documentSelfPath = documentManagementService.uploadDocument("authString", Files.readAllBytes(file.toPath()),
                OUTPUT_FILE_NAME, APPLICATION_DOCX_VALUE);
        String documentDownloadableURL = documentManagementService.generateDownloadableURL(documentSelfPath);
        assertEquals(documentManagementService.generateMarkupDocument(documentDownloadableURL), markup);
        assertNotNull(documentSelfPath);
        assertEquals("/documents/85d97996-22a5-40d7-882e-3a382c8ae1b4", documentSelfPath.getPath());
    }

    @Test
    public void uploadDocumentToDocumentManagementThrowsException() throws IOException, URISyntaxException {
        expectedException.expect(DocumentManagementException.class);
        expectedException.expectMessage("Unable to upload document document.docx to document management");
        when(documentUploadClient.upload(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(unsuccessfulDocumentManagementUploadResponse());
        documentManagementService.uploadDocument("authString", Files.readAllBytes(file.toPath()),
                OUTPUT_FILE_NAME, APPLICATION_DOCX_VALUE);
    }

    private File createTestFile() {
        Path path = Paths.get(OUTPUT_FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("Hello World !!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path.toFile();
    }

    @Test
    public void downloadFile() {
        when(documentDownloadClientApi.downloadBinary(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(responseEntity);

        UploadedDocument uploadedDocument = documentManagementService.downloadFile("authString",
                "http://dm-store:8080/documents/85d97996-22a5-40d7-882e-3a382c8ae1b4/binary");
        assertEquals( "fileName", uploadedDocument.getName());
        assertEquals( "xslx", uploadedDocument.getContentType());

        uploadedDocument = documentManagementService.downloadFile("authString",
                "documents/85d97996-22a5-40d7-882e-3a382c8ae1b4/binary");
        assertEquals( "fileName", uploadedDocument.getName());
        assertEquals( "xslx", uploadedDocument.getContentType());
    }

    @Test(expected = IllegalStateException.class)
    public void downloadFileException() {
        when(documentDownloadClientApi.downloadBinary(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_GATEWAY));
        documentManagementService.downloadFile("authString",
                "documents/85d97996-22a5-40d7-882e-3a382c8ae1b4/binary");
    }
}