package com.fds.warehouseservice.entity.tasks;

import com.fds.warehouseservice.entity.anotation.GeneratedUuidVersionSeven;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "picking_task_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PickingTaskItemEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedUuidVersionSeven
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "picking_task_id", nullable = false)
    private PickingTaskEntity pickingTask;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
