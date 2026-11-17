package com.fds.warehouseservice.entity.reservations;

import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.anotation.GeneratedUuidVersionSeven;
import com.fds.warehouseservice.entity.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
@Getter
@Setter
@NoArgsConstructor
public class StockReservationEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedUuidVersionSeven
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "darkstore_id", nullable = false)
    private DarkstoreEntity darkstore;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @OneToMany(
            mappedBy = "reservation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<StockReservationItemEntity> items = new ArrayList<>();

    public void setItems(List<StockReservationItemEntity> items) {
        this.items = items;
        items.forEach(i -> i.setReservation(this));
    }
}
