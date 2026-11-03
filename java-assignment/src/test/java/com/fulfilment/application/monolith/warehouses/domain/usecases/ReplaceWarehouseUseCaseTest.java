package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validator.WarehouseValidator;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseOperation archiveWarehouseOperation;
  private LocationResolver locationResolver;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    archiveWarehouseOperation = mock(ArchiveWarehouseOperation.class);
    locationResolver = mock(LocationResolver.class);
    useCase = new ReplaceWarehouseUseCase(warehouseStore, archiveWarehouseOperation, locationResolver, new WarehouseValidator());
  }

  private Warehouse buildWarehouse(String code, String location, int capacity, int stock) {
    var w = new Warehouse();
    w.businessUnitCode = code;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }

  @Test
  void testReplaceHappyPath() {
    var current = buildWarehouse("MWH.001", "ZWOLLE-001", 100, 50);
    var newWarehouse = buildWarehouse("MWH.001", "ZWOLLE-001", 120, 50);
    var location = new Location("ZWOLLE-001", 1, 200);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);

    useCase.replace(newWarehouse);

    verify(archiveWarehouseOperation).archive(current);
    verify(warehouseStore).create(newWarehouse);
  }

  @Test
  void testReplaceFailsWhenWarehouseNotFound() {
    when(warehouseStore.findByBusinessUnitCode("MWH.MISSING")).thenReturn(null);

    var newWarehouse = buildWarehouse("MWH.MISSING", "AMSTERDAM-001", 100, 50);

    assertThrows(WebApplicationException.class, () -> useCase.replace(newWarehouse));
    verify(archiveWarehouseOperation, never()).archive(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testReplaceFailsWhenInvalidLocation() {
    var current = buildWarehouse("MWH.001", "ZWOLLE-001", 100, 50);
    var newWarehouse = buildWarehouse("MWH.001", "INVALID-001", 120, 50);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current);
    when(locationResolver.resolveByIdentifier("INVALID-001")).thenReturn(null);

    assertThrows(WebApplicationException.class, () -> useCase.replace(newWarehouse));
    verify(archiveWarehouseOperation, never()).archive(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testReplaceFailsWhenCapacityLessThanCurrentStock() {
    // Current stock is 50, new capacity is 30 — cannot accommodate
    var current = buildWarehouse("MWH.001", "ZWOLLE-001", 100, 50);
    var newWarehouse = buildWarehouse("MWH.001", "ZWOLLE-001", 30, 50);
    var location = new Location("ZWOLLE-001", 1, 200);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);

    assertThrows(WebApplicationException.class, () -> useCase.replace(newWarehouse));
    verify(archiveWarehouseOperation, never()).archive(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testReplaceFailsWhenStockDoesNotMatchCurrentWarehouseStock() {
    // New warehouse stock (30) must equal old warehouse stock (50)
    var current = buildWarehouse("MWH.001", "ZWOLLE-001", 100, 50);
    var newWarehouse = buildWarehouse("MWH.001", "ZWOLLE-001", 120, 30);
    var location = new Location("ZWOLLE-001", 1, 200);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);

    assertThrows(WebApplicationException.class, () -> useCase.replace(newWarehouse));
    verify(archiveWarehouseOperation, never()).archive(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testReplaceSucceedsWithExactCapacityMatchingStock() {
    // Capacity exactly equals current stock
    var current = buildWarehouse("MWH.001", "ZWOLLE-001", 100, 50);
    var newWarehouse = buildWarehouse("MWH.001", "ZWOLLE-001", 50, 50);
    var location = new Location("ZWOLLE-001", 1, 200);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);

    useCase.replace(newWarehouse);

    verify(archiveWarehouseOperation).archive(current);
    verify(warehouseStore).create(newWarehouse);
  }
}
