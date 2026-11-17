package com.fds.warehouseservice.controller.darkstores;

import com.fds.warehouseservice.dto.storagebin.StorageBinResponse;
import com.fds.warehouseservice.dto.storagebin.StorageBinRequest;
import com.fds.warehouseservice.service.darkstores.StorageBinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/darkstores/{darkstoreId}/bins")
@RequiredArgsConstructor
public class StorageBinController {

    private final StorageBinService storageBinService;

    @GetMapping
    public ResponseEntity<Page<StorageBinResponse>> getStorageBins(
            @PathVariable UUID darkstoreId,
            Pageable pageable
    ) {
        Page<StorageBinResponse> bins = storageBinService.getBinsByDarkstoreId(darkstoreId, pageable);
        return ResponseEntity.ok(bins);
    }

    @PostMapping
    public ResponseEntity<StorageBinResponse> createStorageBin(
            @PathVariable UUID darkstoreId,
            @RequestBody @Valid StorageBinRequest request
    ) {
        StorageBinResponse added = storageBinService.addDarkstoreStorageBin(darkstoreId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(added);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StorageBinResponse> getStorageBinById(
            @PathVariable UUID id,
            @PathVariable UUID darkstoreId
    ) {
        StorageBinResponse bin = storageBinService.getBinById(id, darkstoreId);
        return ResponseEntity.ok(bin);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StorageBinResponse> updateStorageBin(
            @PathVariable UUID id,
            @PathVariable UUID darkstoreId,
            @RequestBody @Valid StorageBinRequest request
    ) {
        StorageBinResponse updated = storageBinService.updateStorageBin(id, darkstoreId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StorageBinResponse> deleteStorageBin(
            @PathVariable UUID id,
            @PathVariable UUID darkstoreId
    ) {
        storageBinService.deleteStorageBinById(id, darkstoreId);
        return ResponseEntity.noContent().build();
    }
}
