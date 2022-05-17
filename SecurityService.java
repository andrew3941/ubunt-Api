package com.preving.intranet.gestioncentrosapi.model.services;

import javax.servlet.http.HttpServletRequest;

public interface SecurityService {

    boolean hasAccessToWorkCenter(int workCenterId, HttpServletRequest request);

}
