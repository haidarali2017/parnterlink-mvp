package com.example.partnerlink.infrastructure.mybatis;

import com.example.partnerlink.domain.ApplicationStatus;
import com.example.partnerlink.domain.MerchantApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MerchantApplicationMapper {

    MerchantApplication findByApplicationId(@Param("applicationId") String applicationId);

    int insert(MerchantApplication application);

    int updateStatus(@Param("applicationId") String applicationId,
                     @Param("fromStatus") ApplicationStatus fromStatus,
                     @Param("toStatus") ApplicationStatus toStatus,
                     @Param("failureReason") String failureReason);

    int assignMerchantNumber(@Param("applicationId") String applicationId,
                             @Param("merchantNumber") String merchantNumber);
}
