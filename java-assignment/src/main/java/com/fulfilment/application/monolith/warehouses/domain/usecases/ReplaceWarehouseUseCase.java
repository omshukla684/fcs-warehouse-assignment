package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore,
      ArchiveWarehouseOperation archiveWarehouseOperation,
      LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    // Find the current active warehouse to be replaced
    Warehouse currentWarehouse =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (currentWarehouse == null) {
      throw new WebApplicationException(
          "No active warehouse found with businessUnitCode: " + newWarehouse.businessUnitCode,
          404);
    }

    // Location Validation: the new warehouse location must be valid
    var location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WebApplicationException(
          "Location '" + newWarehouse.location + "' is not a valid location.", 400);
    }

    // Additional Validation 1: Capacity Accommodation
    // The new warehouse must have enough capacity to hold the stock from the old warehouse
    if (newWarehouse.capacity == null || newWarehouse.capacity < currentWarehouse.stock) {
      throw new WebApplicationException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") must be at least as large as the current warehouse stock ("
              + currentWarehouse.stock
              + ").",
          400);
    }

    // Additional Validation 2: Stock Matching
    // The stock of the new warehouse must match the stock of the warehouse being replaced
    if (newWarehouse.stock == null || !newWarehouse.stock.equals(currentWarehouse.stock)) {
      throw new WebApplicationException(
          "New warehouse stock ("
              + newWarehouse.stock
              + ") must match the stock of the warehouse being replaced ("
              + currentWarehouse.stock
              + ").",
          400);
    }

    // Archive the current warehouse (sets archivedAt timestamp)
    archiveWarehouseOperation.archive(currentWarehouse);

    // Create the new warehouse with the same businessUnitCode
    warehouseStore.create(newWarehouse);
  }
}
