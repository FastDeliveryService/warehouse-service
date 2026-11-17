package com.fds.warehouseservice.entity.stocks;

import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import com.fds.warehouseservice.entity.anotation.GeneratedUuidVersionSeven;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_stocks")
@Getter
@Setter
@NoArgsConstructor
public class InventoryStocksEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedUuidVersionSeven
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "darkstore_id", nullable = false)
    private DarkstoreEntity darkstore;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "storage_bin_id", nullable = false)
    private StorageBinEntity storageBin;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @ColumnDefault("0")
    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @ColumnDefault("0")
    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
