package com.preving.intranet.gestioncentrosapi.model.services;

import com.preving.intranet.gestioncentrosapi.model.dao.drawing.DrawingRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.drawingByAttachments.DrawingByAttachmentsRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.entities.EntitiesRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.generalDocument.GeneralDocByAttachmentRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.maintenance.MaintenanceByAttachmentRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.maintenance.MaintenanceCustomRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.provinces.ProvincesRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.vendor.ProviderCustomRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.vendor.ProviderRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.vendor.ProvidersCommonDetailsRepository;
import com.preving.intranet.gestioncentrosapi.model.domain.Drawing;
import com.preving.intranet.gestioncentrosapi.model.domain.DrawingsByAttachment;
import com.preving.intranet.gestioncentrosapi.model.domain.Entity;
import com.preving.intranet.gestioncentrosapi.model.domain.Province;
import com.preving.intranet.gestioncentrosapi.model.domain.generalDocumentation.GeneralDocByAttachment;
import com.preving.intranet.gestioncentrosapi.model.domain.maintenance.MaintenanceByAttachment;
import com.preving.intranet.gestioncentrosapi.model.domain.vendors.Provider;
import com.preving.intranet.gestioncentrosapi.model.domain.vendors.ProvidersCommonDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class CommonManager implements CommonService {

    @Autowired
    private ProvincesRepository provincesRepository;

    @Autowired
    private EntitiesRepository entitiesRepository;

    @Autowired
    private DrawingRepository drawingRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ProviderCustomRepository providerCustomRepository;


    @Autowired
    private ProvidersCommonDetailsRepository providersCommonDetailsRepository;
    @Autowired
    private GeneralDocByAttachmentRepository generalDocByAttachmentRepository;

    @Autowired
    private DrawingByAttachmentsRepository drawingByAttachmentsRepository;
    @Autowired
    private MaintenanceByAttachmentRepository maintenanceByAttachmentRepository;


    @Value("${url-documentos-planos}")
    private String urlDrawingDocuments;

    @Value("${url-documentos-proveedor}")
    private String urlProviderDocuments;

    @Value("${url-documentos-generalDocument}")
    private String urlGeneralDocuments;

    @Value("${url-documentos-mantenimiento}")
    private String urlNewMaintenance;

    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final String CONTENT_TYPE_ZIP = "application/x-zip-compressed";
    private static final String CONTENT_TYPE_DOC = "application/msword";
    private static final String CONTENT_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String CONTENT_TYPE_ODT = "application/vnd.oasis.opendocument.text";
    private static final String CONTENT_TYPE_XLS = "application/vnd.ms-excel";
     private static final String CONTENT_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CONTENT_TYPE_ODS = "application/vnd.oasis.opendocument.spreadsheet";
    private static final String CONTENT_TYPE_JPG = "image/jpg";
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";
    private static final String CONTENT_TYPE_PNG = "image/png";
    private static final String CONTENT_TYPE_RAR= "application/x-rar-compressed";

    private static final int DRAWINGS = 1;
    private static final int PROVIDERS = 2;
    private static final int GENERAL_DOCUMENTS = 3;
    static final int MAINTENANCE = 4;


    @Override
    public List<Province> findAllProvinces(){
        return provincesRepository.findAllByOrderByName();

    }

    @Override
    public List<Entity> findAllEntities() {
        return entitiesRepository.findAllByOrderByName();
    }

    private String contentType(String contentType){
        String typeFile=null;
        switch (contentType){
            case CONTENT_TYPE_PDF:
                typeFile=".pdf";
                break;
            case  CONTENT_TYPE_ZIP:
                typeFile=".zip";
                break;
            case  CONTENT_TYPE_DOC:
                typeFile=".doc";
                break;
            case CONTENT_TYPE_DOCX:
                typeFile=".docx";
                break;
            case CONTENT_TYPE_ODT:
                typeFile=".odt";
                break;
            case CONTENT_TYPE_XLS:
                typeFile=".xls";
                break;
            case CONTENT_TYPE_XLSX:
                typeFile=".xlsx";
                break;
            case CONTENT_TYPE_ODS:
                typeFile=".ods";
                break;
            case CONTENT_TYPE_JPG:
                typeFile=".jpg";
                break;
            case CONTENT_TYPE_JPEG:
                typeFile=".jpg";
                break;
            case CONTENT_TYPE_PNG:
                typeFile=".png";
                break;
            case CONTENT_TYPE_RAR:
                typeFile=".rar";
                break;

        }
        return typeFile;

    }


    public String saveDocumentServer(int workCenterId, int itemId, MultipartFile attachedFile, int tipoDoc) throws IOException {

        String path= null;
        String url = null;

        if (tipoDoc == DRAWINGS) {

            path = urlDrawingDocuments + "/" + workCenterId + "/Drawings/" +itemId;
            url = urlDrawingDocuments + "/"  + workCenterId +  "/Drawings/" +itemId+ "/" + attachedFile.getOriginalFilename();

        } else if(tipoDoc ==PROVIDERS) {
            path = urlProviderDocuments + "/" + workCenterId + "/proveedores/" + itemId;
            url = urlProviderDocuments + "/" + workCenterId + "/proveedores/" + itemId +"/" + attachedFile.getOriginalFilename();

        }else if(tipoDoc ==GENERAL_DOCUMENTS){
            path = urlGeneralDocuments + "/" + workCenterId + "/generalDocuments/" + itemId;
            url = urlGeneralDocuments + "/" + workCenterId + "/generalDocuments/" + itemId +"/" + attachedFile.getOriginalFilename();

        }else  if (tipoDoc == MAINTENANCE){
            path = urlNewMaintenance + "/" + workCenterId + "/maintenance/" + itemId;
            url = urlNewMaintenance + "/" + workCenterId + "/maintenance/" + itemId +"/" + attachedFile.getOriginalFilename();
        }

        File file = new File(url);
        if(file.exists()){
            file.delete();
        }
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
            if (!file.exists()) {
                file.createNewFile();
            }

        } else {
            if (!file.exists()) {
                file.createNewFile();
            }
        }
        attachedFile.transferTo(file);
        return url;
    }

    public boolean deleteDocumentServer(int workCenterId, int itemId, int tipoDoc) throws IOException {

        String docUrl = "";
        Boolean borrado = null;

        if (tipoDoc == DRAWINGS) {
            // Obtenemos la URL del plano para borrarlo del servidor
             DrawingsByAttachment drawingsByAttachment = drawingByAttachmentsRepository.findById(itemId);
             docUrl = drawingsByAttachment.getAttachedUrl();

        } else if (tipoDoc == PROVIDERS){
            // Obtenemos la URL del documento del proveedor para borrarlo del servidor
            ProvidersCommonDetails pCommonDetails =  providersCommonDetailsRepository.findProvidersCommonDetailsById(itemId);
            docUrl = pCommonDetails.getDocUrl();

        } else if (tipoDoc == GENERAL_DOCUMENTS){
            //Obtain General Document URL
            GeneralDocByAttachment gDocAttachment = generalDocByAttachmentRepository.findById(itemId);
            docUrl = gDocAttachment.getAttachedUrl();
        }else if (tipoDoc == MAINTENANCE){
            //Obtain Manitenance URL
            MaintenanceByAttachment maintenanceByAttachment = maintenanceByAttachmentRepository.findById(itemId);
            docUrl = maintenanceByAttachment.getDocumentUrl();
        }

        if (docUrl != null){

            File file = new File(docUrl);

            if (file.exists()) {
                file.delete();
                 borrado = true;
            } else {
                borrado = false;
            }

        } else {
            borrado = true;
        }

        return borrado;

    }

    @Override
    public boolean deleteMaintenanceServer(int workCenterId, int drawingId, int tipoDoc) throws IOException {
        return false;
    }

}

