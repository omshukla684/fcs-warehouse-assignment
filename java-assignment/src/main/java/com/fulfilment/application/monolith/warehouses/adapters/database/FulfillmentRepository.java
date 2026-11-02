package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class FulfillmentRepository implements PanacheRepository<DbWarehouseFulfillment> {

  public Optional<DbWarehouseFulfillment> findByWarehouseAndStore(
      String warehouseCode, String storeIdentifier) {
    return find(
            "warehouseCode = ?1 AND storeIdentifier = ?2", warehouseCode, storeIdentifier)
        .firstResultOptional();
  }

  public List<DbWarehouseFulfillment> findByStore(String storeIdentifier) {
    return find("storeIdentifier = ?1", storeIdentifier).list();
  }

  public List<DbWarehouseFulfillment> findByWarehouse(String warehouseCode) {
    return find("warehouseCode = ?1", warehouseCode).list();
  }

  public WarehouseFulfillment toDomain(DbWarehouseFulfillment db) {
    var model = new WarehouseFulfillment(db.warehouseCode, db.storeIdentifier);
    model.productIdentifiers.addAll(db.productIdentifiers);
    return model;
  }
}
