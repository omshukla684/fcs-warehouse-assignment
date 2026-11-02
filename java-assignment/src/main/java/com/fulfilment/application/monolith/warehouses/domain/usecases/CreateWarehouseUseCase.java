package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {

    // 1. Business Unit Code Uniqueness:
    //    Ensure that no active warehouse already exists with this businessUnitCode
    var existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing != null) {
      throw new WebApplicationException(
          "A warehouse with businessUnitCode '"
              + warehouse.businessUnitCode
              + "' already exists.",
          400);
    }

    // 2. Location Validation:
    //    The warehouse location must correspond to a valid, known location
    var location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WebApplicationException(
          "Location '" + warehouse.location + "' is not a valid location.", 400);
    }

    // 3. Warehouse Creation Feasibility:
    //    Check if we have not reached the maximum number of warehouses for this location
    List<Warehouse> activeAtLocation = warehouseStore.findActiveByLocation(warehouse.location);
    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Location '"
              + warehouse.location
              + "' has already reached its maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ").",
          400);
    }

    // 4. Capacity Validation:
    //    New warehouse capacity must not exceed location's maxCapacity
    int totalCapacityAtLocation =
        activeAtLocation.stream().mapToInt(w -> w.capacity).sum() + warehouse.capacity;
    if (totalCapacityAtLocation > location.maxCapacity) {
      throw new WebApplicationException(
          "Adding this warehouse would exceed the maximum total capacity of location '"
              + warehouse.location
              + "'. Max capacity: "
              + location.maxCapacity
              + ", current total: "
              + (totalCapacityAtLocation - warehouse.capacity)
              + ", requested: "
              + warehouse.capacity
              + ".",
          400);
    }

    // 5. Stock Validation:
    //    Stock must not exceed warehouse capacity
    if (warehouse.stock != null && warehouse.capacity != null
        && warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException(
          "Warehouse stock ("
              + warehouse.stock
              + ") cannot exceed warehouse capacity ("
              + warehouse.capacity
              + ").",
          400);
    }

    // All validations passed – persist the warehouse
    warehouseStore.create(warehouse);
  }
}
