package com.fds.warehouseservice.repository.darkstores;

import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DarkstoreRepository extends JpaRepository<DarkstoreEntity, UUID> {

    Optional<DarkstoreEntity> findByIdAndActiveTrue(UUID id);
    Page<DarkstoreEntity> readAllByActiveTrue(Pageable pageable);

}
