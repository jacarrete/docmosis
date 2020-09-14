package uk.gov.hmcts.ethos.replacement.docmosis.service.excel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleData;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleDetails;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.FilterExcelType;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.MultiplesHelper;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import static uk.gov.hmcts.ecm.common.model.helper.Constants.*;

@Slf4j
@Service("multipleUpdateService")
public class MultipleUpdateService {

    private final ExcelReadingService excelReadingService;
    private final MultipleBatchUpdate1Service multipleBatchUpdate1Service;
    private final MultipleBatchUpdate2Service multipleBatchUpdate2Service;
    private final MultipleBatchUpdate3Service multipleBatchUpdate3Service;

    @Autowired
    public MultipleUpdateService(ExcelReadingService excelReadingService,
                                 MultipleBatchUpdate1Service multipleBatchUpdate1Service,
                                 MultipleBatchUpdate2Service multipleBatchUpdate2Service,
                                 MultipleBatchUpdate3Service multipleBatchUpdate3Service) {
        this.excelReadingService = excelReadingService;
        this.multipleBatchUpdate1Service = multipleBatchUpdate1Service;
        this.multipleBatchUpdate2Service = multipleBatchUpdate2Service;
        this.multipleBatchUpdate3Service = multipleBatchUpdate3Service;
    }

    public void bulkUpdateLogic(String userToken, MultipleDetails multipleDetails, List<String> errors) {

        log.info("Read excel to update logic");

        TreeMap<String, Object> multipleObjects =
                excelReadingService.readExcel(
                        userToken,
                        MultiplesHelper.getExcelBinaryUrl(multipleDetails.getCaseData()),
                        errors,
                        multipleDetails.getCaseData(),
                        FilterExcelType.FLAGS);

        log.info("MultipleObjectsKeySet: " + multipleObjects.keySet());
        log.info("MultipleObjectsValues: " + multipleObjects.values());

        log.info("Add state to the multiple");

        addStateToMultiple(multipleDetails.getCaseData(), multipleObjects.keySet());

        log.info("Logic depending on batch update type");

        batchUpdateLogic(userToken, multipleDetails, errors, multipleObjects);

        log.info("Resetting mid fields");

        MultiplesHelper.resetMidFields(multipleDetails.getCaseData());

    }

    private void addStateToMultiple(MultipleData multipleData, Set<String> caseFiltered) {

        if (!caseFiltered.isEmpty()) {

            multipleData.setState(UPDATING_STATE);

        } else {

            multipleData.setState(OPEN_STATE);

        }
    }

    private void batchUpdateLogic(String userToken, MultipleDetails multipleDetails,
                                  List<String> errors, TreeMap<String, Object> multipleObjects) {

        String batchUpdateType = multipleDetails.getCaseData().getBatchUpdateType();

        if (batchUpdateType.equals(BATCH_UPDATE_TYPE_1)) {

            multipleBatchUpdate1Service.batchUpdate1Logic(
                    userToken,
                    multipleDetails,
                    errors,
                    multipleObjects);


        } else if (batchUpdateType.equals(BATCH_UPDATE_TYPE_2)) {

            multipleBatchUpdate2Service.batchUpdate2Logic(
                    userToken,
                    multipleDetails,
                    errors,
                    multipleObjects);

        } else {

            multipleBatchUpdate3Service.batchUpdate3Logic(
                    userToken,
                    multipleDetails,
                    errors,
                    multipleObjects);

        }

    }

}