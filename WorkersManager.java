package com.preving.intranet.gestioncentrosapi.model.services;

import com.preving.intranet.gestioncentrosapi.model.dao.workers.WorkersCustomRepository;
import com.preving.intranet.gestioncentrosapi.model.dao.workers.WorkersRepository;
import com.preving.intranet.gestioncentrosapi.model.domain.workers.EmpLabHistory;
import com.preving.intranet.gestioncentrosapi.model.domain.workers.Employees;
import com.preving.intranet.gestioncentrosapi.model.domain.workers.WorkersFilter;
import com.preving.intranet.gestioncentrosapi.model.dto.workers.EmployeeProjection;
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
import com.preving.intranet.gestioncentrosapi.model.dao.workers.WorkersRepository;
import com.preving.intranet.gestioncentrosapi.model.domain.workers.Employees;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


@Service
public class WorkersManager implements WorkersService{

    @Autowired
    WorkersRepository workersRepository;


    private static final String EXPORT_TITLE_1 = "Trabajador";
    private static final String EXPORT_TITLE_2 = "Departamento";
    private static final String EXPORT_TITLE_3 = "Puesto";
    private static final String EXPORT_TITLE_4 = "Email";
    private static final String EXPORT_TITLE_5 = "Telefono";
    private static final String EXPORT_TITLE_6 = "Movil";


    @Override
    public List<Employees> findAll() {
        return workersRepository.findAll();
    }

    @Override
    public Employees findById(Long id) {
        return workersRepository.findById(id).orElse(null);
    }

//    @Override
//    public List<Employees> getEmployees() {
//        return workersRepository.findTop30ByOrderByIdAsc();
//    }

//    @Override
//    public List<Employees> findAllByEmpLabHistoryFchSalidaIsNull() {
//        return workersRepository.findAllByEmpLabHistoryFchSalidaIsNullAndEmpLabHistoryDelegacionId(2001);
//    }


    //filterWorkers
    @Override
    public List<Employees> getFilteredEmployees(int workCenterId, WorkersFilter workersFilter) {
        if(workersFilter.getEmployeeId().size() > 0 && workersFilter.getDepartmentId().size() > 0){
           return workersRepository.findAllByEmpLabHistoryFchSalidaIsNullAndEmpLabHistoryDelegacionIdAndIdInAndEmpLabHistoryAreaDepartmentIdIn(workCenterId,workersFilter.getEmployeeId(),workersFilter.getDepartmentId());
        } else if (workersFilter.getDepartmentId().size() > 0) {
            return workersRepository.findAllByEmpLabHistoryFchSalidaIsNullAndEmpLabHistoryDelegacionIdAndEmpLabHistoryAreaDepartmentIdIn(workCenterId,workersFilter.getDepartmentId());
        }else if (workersFilter.getEmployeeId().size() > 0) {
            return  workersRepository.findAllByEmpLabHistoryFchSalidaIsNullAndEmpLabHistoryDelegacionIdAndIdIn(workCenterId,workersFilter.getEmployeeId());
        } else {
            return workersRepository.findAllByEmpLabHistoryFchSalidaIsNullAndEmpLabHistoryDelegacionId(workCenterId);
        }
    }

    @Override
    public List<Employees> getWorkcenterEmployees(int workCenterId){
        return workersRepository.findAllByEmpLabHistoryFchSalidaIsNullAndEmpLabHistoryDelegacionId(workCenterId);
    }

    // exportWorkers
    @Override
    public ResponseEntity<?> exportWorkers(int workCenterId,WorkersFilter workersFilter, HttpServletResponse response, UsuarioWithRoles user) {

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
//        List<Employees> employees = this.workersRepository.getFilteredEmployees(workCenterId, workersFilter, user);
        List<Employees> employees = getFilteredEmployees(workCenterId, workersFilter);
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
        HSSFRow dataRow = null;
        for (int i = 0; i < employees.size(); i++) {
            dataRow = hoja.createRow(1 + i);

            //Employee
            HSSFCell employee  = dataRow.createCell(0);
            employee.setCellValue(employees.get(i).getName().concat(employees.get(i).getSurnames()));

            // department
            HSSFCell department = dataRow.createCell(1);
            HSSFCell job = dataRow.createCell(2);
            for (EmpLabHistory history : employees.get(i).getEmpLabHistory()) {
                if (history.getFchSalida() == null) {
                    department.setCellValue(history.getArea().getDepartment().getName());
                    job.setCellValue(history.getPosition().getName());
                }
            }

            //Email
            HSSFCell email = dataRow.createCell(3);
            email.setCellValue(employees.get(i).getEmpContacto().getEmailPersonal());

            //Telephone
            HSSFCell telephone = dataRow.createCell(4);
            telephone.setCellValue(employees.get(i).getEmpContacto().getTfnoPersonal1());

            //Mobile
            HSSFCell mobile = dataRow.createCell(5);
            mobile.setCellValue(employees.get(i).getEmpContacto().getTfnoPersonal2());

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
    public List<Employees> getEmployeesByWorkCenterId(int workCenterId) {
        return workersRepository.findAllByEmpLabHistoryFchSalidaIsNullAndEmpLabHistoryDelegacionId(workCenterId);
    }

}
