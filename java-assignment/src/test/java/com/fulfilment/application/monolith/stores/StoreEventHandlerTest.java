package com.fulfilment.application.monolith.stores;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StoreEventHandler}.
 *
 * <p>Verifies that the legacy gateway methods are invoked with the correct store data when CDI
 * events are dispatched (simulating the AFTER_SUCCESS transaction phase in a unit-test context
 * by calling the observer methods directly).
 */
public class StoreEventHandlerTest {

  private LegacyStoreManagerGateway legacyGateway;
  private StoreEventHandler handler;

  @BeforeEach
  void setUp() {
    legacyGateway = mock(LegacyStoreManagerGateway.class);
    handler = new StoreEventHandler(legacyGateway);
  }

  @Test
  void onStoreCreated_callsLegacyGatewayCreate() {
    var store = new Store();
    store.name = "Test Store";
    store.quantityProductsInStock = 10;

    handler.onStoreCreated(new StoreCreatedEvent(store));

    verify(legacyGateway).createStoreOnLegacySystem(store);
  }

  @Test
  void onStoreUpdated_callsLegacyGatewayUpdate() {
    var store = new Store();
    store.name = "Updated Store";
    store.quantityProductsInStock = 20;

    handler.onStoreUpdated(new StoreUpdatedEvent(store));

    verify(legacyGateway).updateStoreOnLegacySystem(store);
  }
}
