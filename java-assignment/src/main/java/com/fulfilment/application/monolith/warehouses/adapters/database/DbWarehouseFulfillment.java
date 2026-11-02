package com.fulfilment.application.monolith.warehouses.adapters.database;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing the fulfillment link between a Warehouse and a Store, along with the
 * set of product types that this warehouse fulfills for that store.
 */
@Entity
@Table(
    name = "warehouse_fulfillment",
    uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_code", "store_identifier"}))
public class DbWarehouseFulfillment {

  @Id @GeneratedValue public Long id;

  @Column(name = "warehouse_code", nullable = false)
  public String warehouseCode;

  @Column(name = "store_identifier", nullable = false)
  public String storeIdentifier;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "fulfillment_products",
      joinColumns = @JoinColumn(name = "fulfillment_id"))
  @Column(name = "product_identifier")
  public List<String> productIdentifiers = new ArrayList<>();

  public DbWarehouseFulfillment() {}
}
