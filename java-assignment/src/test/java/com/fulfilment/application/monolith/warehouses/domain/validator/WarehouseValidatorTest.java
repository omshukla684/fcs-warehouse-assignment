package com.fulfilment.application.monolith.warehouses.domain.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WarehouseValidator} — ensures each validation rule is independently
 * exercised, boosting coverage of the validator class.
 */
public class WarehouseValidatorTest {

  private WarehouseValidator validator;

  @BeforeEach
  void setUp() {
    validator = new WarehouseValidator();
  }

  // ── validateUniqueBusinessUnitCode ────────────────────────────────────────

  @Test
  void validateUniqueBusinessUnitCode_throwsWhenDuplicate() {
    var existing = new Warehouse();
    existing.businessUnitCode = "MWH.001";
    assertThrows(
        WebApplicationException.class,
        () -> validator.validateUniqueBusinessUnitCode("MWH.001", existing));
  }

  @Test
  void validateUniqueBusinessUnitCode_passesWhenNullExisting() {
    assertDoesNotThrow(() -> validator.validateUniqueBusinessUnitCode("MWH.NEW", null));
  }

  // ── validateLocationExists ────────────────────────────────────────────────

  @Test
  void validateLocationExists_throwsWhenNull() {
    assertThrows(
        WebApplicationException.class,
        () -> validator.validateLocationExists("INVALID-LOC", null));
  }

  @Test
  void validateLocationExists_passesWhenPresent() {
    var location = new Location("AMSTERDAM-001", 5, 100);
    assertDoesNotThrow(() -> validator.validateLocationExists("AMSTERDAM-001", location));
  }

  // ── validateWarehouseCountAtLocation ──────────────────────────────────────

  @Test
  void validateWarehouseCountAtLocation_throwsWhenAtMax() {
    var location = new Location("ZWOLLE-001", 1, 40);
    var w = new Warehouse();
    w.capacity = 30;
    assertThrows(
        WebApplicationException.class,
        () -> validator.validateWarehouseCountAtLocation(location, List.of(w)));
  }

  @Test
  void validateWarehouseCountAtLocation_passesWhenBelowMax() {
    var location = new Location("ZWOLLE-001", 2, 80);
    var w = new Warehouse();
    w.capacity = 30;
    assertDoesNotThrow(() -> validator.validateWarehouseCountAtLocation(location, List.of(w)));
  }

  @Test
  void validateWarehouseCountAtLocation_passesWhenEmpty() {
    var location = new Location("ZWOLLE-001", 1, 40);
    assertDoesNotThrow(
        () -> validator.validateWarehouseCountAtLocation(location, Collections.emptyList()));
  }

  // ── validateTotalCapacityAtLocation ───────────────────────────────────────

  @Test
  void validateTotalCapacityAtLocation_throwsWhenExceeded() {
    var location = new Location("AMSTERDAM-001", 5, 100);
    var w1 = new Warehouse();
    w1.capacity = 60;
    var w2 = new Warehouse();
    w2.capacity = 30;
    // 90 + 20 = 110 > 100
    assertThrows(
        WebApplicationException.class,
        () -> validator.validateTotalCapacityAtLocation(location, List.of(w1, w2), 20));
  }

  @Test
  void validateTotalCapacityAtLocation_passesWhenExact() {
    var location = new Location("AMSTERDAM-001", 5, 100);
    var w1 = new Warehouse();
    w1.capacity = 60;
    // 60 + 40 = 100 == max → should pass
    assertDoesNotThrow(
        () -> validator.validateTotalCapacityAtLocation(location, List.of(w1), 40));
  }

  @Test
  void validateTotalCapacityAtLocation_passesWhenEmpty() {
    var location = new Location("AMSTERDAM-001", 5, 100);
    assertDoesNotThrow(
        () -> validator.validateTotalCapacityAtLocation(location, Collections.emptyList(), 50));
  }

  // ── validateStockWithinCapacity ───────────────────────────────────────────

  @Test
  void validateStockWithinCapacity_throwsWhenStockExceedsCapacity() {
    var w = new Warehouse();
    w.capacity = 20;
    w.stock = 25;
    assertThrows(WebApplicationException.class, () -> validator.validateStockWithinCapacity(w));
  }

  @Test
  void validateStockWithinCapacity_passesWhenEqual() {
    var w = new Warehouse();
    w.capacity = 20;
    w.stock = 20;
    assertDoesNotThrow(() -> validator.validateStockWithinCapacity(w));
  }

  @Test
  void validateStockWithinCapacity_passesWhenStockNull() {
    var w = new Warehouse();
    w.capacity = 20;
    w.stock = null;
    assertDoesNotThrow(() -> validator.validateStockWithinCapacity(w));
  }

  @Test
  void validateStockWithinCapacity_passesWhenCapacityNull() {
    var w = new Warehouse();
    w.capacity = null;
    w.stock = 20;
    assertDoesNotThrow(() -> validator.validateStockWithinCapacity(w));
  }

  // ── validateCapacityAccommodatesStock ─────────────────────────────────────

  @Test
  void validateCapacityAccommodatesStock_throwsWhenCapacityNull() {
    assertThrows(
        WebApplicationException.class,
        () -> validator.validateCapacityAccommodatesStock(null, 50));
  }

  @Test
  void validateCapacityAccommodatesStock_throwsWhenCapacityLessThanStock() {
    assertThrows(
        WebApplicationException.class,
        () -> validator.validateCapacityAccommodatesStock(30, 50));
  }

  @Test
  void validateCapacityAccommodatesStock_passesWhenCapacityEqualsStock() {
    assertDoesNotThrow(() -> validator.validateCapacityAccommodatesStock(50, 50));
  }

  @Test
  void validateCapacityAccommodatesStock_passesWhenCapacityGreater() {
    assertDoesNotThrow(() -> validator.validateCapacityAccommodatesStock(100, 50));
  }

  // ── validateStockMatchesOnReplacement ─────────────────────────────────────

  @Test
  void validateStockMatchesOnReplacement_throwsWhenNull() {
    assertThrows(
        WebApplicationException.class,
        () -> validator.validateStockMatchesOnReplacement(null, 50));
  }

  @Test
  void validateStockMatchesOnReplacement_throwsWhenDifferent() {
    assertThrows(
        WebApplicationException.class,
        () -> validator.validateStockMatchesOnReplacement(30, 50));
  }

  @Test
  void validateStockMatchesOnReplacement_passesWhenEqual() {
    assertDoesNotThrow(() -> validator.validateStockMatchesOnReplacement(50, 50));
  }
}
