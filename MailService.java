package com.preving.intranet.gestioncentrosapi.model.services;

import java.util.Map;

public interface MailService {

    boolean sendMail(String[] sendTo, Map<String, Object> emailData);

    String getToken();
}
