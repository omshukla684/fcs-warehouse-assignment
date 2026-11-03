package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;
import com.fulfilment.application.monolith.warehouses.domain.ports.FulfillmentStore;
import com.fulfilment.application.monolith.warehouses.domain.validator.FulfillmentValidator;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AssociateWarehouseProductStoreUseCaseTest {

  private FulfillmentStore fulfillmentStore;
  private AssociateWarehouseProductStoreUseCase useCase;

  @BeforeEach
  void setUp() {
    fulfillmentStore = mock(FulfillmentStore.class);
    // Real validator so constraint logic is covered by these tests
    useCase = new AssociateWarehouseProductStoreUseCase(fulfillmentStore, new FulfillmentValidator());
  }

  private WarehouseFulfillment makeLink(String wCode, String storeId, List<String> products) {
    var link = new WarehouseFulfillment(wCode, storeId);
    link.productIdentifiers = new ArrayList<>(products);
    return link;
  }

  @Test
  void testAssociateHappyPath() {
    when(fulfillmentStore.findByStore("STORE-1")).thenReturn(Collections.emptyList());
    when(fulfillmentStore.findByWarehouse("MWH.001")).thenReturn(Collections.emptyList());
    when(fulfillmentStore.findByWarehouseAndStore("MWH.001", "STORE-1"))
        .thenReturn(Optional.empty());

    var saved = makeLink("MWH.001", "STORE-1", List.of("PRODUCT-A"));
    when(fulfillmentStore.save(any())).thenReturn(saved);

    WarehouseFulfillment result = useCase.associate("MWH.001", "STORE-1", "PRODUCT-A");
    assertNotNull(result);
    verify(fulfillmentStore).save(any());
  }

  @Test
  void testAssociateFailsWhenMaxWarehousesPerStoreReached() {
    // 3 distinct warehouses for STORE-1 already exist
    var link1 = makeLink("MWH.001", "STORE-1", List.of("PRODUCT-A"));
    var link2 = makeLink("MWH.002", "STORE-1", List.of("PRODUCT-B"));
    var link3 = makeLink("MWH.003", "STORE-1", List.of("PRODUCT-C"));

    when(fulfillmentStore.findByStore("STORE-1")).thenReturn(List.of(link1, link2, link3));
    when(fulfillmentStore.findByWarehouse("MWH.NEW")).thenReturn(Collections.emptyList());
    when(fulfillmentStore.findByWarehouseAndStore("MWH.NEW", "STORE-1"))
        .thenReturn(Optional.empty());

    // MWH.NEW is NOT in the store yet, 3 warehouses already there — limit hit
    assertThrows(
        WebApplicationException.class,
        () -> useCase.associate("MWH.NEW", "STORE-1", "PRODUCT-D"));
    verify(fulfillmentStore, never()).save(any());
  }

  @Test
  void testAssociateFailsWhenMaxWarehousesPerProductPerStoreReached() {
    // PRODUCT-A already served by 2 warehouses in STORE-1
    var link1 = makeLink("MWH.001", "STORE-1", List.of("PRODUCT-A"));
    var link2 = makeLink("MWH.002", "STORE-1", List.of("PRODUCT-A"));

    when(fulfillmentStore.findByStore("STORE-1")).thenReturn(List.of(link1, link2));
    when(fulfillmentStore.findByWarehouseAndStore("MWH.NEW", "STORE-1"))
        .thenReturn(Optional.empty());
    when(fulfillmentStore.findByWarehouse("MWH.NEW")).thenReturn(Collections.emptyList());

    // Adding a 3rd warehouse to serve PRODUCT-A in STORE-1 should fail
    assertThrows(
        WebApplicationException.class,
        () -> useCase.associate("MWH.NEW", "STORE-1", "PRODUCT-A"));
    verify(fulfillmentStore, never()).save(any());
  }

  @Test
  void testAssociateFailsWhenMaxProductTypesPerWarehouseReached() {
    // MWH.001 already has 5 product types across all stores
    var link1 = makeLink("MWH.001", "STORE-1", List.of("P1", "P2"));
    var link2 = makeLink("MWH.001", "STORE-2", List.of("P3", "P4", "P5"));

    when(fulfillmentStore.findByStore("STORE-3")).thenReturn(Collections.emptyList());
    when(fulfillmentStore.findByWarehouse("MWH.001")).thenReturn(List.of(link1, link2));
    when(fulfillmentStore.findByWarehouseAndStore("MWH.001", "STORE-3"))
        .thenReturn(Optional.empty());

    // 5 distinct products already; adding P6 should fail
    assertThrows(
        WebApplicationException.class,
        () -> useCase.associate("MWH.001", "STORE-3", "P6"));
    verify(fulfillmentStore, never()).save(any());
  }

  @Test
  void testAssociateNewWarehouseToStoreWithExistingWarehousesUnderLimit() {
    // 2 warehouses already in STORE-1, adding a 3rd should be allowed
    var link1 = makeLink("MWH.001", "STORE-1", List.of("PRODUCT-A"));
    var link2 = makeLink("MWH.002", "STORE-1", List.of("PRODUCT-B"));

    when(fulfillmentStore.findByStore("STORE-1")).thenReturn(List.of(link1, link2));
    when(fulfillmentStore.findByWarehouse("MWH.003")).thenReturn(Collections.emptyList());
    when(fulfillmentStore.findByWarehouseAndStore("MWH.003", "STORE-1"))
        .thenReturn(Optional.empty());

    var saved = makeLink("MWH.003", "STORE-1", List.of("PRODUCT-C"));
    when(fulfillmentStore.save(any())).thenReturn(saved);

    // Should not throw — 3rd warehouse is still within limit
    WarehouseFulfillment result = useCase.associate("MWH.003", "STORE-1", "PRODUCT-C");
    assertNotNull(result);
    verify(fulfillmentStore).save(any());
  }

  @Test
  void testAssociateIdempotentWhenProductAlreadyAssociated() {
    // MWH.001 already carries PRODUCT-A for STORE-1
    var existing = makeLink("MWH.001", "STORE-1", List.of("PRODUCT-A"));
    when(fulfillmentStore.findByStore("STORE-1")).thenReturn(List.of(existing));
    when(fulfillmentStore.findByWarehouse("MWH.001")).thenReturn(List.of(existing));
    when(fulfillmentStore.findByWarehouseAndStore("MWH.001", "STORE-1"))
        .thenReturn(Optional.of(existing));
    when(fulfillmentStore.save(any())).thenReturn(existing);

    // Re-associating same product should succeed (idempotent)
    WarehouseFulfillment result = useCase.associate("MWH.001", "STORE-1", "PRODUCT-A");
    assertNotNull(result);
    verify(fulfillmentStore).save(any());
  }
}
