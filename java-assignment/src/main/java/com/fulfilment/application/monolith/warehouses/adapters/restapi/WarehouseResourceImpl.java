package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;

  @Inject private CreateWarehouseOperation createWarehouseOperation;

  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;

  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Override
  public List<com.warehouse.api.beans.Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @Transactional
  public com.warehouse.api.beans.Warehouse createANewWarehouseUnit(
      @NotNull com.warehouse.api.beans.Warehouse data) {
    var warehouse = toDomainWarehouse(data);
    createWarehouseOperation.create(warehouse);
    // Return the warehouse data as accepted
    return toWarehouseResponse(warehouse);
  }

  @Override
  public com.warehouse.api.beans.Warehouse getAWarehouseUnitByID(String id) {
    Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) {
      throw new WebApplicationException(
          "Warehouse with businessUnitCode '" + id + "' not found.", 404);
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) {
      throw new WebApplicationException(
          "Warehouse with businessUnitCode '" + id + "' not found.", 404);
    }
    archiveWarehouseOperation.archive(warehouse);
  }

  @Override
  @Transactional
  public com.warehouse.api.beans.Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull com.warehouse.api.beans.Warehouse data) {
    // Ensure the new warehouse carries the correct businessUnitCode from the path
    data.setBusinessUnitCode(businessUnitCode);
    var newWarehouse = toDomainWarehouse(data);
    replaceWarehouseOperation.replace(newWarehouse);
    // After replacement, the new warehouse is now active
    Warehouse created = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    if (created == null) {
      // Fallback: return the data provided
      return toWarehouseResponse(newWarehouse);
    }
    return toWarehouseResponse(created);
  }

  // ── Mappers ────────────────────────────────────────────────────────────────

  private com.warehouse.api.beans.Warehouse toWarehouseResponse(Warehouse warehouse) {
    var response = new com.warehouse.api.beans.Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }

  private Warehouse toDomainWarehouse(com.warehouse.api.beans.Warehouse apiWarehouse) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = apiWarehouse.getBusinessUnitCode();
    warehouse.location = apiWarehouse.getLocation();
    warehouse.capacity = apiWarehouse.getCapacity();
    warehouse.stock = apiWarehouse.getStock();
    return warehouse;
  }
}
