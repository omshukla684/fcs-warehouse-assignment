package com.fulfilment.application.monolith.warehouses.domain.validator;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

/**
 * Domain validator for {@link Warehouse} business rules.
 *
 * <p>Validation logic is extracted here following the Single Responsibility Principle and the
 * Hexagonal Architecture pattern, keeping use cases free of raw validation code.
 *
 * <p>Rules enforced:
 * <ol>
 *   <li>Business-unit-code uniqueness – no two active warehouses may share the same code.</li>
 *   <li>Location existence – the provided location must be a known, valid location.</li>
 *   <li>Location capacity count – the number of warehouses at a location must not exceed
 *       {@code location.maxNumberOfWarehouses}.</li>
 *   <li>Total capacity – the sum of capacities at a location must not exceed
 *       {@code location.maxCapacity}.</li>
 *   <li>Stock / capacity coherence – a warehouse's stock must not exceed its own capacity.</li>
 * </ol>
 */
@ApplicationScoped
public class WarehouseValidator {

  /**
   * Validates that no active warehouse already carries the requested business-unit code.
   *
   * @param businessUnitCode the code to check
   * @param existing         the currently persisted active warehouse (or {@code null} if none)
   * @throws WebApplicationException HTTP 400 when a duplicate is found
   */
  public void validateUniqueBusinessUnitCode(String businessUnitCode, Warehouse existing) {
    if (existing != null) {
      throw new WebApplicationException(
          "A warehouse with businessUnitCode '"
              + businessUnitCode
              + "' already exists.",
          400);
    }
  }

  /**
   * Validates that the location exists (i.e. is not {@code null}).
   *
   * @param locationIdentifier the identifier that was requested
   * @param location           the resolved location (or {@code null} if unknown)
   * @throws WebApplicationException HTTP 400 when the location is unknown
   */
  public void validateLocationExists(String locationIdentifier, Location location) {
    if (location == null) {
      throw new WebApplicationException(
          "Location '" + locationIdentifier + "' is not a valid location.", 400);
    }
  }

  /**
   * Validates that adding one more warehouse to a location does not exceed the allowed count.
   *
   * @param location           the resolved location
   * @param activeAtLocation   currently active warehouses at that location
   * @throws WebApplicationException HTTP 400 when the limit is reached
   */
  public void validateWarehouseCountAtLocation(
      Location location, List<Warehouse> activeAtLocation) {
    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Location '"
              + location.identification
              + "' has already reached its maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ").",
          400);
    }
  }

  /**
   * Validates that adding a warehouse with the given capacity does not exceed the location's total
   * capacity limit.
   *
   * @param location              the resolved location
   * @param activeAtLocation      currently active warehouses at that location
   * @param additionalCapacity    the capacity of the warehouse being added
   * @throws WebApplicationException HTTP 400 when the limit would be exceeded
   */
  public void validateTotalCapacityAtLocation(
      Location location, List<Warehouse> activeAtLocation, int additionalCapacity) {
    int currentTotal = activeAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (currentTotal + additionalCapacity > location.maxCapacity) {
      throw new WebApplicationException(
          "Adding this warehouse would exceed the maximum total capacity of location '"
              + location.identification
              + "'. Max capacity: "
              + location.maxCapacity
              + ", current total: "
              + currentTotal
              + ", requested: "
              + additionalCapacity
              + ".",
          400);
    }
  }

  /**
   * Validates that the warehouse stock does not exceed its own capacity.
   *
   * @param warehouse the warehouse being created/replaced
   * @throws WebApplicationException HTTP 400 when stock exceeds capacity
   */
  public void validateStockWithinCapacity(Warehouse warehouse) {
    if (warehouse.stock != null
        && warehouse.capacity != null
        && warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException(
          "Warehouse stock ("
              + warehouse.stock
              + ") cannot exceed warehouse capacity ("
              + warehouse.capacity
              + ").",
          400);
    }
  }

  /**
   * Validates that the new warehouse capacity can accommodate the stock being transferred from an
   * old (replaced) warehouse.
   *
   * @param newCapacity  the capacity of the replacement warehouse
   * @param currentStock the current stock of the warehouse being replaced
   * @throws WebApplicationException HTTP 400 when capacity is insufficient
   */
  public void validateCapacityAccommodatesStock(Integer newCapacity, Integer currentStock) {
    if (newCapacity == null || newCapacity < currentStock) {
      throw new WebApplicationException(
          "New warehouse capacity ("
              + newCapacity
              + ") must be at least as large as the current warehouse stock ("
              + currentStock
              + ").",
          400);
    }
  }

  /**
   * Validates that the new warehouse's stock matches the old warehouse's stock (stock-matching
   * constraint for replacements).
   *
   * @param newStock     the stock declared on the replacement warehouse
   * @param currentStock the stock of the warehouse being replaced
   * @throws WebApplicationException HTTP 400 when stocks differ
   */
  public void validateStockMatchesOnReplacement(Integer newStock, Integer currentStock) {
    if (newStock == null || !newStock.equals(currentStock)) {
      throw new WebApplicationException(
          "New warehouse stock ("
              + newStock
              + ") must match the stock of the warehouse being replaced ("
              + currentStock
              + ").",
          400);
    }
  }
}
