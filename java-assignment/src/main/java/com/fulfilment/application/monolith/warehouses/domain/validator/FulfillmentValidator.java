package com.fulfilment.application.monolith.warehouses.domain.validator;

import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain validator for {@link WarehouseFulfillment} business constraints.
 *
 * <p>Validation logic is extracted here following the Single Responsibility Principle and the
 * Hexagonal Architecture pattern, keeping use cases clean and testable.
 *
 * <p>Constraints enforced:
 * <ol>
 *   <li>Max {@value WarehouseFulfillment#MAX_WAREHOUSES_PER_STORE} warehouses per store.</li>
 *   <li>Max {@value WarehouseFulfillment#MAX_WAREHOUSES_PER_PRODUCT_PER_STORE} warehouses
 *       per product per store.</li>
 *   <li>Max {@value WarehouseFulfillment#MAX_PRODUCT_TYPES_PER_WAREHOUSE} product types
 *       per warehouse.</li>
 * </ol>
 */
@ApplicationScoped
public class FulfillmentValidator {

  /**
   * Validates that adding the warehouse to the store does not exceed the per-store warehouse limit.
   *
   * @param warehouseCode     the warehouse being added
   * @param storeIdentifier   the target store
   * @param existingForStore  all current fulfillment records for this store
   * @throws WebApplicationException HTTP 400 when the limit is exceeded
   */
  public void validateMaxWarehousesPerStore(
      String warehouseCode,
      String storeIdentifier,
      List<WarehouseFulfillment> existingForStore) {

    Set<String> distinctWarehouses =
        existingForStore.stream()
            .map(f -> f.warehouseCode)
            .collect(Collectors.toSet());

    if (!distinctWarehouses.contains(warehouseCode)
        && distinctWarehouses.size() >= WarehouseFulfillment.MAX_WAREHOUSES_PER_STORE) {
      throw new WebApplicationException(
          "Store '"
              + storeIdentifier
              + "' is already fulfilled by the maximum number of warehouses ("
              + WarehouseFulfillment.MAX_WAREHOUSES_PER_STORE
              + ").",
          400);
    }
  }

  /**
   * Validates that adding a product to a warehouse-store combination does not violate the limit of
   * warehouses per product per store.
   *
   * @param warehouseCode                    the warehouse being added
   * @param storeIdentifier                  the target store
   * @param productIdentifier                the product being associated
   * @param existingForStore                 all current fulfillment records for this store
   * @param warehouseAlreadyCarriesProduct   true if the warehouse already carries this product
   * @throws WebApplicationException HTTP 400 when the limit is exceeded
   */
  public void validateMaxWarehousesPerProductPerStore(
      String warehouseCode,
      String storeIdentifier,
      String productIdentifier,
      List<WarehouseFulfillment> existingForStore,
      boolean warehouseAlreadyCarriesProduct) {

    long warehousesForProductInStore =
        existingForStore.stream()
            .filter(f -> f.productIdentifiers.contains(productIdentifier))
            .count();

    if (!warehouseAlreadyCarriesProduct
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
  }

  /**
   * Validates that adding a product type to the warehouse does not exceed the per-warehouse product
   * type limit.
   *
   * @param warehouseCode               the target warehouse
   * @param productIdentifier           the product being added
   * @param existingForWarehouse        all current fulfillment records for this warehouse
   * @param warehouseAlreadyCarriesProduct true if the warehouse already carries this product
   * @throws WebApplicationException HTTP 400 when the limit is exceeded
   */
  public void validateMaxProductTypesPerWarehouse(
      String warehouseCode,
      String productIdentifier,
      List<WarehouseFulfillment> existingForWarehouse,
      boolean warehouseAlreadyCarriesProduct) {

    Set<String> allProductsInWarehouse =
        existingForWarehouse.stream()
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
  }
}
