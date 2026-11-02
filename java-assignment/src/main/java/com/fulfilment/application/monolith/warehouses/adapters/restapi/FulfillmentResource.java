package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;
import com.fulfilment.application.monolith.warehouses.domain.usecases.AssociateWarehouseProductStoreUseCase;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for the bonus feature: associating Warehouses as fulfillment units of Products to
 * Stores.
 *
 * <p>POST /warehouse/{warehouseCode}/store/{storeIdentifier}/product/{productIdentifier}
 */
@Path("/warehouse/{warehouseCode}/store/{storeIdentifier}/product/{productIdentifier}")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class FulfillmentResource {

  @Inject AssociateWarehouseProductStoreUseCase associateUseCase;

  /**
   * Associates a product with a warehouse-store combination.
   *
   * @param warehouseCode   business unit code of the warehouse
   * @param storeIdentifier store name / identifier
   * @param productIdentifier product name / identifier
   * @return the updated fulfillment record
   */
  @POST
  public Response associate(
      @PathParam("warehouseCode") String warehouseCode,
      @PathParam("storeIdentifier") String storeIdentifier,
      @PathParam("productIdentifier") String productIdentifier) {

    WarehouseFulfillment result =
        associateUseCase.associate(warehouseCode, storeIdentifier, productIdentifier);
    return Response.ok(result).status(201).build();
  }
}
