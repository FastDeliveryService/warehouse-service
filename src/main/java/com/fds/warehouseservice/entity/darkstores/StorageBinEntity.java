package com.fds.warehouseservice.entity.darkstores;

import com.fds.warehouseservice.entity.anotation.GeneratedUuidVersionSeven;
import com.fds.warehouseservice.entity.enums.StorageBinZone;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "storage_bins")
@Getter
@Setter
@NoArgsConstructor
public class StorageBinEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedUuidVersionSeven
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "darkstore_id", nullable = false)
    private DarkstoreEntity darkstore;

    @Column(name = "bin_code", nullable = false, length = 100)
    private String binCode;

    @Column(name = "zone", nullable = false)
    @Enumerated(EnumType.STRING)
    private StorageBinZone zone;

    @Column(name = "aisle", length = 100)
    private String aisle;

    @Column(name = "shelf", length = 100)
    private String shelf;

    @ColumnDefault("true")
    @Column(name = "active", nullable = false)
    private Boolean active;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StorageBinEntity that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
