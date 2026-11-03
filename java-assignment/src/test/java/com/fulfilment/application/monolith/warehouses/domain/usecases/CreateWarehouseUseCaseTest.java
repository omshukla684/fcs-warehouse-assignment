package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validator.WarehouseValidator;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    // Use real validator so validation logic is also exercised in these tests
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver, new WarehouseValidator());
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
  void testCreateHappyPath() {
    var location = new Location("AMSTERDAM-001", 5, 100);
    var warehouse = buildWarehouse("MWH.NEW", "AMSTERDAM-001", 20, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001")).thenReturn(Collections.emptyList());

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
  }

  @Test
  void testCreateFailsWhenBusinessUnitCodeAlreadyExists() {
    var existing = buildWarehouse("MWH.001", "ZWOLLE-001", 100, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    var warehouse = buildWarehouse("MWH.001", "AMSTERDAM-001", 20, 5);

    assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreateFailsWhenLocationIsInvalid() {
    when(warehouseStore.findByBusinessUnitCode(anyString())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("INVALID-LOC")).thenReturn(null);

    var warehouse = buildWarehouse("MWH.NEW", "INVALID-LOC", 20, 5);

    assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreateFailsWhenMaxWarehousesReached() {
    var location = new Location("ZWOLLE-001", 1, 40);
    var existing = buildWarehouse("MWH.001", "ZWOLLE-001", 30, 5);

    when(warehouseStore.findByBusinessUnitCode(anyString())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
    when(warehouseStore.findActiveByLocation("ZWOLLE-001")).thenReturn(List.of(existing));

    var newWarehouse = buildWarehouse("MWH.002", "ZWOLLE-001", 10, 5);

    assertThrows(WebApplicationException.class, () -> useCase.create(newWarehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreateFailsWhenCapacityExceedsLocationMax() {
    var location = new Location("AMSTERDAM-001", 5, 100);
    var existing1 = buildWarehouse("MWH.001", "AMSTERDAM-001", 60, 10);
    var existing2 = buildWarehouse("MWH.002", "AMSTERDAM-001", 30, 5);

    when(warehouseStore.findByBusinessUnitCode(anyString())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(List.of(existing1, existing2));

    // Total existing = 90, adding 20 = 110, which exceeds 100
    var newWarehouse = buildWarehouse("MWH.NEW", "AMSTERDAM-001", 20, 5);

    assertThrows(WebApplicationException.class, () -> useCase.create(newWarehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreateFailsWhenStockExceedsCapacity() {
    var location = new Location("AMSTERDAM-001", 5, 100);

    when(warehouseStore.findByBusinessUnitCode(anyString())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001")).thenReturn(Collections.emptyList());

    var warehouse = buildWarehouse("MWH.NEW", "AMSTERDAM-001", 20, 25);

    assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreateSucceedsWhenExactCapacityMatchesLocationMax() {
    var location = new Location("ZWOLLE-001", 1, 40);

    when(warehouseStore.findByBusinessUnitCode(anyString())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
    when(warehouseStore.findActiveByLocation("ZWOLLE-001")).thenReturn(Collections.emptyList());

    var warehouse = buildWarehouse("MWH.NEW", "ZWOLLE-001", 40, 10);

    useCase.create(warehouse);
    verify(warehouseStore).create(warehouse);
  }
}
