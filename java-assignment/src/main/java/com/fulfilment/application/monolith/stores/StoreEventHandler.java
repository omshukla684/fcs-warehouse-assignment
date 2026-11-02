package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

/**
 * Handles {@link StoreEvent} CDI events after the transaction has successfully committed.
 *
 * <p>This ensures the legacy system integration is only triggered once the data is durably stored
 * in our database, preventing race conditions or ghost notifications.
 */
@ApplicationScoped
public class StoreEventHandler {

  private final LegacyStoreManagerGateway legacyStoreManagerGateway;

  public StoreEventHandler(LegacyStoreManagerGateway legacyStoreManagerGateway) {
    this.legacyStoreManagerGateway = legacyStoreManagerGateway;
  }

  /**
   * Invoked after the JTA transaction successfully commits. Uses
   * {@link TransactionPhase#AFTER_SUCCESS} to guarantee the DB change is durable before
   * notifying the legacy system.
   */
  public void onStoreCreated(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreCreatedEvent event) {
    legacyStoreManagerGateway.createStoreOnLegacySystem(event.store());
  }

  public void onStoreUpdated(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreUpdatedEvent event) {
    legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store());
  }
}
