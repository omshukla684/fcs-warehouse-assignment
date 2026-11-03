package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;
import java.util.List;
import java.util.Optional;

/**
 * Output port: persistence operations for fulfillment associations.
 *
 * <p>Implementations live in the infrastructure/adapter layer.
 */
public interface FulfillmentStore {

  Optional<WarehouseFulfillment> findByWarehouseAndStore(String warehouseCode, String storeIdentifier);

  List<WarehouseFulfillment> findByStore(String storeIdentifier);

  List<WarehouseFulfillment> findByWarehouse(String warehouseCode);

  WarehouseFulfillment save(WarehouseFulfillment fulfillment);
}
