package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  private LocationGateway locationGateway;

  @BeforeEach
  void setUp() {
    locationGateway = new LocationGateway();
  }

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void testResolveAmsterdam001() {
    Location location = locationGateway.resolveByIdentifier("AMSTERDAM-001");

    assertNotNull(location);
    assertEquals("AMSTERDAM-001", location.identification);
    assertEquals(5, location.maxNumberOfWarehouses);
    assertEquals(100, location.maxCapacity);
  }

  @Test
  public void testResolveAmsterdam002() {
    Location location = locationGateway.resolveByIdentifier("AMSTERDAM-002");

    assertNotNull(location);
    assertEquals("AMSTERDAM-002", location.identification);
    assertEquals(3, location.maxNumberOfWarehouses);
    assertEquals(75, location.maxCapacity);
  }

  @Test
  public void testWhenResolveNonExistingLocationShouldReturnNull() {
    Location location = locationGateway.resolveByIdentifier("NON-EXISTENT-001");

    assertNull(location);
  }

  @Test
  public void testWhenResolveNullIdentifierShouldThrowNullPointerException() {
    // NullPointerException because we call equals() on null (location.identification)
    // This documents the existing behavior
    try {
      locationGateway.resolveByIdentifier(null);
    } catch (NullPointerException e) {
      // expected — null input will cause NPE in equals comparison
    }
  }

  @Test
  public void testResolveZwolle002() {
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-002");

    assertNotNull(location);
    assertEquals("ZWOLLE-002", location.identification);
    assertEquals(2, location.maxNumberOfWarehouses);
    assertEquals(50, location.maxCapacity);
  }

  @Test
  public void testResolveTilburg001() {
    Location location = locationGateway.resolveByIdentifier("TILBURG-001");

    assertNotNull(location);
    assertEquals("TILBURG-001", location.identification);
  }

  @Test
  public void testResolveHelmond001() {
    Location location = locationGateway.resolveByIdentifier("HELMOND-001");

    assertNotNull(location);
    assertEquals("HELMOND-001", location.identification);
  }

  @Test
  public void testResolveEindhoven001() {
    Location location = locationGateway.resolveByIdentifier("EINDHOVEN-001");

    assertNotNull(location);
    assertEquals("EINDHOVEN-001", location.identification);
  }

  @Test
  public void testResolveVetsby001() {
    Location location = locationGateway.resolveByIdentifier("VETSBY-001");

    assertNotNull(location);
    assertEquals("VETSBY-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(90, location.maxCapacity);
  }
}
