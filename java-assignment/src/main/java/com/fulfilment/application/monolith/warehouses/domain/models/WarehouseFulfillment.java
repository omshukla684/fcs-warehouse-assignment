package com.fulfilment.application.monolith.warehouses.domain.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the fulfillment association between a Warehouse, a Store and a set of Products.
 *
 * <p>Business constraints:
 * <ol>
 *   <li>Each Product can be fulfilled by a maximum of 2 different Warehouses per Store</li>
 *   <li>Each Store can be fulfilled by a maximum of 3 different Warehouses</li>
 *   <li>Each Warehouse can store a maximum of 5 types of Products</li>
 * </ol>
 */
public class WarehouseFulfillment {

  public static final int MAX_WAREHOUSES_PER_STORE = 3;
  public static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  public static final int MAX_PRODUCT_TYPES_PER_WAREHOUSE = 5;

  /** Warehouse businessUnitCode */
  public String warehouseCode;

  /** Store name or identifier */
  public String storeIdentifier;

  /** Product names/identifiers that this warehouse fulfills for this store */
  public List<String> productIdentifiers = new ArrayList<>();

  public WarehouseFulfillment() {}

  public WarehouseFulfillment(String warehouseCode, String storeIdentifier) {
    this.warehouseCode = warehouseCode;
    this.storeIdentifier = storeIdentifier;
  }
}
