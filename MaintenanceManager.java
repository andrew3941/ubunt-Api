package com.preving.intranet.gestioncentrosapi.model.services;
import com.preving.intranet.gestioncentrosapi.model.dao.maintenance.*;
import com.preving.intranet.gestioncentrosapi.model.dao.users.UserCustomRepository;
import com.preving.intranet.gestioncentrosapi.model.domain.User;
import com.preving.intranet.gestioncentrosapi.model.domain.maintenance.*;
import com.preving.intranet.gestioncentrosapi.model.domain.workCenters.WorkCenter;
import com.preving.security.JwtTokenUtil;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import com.preving.security.domain.UsuarioWithRoles;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;


import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class MaintenanceManager implements MaintenanceService {

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private MaintenanceCustomRepository maintenanceCustomRepository;


    private static final String EXPORT_TITLE_1 = "Fecha";
    private static final String EXPORT_TITLE_2 = "Tipo";
    private static final String EXPORT_TITLE_3 = "Concepto";
    private static final String EXPORT_TITLE_4 = "Proveedor";
    private static final String EXPORT_TITLE_5 = "Periodicidad";
    private static final String EXPORT_TITLE_6 = "Importe";


    private static final int NEW_MAINTENANCE = 4;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Autowired
    private MaintenanceByAttachmentRepository maintenanceByAttachmentRepository;

    @Autowired
    private MaintenanceTypesRepository maintenanceTypesRepository;

    @Autowired
    private CommonService commonService;

    @Autowired
    private MaintenanceByWorkCentersRepository maintenanceByWorkCentersRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserCustomRepository userCustomRepository;


    @Override
    public Maintenance getMaintenanceById(int maintenanceId){
        return maintenanceRepository.findMaintenanceById(maintenanceId);
    }

    public ResponseEntity<?> downloadMaintenanceDoc(HttpServletRequest request, int workCenterId, int maintenanceId) {
        Maintenance maintenance = null;
        File file = null;
        byte[] content = null;

        try {
            String docUrl = this.maintenanceCustomRepository.findDocUrlByMaintenanceId(maintenanceId, workCenterId);

            file = new File(docUrl);
            if (file.exists()) {
                content = Files.readAllBytes(file.toPath());
            } else {
                return new ResponseEntity<>("File not found", HttpStatus.NOT_FOUND);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>("Unknown error", HttpStatus.INTERNAL_SERVER_ERROR);
        }


        return new ResponseEntity<byte[]>(content, HttpStatus.OK);
    }


    @Transactional
    public ResponseEntity<?> editMaintenance(int workCenterId, Maintenance maintenance, MultipartFile[] attachedFile, HttpServletRequest request) {

        long userId = this.jwtTokenUtil.getUserWithRolesFromToken(request).getId();

        maintenance.setModifiedBy(new User());
        maintenance.getModifiedBy().setId(userId);

        try {

            maintenanceRepository.editMaintenance(maintenance);

            if (attachedFile.length > 0) {

                for (MultipartFile mtFile : attachedFile){

                    MaintenanceByAttachment maintenanceByAttachment = new MaintenanceByAttachment();

                    maintenanceByAttachment.setMaintenance(maintenance);
                    maintenanceByAttachment.setDocName(mtFile.getOriginalFilename());
                    maintenanceByAttachment.setDocumentContentType(mtFile.getContentType());
                    maintenanceByAttachment.setDocumentUrl("default_Url");

                    MaintenanceByAttachment savedMaintenanceFile = maintenanceByAttachmentRepository.save(maintenanceByAttachment);

                    String url = null;
                    // Guardamos documento en el server
                    url = commonService.saveDocumentServer(workCenterId, maintenance.getId(), mtFile, NEW_MAINTENANCE);

                    // Actualizamos la ruta del documento guardado
                    if (url != null) {
                        this.maintenanceByAttachmentRepository.updateNewMaintenanceByAttachmentUrl(savedMaintenanceFile.getId(), url);
                    }

                }
            }

        }catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }



    @Autowired
    public MaintenanceManager(MaintenanceRepository maintenanceRepository) {
        this.maintenanceRepository = maintenanceRepository;
    }

    @Override
    public List<Maintenance> findAllMaintenance(){
        return maintenanceRepository.findMaintenancesByDeletedByIsNullOrderByCreatedDesc();
    }


    @Override
    public List<Maintenance> getFilteredMaintenances(int workCenterId, MaintenanceFilter maintenanceFilter, UsuarioWithRoles user) {
        return this.maintenanceCustomRepository.getMaintenanceFiltered(workCenterId, maintenanceFilter, user);
    }



    /**
     * Guardar mantenimiento
     *
     * @Override
     */
    //Logic to Save New Maintenance
    @Transactional
    public ResponseEntity<?> saveNewMaintenance(int workCenterId, Maintenance newMaintenance, MultipartFile[] attachedFile, HttpServletRequest request) {

        long userId = this.jwtTokenUtil.getUserWithRolesFromToken(request).getId();

        try {

            newMaintenance.setCreated(new Date());
            newMaintenance.getCreatedBy().setId(userId);
            newMaintenance.getWorkCenter().setId(workCenterId);

            // Save maintenance
            Maintenance saveMaintenance = this.maintenanceRepository.save(newMaintenance);

            if (attachedFile.length > 0) {

                for (MultipartFile mpFile : attachedFile) {

                    MaintenanceByAttachment maintenanceByAttachment = new MaintenanceByAttachment();

                    maintenanceByAttachment.setMaintenance(newMaintenance);
                    maintenanceByAttachment.setDocumentUrl("DOC_URL");
                    maintenanceByAttachment.setDocName(mpFile.getOriginalFilename());
                    maintenanceByAttachment.setDocumentContentType(mpFile.getContentType());
                    this.maintenanceByAttachmentRepository.save(maintenanceByAttachment);

                    String url = null;

                    // Guardamos documento en el server
                    url = commonService.saveDocumentServer(workCenterId, saveMaintenance.getId(), mpFile, NEW_MAINTENANCE);

                    // Actualizamos la ruta del documento guardado
                    if (url != null) {
                        this.maintenanceByAttachmentRepository.updateNewMaintenanceByAttachmentUrl(maintenanceByAttachment.getId(), url);
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
    public ResponseEntity<?> deleteMaintenance(HttpServletRequest request, int workCenterId, int maintenanceId) {
        long mId = this.jwtTokenUtil.getUserWithRolesFromToken(request).getId();

        Maintenance maintenance = this.maintenanceRepository.findMaintenanceById(maintenanceId);

        if (maintenance==null){
            return new ResponseEntity <>(HttpStatus.NOT_FOUND);
        }

        try {
            this.maintenanceRepository.maintenanceLogicDeleted((int) mId,maintenanceId);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public List<MaintenanceTypes> getAllMaintenanceTypes() {
        return maintenanceTypesRepository.findAllByActiveIsTrue();
    }

//METHOD FOR EXPORT MAINTENANCE
    @Override
    public ResponseEntity<?> exportMaintenance(int workCenterId, MaintenanceFilter maintenanceFilter, HttpServletResponse response, UsuarioWithRoles user) {
        byte[] content=null;

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet hoja = workbook.createSheet();
        workbook.setSheetName(0, "performances");
        // We create style for the header
        CellStyle cellStyleHeaders = workbook.createCellStyle();
        CellStyle dateCell = workbook.createCellStyle();
        Font font = workbook.createFont();
        // TODO color the background of the headers
        font.setBold(true);
        cellStyleHeaders.setFont(font);
        //style for date format
        CellStyle cellStyleData = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        cellStyleData.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));

        // We get the data
        List<Maintenance> maintenances = this.maintenanceCustomRepository.getMaintenanceFiltered(workCenterId,maintenanceFilter, user);

        String[] titleArray = {EXPORT_TITLE_1, EXPORT_TITLE_2, EXPORT_TITLE_3, EXPORT_TITLE_4, EXPORT_TITLE_5, EXPORT_TITLE_6};

        // We create a row in the sheet at position 0 for the headers
        HSSFRow headerRow = hoja.createRow(0);

        // We create the headers
        for (int i = 0; i < titleArray.length; i++) {
            HSSFCell celda = headerRow.createCell(i);
            celda.setCellValue(titleArray[i]);
            celda.setCellStyle(cellStyleHeaders);
        }

        // We create the rows
        for (int i = 0; i < maintenances.size(); i++) {
            HSSFRow dataRow = hoja.createRow(1 + i);


            // date
            HSSFCell date = dataRow.createCell(0);
            date.setCellValue(maintenances.get(i).getDate());
            date.setCellStyle(cellStyleData);

            // type
            HSSFCell type = dataRow.createCell(1);
            type.setCellValue(maintenances.get(i).getMaintenanceTypes().getDenomination());

            // concept
            HSSFCell concept = dataRow.createCell(2);
            concept.setCellValue(maintenances.get(i).getConcept());


            // provider
            HSSFCell provider = dataRow.createCell(3);
            provider.setCellValue(maintenances.get(i).getProvider().getName());

            // periodicity
            HSSFCell periodicity = dataRow.createCell(4);
            periodicity.setCellValue(maintenances.get(i).getExpenditurePeriod().getName());

            // amount
            HSSFCell amount = dataRow.createCell(5);
            amount.setCellValue(maintenances.get(i).getAmount());
        }

        // adjust columns
        for (int i = 0; i < titleArray.length; i++) {
            hoja.autoSizeColumn(i);
        }

        try {
            String nombreFichero = "report-actions";
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "inline; filename=\"" +
                    java.net.URLEncoder.encode(nombreFichero, "UTF-8")
                    + "\"");

            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();

        } catch (IOException ex) {
            return new ResponseEntity<byte[]>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<byte[]>(content, HttpStatus.OK);
    }


    @Override
    public ResponseEntity<?> maintenanceDeleteAttachment(int workCenterId, int attachedId) throws IOException {

        try {
            commonService.deleteDocumentServer(workCenterId, attachedId, NEW_MAINTENANCE);
            maintenanceByAttachmentRepository.deleteById(attachedId);

            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
//
//    @Override
//    @Scheduled(cron = "0 00 00 * * *") //Every day at 12am
//    public void SendMaintenanceEndDayNotification(){
//        System.out.println("Start of automated maintenance notification process");
//        Date twoWeeksPrior = new Date((new Date().getYear()),(new Date().getMonth()),(new Date().getDate() + 14));
//
//        List<Maintenance> maintenances = maintenanceRepository.findAllByDate(twoWeeksPrior);
//
//        for (Maintenance maintenance: maintenances) {
//            //TODO maintenances should have workCenter in them
//            Map <String,Object> emailData = new HashMap<>();
//
//            WorkCenter workCenter = maintenanceByWorkCentersRepository.findByMaintenance(maintenance).getWorkCenter();
//            String formatedDate = simpleDateFormat.format(maintenance.getDate());
//            emailData.put("fecha",formatedDate);
//            emailData.put("documento",maintenance.getConcept());
//            emailData.put("centro",workCenter.getName());
//
//            List<String> sendToList = new ArrayList<>();
//
//            sendToList.add(workCenter.getHeadPerson().getEmail());
//
//            //Get admins and managers
//            List<User> adminsAndMAnagers = userCustomRepository.findAdminsAndManagers();
//
//            adminsAndMAnagers.forEach(admin -> {
//                sendToList.add(admin.getEmail());
//            });
//
//            mailService.sendMail(sendToList.toArray(new String[0]), emailData);
//        }
//    }
}

