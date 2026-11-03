package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;
import com.fulfilment.application.monolith.warehouses.domain.ports.AssociateFulfillmentOperation;
import com.fulfilment.application.monolith.warehouses.fulfillment.event.FulfillmentCreatedEvent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST adapter (inbound) for the fulfillment association feature.
 *
 * <p>Endpoint: {@code POST /warehouse/{warehouseCode}/store/{storeIdentifier}/product/{productIdentifier}}
 *
 * <p>Fires a {@link FulfillmentCreatedEvent} after the use case completes, enabling decoupled
 * downstream reactions (e.g. notifications, audit logs).
 */
@Path("/warehouse/{warehouseCode}/store/{storeIdentifier}/product/{productIdentifier}")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FulfillmentResource {

  @Inject AssociateFulfillmentOperation associateFulfillmentOperation;

  @Inject Event<FulfillmentCreatedEvent> fulfillmentCreatedEvent;

  /**
   * Associates a product with a warehouse-store combination.
   *
   * @param warehouseCode     business unit code of the warehouse
   * @param storeIdentifier   store name / identifier
   * @param productIdentifier product name / identifier
   * @return the updated fulfillment record (HTTP 201)
   */
  @POST
  public Response associate(
      @PathParam("warehouseCode") String warehouseCode,
      @PathParam("storeIdentifier") String storeIdentifier,
      @PathParam("productIdentifier") String productIdentifier) {

    WarehouseFulfillment result =
        associateFulfillmentOperation.associate(warehouseCode, storeIdentifier, productIdentifier);

    // Fire domain event – observers may react after the transaction commits
    fulfillmentCreatedEvent.fire(new FulfillmentCreatedEvent(result));

    return Response.status(201).entity(result).build();
  }
}
