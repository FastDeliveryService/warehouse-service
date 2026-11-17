package com.fds.warehouseservice.entity.darkstores;

import com.fds.warehouseservice.entity.anotation.GeneratedUuidVersionSeven;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "darkstores")
@Getter
@Setter
@NoArgsConstructor
public class DarkstoreEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedUuidVersionSeven
    private UUID id;

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 8)
    private BigDecimal longitude;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ColumnDefault("true")
    @Column(name = "active", nullable = false)
    private Boolean active;

    @OneToMany(
            mappedBy = "darkstore",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<StorageBinEntity> storageBins = new HashSet<>();
}
