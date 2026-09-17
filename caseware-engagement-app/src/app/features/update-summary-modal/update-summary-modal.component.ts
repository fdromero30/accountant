import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import {
  CHANGE_TYPE_LABELS,
  ImpactLevel,
  IMPACT_LABELS,
  IMPACT_LEVELS,
} from '../../core/models/engagement.model';
import { EngagementStore } from '../../core/store/engagement.store';

/**
 * Update summary of one engagement: the upgrade route, how disruptive each change is and the changes
 * themselves, grouped by template section.
 *
 * It only shows what the backend returns: applying an update is out of scope, so there is no action
 * to approve or deny, and there is no filter to narrow the changes down.
 */
@Component({
  selector: 'app-update-summary-modal',
  templateUrl: './update-summary-modal.component.html',
  styleUrl: './update-summary-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'close()',
  },
})
export class UpdateSummaryModalComponent {
  protected readonly store = inject(EngagementStore);

  protected readonly impactLevels = IMPACT_LEVELS;
  protected readonly impactLabels = IMPACT_LABELS;
  protected readonly changeTypeLabels = CHANGE_TYPE_LABELS;

  protected impactCount(impactLevel: ImpactLevel): number {
    return this.store.summaryImpactCounts()[impactLevel];
  }

  /** Closes the modal without touching the update. */
  protected close(): void {
    this.store.closeSummary();
  }
}
