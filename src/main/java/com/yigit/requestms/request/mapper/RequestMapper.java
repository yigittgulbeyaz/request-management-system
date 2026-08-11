package com.yigit.requestms.request.mapper;

import com.yigit.requestms.request.dto.CustomerRequestDetailDto;
import com.yigit.requestms.request.entity.RequestEntity;
import org.springframework.stereotype.Component;

@Component
public class RequestMapper {

    // The grid DTO is built by the repository projection instead of here: going
    // through the entity would load rows this mapper would then throw most of
    // away, and would trigger the lazy customer association per row.
    public CustomerRequestDetailDto toCustomerDetail(RequestEntity entity) {
        return new CustomerRequestDetailDto(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getCreatedAt(),
                entity.getClosedAt()
        );
    }
}