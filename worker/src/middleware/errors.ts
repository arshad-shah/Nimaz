// Typed error envelope shared by all middleware and the dispatcher.
// RATE_LIMITED is a pass-through of the AI Gateway's rate limit (the Worker
// keeps no counters of its own); ATTESTATION_FAILED is an explicit failed
// Play Integrity verdict (see integrity.ts).

export type ErrorCode =
  | "RATE_LIMITED"
  | "ATTESTATION_FAILED"
  | "BUDGET_EXCEEDED"
  | "INVALID_INPUT"
  | "UPSTREAM_ERROR";

const STATUS: Record<ErrorCode, number> = {
  INVALID_INPUT: 400,
  ATTESTATION_FAILED: 403,
  RATE_LIMITED: 429,
  UPSTREAM_ERROR: 502,
  BUDGET_EXCEEDED: 503,
};

export class ApiError extends Error {
  constructor(
    readonly code: ErrorCode,
    message: string,
    readonly retryAfterSeconds?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }

  get status(): number {
    return STATUS[this.code];
  }

  toBody() {
    return {
      error: {
        code: this.code,
        message: this.message,
        ...(this.retryAfterSeconds !== undefined
          ? { retryAfterSeconds: this.retryAfterSeconds }
          : {}),
      },
    };
  }
}
