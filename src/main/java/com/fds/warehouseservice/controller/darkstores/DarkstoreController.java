package com.fds.warehouseservice.controller.darkstores;

import com.fds.warehouseservice.dto.darkstores.DarkstoreRequest;
import com.fds.warehouseservice.dto.darkstores.DarkstoreResponse;
import com.fds.warehouseservice.service.darkstores.DarkstoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/darkstores")
@RequiredArgsConstructor
public class DarkstoreController {

    private final DarkstoreService darkstoreService;

    @GetMapping
    public ResponseEntity<Page<DarkstoreResponse>> getAllDarkstores(Pageable pageable) {
        Page<DarkstoreResponse> allDarkstores = darkstoreService.getAllDarkstores(pageable);

        return ResponseEntity.ok(allDarkstores);
    }

    @PostMapping
    public ResponseEntity<DarkstoreResponse>  createDarkstore(
            @RequestBody @Valid DarkstoreRequest darkstoreRequest) {
        DarkstoreResponse darkstoreResponse = darkstoreService.addDarkstore(darkstoreRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(darkstoreResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DarkstoreResponse> getDarkstore(@PathVariable UUID id) {
        DarkstoreResponse response = darkstoreService.getDarkstore(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DarkstoreResponse> updateDarkstore(
            @PathVariable UUID id,
            @RequestBody @Valid DarkstoreRequest darkstoreRequest) {
        DarkstoreResponse darkstoreResponse = darkstoreService.updateDarkstore(id, darkstoreRequest);
        return ResponseEntity.ok(darkstoreResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDarkstore(@PathVariable UUID id) {
        darkstoreService.deleteDarkstore(id);
        return ResponseEntity.noContent().build();
    }
}
