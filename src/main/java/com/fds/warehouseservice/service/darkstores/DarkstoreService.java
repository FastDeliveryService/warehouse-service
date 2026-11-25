package com.fds.warehouseservice.service.darkstores;

import com.fds.warehouseservice.dto.darkstores.DarkstoreRequest;
import com.fds.warehouseservice.dto.darkstores.DarkstoreResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DarkstoreService {
    Page<DarkstoreResponse> getAllDarkstores(Pageable pageable);
    DarkstoreResponse getDarkstore(UUID id);
    DarkstoreResponse addDarkstore(DarkstoreRequest darkstoreRequest);
    DarkstoreResponse updateDarkstore(UUID id, DarkstoreRequest darkstoreRequest);
    void deleteDarkstore(UUID id);
}
