# Operator answer: timestamp range policy

The preparation report `002-preparation.md` asked how to handle stored or configured `Long` timestamps outside korlibs `DateTime`'s exact millisecond range, including the existing `Long.MAX_VALUE` overflow regression. On 2026-09-27, the operator answered: “lets council decide, what to do in that case”.

The operator delegates the compatibility policy to the full council. The council must compare preservation, rejection, and other feasible treatments against the requested `DateTime` rule, data integrity, and existing behavior; propose a concrete policy with tests and rollout/rollback effects; and reach the required complete-roster consensus. This answer does not preselect the proposed fail-closed range restriction.
