import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';

import {
  EngagementItemDto,
  STATUS_LABELS,
  hasPendingUpdates,
} from '../../core/models/engagement.model';
import { EngagementStore } from '../../core/store/engagement.store';
import { UpdateSummaryModalComponent } from '../update-summary-modal/update-summary-modal.component';

/**
 * Engagement list screen: every engagement of the firm with the template version it runs on and the
 * updates waiting for it. Selecting an engagement that is behind opens the update summary modal.
 *
 * Read only screen: it renders the rows as they come from the backend, without search, filter or
 * sorting.
 */
@Component({
  selector: 'app-engagement-list',
  imports: [UpdateSummaryModalComponent],
  templateUrl: './engagement-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EngagementListComponent implements OnInit {
  protected readonly store = inject(EngagementStore);

  ngOnInit(): void {
    this.store.loadEngagements();
  }

  protected statusLabel(engagement: EngagementItemDto): string {
    return STATUS_LABELS[engagement.status];
  }

  /** Whether the engagement has changes to review, i.e. whether the row is actionable. */
  protected canReview(engagement: EngagementItemDto): boolean {
    return hasPendingUpdates(engagement);
  }

  protected review(engagement: EngagementItemDto): void {
    if (this.canReview(engagement)) {
      this.store.openSummary(engagement.engagementId);
    }
  }
}
