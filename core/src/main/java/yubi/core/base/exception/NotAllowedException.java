package yubi.core.base.exception;

import yubi.core.base.exception.BaseException;

public class NotAllowedException extends BaseException {
    public NotAllowedException(String message) {
        super(message);
    }

    public NotAllowedException(String message, int errCode) {
        super(message);
        this.setErrCode(errCode);
    }

    public NotAllowedException() {
        super();
    }

}
