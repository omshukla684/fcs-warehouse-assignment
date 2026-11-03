package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;
import com.fulfilment.application.monolith.warehouses.domain.ports.FulfillmentStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Database adapter (infrastructure layer) for {@link FulfillmentStore}.
 *
 * <p>Translates between the JPA entity ({@link DbWarehouseFulfillment}) and the domain model
 * ({@link WarehouseFulfillment}), implementing the outbound port defined in the domain layer.
 */
@ApplicationScoped
public class FulfillmentRepository
    implements FulfillmentStore, PanacheRepository<DbWarehouseFulfillment> {

  // ── FulfillmentStore port implementation ─────────────────────────────────

  @Override
  public Optional<WarehouseFulfillment> findByWarehouseAndStore(
      String warehouseCode, String storeIdentifier) {
    return find("warehouseCode = ?1 AND storeIdentifier = ?2", warehouseCode, storeIdentifier)
        .firstResultOptional()
        .map(this::toDomain);
  }

  @Override
  public List<WarehouseFulfillment> findByStore(String storeIdentifier) {
    return find("storeIdentifier = ?1", storeIdentifier).stream()
        .map(this::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<WarehouseFulfillment> findByWarehouse(String warehouseCode) {
    return find("warehouseCode = ?1", warehouseCode).stream()
        .map(this::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public WarehouseFulfillment save(WarehouseFulfillment fulfillment) {
    Optional<DbWarehouseFulfillment> existing =
        find(
                "warehouseCode = ?1 AND storeIdentifier = ?2",
                fulfillment.warehouseCode,
                fulfillment.storeIdentifier)
            .firstResultOptional();

    DbWarehouseFulfillment dbEntity =
        existing.orElseGet(
            () -> {
              var newEntity = new DbWarehouseFulfillment();
              newEntity.warehouseCode = fulfillment.warehouseCode;
              newEntity.storeIdentifier = fulfillment.storeIdentifier;
              persist(newEntity);
              return newEntity;
            });

    // Sync product identifiers
    for (String productId : fulfillment.productIdentifiers) {
      if (!dbEntity.productIdentifiers.contains(productId)) {
        dbEntity.productIdentifiers.add(productId);
      }
    }

    return toDomain(dbEntity);
  }

  // ── Mapper ────────────────────────────────────────────────────────────────

  public WarehouseFulfillment toDomain(DbWarehouseFulfillment db) {
    var model = new WarehouseFulfillment(db.warehouseCode, db.storeIdentifier);
    model.productIdentifiers.addAll(db.productIdentifiers);
    return model;
  }
}
