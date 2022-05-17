package com.preving.intranet.gestioncentrosapi.model.services;

import com.preving.intranet.gestioncentrosapi.model.dao.vehicles.BrandsRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.vehicles.VehiclesRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.vehicles.VehiclesCustomRepository;
import com.preving.intranet.gestioncentrosapi.model.domain.maintenance.Maintenance;
import com.preving.intranet.gestioncentrosapi.model.domain.maintenance.MaintenanceFilter;
import com.preving.intranet.gestioncentrosapi.model.domain.vehicles.Brands;
import com.preving.intranet.gestioncentrosapi.model.domain.vehicles.Vehicles;
import com.preving.intranet.gestioncentrosapi.model.domain.vehicles.Vehicles;
import com.preving.intranet.gestioncentrosapi.model.domain.vehicles.VehiclesFilter;
import com.preving.security.domain.UsuarioWithRoles;
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
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class VehiclesManager implements VehiclesService {
    private static final String EXPORT_TITLE_1 = "Matrícula";
    static final String EXPORT_TITLE_2 = "Marca";
    static final String EXPORT_TITLE_3 = "Modelo";
    static final String EXPORT_TITLE_4 = "Modo compra";
    static final String EXPORT_TITLE_5 = "Responsable";
    static final String EXPORT_TITLE_6 = "Responsible"; //Why is this twice?
    static final String EXPORT_TITLE_7 = "Fecha compra";
    static final String EXPORT_TITLE_8 = "Fecha vencimiento";
    static final String EXPORT_TITLE_9 = "Precio";
    static final String EXPORT_TITLE_10 = "Activo";

    @Autowired
    private VehiclesRepository vehiclesRepository;
    @Autowired
    private VehiclesCustomRepository vehiclesCustomRepository;
    @Autowired
    private BrandsRepository brandsRepository;

    @Override
    public List<Vehicles> getFilteredVehicles(int workCenterId, VehiclesFilter vehiclesFilter, UsuarioWithRoles user) {
        return this.vehiclesCustomRepository.getVehiclesFiltered(workCenterId, vehiclesFilter, user);
    }

    @Override
    public List<Brands> getAllBrandTypes() {
        return brandsRepository.findAllByOrderByName();
    }

    @Override
    public List<Vehicles> findAllVehiclesByWorkCenter(int workCenterId) {
        return vehiclesRepository.findAllByWorkCenterIdAndUserUnsubscribeNotNull(workCenterId);
    }

    // exportVehicles
    @Override
    public ResponseEntity<?> exportVehicle(int workCenterId, VehiclesFilter vehiclesFilter, HttpServletResponse response, UsuarioWithRoles user) {
        byte[] content = null;

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
        List<Vehicles> vehicles = this.vehiclesCustomRepository.getVehiclesFiltered(workCenterId, vehiclesFilter, user);
        String[] titleArray = {EXPORT_TITLE_1, EXPORT_TITLE_2, EXPORT_TITLE_3, EXPORT_TITLE_4, EXPORT_TITLE_5, EXPORT_TITLE_7, EXPORT_TITLE_8,EXPORT_TITLE_10};
        // We create a row in the sheet at position 0 for the headers
        HSSFRow headerRow = hoja.createRow(0);

        // We create the headers
        for (int i = 0; i < titleArray.length; i++) {
            HSSFCell celda = headerRow.createCell(i);
            celda.setCellValue(titleArray[i]);
            celda.setCellStyle(cellStyleHeaders);
        }

        // We create the rows
        HSSFRow dataRow = null;
        for (int i = 0; i < vehicles.size(); i++) {
            dataRow = hoja.createRow(1 + i);

            // enrollment
            HSSFCell enrollment = dataRow.createCell(0);
            enrollment.setCellValue(vehicles.get(i).getEnrollment());

            // Brand
            HSSFCell brands = dataRow.createCell(1);
            brands.setCellValue(vehicles.get(i).getBrands().getName());

            // model
            HSSFCell model = dataRow.createCell(2);
            model.setCellValue(vehicles.get(i).getModel());

            // purchaseMode
            HSSFCell purchaseMode = dataRow.createCell(3);
            purchaseMode.setCellValue(vehicles.get(i).getPurchaseMode());

            // responsible
            HSSFCell responsibleId = dataRow.createCell(4);
            responsibleId.setCellValue(vehicles.get(i).getResponsibleId().getFirstname().concat(vehicles.get(i).getResponsibleId().getLastname()));
            // PurchaseDate
            HSSFCell purchaseDate = dataRow.createCell(5);
            if (vehicles.get(i).getPurchaseDate() != null){
                purchaseDate.setCellValue(vehicles.get(i).getPurchaseDate());
            }
            purchaseDate.setCellStyle(cellStyleData);

            // expirationDate
            HSSFCell expirationDate = dataRow.createCell(6);
            if (vehicles.get(i).getExpirationDate() != null){
                expirationDate.setCellValue(vehicles.get(i).getExpirationDate());
            }
            expirationDate.setCellStyle(cellStyleData);

            // active
            HSSFCell active = dataRow.createCell(7);
            active.setCellValue(vehicles.get(i).getActive());
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
}







