/**
 * Wire contract of the engagement transformation backend.
 *
 * Every interface mirrors one Java record of `com.caseware.transformation.model.output`
 * field for field, so a response can be typed without any mapping layer.
 */

/** Normalized kind of template change. Mirrors `ChangeType`. */
export type ChangeType = 'ADDED' | 'MODIFIED' | 'REMOVED';

/** How disruptive a change is, ordered from least to most disruptive. Mirrors `ImpactLevel`. */
export type ImpactLevel = 'LOW' | 'MEDIUM' | 'HIGH';

/**
 * Alignment of an engagement with the latest published version of its template.
 * Mirrors `UpdateStatus`; `COMPUTING_SUMMARY` is reserved for asynchronous summaries and is never
 * produced by the current backend.
 */
export type UpdateStatus = 'UP_TO_DATE' | 'PENDING_UPDATE' | 'COMPUTING_SUMMARY';

/** One row of the engagement list view. Mirrors `EngagementItemDto`. */
export interface EngagementItemDto {
  engagementId: string;
  name: string;
  templateId: string;
  templateDisplayName: string;
  currentVersion: number;
  latestVersion: number;
  versionsBehind: number;
  status: UpdateStatus;
}

/** Response payload backing the engagement list screen. Mirrors `EngagementListResponse`. */
export interface EngagementListResponse {
  totalCount: number;
  pendingUpdatesCount: number;
  engagements: EngagementItemDto[];
}

/** A single template change expressed for a non technical audience. Mirrors `HumanReadableChange`. */
export interface HumanReadableChange {
  /** Deterministic identifier: the JSON pointer of the node, suffixed with `#2`, `#3`... if touched twice. */
  id: string;
  /** Key of the template section the change belongs to; `null` outside `/sections`. */
  sectionKey: string | null;
  /** Human readable section name; `null` when the change is not scoped to a section. */
  sectionDisplayName: string | null;
  changeType: ChangeType;
  title: string;
  description: string;
  impactLevel: ImpactLevel;
  actionRequired: boolean;
  oldValueSummary: string | null;
  newValueSummary: string | null;
}

/** Detailed view of the template updates waiting for one engagement. Mirrors `PendingUpdateSummaryResponse`. */
export interface PendingUpdateSummaryResponse {
  engagementId: string;
  engagementName: string;
  templateId: string;
  templateDisplayName: string;
  /** Template version the engagement currently lives on. */
  currentVersion: number;
  /** Template version the engagement would be upgraded to. */
  targetVersion: number;
  /** Target version of every accumulated diff; empty when nothing is pending. */
  accumulatedVersions: number[];
  status: UpdateStatus;
  /** ISO-8601 instant at which the summary was produced. */
  computedAt: string;
  totalChangesCount: number;
  changes: HumanReadableChange[];
}

/** Changes of one section, in the order the backend reported them. */
export interface ChangeSectionGroup {
  sectionKey: string | null;
  sectionDisplayName: string;
  changes: HumanReadableChange[];
}

/** Number of changes per impact level. */
export type ImpactCounts = Record<ImpactLevel, number>;

/** Label rendered for a change not scoped to a concrete section. */
export const UNSECTIONED_LABEL = 'General';

/** Impact levels ordered from most to least disruptive. */
export const IMPACT_LEVELS: readonly ImpactLevel[] = ['HIGH', 'MEDIUM', 'LOW'];

/** Display labels for the impact levels. */
export const IMPACT_LABELS: Readonly<Record<ImpactLevel, string>> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
};

/** Display labels for the change types. */
export const CHANGE_TYPE_LABELS: Readonly<Record<ChangeType, string>> = {
  ADDED: 'Added',
  MODIFIED: 'Modified',
  REMOVED: 'Removed',
};

/** Display labels for the update statuses. */
export const STATUS_LABELS: Readonly<Record<UpdateStatus, string>> = {
  UP_TO_DATE: 'Up to date',
  PENDING_UPDATE: 'Update available',
  COMPUTING_SUMMARY: 'Computing summary',
};

/**
 * Whether the engagement has template changes waiting to be reviewed.
 *
 * A plain function rather than a method: the payload comes from JSON, so methods would never survive
 * deserialization.
 */
export function hasPendingUpdates(engagement: EngagementItemDto): boolean {
  return engagement.status === 'PENDING_UPDATE';
}

/** Sort key of an impact level, most disruptive first. */
export function impactRank(level: ImpactLevel): number {
  return IMPACT_LEVELS.indexOf(level);
}

/** Comparator ordering changes from most to least disruptive. */
export function compareByImpactDesc(left: HumanReadableChange, right: HumanReadableChange): number {
  return impactRank(left.impactLevel) - impactRank(right.impactLevel);
}

/** Counts the changes of each impact level. */
export function countByImpact(changes: readonly HumanReadableChange[]): ImpactCounts {
  const counts: ImpactCounts = { LOW: 0, MEDIUM: 0, HIGH: 0 };
  for (const change of changes) {
    counts[change.impactLevel] += 1;
  }
  return counts;
}

/** Groups changes by section, preserving the order in which the sections first appear. */
export function groupBySection(changes: readonly HumanReadableChange[]): ChangeSectionGroup[] {
  const groups = new Map<string, ChangeSectionGroup>();
  for (const change of changes) {
    const displayName = change.sectionDisplayName ?? UNSECTIONED_LABEL;
    const group = groups.get(displayName) ?? {
      sectionKey: change.sectionKey,
      sectionDisplayName: displayName,
      changes: [],
    };
    group.changes.push(change);
    groups.set(displayName, group);
  }
  return [...groups.values()];
}
