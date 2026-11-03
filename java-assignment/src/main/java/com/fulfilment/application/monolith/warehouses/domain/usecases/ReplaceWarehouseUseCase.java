package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validator.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Use case: replace the current active warehouse with a new one.
 *
 * <p>Archives the old warehouse and creates the new one, delegating all validation to
 * {@link WarehouseValidator}.
 */
@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;
  private final LocationResolver locationResolver;
  private final WarehouseValidator warehouseValidator;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore,
      ArchiveWarehouseOperation archiveWarehouseOperation,
      LocationResolver locationResolver,
      WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
    this.locationResolver = locationResolver;
    this.warehouseValidator = warehouseValidator;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    // Find the current active warehouse to be replaced
    Warehouse currentWarehouse =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (currentWarehouse == null) {
      throw new jakarta.ws.rs.WebApplicationException(
          "No active warehouse found with businessUnitCode: " + newWarehouse.businessUnitCode,
          404);
    }

    // Location Validation
    var location = locationResolver.resolveByIdentifier(newWarehouse.location);
    warehouseValidator.validateLocationExists(newWarehouse.location, location);

    // Capacity Accommodation: new capacity must cover existing stock
    warehouseValidator.validateCapacityAccommodatesStock(
        newWarehouse.capacity, currentWarehouse.stock);

    // Stock Matching: new stock must equal old stock
    warehouseValidator.validateStockMatchesOnReplacement(
        newWarehouse.stock, currentWarehouse.stock);

    // Archive the current warehouse (sets archivedAt timestamp)
    archiveWarehouseOperation.archive(currentWarehouse);

    // Create the replacement warehouse
    warehouseStore.create(newWarehouse);
  }
}
