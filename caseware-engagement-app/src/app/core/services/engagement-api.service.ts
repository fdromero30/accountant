import { InjectionToken, Injectable, inject } from '@angular/core';
import { Observable, delay, of } from 'rxjs';

import {
  EngagementListResponse,
  PendingUpdateSummaryResponse,
} from '../models/engagement.model';
import {
  MOCK_ENGAGEMENTS_RESPONSE,
  MOCK_PENDING_SUMMARIES,
} from '../../shared/fixtures/mock-engagements';

/** Simulated round trip of the stubbed endpoints; `0` in tests so responses stay synchronous. */
export const API_LATENCY_MS = new InjectionToken<number>('API_LATENCY_MS', {
  providedIn: 'root',
  factory: () => 250,
});

/**
 * Data access of the engagement screens.
 *
 * The backend is not wired in yet, so every method answers from the static fixtures under
 * `shared/fixtures` and each one documents the endpoint it will call next to the request that has to
 * replace the fixture. Only `EngagementStore` talks to this service.
 */
@Injectable({ providedIn: 'root' })
export class EngagementApiService {

  private readonly latency = inject(API_LATENCY_MS);


  /**
   * Engagements of the signed in firm together with their template versions.
   *
   * TODO(api): return this.http.get<EngagementListResponse>(`${this.baseUrl}/engagements`).
   */
  getEngagements(): Observable<EngagementListResponse> {
    return this.respond(MOCK_ENGAGEMENTS_RESPONSE);
  }

  /**
   * Human readable changes waiting for one engagement.
   *
   * Answers `null` when the backend reports no summary for the engagement, which is the case for the
   * ones that are already up to date.
   *
   * TODO(api): return this.http.get<PendingUpdateSummaryResponse>(
   *   `${this.baseUrl}/engagements/${engagementId}/pending-update-summary`).
   */
  getPendingUpdateSummary(engagementId: string): Observable<PendingUpdateSummaryResponse | null> {
    return this.respond(MOCK_PENDING_SUMMARIES[engagementId] ?? null);
  }

  private respond<T>(payload: T): Observable<T> {
    return this.latency > 0 ? of(payload).pipe(delay(this.latency)) : of(payload);
  }
}
