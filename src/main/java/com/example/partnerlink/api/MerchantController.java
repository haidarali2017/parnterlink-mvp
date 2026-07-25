package com.example.partnerlink.api;

import com.example.partnerlink.api.dto.ApplyRequest;
import com.example.partnerlink.api.dto.ApplyResponse;
import com.example.partnerlink.application.MerchantApplicationService;
import com.example.partnerlink.application.MerchantApplicationService.ApplyResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchants")
public class MerchantController {

    private final MerchantApplicationService service;

    public MerchantController(MerchantApplicationService service) {
        this.service = service;
    }

    /**
     * Idempotent apply. First call creates APPLIED→SCREENING and kicks off async MUN.
     * Retries with the same applicationId return the stored row without a second MUN call.
     */
    @PostMapping("/apply")
    public ResponseEntity<ApplyResponse> apply(@Valid @RequestBody ApplyRequest request) {
        ApplyResult result = service.apply(request.getApplicationId(), request.getMerchantName());
        HttpStatus status = result.created() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApplyResponse.from(result.application()));
    }

    @GetMapping("/{applicationId}")
    public ApplyResponse get(@PathVariable String applicationId) {
        return ApplyResponse.from(service.getByApplicationId(applicationId));
    }
}
