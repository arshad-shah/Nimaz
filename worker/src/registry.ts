import type { Capability } from "./capabilities/types";
import { askWithProof } from "./capabilities/ask-with-proof";
import { searchPlan } from "./capabilities/search-plan";

// The capability registry. Every AI feature is one entry here.
// To add a capability: create capabilities/<id>.ts exporting a Capability,
// then register it below. Nothing else in the pipeline changes.
const capabilities: Array<Capability<unknown, unknown>> = [
  askWithProof as unknown as Capability<unknown, unknown>,
  searchPlan as unknown as Capability<unknown, unknown>,
];

const registry = new Map<string, Capability<unknown, unknown>>(
  capabilities.map((c) => [c.id, c]),
);

export function getCapability(
  id: string,
): Capability<unknown, unknown> | undefined {
  return registry.get(id);
}

export function listCapabilityIds(): string[] {
  return [...registry.keys()];
}
