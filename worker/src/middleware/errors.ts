// Typed error envelope shared by all middleware and the dispatcher.

export type ErrorCode =
  | "RATE_LIMITED"
  | "BUDGET_EXCEEDED"
  | "ATTESTATION_FAILED"
  | "INVALID_INPUT"
  | "UPSTREAM_ERROR";

const STATUS: Record<ErrorCode, number> = {
  INVALID_INPUT: 400,
  ATTESTATION_FAILED: 401,
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
