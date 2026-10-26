/**
 * In-memory and cache-based idempotency tracker for Meta Messenger events.
 * Prevents processing duplicate mid (Message IDs) or duplicate webhooks.
 */
export class DeduplicationManager {
  private processedMids: Map<string, number> = new Map();
  private ttlMs: number;

  constructor(ttlMs: number = 10 * 60 * 1000) {
    this.ttlMs = ttlMs;
  }

  /**
   * Checks if an event/mid has already been processed.
   * If not, marks it as processed and returns false.
   * If it was already processed, returns true.
   */
  public isDuplicate(mid: string): boolean {
    this.cleanup();
    if (this.processedMids.has(mid)) {
      return true;
    }
    this.processedMids.set(mid, Date.now());
    return false;
  }

  private cleanup(): void {
    const now = Date.now();
    for (const [mid, timestamp] of this.processedMids.entries()) {
      if (now - timestamp > this.ttlMs) {
        this.processedMids.delete(mid);
      }
    }
  }

  public clear(): void {
    this.processedMids.clear();
  }
}

export const deduplicationManager = new DeduplicationManager();
