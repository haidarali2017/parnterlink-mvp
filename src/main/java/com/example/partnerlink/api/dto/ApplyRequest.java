package com.example.partnerlink.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ApplyRequest {

    @NotBlank
    @Size(min = 8, max = 36)
    private String applicationId;

    @NotBlank
    @Size(max = 255)
    private String merchantName;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
}
