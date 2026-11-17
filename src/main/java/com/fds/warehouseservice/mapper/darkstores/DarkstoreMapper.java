package com.fds.warehouseservice.mapper.darkstores;

import com.fds.warehouseservice.dto.darkstores.DarkstoreRequest;
import com.fds.warehouseservice.dto.darkstores.DarkstoreResponse;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DarkstoreMapper {

    DarkstoreResponse toDarkstoreResponse(DarkstoreEntity darkstoreRequest);

    @Mapping(target = "active", constant = "true")
    DarkstoreEntity  toDarkstoreEntity(DarkstoreRequest darkstoreResponse);

    DarkstoreEntity updateFromRequest(@MappingTarget DarkstoreEntity toUpdate, DarkstoreRequest darkstoreRequest);
}
