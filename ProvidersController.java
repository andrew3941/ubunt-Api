package com.preving.intranet.gestioncentrosapi.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.preving.intranet.gestioncentrosapi.model.domain.vendors.Provider;
import com.preving.intranet.gestioncentrosapi.model.domain.vendors.ProviderFilter;
import com.preving.intranet.gestioncentrosapi.model.domain.vendors.specificData.ProviderDetail;
import com.preving.intranet.gestioncentrosapi.model.services.ProviderService;
import com.preving.security.JwtTokenUtil;
import com.preving.security.domain.UsuarioWithRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Type;
import java.util.List;



@RestController
@RequestMapping(path = "/workCenters")
@CrossOrigin(origins = "http://localhost:4200")
public class ProvidersController {

    @Autowired
    public ProviderService providerService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Value("${modo-debug}")
    private boolean modoDebug;

    /**
     *
     * @param workCenterId
     * @return
     */
    @RequestMapping(value = "{workCenterId}/providers/types", method = RequestMethod.GET)
    public ResponseEntity<?> getProviderTypes( @PathVariable(value="workCenterId") int workCenterId){

        try {
            return new ResponseEntity<>(providerService.getProviderTypes(workCenterId), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @RequestMapping(value = "{workCenterId}/providers/evaluations", method = RequestMethod.GET)
    public ResponseEntity<?> getProviderEvaluationTypes( @PathVariable(value="workCenterId") int workCenterId){

        try {
            return new ResponseEntity<>(providerService.getProviderEvaluationTypes(workCenterId), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @RequestMapping(value = "{workCenterId}/providers/areas", method = RequestMethod.GET)
    public ResponseEntity<?> getProviderArea( @PathVariable(value="workCenterId") int workCenterId){

        try {
            return new ResponseEntity<>(providerService.getProviderArea(workCenterId), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @RequestMapping(value = "{workCenterId}/providers/periodicity", method = RequestMethod.GET)
    public ResponseEntity<?> getExpenditurePeriod( @PathVariable(value="workCenterId") int workCenterId){

        try {
            return new ResponseEntity<>(providerService.getExpenditurePeriod(workCenterId), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * Obtain filter based on providerFilter Class
     * @param providerFilter
     * @param workCenterId
     * @return
     */

    @RequestMapping(value = "{workCenterId}/providers/filter", method = RequestMethod.POST)
    public ResponseEntity<?> findWorkCenterByFilter(HttpServletRequest request,
                                                    @RequestBody ProviderFilter providerFilter,
                                                    @PathVariable(value="workCenterId") int workCenterId) {

        try {
            UsuarioWithRoles user = this.jwtTokenUtil.getUserWithRolesFromToken(request);
            return new ResponseEntity<>(this.providerService.getProviders(workCenterId, providerFilter, user), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @RequestMapping(value = "{workCenterId}/providers/add", method = RequestMethod.POST)
    public ResponseEntity<?> saveProvider(
            @RequestParam("provider") String myProvider,
            @RequestParam("specificData") String specificData,
            @PathVariable("workCenterId") int workCenterId,
            @RequestParam(value="attachedFile", required = false) MultipartFile attachedFile,
            HttpServletRequest request) {

        ResponseEntity<?> response=null;
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        Provider newProvider = gson.fromJson(myProvider, Provider.class);

        Type listType = new TypeToken<List<ProviderDetail>>(){}.getType();
        List<ProviderDetail> resourceTypes = gson.fromJson(specificData, listType);

        response = providerService.saveProvider(workCenterId, newProvider, resourceTypes, attachedFile, request);

        return response;
    }

    /**
     * Obtiene un proveedor de centro por Id
     * @param workCenterId
     * @param providerId
     * @return
     */
    @RequestMapping(value = "{workCenterId}/provider/{providerId}", method = RequestMethod.GET)
    public ResponseEntity<?> findWorkCenterById(@PathVariable(value = "workCenterId") int workCenterId,
                                                @PathVariable(value = "providerId") int providerId){

        try {
            return new ResponseEntity<>(providerService.getProviderById(workCenterId, providerId), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @RequestMapping(value = "{workCenterId}/providers/{providerId}/edit", method = RequestMethod.POST)
    public ResponseEntity<?> editProvider(@RequestParam("provider") String myProvider,
                                          @RequestParam("specificData") String specificData,
                                          @PathVariable("workCenterId") int workCenterId,
                                          @PathVariable("providerId") int providerId,
                                          @RequestParam(value="attachedFile", required = false) MultipartFile attachedFile,
                                          HttpServletRequest request) {

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        Provider provider = gson.fromJson(myProvider, Provider.class);

        Type listType = new TypeToken<List<ProviderDetail>>(){}.getType();
        List<ProviderDetail> details = gson.fromJson(specificData, listType);

        ResponseEntity<?> response=null;

        response = providerService.editProvider(workCenterId, providerId, provider, details, attachedFile, request);

        return response;

    }

    /**
     * Exportación de actuaciones por filtro de fechas
     * @param
     * @return
     */
    @RequestMapping(value="exportProviders", method = RequestMethod.POST)
    public ResponseEntity<?> exportActions(HttpServletRequest request,
                                           HttpServletResponse response,
                                           @RequestParam ("filterProviderList") String providerList) {

        ResponseEntity<?> resp = null;
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        ProviderFilter providerFilter = gson.fromJson(providerList, ProviderFilter.class);

        try {
            UsuarioWithRoles user = this.jwtTokenUtil.getUserWithRolesFromToken(request);
            return new ResponseEntity<>(providerService.exportProvider(providerFilter, response, user), HttpStatus.OK);
        } catch (DataAccessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * Descargamos el fichero de un servicio de
     * @param providerId
     * @param request
     * @return
     */
    @RequestMapping(value = "{workCenterId}/providers/{providerId}/download", method = RequestMethod.GET)
    public ResponseEntity<?> downloadProviderDoc(HttpServletRequest request,
                                                 @PathVariable(value = "providerId") int providerId,
                                                 @PathVariable(value = "workCenterId") int workCenterId) {

        return ( providerService.downloadProviderDoc(request, workCenterId, providerId));
    }

    @RequestMapping(value = "{providerTypeId}/specific-form", method = RequestMethod.GET)
    public ResponseEntity<?> specificProviderForm(HttpServletRequest request,
                                                  @PathVariable(value = "providerTypeId") int providerTypeId) {

        try {
            return new ResponseEntity<>(providerService.getSpecificProviderForm(providerTypeId), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * Scheduled process to activate providers when the start date matches with the current date
     */
    @Scheduled(cron="0 5 0 * * ?")
    public void activateProviders() {

        if(!modoDebug) {

            try {
                providerService.activateProvider();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * Scheduled process to finalize providers when the end date matches with the current date
     */
    @Scheduled(cron="0 15 0 * * ?")
    public void desactivateProviders() {

        if(!modoDebug) {

            try {
                providerService.desactivateProvider();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }


}
