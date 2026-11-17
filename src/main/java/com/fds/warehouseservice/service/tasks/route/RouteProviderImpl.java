package com.fds.warehouseservice.service.tasks.route;

import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import com.fds.warehouseservice.entity.stocks.InventoryStocksEntity;
import com.fds.warehouseservice.entity.tasks.PickingTaskEntity;
import com.fds.warehouseservice.entity.tasks.PickingTaskItemEntity;
import com.fds.warehouseservice.repository.stocks.InventoryStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RouteProviderImpl implements RouteProvider {

    private final InventoryStockRepository stockRepository;

    @Override
    public String provideRoute(PickingTaskEntity task) {
        final String itemRoute = "SKU: %s, Quantity: %d, Bin Code: %s, Zone: %s, Aisle: %s, Shelf: %s";

        List<UUID> requiredProductIds = task.getPickingTaskItems().stream()
                .map(PickingTaskItemEntity::getProductId)
                .toList();

        List<InventoryStocksEntity> stocks = stockRepository.getByDarkstoreIdAndProductIdIn(
                task.getDarkstore().getId(), requiredProductIds);

        Map<UUID, InventoryStocksEntity> products = new HashMap<>();
        stocks.forEach(stock -> products.put(stock.getProductId(), stock));

        return task.getPickingTaskItems().stream()
                .sorted(Comparator.comparing((PickingTaskItemEntity taskItem) ->
                                products.get(taskItem.getProductId()).getStorageBin().getZone())
                        .thenComparing(taskItem ->
                                products.get(taskItem.getProductId()).getStorageBin().getShelf()))
                .map((PickingTaskItemEntity taskItem) -> {
                            InventoryStocksEntity stock = products.get(taskItem.getProductId());
                            StorageBinEntity bin = stock.getStorageBin();

                            return String.format(
                                    itemRoute,
                                    stock.getSku(),
                                    taskItem.getQuantity(),
                                    bin.getBinCode(),
                                    bin.getZone(),
                                    bin.getAisle(),
                                    bin.getShelf());
                        }
                ).collect(Collectors.joining(";"));
    }
}
