package com.fds.warehouseservice.entity.tasks;

import com.fds.warehouseservice.entity.anotation.GeneratedUuidVersionSeven;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.enums.PickingTaskStatus;
import com.fds.warehouseservice.entity.reservations.StockReservationEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "picking_tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PickingTaskEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedUuidVersionSeven
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "darkstore_id", nullable = false)
    private DarkstoreEntity darkstore;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private StockReservationEntity reservation;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PickingTaskStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "pickingTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PickingTaskItemEntity> pickingTaskItems = new ArrayList<>();
}
