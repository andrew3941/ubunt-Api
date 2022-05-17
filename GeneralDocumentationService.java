package com.preving.intranet.gestioncentrosapi.model.services;

import com.preving.intranet.gestioncentrosapi.model.domain.generalDocumentation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.io.IOException;

public interface GeneralDocumentationService {

    List<GeneralDocumentation> getGeneralDocumentationListByWorkCenter(int workCenterId);

    List<GeneralDocumentationTypes> getGeneralDocTypes();

    List<CertificateTypes> getCertificateTypes();

    List<TaxesTypes> getTaxesTypes();

    ResponseEntity<?> saveGeneralDocumentation(int workCenterId, GeneralDocumentation newGeneralDoc, MultipartFile[] attachedFile, HttpServletRequest request);

    ResponseEntity<?> editGeneralDoc(int workCenterId, int generalDocId,  GeneralDocumentation generalDoc, MultipartFile[] attachedFile, HttpServletRequest request);

    GeneralDocumentation getGeneralDocById(int generalDocId);

    ResponseEntity<?> downloadZipAttachment(int itemId, int type, HttpServletResponse response);

    ResponseEntity<?> deleteAttachment(int workCenterId, int attachedId) throws IOException;

//    void SendContractsAlarmDateNotification();

//    void SendInsuranceEndDayNotification();
}
