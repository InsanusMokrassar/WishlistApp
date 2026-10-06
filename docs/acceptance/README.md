# Navigation model guide

The common [graph of navigation](../../NAVIGATION.md) lives at the project root.
Every change must consult and edit it, including an explicit impact record when
navigation is unaffected. Keep the shared Mermaid graph and its transition index
there; this directory contains supporting feature details, not a second overview.

The [feature acceptance contract](../../agents/FEATURE_ACCEPTANCE.md) defines the
authoritative record format, evidence and maintenance policy. Add accepted feature
records at `features/<feature-id>.md`, link them from the root graph and include
their states/transitions in that graph with matching stable IDs. Link shared states
to their owning records, responsible implementation symbols or descriptive `TBD`,
and separate test/evidence references. Any optional detailed diagram must agree
with the common graph and cannot substitute for updating it.

The [wishlist creation example](../../agents/examples/wishlist-acceptance.md) is a
proposed plan, not accepted product navigation or proof of execution. The root
graph identifies unmodeled journeys and coverage limits. See the
[browser gate guide](../../README.md#served-web-browser-verification) for existing
smoke-check setup and evidence.
