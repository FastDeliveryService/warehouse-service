package com.fds.warehouseservice.repository.tasks;

import com.fds.warehouseservice.entity.enums.PickingTaskStatus;
import com.fds.warehouseservice.entity.tasks.PickingTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PickingTaskRepository extends JpaRepository<PickingTaskEntity, UUID> {

    Optional<PickingTaskEntity> findFirstByStatusOrderByCreatedAt(PickingTaskStatus status);
}
