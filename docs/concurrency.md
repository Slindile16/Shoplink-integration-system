# Phase 7: Concurrent orders and stock consistency

Inventory Service owns available stock and reservations. Order Service submits a
reservation directly: a separate stock check could become stale before reservation.

## Atomic reservations

`InventoryService` synchronizes reads, reservations, and releases on the same
service instance. Each reservation combines repeated product lines, checks every
product, and deducts all quantities while holding that monitor. Competing orders
cannot both reserve the same remaining units. A rejected basket changes no stock.

Replaying the same reservation ID and quantities succeeds without deducting again.
A different basket for an existing ID returns `409`. Releasing restores the saved
quantities exactly once; subsequent releases succeed without changing stock.
A released ID cannot reserve again. A replay racing with release either observes
the existing reservation or returns `409` after release.

Order and Inventory services reject repeated-line quantity totals greater than
`Integer.MAX_VALUE` with `422` before reserving stock. This prevents integer
overflow from turning a positive request into an incorrect stock deduction.
Ordinary stock shortages return `409`.

## Scope

These guarantees apply to one Inventory Service process. Stock, reservations, and
released IDs remain in memory and reset on restart. Run one inventory instance;
multiple independent instances do not share stock or locks. A shared database
implementation needs transactions or conditional stock updates and a unique
reservation ID constraint to preserve these guarantees across instances.
Network failures and crashes between services still need reconciliation; this
phase does not introduce durable transactions across HTTP and JMS.
