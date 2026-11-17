package com.fds.warehouseservice.exceptions;

import java.util.UUID;

public class NotEnoughStockException extends RuntimeException {
    public NotEnoughStockException(UUID productId, UUID darkstoreId) {
        super(String.format("There is not enough product with id %s at darkstore %s", productId, darkstoreId));
    }
}
