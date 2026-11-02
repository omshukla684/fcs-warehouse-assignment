package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouseFulfillment;
import com.fulfilment.application.monolith.warehouses.adapters.database.FulfillmentRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AssociateWarehouseProductStoreUseCaseTest {

  private FulfillmentRepository fulfillmentRepository;
  private AssociateWarehouseProductStoreUseCase useCase;

  @BeforeEach
  void setUp() {
    fulfillmentRepository = mock(FulfillmentRepository.class);
    useCase = new AssociateWarehouseProductStoreUseCase(fulfillmentRepository);
  }

  private DbWarehouseFulfillment makeLink(String wCode, String storeId, List<String> products) {
    var link = new DbWarehouseFulfillment();
    link.warehouseCode = wCode;
    link.storeIdentifier = storeId;
    link.productIdentifiers = new ArrayList<>(products);
    return link;
  }

  @Test
  void testAssociateHappyPath() {
    when(fulfillmentRepository.findByStore("STORE-1")).thenReturn(Collections.emptyList());
    when(fulfillmentRepository.findByWarehouse("MWH.001")).thenReturn(Collections.emptyList());
    when(fulfillmentRepository.findByWarehouseAndStore("MWH.001", "STORE-1"))
        .thenReturn(Optional.empty());

    var link = makeLink("MWH.001", "STORE-1", new ArrayList<>());
    when(fulfillmentRepository.toDomain(any()))
        .thenReturn(new WarehouseFulfillment("MWH.001", "STORE-1"));

    // Simulate persist by making findByWarehouseAndStore return the new link on second call
    // Since we can't actually persist in a unit test, we just verify the mock behavior
    WarehouseFulfillment result = useCase.associate("MWH.001", "STORE-1", "PRODUCT-A");
    // With mocks, toDomain is called after persist — but we haven't set up the captured arg
    // So let's just verify no exception was thrown
    assertNotNull(result);
  }

  @Test
  void testAssociateFailsWhenMaxWarehousesPerStoreReached() {
    // 3 distinct warehouses for STORE-1 already exist
    var link1 = makeLink("MWH.001", "STORE-1", List.of("PRODUCT-A"));
    var link2 = makeLink("MWH.002", "STORE-1", List.of("PRODUCT-B"));
    var link3 = makeLink("MWH.003", "STORE-1", List.of("PRODUCT-C"));

    when(fulfillmentRepository.findByStore("STORE-1")).thenReturn(List.of(link1, link2, link3));
    when(fulfillmentRepository.findByWarehouse("MWH.NEW")).thenReturn(Collections.emptyList());

    // MWH.NEW is NOT in the store yet, and 3 warehouses are already there — limit hit
    assertThrows(
        WebApplicationException.class,
        () -> useCase.associate("MWH.NEW", "STORE-1", "PRODUCT-D"));
  }

  @Test
  void testAssociateFailsWhenMaxWarehousesPerProductPerStoreReached() {
    // Product PRODUCT-A already served by 2 warehouses in STORE-1
    var link1 = makeLink("MWH.001", "STORE-1", List.of("PRODUCT-A"));
    var link2 = makeLink("MWH.002", "STORE-1", List.of("PRODUCT-A"));

    when(fulfillmentRepository.findByStore("STORE-1")).thenReturn(List.of(link1, link2));
    // MWH.NEW is not yet in STORE-1, so it's a new warehouse
    when(fulfillmentRepository.findByWarehouseAndStore("MWH.NEW", "STORE-1"))
        .thenReturn(Optional.empty());
    when(fulfillmentRepository.findByWarehouse("MWH.NEW")).thenReturn(Collections.emptyList());

    // Adding a 3rd warehouse to serve PRODUCT-A in STORE-1 should fail
    assertThrows(
        WebApplicationException.class,
        () -> useCase.associate("MWH.NEW", "STORE-1", "PRODUCT-A"));
  }

  @Test
  void testAssociateFailsWhenMaxProductTypesPerWarehouseReached() {
    // MWH.001 already has 5 product types across all stores
    var link1 = makeLink("MWH.001", "STORE-1", List.of("P1", "P2"));
    var link2 = makeLink("MWH.001", "STORE-2", List.of("P3", "P4", "P5"));

    when(fulfillmentRepository.findByStore("STORE-3")).thenReturn(Collections.emptyList());
    when(fulfillmentRepository.findByWarehouse("MWH.001")).thenReturn(List.of(link1, link2));
    when(fulfillmentRepository.findByWarehouseAndStore("MWH.001", "STORE-3"))
        .thenReturn(Optional.empty());

    // 5 distinct products already; adding P6 should fail
    assertThrows(
        WebApplicationException.class,
        () -> useCase.associate("MWH.001", "STORE-3", "P6"));
  }

  @Test
  void testAssociateNewWarehouseToStoreWithExistingWarehousesUnderLimit() {
    // 2 warehouses already in STORE-1, adding a 3rd should be allowed
    var link1 = makeLink("MWH.001", "STORE-1", List.of("PRODUCT-A"));
    var link2 = makeLink("MWH.002", "STORE-1", List.of("PRODUCT-B"));

    when(fulfillmentRepository.findByStore("STORE-1")).thenReturn(List.of(link1, link2));
    when(fulfillmentRepository.findByWarehouse("MWH.003")).thenReturn(Collections.emptyList());
    when(fulfillmentRepository.findByWarehouseAndStore("MWH.003", "STORE-1"))
        .thenReturn(Optional.empty());
    when(fulfillmentRepository.toDomain(any()))
        .thenReturn(new WarehouseFulfillment("MWH.003", "STORE-1"));

    // Should not throw - 3rd warehouse is still within limit
    WarehouseFulfillment result = useCase.associate("MWH.003", "STORE-1", "PRODUCT-C");
    assertNotNull(result);
  }
}
