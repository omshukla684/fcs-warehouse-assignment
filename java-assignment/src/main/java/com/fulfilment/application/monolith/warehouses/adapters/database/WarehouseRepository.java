package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    // Only return active (non-archived) warehouses
    return find("archivedAt IS NULL").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    var dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = LocalDateTime.now();
    dbWarehouse.archivedAt = null;
    persist(dbWarehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse =
        find("businessUnitCode = ?1 AND archivedAt IS NULL", warehouse.businessUnitCode)
            .firstResult();
    if (dbWarehouse == null) {
      throw new IllegalArgumentException(
          "No active warehouse found with businessUnitCode: " + warehouse.businessUnitCode);
    }
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.archivedAt = warehouse.archivedAt;
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse dbWarehouse =
        find("businessUnitCode = ?1 AND archivedAt IS NULL", warehouse.businessUnitCode)
            .firstResult();
    if (dbWarehouse != null) {
      delete(dbWarehouse);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    Optional<DbWarehouse> result =
        find("businessUnitCode = ?1 AND archivedAt IS NULL", buCode).firstResultOptional();
    return result.map(DbWarehouse::toWarehouse).orElse(null);
  }

  /**
   * Returns all active warehouses at a given location (used for capacity/count validations).
   */
  public List<Warehouse> findActiveByLocation(String location) {
    return find("location = ?1 AND archivedAt IS NULL", location)
        .stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }
}
