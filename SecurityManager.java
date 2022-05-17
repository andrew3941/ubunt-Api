package com.preving.intranet.gestioncentrosapi.model.services;

import com.preving.intranet.gestioncentrosapi.model.dao.workCenters.WorkCentersRepository;
import com.preving.intranet.gestioncentrosapi.model.domain.workCenters.WorkCenter;
import com.preving.security.JwtTokenUtil;
import com.preving.security.domain.UsuarioWithRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class SecurityManager implements SecurityService {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private WorkCentersRepository workCentersRepository;

    private final static String GC_ADMINISTRATOR_ROL_NAME = "44-25102";
    private final static String GC_MANAGER_ROL_NAME = "44-25103";
    private final static String GC_READING_ROL_NAME = "44-25104";

    @Override
    public boolean hasAccessToWorkCenter(int workCenterId, HttpServletRequest request) {

        UsuarioWithRoles user = this.jwtTokenUtil.getUserWithRolesFromToken(request);
        WorkCenter workCenter = this.workCentersRepository.findWorkCenterById(workCenterId);
        boolean access = true;

        if(!user.hasRole(GC_ADMINISTRATOR_ROL_NAME)) {

            if(user.hasRole(GC_MANAGER_ROL_NAME)) {
                if (workCenter.getHeadPerson() == null) {
                    // TODO ??
                    access = false;
                } else {
                    access = workCenter.getHeadPerson().getId() == user.getId();
                }
            }

        }

        return access;

    }

}
