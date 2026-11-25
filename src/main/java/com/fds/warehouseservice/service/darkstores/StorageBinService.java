package com.fds.warehouseservice.service.darkstores;

import com.fds.warehouseservice.dto.storagebin.StorageBinResponse;
import com.fds.warehouseservice.dto.storagebin.StorageBinRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StorageBinService {

    Page<StorageBinResponse> getBinsByDarkstoreId(UUID darkstoreId, Pageable pageable);
    StorageBinResponse getBinById(UUID binId, UUID darkstoreId);
    StorageBinResponse addDarkstoreStorageBin(UUID darkstoreId, StorageBinRequest request);
    StorageBinResponse updateStorageBin(UUID binId, UUID darkstoreId, StorageBinRequest request);
    void deleteStorageBinById(UUID binId, UUID darkstoreId);
}
