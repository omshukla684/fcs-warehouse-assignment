package com.fulfilment.application.monolith.warehouses.fulfillment.event;

import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;

/**
 * Domain event fired whenever a new fulfillment association is successfully created.
 *
 * <p>Consumers can observe this event (e.g. with {@code @Observes}) to react after the
 * originating transaction has committed.
 *
 * @param fulfillment the fulfillment record that was created
 */
public record FulfillmentCreatedEvent(WarehouseFulfillment fulfillment) {}
