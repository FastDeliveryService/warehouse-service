package com.fds.warehouseservice.entity.stocks;

import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.anotation.GeneratedUuidVersionSeven;
import com.fds.warehouseservice.entity.enums.StockAuditOperationType;
import com.fds.warehouseservice.entity.enums.StockAuditReason;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_audit_logs")
@Getter
@Builder
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StockAuditLogEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedUuidVersionSeven
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "darkstore_id", nullable = false)
    private DarkstoreEntity darkstore;

    @NotNull
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "operation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private StockAuditOperationType operationType;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "resulting_quantity", nullable = false)
    private Integer resultingQuantity;

    @Column(name = "reason_code")
    @Enumerated(EnumType.STRING)
    private StockAuditReason reasonCode;

    @Column(name = "notes")
    private String notes;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
