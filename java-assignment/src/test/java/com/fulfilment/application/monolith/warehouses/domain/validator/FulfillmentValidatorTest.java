package com.fulfilment.application.monolith.warehouses.domain.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseFulfillment;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FulfillmentValidator}.
 */
public class FulfillmentValidatorTest {

  private FulfillmentValidator validator;

  @BeforeEach
  void setUp() {
    validator = new FulfillmentValidator();
  }

  private WarehouseFulfillment link(String wCode, String store, List<String> products) {
    var f = new WarehouseFulfillment(wCode, store);
    f.productIdentifiers.addAll(products);
    return f;
  }

  // ── validateMaxWarehousesPerStore ────────────────────────────────────────

  @Test
  void maxWarehousesPerStore_throwsWhenLimitReachedAndWarehouseIsNew() {
    var l1 = link("MWH.001", "S1", List.of("P1"));
    var l2 = link("MWH.002", "S1", List.of("P2"));
    var l3 = link("MWH.003", "S1", List.of("P3"));
    assertThrows(
        WebApplicationException.class,
        () -> validator.validateMaxWarehousesPerStore("MWH.NEW", "S1", List.of(l1, l2, l3)));
  }

  @Test
  void maxWarehousesPerStore_passesWhenWarehouseAlreadyInStore() {
    // MWH.001 is already one of the 3, so adding it again is allowed
    var l1 = link("MWH.001", "S1", List.of("P1"));
    var l2 = link("MWH.002", "S1", List.of("P2"));
    var l3 = link("MWH.003", "S1", List.of("P3"));
    assertDoesNotThrow(
        () -> validator.validateMaxWarehousesPerStore("MWH.001", "S1", List.of(l1, l2, l3)));
  }

  @Test
  void maxWarehousesPerStore_passesWhenUnderLimit() {
    var l1 = link("MWH.001", "S1", List.of("P1"));
    var l2 = link("MWH.002", "S1", List.of("P2"));
    assertDoesNotThrow(
        () -> validator.validateMaxWarehousesPerStore("MWH.NEW", "S1", List.of(l1, l2)));
  }

  @Test
  void maxWarehousesPerStore_passesWhenEmpty() {
    assertDoesNotThrow(
        () -> validator.validateMaxWarehousesPerStore("MWH.NEW", "S1", Collections.emptyList()));
  }

  // ── validateMaxWarehousesPerProductPerStore ───────────────────────────────

  @Test
  void maxWarehousesPerProductPerStore_throwsWhenLimitReachedAndWarehouseNew() {
    var l1 = link("MWH.001", "S1", List.of("PA"));
    var l2 = link("MWH.002", "S1", List.of("PA"));
    assertThrows(
        WebApplicationException.class,
        () ->
            validator.validateMaxWarehousesPerProductPerStore(
                "MWH.NEW", "S1", "PA", List.of(l1, l2), false));
  }

  @Test
  void maxWarehousesPerProductPerStore_passesWhenWarehouseAlreadyCarriesProduct() {
    // Even with 2 links for PA, if MWH.001 already carries it, no violation
    var l1 = link("MWH.001", "S1", List.of("PA"));
    var l2 = link("MWH.002", "S1", List.of("PA"));
    assertDoesNotThrow(
        () ->
            validator.validateMaxWarehousesPerProductPerStore(
                "MWH.001", "S1", "PA", List.of(l1, l2), true));
  }

  @Test
  void maxWarehousesPerProductPerStore_passesWhenUnderLimit() {
    var l1 = link("MWH.001", "S1", List.of("PA"));
    assertDoesNotThrow(
        () ->
            validator.validateMaxWarehousesPerProductPerStore(
                "MWH.NEW", "S1", "PA", List.of(l1), false));
  }

  // ── validateMaxProductTypesPerWarehouse ───────────────────────────────────

  @Test
  void maxProductTypesPerWarehouse_throwsWhenAtLimitAndNewProduct() {
    var l1 = link("MWH.001", "S1", List.of("P1", "P2"));
    var l2 = link("MWH.001", "S2", List.of("P3", "P4", "P5"));
    assertThrows(
        WebApplicationException.class,
        () ->
            validator.validateMaxProductTypesPerWarehouse(
                "MWH.001", "P6", List.of(l1, l2), false));
  }

  @Test
  void maxProductTypesPerWarehouse_passesWhenProductAlreadyInWarehouse() {
    var l1 = link("MWH.001", "S1", List.of("P1", "P2"));
    var l2 = link("MWH.001", "S2", List.of("P3", "P4", "P5"));
    // P1 already in warehouse — no violation
    assertDoesNotThrow(
        () ->
            validator.validateMaxProductTypesPerWarehouse(
                "MWH.001", "P1", List.of(l1, l2), true));
  }

  @Test
  void maxProductTypesPerWarehouse_passesWhenUnderLimit() {
    var l1 = link("MWH.001", "S1", List.of("P1", "P2", "P3", "P4"));
    // 4 products, adding P5 → still within limit of 5
    assertDoesNotThrow(
        () ->
            validator.validateMaxProductTypesPerWarehouse("MWH.001", "P5", List.of(l1), false));
  }

  @Test
  void maxProductTypesPerWarehouse_passesWhenEmpty() {
    assertDoesNotThrow(
        () ->
            validator.validateMaxProductTypesPerWarehouse(
                "MWH.001", "P1", Collections.emptyList(), false));
  }
}
