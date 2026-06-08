package datart.server.base.params;

import datart.core.base.consts.Const;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
@NotNull
public class UserResetPasswordParam {

    @NotBlank
    private String token;

    @NotBlank
    private String verifyCode;

    @NotBlank
    @Pattern(regexp = Const.REG_USER_PASSWORD, message = "Password length should be 6-20 characters")
    private String newPassword;

}
