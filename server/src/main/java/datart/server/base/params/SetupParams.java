package datart.server.base.params;

import lombok.Data;

import jakarta.validation.Valid;

@Data
public class SetupParams {

    @Valid
    private UserRegisterParam user;
}
