package com.preving.intranet.gestioncentrosapi.model.services;

import com.preving.intranet.gestioncentrosapi.model.domain.vendors.*;
import com.preving.intranet.gestioncentrosapi.model.domain.vendors.specificData.ProviderDetail;
import com.preving.intranet.gestioncentrosapi.model.domain.vendors.specificData.ProviderDetailConf;
import com.preving.intranet.gestioncentrosapi.model.domain.workCenters.WorkCenter;
import com.preving.security.domain.UsuarioWithRoles;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface ProviderService {

    List<Provider> getProviders(int workCenterId, ProviderFilter providerFilter, UsuarioWithRoles user);

    List<ProviderTypes> getProviderTypes(int workCenterId);

    List<ProviderArea> getProviderArea(int workCenterId);

    List<ProviderEvaluationTypes> getProviderEvaluationTypes(int workCenterId);

    List<ExpenditurePeriod> getExpenditurePeriod(int workCenterId);

    ResponseEntity<?> saveProvider(int workCenterId, Provider newProvider, List<ProviderDetail> specificData,
                                   MultipartFile attachedFile, HttpServletRequest request);

    ResponseEntity<?> editProvider(int workCenterId, int providerId, Provider provider, List<ProviderDetail> details,
                                   MultipartFile attachedFile, HttpServletRequest request);

    void addNewWorkCentersToProviders(WorkCenter workCenter, HttpServletRequest request);

    ResponseEntity<?> exportProvider(ProviderFilter providerFilter, HttpServletResponse response, UsuarioWithRoles user);

    Provider getProviderById(int workCenterId, int providerId);

    ResponseEntity<?> downloadProviderDoc(HttpServletRequest request, int workCenterId, int providerId);

    void desactivateProvider();

    void activateProvider();

    List<ProviderDetailConf> getSpecificProviderForm(int providerTypeId);

    List<Provider> getProvidersByWorkCenter(int workCenterId, boolean allProviders);

    void deactivateEndDateToday();
}
