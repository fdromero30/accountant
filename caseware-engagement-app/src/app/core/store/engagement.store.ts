import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, catchError, pipe, switchMap, tap } from 'rxjs';

import {
  ChangeSectionGroup,
  EngagementItemDto,
  HumanReadableChange,
  ImpactCounts,
  PendingUpdateSummaryResponse,
  countByImpact,
  groupBySection,
  hasPendingUpdates,
} from '../models/engagement.model';
import { EngagementApiService } from '../services/engagement-api.service';

/**
 * Everything the engagement screens read and write.
 *
 * Plain display state: the screens show what the backend returns, so there is no search term, filter
 * or sort key to keep here.
 */
interface EngagementState {
  /** Engagements as returned by the backend. */
  engagements: EngagementItemDto[];
  /** Number of engagements of the response. */
  totalCount: number;
  /** Engagements that have at least one pending template change. */
  pendingUpdatesCount: number;
  /** Engagement the modal is open for; `null` while the modal is closed. */
  selectedEngagementId: string | null;
  /** Summary of the selected engagement; `null` until it arrives. */
  summary: PendingUpdateSummaryResponse | null;
  /** Whether the engagement list is being loaded. */
  listLoading: boolean;
  /** Whether the summary of the selected engagement is being loaded. */
  summaryLoading: boolean;
  /** Human readable failure of the list request, if any. */
  listError: string | null;
  /** Human readable failure of the summary request, if any. */
  summaryError: string | null;
}

const initialState: EngagementState = {
  engagements: [],
  totalCount: 0,
  pendingUpdatesCount: 0,
  selectedEngagementId: null,
  summary: null,
  listLoading: false,
  summaryLoading: false,
  listError: null,
  summaryError: null,
};

/** Human readable message of a failed request. */
function toErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback;
}

/**
 * Single source of truth of the engagement screens.
 *
 * A `signalStore` with the usual three features: `withState` holds the state, `withComputed` exposes
 * the derived views the templates read and `withMethods` is the only place where state changes.
 *
 * `withMethods` is also where the requests are triggered: every method calls `EngagementApiService`
 * and patches the state with the incoming data, so the components only call store methods and never
 * touch the API. The store is a root singleton, so the list and the modal always share the same
 * instance.
 *
 * Scope is display only: filtering, sorting and applying an update are out of scope.
 */
export const EngagementStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => {
    /** Engagement row the modal belongs to, or `null` while nothing is selected. */
    const selectedEngagement = computed<EngagementItemDto | null>(
      () =>
        store.engagements().find((item) => item.engagementId === store.selectedEngagementId()) ??
        null
    );

    /** Every change of the loaded summary. */
    const summaryChanges = computed<readonly HumanReadableChange[]>(
      () => store.summary()?.changes ?? []
    );

    return {
      selectedEngagement,
      summaryChanges,

      /** Whether there is nothing to render because the response had no engagement. */
      isEmpty: computed(() => !store.listLoading() && store.engagements().length === 0),

      /** Whether the modal has to be rendered. */
      isSummaryOpen: computed(() => store.selectedEngagementId() !== null),

      /** Changes grouped by section, in backend order. */
      summaryGroups: computed<readonly ChangeSectionGroup[]>(() => groupBySection(summaryChanges())),

      /** Number of changes per impact level, calculated on every change of the summary. */
      summaryImpactCounts: computed<ImpactCounts>(() => countByImpact(summaryChanges())),

      /** Number of changes the accountant has to act on. */
      actionRequiredCount: computed(
        () => summaryChanges().filter((change) => change.actionRequired).length
      ),
    };
  }),
  withMethods((store, api = inject(EngagementApiService)) => {
    /** Loads the summary of one engagement and patches the state with the incoming data. */
    const loadSummary = rxMethod<string>(
      pipe(
        tap(() => patchState(store, { summaryLoading: true, summaryError: null })),
        switchMap((engagementId) =>
          api.getPendingUpdateSummary(engagementId).pipe(
            catchError((error: unknown) => {
              patchState(store, {
                summaryLoading: false,
                summaryError: toErrorMessage(error, 'Unable to load the pending changes.'),
              });
              return EMPTY;
            }),
            tap((summary) =>
              patchState(store, {
                summary,
                summaryLoading: false,
                summaryError:
                  summary === null ? 'This engagement has no pending changes to review.' : null,
              })
            )
          )
        )
      )
    );

    return {
      /** Loads the engagement list. */
      loadEngagements: rxMethod<void>(
        pipe(
          tap(() => patchState(store, { listLoading: true, listError: null })),
          switchMap(() =>
            api.getEngagements().pipe(
              catchError((error: unknown) => {
                patchState(store, {
                  listLoading: false,
                  listError: toErrorMessage(error, 'Unable to load the engagements.'),
                });
                return EMPTY;
              }),
              tap((response) =>
                patchState(store, {
                  engagements: response.engagements,
                  totalCount: response.totalCount,
                  pendingUpdatesCount: response.pendingUpdatesCount,
                  listLoading: false,
                })
              )
            )
          )
        )
      ),

      /**
       * Opens the update summary of an engagement, loading it on first access.
       *
       * Engagements that are up to date are ignored: they have no pending change to review.
       */
      openSummary(engagementId: string): void {
        const engagement = store.engagements().find((item) => item.engagementId === engagementId);
        if (engagement === undefined || !hasPendingUpdates(engagement)) {
          return;
        }

        patchState(store, { selectedEngagementId: engagementId });

        // Already loaded for this engagement: reopening the modal does not hit the endpoint again.
        if (store.summary()?.engagementId === engagementId) {
          return;
        }
        loadSummary(engagementId);
      },

      /** Closes the modal. */
      closeSummary(): void {
        patchState(store, { selectedEngagementId: null });
      },
    };
  })
);
