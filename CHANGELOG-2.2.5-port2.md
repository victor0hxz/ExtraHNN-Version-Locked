# Extra Hostile Neural Networks Version Locked — 2.2.5-port2

Minecraft 26.1.2 / NeoForge / ATM11 0.9.0.

- Fixed simulation progress resetting when automation previews or rolls back an input refill.
- Inventory transaction rollback now preserves unchanged slots; commit notifications are limited to changed slots.
- Equivalent copies of the combined data model no longer cancel an already paid simulation cycle.
- Adding prediction matrices during a cycle preserves progress and leaves the added matrices available for the next cycle.
- Verified all four simulation tiers with committed refills, aborted refills, equivalent model copies, single batch consumption and expected output delivery.

Community Version Locked build. Original authors: LMor and nexxx_7. Original MIT license and credits preserved.

Install the same version on the server and clients. This patch prevents future cycle resets; it does not refund previously lost inputs.
