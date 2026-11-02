package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouseFulfillment;
import com.fulfilment.application.monolith.warehouses.adapters.database.FulfillmentRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Associates a Product as being fulfilled by a Warehouse for a given Store, while enforcing:
 *
 * <ol>
 *   <li>Max 2 different Warehouses per Product per Store</li>
 *   <li>Max 3 different Warehouses per Store</li>
 *   <li>Max 5 different Product types per Warehouse</li>
 * </ol>
 */
@ApplicationScoped
public class AssociateWarehouseProductStoreUseCase {

  private final FulfillmentRepository fulfillmentRepository;

  public AssociateWarehouseProductStoreUseCase(FulfillmentRepository fulfillmentRepository) {
    this.fulfillmentRepository = fulfillmentRepository;
  }

  @Transactional
  public WarehouseFulfillment associate(
      String warehouseCode, String storeIdentifier, String productIdentifier) {

    // --- Constraint 2: Max 3 warehouses per store ---
    List<DbWarehouseFulfillment> warehousesForStore =
        fulfillmentRepository.findByStore(storeIdentifier);
    Set<String> distinctWarehousesInStore =
        warehousesForStore.stream()
            .map(f -> f.warehouseCode)
            .collect(Collectors.toSet());
    // If this warehouse is new to this store, check the limit
    if (!distinctWarehousesInStore.contains(warehouseCode)
        && distinctWarehousesInStore.size() >= WarehouseFulfillment.MAX_WAREHOUSES_PER_STORE) {
      throw new WebApplicationException(
          "Store '"
              + storeIdentifier
              + "' is already fulfilled by the maximum number of warehouses ("
              + WarehouseFulfillment.MAX_WAREHOUSES_PER_STORE
              + ").",
          400);
    }

    // --- Constraint 1: Max 2 warehouses per product per store ---
    long warehousesForProductInStore =
        warehousesForStore.stream()
            .filter(f -> f.productIdentifiers.contains(productIdentifier))
            .count();
    // If this warehouse doesn't yet carry this product for this store
    boolean thisWarehouseAlreadyCarriesProduct =
        fulfillmentRepository
            .findByWarehouseAndStore(warehouseCode, storeIdentifier)
            .map(f -> f.productIdentifiers.contains(productIdentifier))
            .orElse(false);
    if (!thisWarehouseAlreadyCarriesProduct
        && warehousesForProductInStore
            >= WarehouseFulfillment.MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new WebApplicationException(
          "Product '"
              + productIdentifier
              + "' is already fulfilled by the maximum number of warehouses ("
              + WarehouseFulfillment.MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + ") for store '"
              + storeIdentifier
              + "'.",
          400);
    }

    // --- Constraint 3: Max 5 product types per warehouse ---
    List<DbWarehouseFulfillment> warehouseLinks =
        fulfillmentRepository.findByWarehouse(warehouseCode);
    Set<String> allProductsInWarehouse =
        warehouseLinks.stream()
            .flatMap(f -> f.productIdentifiers.stream())
            .collect(Collectors.toSet());
    if (!allProductsInWarehouse.contains(productIdentifier)
        && allProductsInWarehouse.size() >= WarehouseFulfillment.MAX_PRODUCT_TYPES_PER_WAREHOUSE) {
      throw new WebApplicationException(
          "Warehouse '"
              + warehouseCode
              + "' already stores the maximum number of product types ("
              + WarehouseFulfillment.MAX_PRODUCT_TYPES_PER_WAREHOUSE
              + ").",
          400);
    }

    // All constraints satisfied — upsert the fulfillment link
    DbWarehouseFulfillment fulfillment =
        fulfillmentRepository
            .findByWarehouseAndStore(warehouseCode, storeIdentifier)
            .orElseGet(
                () -> {
                  var newLink = new DbWarehouseFulfillment();
                  newLink.warehouseCode = warehouseCode;
                  newLink.storeIdentifier = storeIdentifier;
                  fulfillmentRepository.persist(newLink);
                  return newLink;
                });

    if (!fulfillment.productIdentifiers.contains(productIdentifier)) {
      fulfillment.productIdentifiers.add(productIdentifier);
    }

    return fulfillmentRepository.toDomain(fulfillment);
  }
}
