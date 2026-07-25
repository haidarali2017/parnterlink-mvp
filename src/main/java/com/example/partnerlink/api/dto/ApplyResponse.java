package com.example.partnerlink.api.dto;

import com.example.partnerlink.domain.ApplicationStatus;
import com.example.partnerlink.domain.MerchantApplication;

public class ApplyResponse {

    private String applicationId;
    private String merchantName;
    private String merchantNumber;
    private ApplicationStatus status;
    private String failureReason;

    public static ApplyResponse from(MerchantApplication app) {
        ApplyResponse r = new ApplyResponse();
        r.applicationId = app.getApplicationId();
        r.merchantName = app.getMerchantName();
        r.merchantNumber = app.getMerchantNumber();
        r.status = app.getStatus();
        r.failureReason = app.getFailureReason();
        return r;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getMerchantNumber() {
        return merchantNumber;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
