package datart.server.service;

import datart.server.base.dto.SystemInfo;
import datart.server.base.params.SetupParams;

import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;

public interface SysService {

    SystemInfo getSysInfo();

    boolean setup(SetupParams userRegisterParam) throws MessagingException, UnsupportedEncodingException;
}
