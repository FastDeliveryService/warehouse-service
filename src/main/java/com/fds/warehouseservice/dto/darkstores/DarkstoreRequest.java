package com.fds.warehouseservice.dto.darkstores;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DarkstoreRequest(

        @NotBlank
        @Size(max = 50)
        String code,

        @NotBlank
        String name,

        @NotBlank
        String address,

        @Min(-90)
        @Max(90)
        @NotNull
        BigDecimal latitude,

        @Min(-180)
        @Max(180)
        @NotNull
        BigDecimal longitude
) {
}
