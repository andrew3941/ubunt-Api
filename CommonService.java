package com.preving.intranet.gestioncentrosapi.model.services;

import com.preving.intranet.gestioncentrosapi.model.domain.Entity;
import com.preving.intranet.gestioncentrosapi.model.domain.Province;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;




public interface CommonService {
    boolean deleteMaintenanceServer(int workCenterId, int drawingId, int tipoDoc) throws IOException;
    List<Province> findAllProvinces();

    List<Entity> findAllEntities();

    String saveDocumentServer(int workCenterId, int drawingId, MultipartFile attachedFile, int tipoDoc) throws IOException;

    boolean deleteDocumentServer(int workCenterId, int drawingId, int tipoDoc) throws IOException;

}
