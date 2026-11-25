package com.fds.warehouseservice.repository.darkstores;

import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorageBinRepository extends JpaRepository<StorageBinEntity, UUID> {

    Page<StorageBinEntity> readAllByDarkstoreIdAndActiveTrue(UUID darkstoreId, Pageable pageable);

    Optional<StorageBinEntity> findByIdAndDarkstoreIdAndActiveTrue(UUID id, UUID darkstoreId);
}
