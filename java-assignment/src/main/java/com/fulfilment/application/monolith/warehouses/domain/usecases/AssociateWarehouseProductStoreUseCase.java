package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;
import com.fulfilment.application.monolith.warehouses.domain.ports.AssociateFulfillmentOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.FulfillmentStore;
import com.fulfilment.application.monolith.warehouses.domain.validator.FulfillmentValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Use case: associate a Product as being fulfilled by a Warehouse for a given Store.
 *
 * <p>Delegates all business-constraint validation to {@link FulfillmentValidator} and all
 * persistence to {@link FulfillmentStore}, keeping this class focused on orchestrating the
 * workflow.
 *
 * <p>Constraints (see {@link WarehouseFulfillment} constants):
 * <ol>
 *   <li>Max 2 different Warehouses per Product per Store</li>
 *   <li>Max 3 different Warehouses per Store</li>
 *   <li>Max 5 different Product types per Warehouse</li>
 * </ol>
 */
@ApplicationScoped
public class AssociateWarehouseProductStoreUseCase implements AssociateFulfillmentOperation {

  private final FulfillmentStore fulfillmentStore;
  private final FulfillmentValidator fulfillmentValidator;

  public AssociateWarehouseProductStoreUseCase(
      FulfillmentStore fulfillmentStore, FulfillmentValidator fulfillmentValidator) {
    this.fulfillmentStore = fulfillmentStore;
    this.fulfillmentValidator = fulfillmentValidator;
  }

  @Override
  @Transactional
  public WarehouseFulfillment associate(
      String warehouseCode, String storeIdentifier, String productIdentifier) {

    // Load existing data
    List<WarehouseFulfillment> forStore = fulfillmentStore.findByStore(storeIdentifier);
    List<WarehouseFulfillment> forWarehouse = fulfillmentStore.findByWarehouse(warehouseCode);
    Optional<WarehouseFulfillment> existing =
        fulfillmentStore.findByWarehouseAndStore(warehouseCode, storeIdentifier);

    boolean warehouseAlreadyCarriesProduct =
        existing.map(f -> f.productIdentifiers.contains(productIdentifier)).orElse(false);

    // Constraint 1: max warehouses per store
    fulfillmentValidator.validateMaxWarehousesPerStore(warehouseCode, storeIdentifier, forStore);

    // Constraint 2: max warehouses per product per store
    fulfillmentValidator.validateMaxWarehousesPerProductPerStore(
        warehouseCode,
        storeIdentifier,
        productIdentifier,
        forStore,
        warehouseAlreadyCarriesProduct);

    // Constraint 3: max product types per warehouse
    fulfillmentValidator.validateMaxProductTypesPerWarehouse(
        warehouseCode, productIdentifier, forWarehouse, warehouseAlreadyCarriesProduct);

    // All constraints satisfied — upsert the fulfillment link
    WarehouseFulfillment fulfillment =
        existing.orElseGet(() -> new WarehouseFulfillment(warehouseCode, storeIdentifier));

    if (!fulfillment.productIdentifiers.contains(productIdentifier)) {
      fulfillment.productIdentifiers.add(productIdentifier);
    }

    return fulfillmentStore.save(fulfillment);
  }
}
