package com.impansini.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class KeyAndPasswordDTO {

    @NotBlank
    @Size(min = 10, max = 10)
    private String key;

    @NotBlank
    private String newPassword;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "KeyAndPasswordDTO{" +
                "key='" + key + '\'' +
                //", newPassword='" + newPassword + '\'' +
                '}';
    }
}
