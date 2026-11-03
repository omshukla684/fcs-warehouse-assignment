package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validator.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Use case: create a new active warehouse.
 *
 * <p>Delegates all business-rule checks to {@link WarehouseValidator} and persistence to
 * {@link WarehouseStore}, keeping this class focused solely on orchestrating the workflow.
 */
@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private final WarehouseValidator warehouseValidator;

  public CreateWarehouseUseCase(
      WarehouseStore warehouseStore,
      LocationResolver locationResolver,
      WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
    this.warehouseValidator = warehouseValidator;
  }

  @Override
  public void create(Warehouse warehouse) {

    // 1. Business Unit Code Uniqueness
    var existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    warehouseValidator.validateUniqueBusinessUnitCode(warehouse.businessUnitCode, existing);

    // 2. Location Validation
    var location = locationResolver.resolveByIdentifier(warehouse.location);
    warehouseValidator.validateLocationExists(warehouse.location, location);

    // 3. Warehouse Count Feasibility
    List<Warehouse> activeAtLocation = warehouseStore.findActiveByLocation(warehouse.location);
    warehouseValidator.validateWarehouseCountAtLocation(location, activeAtLocation);

    // 4. Total Capacity at Location
    warehouseValidator.validateTotalCapacityAtLocation(
        location, activeAtLocation, warehouse.capacity);

    // 5. Stock / Capacity Coherence
    warehouseValidator.validateStockWithinCapacity(warehouse);

    // All validations passed – persist the warehouse
    warehouseStore.create(warehouse);
  }
}
