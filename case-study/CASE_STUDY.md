# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

**Key Challenges:**
- **Multi-dimensional cost attribution**: A single shipment may involve labor at a warehouse, fuel for transport, and overhead shared across multiple warehouses at a location — allocating costs requires clear attribution rules.
- **Dynamic vs. fixed cost split**: Labor is variable (scales with volume), but rent and utilities are fixed. How do you fairly allocate shared fixed costs across multiple tenants or warehouses at the same location?
- **Real-time vs. batch cost capture**: Operational costs (e.g., fuel, labor hours) are often captured in disparate systems (ERP, WMS, logistics platforms). Ensuring timely, accurate data ingestion is a key integration challenge.
- **Product mix complexity**: When a warehouse stores 5+ product types for multiple stores, allocating per-product costs requires robust cost-driver identification (e.g., volume, weight, handling complexity).

**Questions I would ask before scoping:**
1. What cost categories need tracking — direct only (labor, transport, inventory), or also indirect (overheads, depreciation)?
2. Are costs captured in real-time or as end-of-period journal entries?
3. Which system holds the authoritative cost data — ERP, WMS, or a combination?
4. How are shared-location costs (e.g., a location hosting 3 warehouses) currently allocated?
5. Are there regulatory or financial reporting requirements (IFRS 16, ABC costing) that constrain the allocation methodology?
6. What granularity is needed — per SKU, per store, per warehouse, per location?

**Considerations:**
- Implement a cost-driver model (activity-based costing) rather than simple headcount or area-based allocation to produce more meaningful insights.
- A data lake or cost ledger pattern (append-only cost events) makes historical analysis and reprocessing easier.
- Design the data model to support the `Warehouse → Store → Product` relationship established in this system, enabling drill-down from location-level to product-level costs.

---

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

**Identifying Opportunities:**
1. **Warehouse utilization analysis**: Are warehouses running below their capacity limits? Low utilization means fixed costs spread over fewer units. The capacity/stock model in this system enables this analysis directly.
2. **Location consolidation**: If ZWOLLE-001 has max 1 warehouse and AMSTERDAM-001 allows 5, could consolidating demand at higher-capacity locations reduce fixed costs?
3. **Product routing optimization**: With the Warehouse→Product→Store fulfillment model, identifying products that travel the longest distances or cross multiple fulfillment hops reveals transport cost reduction opportunities.
4. **Labor scheduling vs. demand patterns**: Aligning labor costs with inbound/outbound volume patterns (seasonal spikes vs. troughs).

**Prioritization Framework:**
- Plot strategies on an impact × effort matrix.
- Quick wins (high impact, low effort): Optimize routing for high-volume products.
- Strategic initiatives (high impact, high effort): Warehouse consolidation or network redesign.
- Avoid (low impact, high effort): Over-engineering allocation models for minor cost categories.

**Expected Outcomes:**
- 5-15% reduction in per-unit fulfillment cost through utilization improvement.
- Reduced transport costs via smarter warehouse-to-store product assignment.
- Improved forecasting accuracy leading to lower safety stock (inventory holding cost reduction).

**Implementation Approach:**
- Start with a baseline cost report per warehouse and per store.
- Run A/B experiments on routing changes before full rollout.
- Continuously monitor KPIs (cost per order, utilization rate, stock turnover) in a dashboard tied to this system's data.

---

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

**Importance:**
- Financial systems (ERP, GL) hold authoritative cost and budget data. Without integration, operational decisions are made on incomplete information, leading to budget overruns discovered only at period-end.
- Regulatory compliance (audit trails, IFRS reporting) requires cost data to reconcile with the general ledger.
- Integrated systems eliminate manual re-keying of data, reducing errors and delays.

**Integration Approaches:**
1. **Event-driven / CDC (Change Data Capture)**: When a Warehouse is created, replaced, or archived, publish domain events (e.g., `WarehouseCreated`, `WarehouseArchived`) to a message broker (Kafka, RabbitMQ). The financial system consumes these events and opens/closes cost centers accordingly. This is loosely coupled and resilient.
2. **REST API synchronization**: Financial system polls the Warehouse API or subscribes to webhooks. Simpler but introduces coupling and potential for missed events.
3. **Shared data store / data lake**: Both systems write to a central data lake; analytics and reporting happen there. Good for reporting but not real-time.

**For this codebase specifically:**
The CDI `@TransactionPhase.AFTER_SUCCESS` pattern already implemented for the Store legacy integration is a good precursor to event-driven integration. Extending this to publish to a message broker (instead of just writing a temp file) would enable real-time financial system sync.

**Key Questions:**
1. What is the financial system's preferred integration method (REST, message broker, file-based)?
2. What is the acceptable latency for cost data to appear in financial reports (real-time, hourly, daily)?
3. Are there idempotency requirements — can the financial system handle duplicate events?
4. Who owns the master data for cost centers — the fulfillment system or the ERP?

**Ensuring Seamless Synchronization:**
- Implement at-least-once delivery with idempotent consumers.
- Use correlation IDs (e.g., `businessUnitCode`) to link cost center records between systems.
- Build reconciliation jobs to detect and alert on divergence between systems.
- Maintain an event store / outbox table to guarantee no events are lost even during downstream system outages.

---

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

**Importance:**
- Accurate budgets set realistic targets for operational costs per warehouse and per store.
- Forecasting enables proactive capacity planning — knowing when a location is approaching its warehouse or capacity limit allows the company to plan replacements or expansions in advance.
- Without forecasting, the company reacts to capacity issues rather than anticipating them.

**Design Considerations for a Forecasting System:**
1. **Historical data model**: The `archivedAt` timestamp on warehouses provides a history of warehouse lifecycles — when were they created, when archived, what capacity did they have? This is a natural starting point for time-series cost analysis.
2. **Demand signals**: Integrate order volume data (from Store/Product) to correlate with warehouse utilization and costs. Seasonality patterns are critical for fulfillment.
3. **Scenario modeling**: Allow planners to model "what-if" scenarios — e.g., if AMSTERDAM-001 gets a 5th warehouse, what's the cost impact?
4. **Budget vs. actuals tracking**: The system must compare budgeted costs against actuals at warehouse and store granularity, and surface variances in near-real-time.

**Key Questions:**
1. What is the forecasting horizon — monthly, quarterly, annual?
2. What data is available historically — at least 2-3 years is recommended for seasonal forecasting.
3. Who creates budgets — finance team only, or warehouse operations too?
4. What level of automation is desired — purely manual input, ML-assisted forecasting, or fully automated?

**Implementation Approach:**
- Phase 1: Aggregate historical warehouse cost data and build baseline reports.
- Phase 2: Add budget entry capability (by warehouse, by cost category, by period).
- Phase 3: Integrate ML-based demand forecasting (e.g., Prophet, ARIMA) to predict future utilization and costs.
- Phase 4: Build variance alerts and "approaching budget limit" notifications.

---

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

**Why Preserving Cost History Matters:**
- The `businessUnitCode` represents a logical business unit — a geographic area or operational unit — not just a physical building. Replacing a warehouse at the same unit means costs should be traceable across generations of that unit.
- Audit and compliance: Financial audits require full traceability of costs to the asset or cost center that incurred them. If a warehouse is simply deleted (instead of archived), its cost history becomes unattributable.
- Budget baseline: The new warehouse's budget should be informed by the old warehouse's historical cost performance. If the old unit consistently overran budget, the new one needs adjusted assumptions.
- Depreciation and asset tracking: If the warehouse is a capitalized asset, archiving (not deleting) it preserves the depreciation timeline in the financial system.

**How This Codebase Supports Cost History Preservation:**
The `archivedAt` timestamp approach (vs. hard delete) is the correct design decision here. The archived warehouse record remains in the database, preserving the full lifecycle:
- When it was created (`createdAt`)
- When it was archived (`archivedAt`)
- What its capacity and stock were at the time of replacement

This means cost data linked to the old `businessUnitCode + createdAt/archivedAt` window can be accurately attributed, while costs from the new warehouse accrue from its `createdAt` forward.

**Cost Control Considerations for Replacement:**
1. The replacement validation (new capacity ≥ old stock, new stock = old stock) ensures no operational gap — the new warehouse can immediately carry the load of the old one without a costly interim period.
2. The new warehouse's budget should start from the old warehouse's final actuals as a baseline, adjusted for any new capacity or location changes.
3. A "replacement event" should trigger a financial system notification (close old cost center, open new one with the same BUC identifier but a new validity period) — this is where the event-driven integration from Scenario 3 applies directly.

**Questions I would ask:**
1. Does the financial system currently track cost centers by `businessUnitCode` or by the physical warehouse DB record ID?
2. Is there a need for a "transition cost" ledger entry when a warehouse is replaced (e.g., moving inventory, setup costs)?
3. How long should archived warehouses be retained in the operational database vs. moved to a cold archive?
4. Are there contractual obligations (e.g., lease terms) that need to be tracked alongside the warehouse lifecycle?

---

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
