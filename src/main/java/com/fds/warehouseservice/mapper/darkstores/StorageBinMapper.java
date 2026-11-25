package com.fds.warehouseservice.mapper.darkstores;

import com.fds.warehouseservice.dto.storagebin.StorageBinRequest;
import com.fds.warehouseservice.dto.storagebin.StorageBinResponse;
import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import com.fds.warehouseservice.entity.enums.StorageBinZone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = DarkstoreMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StorageBinMapper {

    @Mapping(target = "darkstoreId", source = "darkstore.id")
    StorageBinResponse toBinResponse(StorageBinEntity entity);

    @Mapping(target = "active", constant = "true")
    StorageBinEntity toBinEntity(StorageBinRequest request);

    StorageBinEntity updateFromRequest(@MappingTarget StorageBinEntity entityToUpdate, StorageBinRequest request);

    default StorageBinZone getStorageBinZone(String zone) {
        return zone == null ? null : StorageBinZone.fromStringCaseIgnore(zone);
    }
}
