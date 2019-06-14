package uk.gov.hmcts.ethos.replacement.docmosis.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ethos.replacement.docmosis.exceptions.CaseCreationException;
import uk.gov.hmcts.ethos.replacement.docmosis.model.helper.DefaultValues;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service("defaultValuesReaderService")
public class DefaultValuesReaderService {

    public static final String PRE_DEFAULT_XLSX_FILE_PATH = "preDefaultValues.xlsx";
    public static final String POST_DEFAULT_XLSX_FILE_PATH = "postDefaultValues.xlsx";
    private static final String MESSAGE = "Failed to add default values: ";
    public static final String MANCHESTER_CASE_TYPE_ID = "EmpTrib_MVP_1.0_Manc";
    static final String GLASGOW_CASE_TYPE_ID = "EmpTrib_MVP_1.0_Glas";

    public DefaultValues getDefaultValues(String filePath, String caseTypeId) {
        List<String> values = new ArrayList<>();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            workbook.getSheetAt(0).forEach(row -> {
                if (row.getRowNum() != 0) {
                    row.forEach(cell -> {
                        if (cell.getColumnIndex() == 1) {
                            if (cell.getCellType() == CellType.STRING) {
                                values.add(cell.getStringCellValue());
                            }
                            else if(cell.getCellType() == CellType.NUMERIC) {
                                values.add(NumberToTextConverter.toText(cell.getNumericCellValue()));
                            }
                        }
                    });
                }
            });
        } catch (Exception ex) {
            throw new CaseCreationException(MESSAGE + ex.getMessage());
        }
        if (filePath.equals(PRE_DEFAULT_XLSX_FILE_PATH))
            return populatePreDefaultValues(values);
        else {
            return populatePostDefaultValues(values, caseTypeId);
        }
    }

    private DefaultValues populatePreDefaultValues(List<String> values) {
        return DefaultValues.builder()
                .claimantTypeOfClaimant(values.get(0))
                .build();
    }

    private DefaultValues populatePostDefaultValues(List<String> values, String caseTypeId) {
        if (caseTypeId.equals(MANCHESTER_CASE_TYPE_ID)) {
            String tribunalCorrespondenceAddress = values.get(1);
            String tribunalCorrespondenceTelephone = values.get(2);
            return DefaultValues.builder()
                    .positionType(values.get(0))
                    .tribunalCorrespondenceAddress(tribunalCorrespondenceAddress)
                    .tribunalCorrespondenceTelephone(tribunalCorrespondenceTelephone)
                    .tribunalCorrespondenceFax(values.get(3))
                    .tribunalCorrespondenceDX(values.get(4))
                    .tribunalCorrespondenceEmail(values.get(5))
                    .build();
        }
        return DefaultValues.builder()
                .positionType(values.get(0))
                .tribunalCorrespondenceAddress(values.get(6))
                .tribunalCorrespondenceTelephone(values.get(7))
                .tribunalCorrespondenceFax(values.get(8))
                .tribunalCorrespondenceDX(values.get(9))
                .tribunalCorrespondenceEmail(values.get(10))
                .build();
    }

}
