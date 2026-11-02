package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  private Warehouse buildWarehouse(String code) {
    var w = new Warehouse();
    w.businessUnitCode = code;
    w.location = "AMSTERDAM-001";
    w.capacity = 100;
    w.stock = 50;
    return w;
  }

  @Test
  void testArchiveHappyPath() {
    var existing = buildWarehouse("MWH.001");
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    var toArchive = buildWarehouse("MWH.001");
    useCase.archive(toArchive);

    // Verify archivedAt was set and update was called
    assertNotNull(existing.archivedAt);
    verify(warehouseStore).update(existing);
  }

  @Test
  void testArchiveFailsWhenWarehouseNotFound() {
    when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    var toArchive = buildWarehouse("MWH.999");

    assertThrows(WebApplicationException.class, () -> useCase.archive(toArchive));
    verify(warehouseStore, never()).update(any());
  }

  @Test
  void testArchiveSetsTimestamp() {
    var existing = buildWarehouse("MWH.001");
    existing.archivedAt = null;

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    LocalDateTime before = LocalDateTime.now().minusSeconds(1);
    useCase.archive(buildWarehouse("MWH.001"));
    LocalDateTime after = LocalDateTime.now().plusSeconds(1);

    assertNotNull(existing.archivedAt);
    assert existing.archivedAt.isAfter(before);
    assert existing.archivedAt.isBefore(after);
  }
}
