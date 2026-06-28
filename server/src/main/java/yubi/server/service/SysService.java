package yubi.server.service;

import yubi.server.base.dto.SystemInfo;
import yubi.server.base.params.SetupParams;

import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;

public interface SysService {

    SystemInfo getSysInfo();

    boolean setup(SetupParams userRegisterParam) throws MessagingException, UnsupportedEncodingException;
}
