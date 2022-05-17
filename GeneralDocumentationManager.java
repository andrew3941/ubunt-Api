package com.preving.intranet.gestioncentrosapi.model.services;

import com.preving.intranet.gestioncentrosapi.model.dao.drawingByAttachments.DrawingByAttachmentsRepository;
import com.google.gson.Gson;
import com.preving.intranet.gestioncentrosapi.model.dao.generalDocument.*;
import com.preving.intranet.gestioncentrosapi.model.domain.DrawingsByAttachment;
import com.preving.intranet.gestioncentrosapi.model.domain.EmailRaw;
import com.preving.intranet.gestioncentrosapi.model.domain.User;
import com.preving.intranet.gestioncentrosapi.model.domain.generalDocumentation.*;
import com.preving.security.JwtTokenUtil;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class GeneralDocumentationManager implements GeneralDocumentationService {

    @Autowired
    private GeneralDocumentationRepository generalDocumentationRepository;

    @Autowired
    GeneralDocTypesRepository generalDocTypesRepository;

    @Autowired
    private CertificateTypesRepository certificateTypesRepository;

    @Autowired
    private TaxesTypesRepository taxesTypesRepository;

    @Autowired
    private GeneralDocByAttachmentRepository generalDocByAttachmentRepository;

    @Autowired
    private CommonService commonService;

    @Autowired
    private MailService mailService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private CommonManager commonManager;

    @Autowired
    private DrawingByAttachmentsRepository drawingByAttachmentsRepository;

    private static final int GENERAL_DOCUMENTS = 3;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public List<GeneralDocumentation> getGeneralDocumentationListByWorkCenter(int workCenterId) {

       return generalDocumentationRepository.findGeneralDocumentationsByWorkCenterIdAndDeletedIsNullOrderByCreatedDesc(workCenterId);

    }

    @Override
    public List<GeneralDocumentationTypes> getGeneralDocTypes() {
        return generalDocTypesRepository.findAll();
    }

    @Override
    public List<CertificateTypes> getCertificateTypes() {
        return certificateTypesRepository.findAll();
    }

    @Override
    public List<TaxesTypes> getTaxesTypes() {
        return taxesTypesRepository.findAll();
    }

    @Override
    public ResponseEntity<?> saveGeneralDocumentation(int workCenterId, GeneralDocumentation newGeneralDoc, MultipartFile[] attachedFile, HttpServletRequest request) {

        long userId = this.jwtTokenUtil.getUserWithRolesFromToken(request).getId();

        newGeneralDoc.setCreated(new Date());
        newGeneralDoc.getCreatedBy().setId(userId);
        newGeneralDoc.getWorkCenter().setId(workCenterId);

        try{

            GeneralDocumentation savedGeneralDoc = this.generalDocumentationRepository.save(newGeneralDoc);

            if (attachedFile.length > 0) {

                for (MultipartFile mpFile : attachedFile) {

                    GeneralDocByAttachment generalDocByAttachment = new GeneralDocByAttachment();

                    generalDocByAttachment.setGeneralDoc(savedGeneralDoc);
                    generalDocByAttachment.setAttachedUrl("Doc_Url");
                    generalDocByAttachment.setAttachedName(mpFile.getOriginalFilename());
                    generalDocByAttachment.setAttachedContentType(mpFile.getContentType());

                    this.generalDocByAttachmentRepository.save(generalDocByAttachment);

                    String url = null;

                    // Guardamos documento en el server
                    url = commonService.saveDocumentServer(workCenterId, savedGeneralDoc.getId(), mpFile, GENERAL_DOCUMENTS);

                    // Actualizamos la ruta del documento guardado
                    if (url != null) {
                        this.generalDocByAttachmentRepository.updateGeneralDocByAttachmentUrl(generalDocByAttachment.getId(), url);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> editGeneralDoc(int workCenterId, int generalDocId, GeneralDocumentation generalDoc, MultipartFile[] attachedFile, HttpServletRequest request) {

        long userId = this.jwtTokenUtil.getUserWithRolesFromToken(request).getId();

        generalDoc.setModifiedBy(new User());
        generalDoc.getModifiedBy().setId(userId);
        generalDoc.getWorkCenter().setId(workCenterId);
        String url= null;

        try {

            generalDocumentationRepository.editGeneralDoc(generalDoc);

            if (attachedFile.length > 0) {

                for (MultipartFile mpFile : attachedFile){

                    GeneralDocByAttachment newGeneralDocByAttach = new GeneralDocByAttachment();

                    newGeneralDocByAttach.setGeneralDoc(generalDoc);
                    newGeneralDocByAttach.setAttachedUrl("Doc_Url");
                    newGeneralDocByAttach.setAttachedName(mpFile.getOriginalFilename());
                    newGeneralDocByAttach.setAttachedContentType(mpFile.getContentType());

                    GeneralDocByAttachment generalDocByAttach = this.generalDocByAttachmentRepository.save(newGeneralDocByAttach);

                    // Guardamos documento en el server
                    url = commonService.saveDocumentServer(workCenterId, generalDoc.getId(), mpFile, GENERAL_DOCUMENTS);

                    // Actualizamos la ruta del documento guardado
                    if (url != null) {
                        this.generalDocByAttachmentRepository.updateGeneralDocByAttachmentUrl(generalDocByAttach.getId(), url);
                    }
                }
            }

            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void generalDocAttachmentsCombo(int workCenterId, GeneralDocumentation newGeneralDoc, List<GeneralDocByAttachment> attachments, MultipartFile[] attachedFile) throws Exception {

        String url= null;

        for (MultipartFile mpf : attachedFile) {
            GeneralDocByAttachment generalDocByAttachment = new GeneralDocByAttachment();
            generalDocByAttachment.setAttachedName(mpf.getOriginalFilename());
            generalDocByAttachment.setAttachedContentType(mpf.getContentType());
            generalDocByAttachment.setAttachedUrl("doc_url");
            generalDocByAttachment.setGeneralDoc(newGeneralDoc);

            this.generalDocByAttachmentRepository.save(generalDocByAttachment);

            url = commonManager.saveDocumentServer(workCenterId, newGeneralDoc.getId(), mpf, GENERAL_DOCUMENTS);

            if (url != null) {
                this.generalDocByAttachmentRepository.updateGeneralDocByAttachmentUrl(generalDocByAttachment.getId(), url);
            }

        }
    }

    @Override
    public GeneralDocumentation getGeneralDocById(int generalDocId) {

        GeneralDocumentation generalDoc = generalDocumentationRepository.findGeneralDocumentationById(generalDocId);

        List<GeneralDocByAttachment>  gAttachDocuments = generalDocByAttachmentRepository.findAllByGeneralDoc(generalDoc);

        generalDoc.setGeneralDocByAttachments(gAttachDocuments);

        return generalDoc;
    }

    @Override
    public ResponseEntity<?> downloadZipAttachment(int itemId, int type, HttpServletResponse response) {

        File file = null;
        byte[] content = new byte[1024];
        List<DrawingsByAttachment> dba = null;
        List<GeneralDocByAttachment> gda = null;
        FileInputStream in = null;

        try ( ZipOutputStream zos = new ZipOutputStream(response.getOutputStream()) ) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=data.zip");

            // Type 0 --> Drawigns
            // Type 1 --> GeneralDoc
            if (type == 0) {
                dba = this.drawingByAttachmentsRepository.findAllByDrawing_Id(itemId);

                for (DrawingsByAttachment drawing : dba) {
                    file = new File(drawing.getAttachedUrl());

                    ZipEntry ze= new ZipEntry(file.getName());
                    zos.putNextEntry(ze);
                    in = new FileInputStream(file);

                    int len;
                    while ((len = in.read(content)) > 0) {
                        zos.write(content, 0, len);
                    }
                }
            } else {
                gda = this.generalDocByAttachmentRepository.findAllByGeneralDocId(itemId);

                for (GeneralDocByAttachment generalDoc : gda) {
                    file = new File(generalDoc.getAttachedUrl());

                    ZipEntry ze= new ZipEntry(file.getName());
                    zos.putNextEntry(ze);
                    in = new FileInputStream(file);

                    int len;
                    while ((len = in.read(content)) > 0) {
                        zos.write(content, 0, len);
                    }
                }
            }

            in.close();
            zos.closeEntry();

            //remember close it
            zos.close();

            System.out.println("ZIP done");

        }catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>("Unknown error",HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<byte[]>(content, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> deleteAttachment(int workCenterId, int attachedId) throws IOException {

        try {

            commonService.deleteDocumentServer(workCenterId, attachedId, GENERAL_DOCUMENTS);
            generalDocByAttachmentRepository.deleteById(attachedId);

            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

//    @Override
//    @Scheduled(cron = "0 00 00 * * *") //Every day at 12am
//    public void SendContractsAlarmDateNotification(){
//        Date today = new Date();
//        System.out.println("Start of automated notification process for Contracts");
//        String contractsName = "CONTRATO ALQUILER";
//
//        Date twoWeeksPrior = new Date((today.getYear()),(today.getMonth()),(today.getDate() + 14));
//        List<GeneralDocumentation> warningContracts = generalDocumentationRepository.findByDocumentAlarmDateAndGeneralDocTypesName(twoWeeksPrior,contractsName);
//
//        warningContracts.forEach(contract -> {
//            Map<String, Object> emailData = new HashMap<>();
//
//            //Formatting the date cause i cant be asked to do it in velocity
//            String formatedDate = simpleDateFormat.format(contract.getDocumentEndDate());
//            emailData.put("fecha",formatedDate);
//            emailData.put("documento",contract.getDocumentName());
//            emailData.put("centro",contract.getWorkCenter().getName());
//
//            List<String> sendToList = new ArrayList<>();
//            sendToList.add(contract.getEmail());
//
//            mailService.sendMail(sendToList.toArray(new String[0]), emailData);
//        });
//    }
//
//    @Override
//    @Scheduled(cron = "0 00 00 * * *") //Every day at 12am
//    public void SendInsuranceEndDayNotification(){
//        Date today = new Date();
//        System.out.println("Start of automated notification process for insurances");
//        String insurancesName = "SEGURO LOCAL";
//        Date twoMonthsPrior = new Date((today.getYear()),(today.getMonth() + 2),(today.getDate()));
//        List<GeneralDocumentation> endedInsurances = generalDocumentationRepository.findByDocumentEndDateAndGeneralDocTypesName(twoMonthsPrior,insurancesName);
//
//        endedInsurances.forEach(insurance ->{
//            Map<String, Object> emailData = new HashMap<>();
//            String formatedDate = simpleDateFormat.format(insurance.getDocumentEndDate());
//            emailData.put("fecha",formatedDate);
//            emailData.put("documento",insurance.getDocumentName());
//            emailData.put("centro",insurance.getWorkCenter().getName());
//
//                List<String> sendToList = new ArrayList<>();
//                sendToList.add(insurance.getEmail());
//            mailService.sendMail(sendToList.toArray(new String[0]), emailData);
//        });
//    }

}
