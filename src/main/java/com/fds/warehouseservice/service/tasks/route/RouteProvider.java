package com.fds.warehouseservice.service.tasks.route;

import com.fds.warehouseservice.entity.tasks.PickingTaskEntity;

public interface RouteProvider {

    String provideRoute(PickingTaskEntity task);
}
