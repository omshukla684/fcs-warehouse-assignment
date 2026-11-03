package com.fulfilment.application.monolith.warehouses.domain.ports;

/**
 * Output port: fulfillment-specific association operation.
 */
public interface AssociateFulfillmentOperation {

  /**
   * Associates a product as being fulfilled by a warehouse for a given store.
   *
   * @param warehouseCode     the warehouse business-unit code
   * @param storeIdentifier   the store name/identifier
   * @param productIdentifier the product name/identifier
   * @return the updated (or newly created) fulfillment record
   */
  com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment associate(
      String warehouseCode, String storeIdentifier, String productIdentifier);
}
