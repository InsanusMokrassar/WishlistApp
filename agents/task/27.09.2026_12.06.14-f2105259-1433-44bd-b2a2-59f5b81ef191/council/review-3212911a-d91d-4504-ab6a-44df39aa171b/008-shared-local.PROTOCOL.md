# Local council capacity override

For task `27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191`, the operator approved two independent batches of two participants for each four-role proposal or voting wave. The coordinator freezes and supplies one identical packet to all four roles, and hides every current-wave peer output from all participants until the whole wave is complete. This overrides only the simultaneous-launch/no-batching requirement of `PROTOCOL.md`; every role still submits an independent proposal and a fresh vote on the same candidate in each voting cycle. Treat the two batches as one complete wave for all other protocol checks.

If host cleanup delays a participant launch within a batch, the coordinator records the attempt and retries after capacity clears. The delayed participant still receives the original frozen packet and no current-wave peer output.
