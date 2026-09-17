import {
  EngagementListResponse,
  PendingUpdateSummaryResponse,
} from '../../core/models/engagement.model';

/**
 * Static replacement for the engagement endpoints while no backend is wired in.
 *
 * Both constants are a verbatim copy of the payload the Java
 * `EngagementTransformationService` returns for `src/test/resources/fixtures` (dumped with a fixed
 * clock, so `computedAt` is stable): 12 engagements
 * (6 up to date, 6 pending), 6 pending summaries and
 * 26 changes (3 HIGH, 20 MEDIUM, 3 LOW).
 */

/** Backing payload of the engagement list screen. */
export const MOCK_ENGAGEMENTS_RESPONSE: EngagementListResponse = {
  totalCount: 12,
  pendingUpdatesCount: 6,
  engagements: [
    {
      engagementId: "ENG-1001",
      name: "Northstar Manufacturing 2026",
      templateId: "AUDIT-CA",
      templateDisplayName: "Canadian Audit Engagement",
      currentVersion: 5,
      latestVersion: 5,
      versionsBehind: 0,
      status: "UP_TO_DATE"
    },
    {
      engagementId: "ENG-1002",
      name: "Maple Ridge Foods 2026",
      templateId: "AUDIT-CA",
      templateDisplayName: "Canadian Audit Engagement",
      currentVersion: 4,
      latestVersion: 5,
      versionsBehind: 1,
      status: "PENDING_UPDATE"
    },
    {
      engagementId: "ENG-1003",
      name: "Harbourview Logistics 2026",
      templateId: "AUDIT-CA",
      templateDisplayName: "Canadian Audit Engagement",
      currentVersion: 3,
      latestVersion: 5,
      versionsBehind: 2,
      status: "PENDING_UPDATE"
    },
    {
      engagementId: "ENG-1004",
      name: "Pinecrest Holdings 2026",
      templateId: "AUDIT-CA",
      templateDisplayName: "Canadian Audit Engagement",
      currentVersion: 5,
      latestVersion: 5,
      versionsBehind: 0,
      status: "UP_TO_DATE"
    },
    {
      engagementId: "ENG-1005",
      name: "Cedar Peak Services 2026",
      templateId: "REVIEW-CA",
      templateDisplayName: "Canadian Review Engagement",
      currentVersion: 8,
      latestVersion: 8,
      versionsBehind: 0,
      status: "UP_TO_DATE"
    },
    {
      engagementId: "ENG-1006",
      name: "Westmount Consulting 2026",
      templateId: "REVIEW-CA",
      templateDisplayName: "Canadian Review Engagement",
      currentVersion: 7,
      latestVersion: 8,
      versionsBehind: 1,
      status: "PENDING_UPDATE"
    },
    {
      engagementId: "ENG-1007",
      name: "Bluewater Hospitality 2026",
      templateId: "REVIEW-CA",
      templateDisplayName: "Canadian Review Engagement",
      currentVersion: 6,
      latestVersion: 8,
      versionsBehind: 2,
      status: "PENDING_UPDATE"
    },
    {
      engagementId: "ENG-1008",
      name: "Summit Property Group 2026",
      templateId: "REVIEW-CA",
      templateDisplayName: "Canadian Review Engagement",
      currentVersion: 8,
      latestVersion: 8,
      versionsBehind: 0,
      status: "UP_TO_DATE"
    },
    {
      engagementId: "ENG-1009",
      name: "Northern Grid Energy 2026",
      templateId: "RISK-CA",
      templateDisplayName: "Canadian Risk Assessment",
      currentVersion: 12,
      latestVersion: 12,
      versionsBehind: 0,
      status: "UP_TO_DATE"
    },
    {
      engagementId: "ENG-1010",
      name: "Greenfield Health Services 2026",
      templateId: "RISK-CA",
      templateDisplayName: "Canadian Risk Assessment",
      currentVersion: 11,
      latestVersion: 12,
      versionsBehind: 1,
      status: "PENDING_UPDATE"
    },
    {
      engagementId: "ENG-1011",
      name: "Stonebridge Construction 2026",
      templateId: "RISK-CA",
      templateDisplayName: "Canadian Risk Assessment",
      currentVersion: 10,
      latestVersion: 12,
      versionsBehind: 2,
      status: "PENDING_UPDATE"
    },
    {
      engagementId: "ENG-1012",
      name: "Prairie Star Investments 2026",
      templateId: "RISK-CA",
      templateDisplayName: "Canadian Risk Assessment",
      currentVersion: 12,
      latestVersion: 12,
      versionsBehind: 0,
      status: "UP_TO_DATE"
    }
  ]
};

/** Pending updates of a single engagement, keyed by engagement id. */
export const MOCK_PENDING_SUMMARIES: Readonly<Record<string, PendingUpdateSummaryResponse>> = {
  "ENG-1002": {
    engagementId: "ENG-1002",
    engagementName: "Maple Ridge Foods 2026",
    templateId: "AUDIT-CA",
    templateDisplayName: "Canadian Audit Engagement",
    currentVersion: 4,
    targetVersion: 5,
    accumulatedVersions: [5],
    status: "PENDING_UPDATE",
    computedAt: "2026-09-16T12:00:00Z",
    totalChangesCount: 3,
    changes: [
      {
        id: "/sections/planning/questions/3/label",
        sectionKey: "planning",
        sectionDisplayName: "Planning",
        changeType: "MODIFIED",
        title: "Label",
        description: "Changed /sections/planning/questions/3/label from \"Has management identified significant estimates?\" to \"Has management identified significant accounting estimates and related estimation uncertainty?\".",
        impactLevel: "LOW",
        actionRequired: false,
        oldValueSummary: "Has management identified significant estimates?",
        newValueSummary: "Has management identified significant accounting estimates and related estimation uncertainty?"
      },
      {
        id: "/sections/materiality/guidance/thresholdPercent",
        sectionKey: "materiality",
        sectionDisplayName: "Materiality",
        changeType: "MODIFIED",
        title: "Threshold Percent",
        description: "Changed /sections/materiality/guidance/thresholdPercent from \"4.5\" to \"4.0\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: "4.5",
        newValueSummary: "4.0"
      },
      {
        id: "/sections/completion/checklists/subsequent-events",
        sectionKey: "completion",
        sectionDisplayName: "Completion",
        changeType: "ADDED",
        title: "Subsequent events review",
        description: "Added \"Subsequent events review\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: null,
        newValueSummary: "Subsequent events review"
      }
    ]
  },
  "ENG-1003": {
    engagementId: "ENG-1003",
    engagementName: "Harbourview Logistics 2026",
    templateId: "AUDIT-CA",
    templateDisplayName: "Canadian Audit Engagement",
    currentVersion: 3,
    targetVersion: 5,
    accumulatedVersions: [4, 5],
    status: "PENDING_UPDATE",
    computedAt: "2026-09-16T12:00:00Z",
    totalChangesCount: 6,
    changes: [
      {
        id: "/sections/planning/questions/7",
        sectionKey: "planning",
        sectionDisplayName: "Planning",
        changeType: "ADDED",
        title: "Were any new fraud risk factors identified during planning?",
        description: "Added \"Were any new fraud risk factors identified during planning?\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: null,
        newValueSummary: "Were any new fraud risk factors identified during planning?"
      },
      {
        id: "/sections/materiality/guidance/thresholdPercent",
        sectionKey: "materiality",
        sectionDisplayName: "Materiality",
        changeType: "MODIFIED",
        title: "Threshold Percent",
        description: "Changed /sections/materiality/guidance/thresholdPercent from \"5.0\" to \"4.5\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: "5.0",
        newValueSummary: "4.5"
      },
      {
        id: "/sections/planning/procedures/legacy-risk-confirmation",
        sectionKey: "planning",
        sectionDisplayName: "Planning",
        changeType: "REMOVED",
        title: "Confirm legacy risk classification",
        description: "Removed \"Confirm legacy risk classification\".",
        impactLevel: "HIGH",
        actionRequired: true,
        oldValueSummary: "Confirm legacy risk classification",
        newValueSummary: null
      },
      {
        id: "/sections/planning/questions/3/label",
        sectionKey: "planning",
        sectionDisplayName: "Planning",
        changeType: "MODIFIED",
        title: "Label",
        description: "Changed /sections/planning/questions/3/label from \"Has management identified significant estimates?\" to \"Has management identified significant accounting estimates and related estimation uncertainty?\".",
        impactLevel: "LOW",
        actionRequired: false,
        oldValueSummary: "Has management identified significant estimates?",
        newValueSummary: "Has management identified significant accounting estimates and related estimation uncertainty?"
      },
      {
        id: "/sections/materiality/guidance/thresholdPercent#2",
        sectionKey: "materiality",
        sectionDisplayName: "Materiality",
        changeType: "MODIFIED",
        title: "Threshold Percent",
        description: "Changed /sections/materiality/guidance/thresholdPercent from \"4.5\" to \"4.0\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: "4.5",
        newValueSummary: "4.0"
      },
      {
        id: "/sections/completion/checklists/subsequent-events",
        sectionKey: "completion",
        sectionDisplayName: "Completion",
        changeType: "ADDED",
        title: "Subsequent events review",
        description: "Added \"Subsequent events review\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: null,
        newValueSummary: "Subsequent events review"
      }
    ]
  },
  "ENG-1006": {
    engagementId: "ENG-1006",
    engagementName: "Westmount Consulting 2026",
    templateId: "REVIEW-CA",
    templateDisplayName: "Canadian Review Engagement",
    currentVersion: 7,
    targetVersion: 8,
    accumulatedVersions: [8],
    status: "PENDING_UPDATE",
    computedAt: "2026-09-16T12:00:00Z",
    totalChangesCount: 3,
    changes: [
      {
        id: "/metadata/displayName",
        sectionKey: null,
        sectionDisplayName: null,
        changeType: "MODIFIED",
        title: "Display Name",
        description: "Changed /metadata/displayName from \"Canadian Review Engagement\" to \"Canadian Review Engagement 2026\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: "Canadian Review Engagement",
        newValueSummary: "Canadian Review Engagement 2026"
      },
      {
        id: "/sections/analytics/procedures/2/tolerance",
        sectionKey: "analytics",
        sectionDisplayName: "Analytics",
        changeType: "MODIFIED",
        title: "Tolerance",
        description: "Changed /sections/analytics/procedures/2/tolerance from \"0.12\" to \"0.1\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: "0.12",
        newValueSummary: "0.1"
      },
      {
        id: "/sections/completion/checklists/going-concern",
        sectionKey: "completion",
        sectionDisplayName: "Completion",
        changeType: "ADDED",
        title: "Going concern evaluation",
        description: "Added \"Going concern evaluation\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: null,
        newValueSummary: "Going concern evaluation"
      }
    ]
  },
  "ENG-1007": {
    engagementId: "ENG-1007",
    engagementName: "Bluewater Hospitality 2026",
    templateId: "REVIEW-CA",
    templateDisplayName: "Canadian Review Engagement",
    currentVersion: 6,
    targetVersion: 8,
    accumulatedVersions: [8],
    status: "PENDING_UPDATE",
    computedAt: "2026-09-16T12:00:00Z",
    totalChangesCount: 5,
    changes: [
      {
        id: "/metadata/displayName",
        sectionKey: null,
        sectionDisplayName: null,
        changeType: "MODIFIED",
        title: "Display Name",
        description: "Changed /metadata/displayName from \"Canadian Review Engagement\" to \"Canadian Review Engagement 2026\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: "Canadian Review Engagement",
        newValueSummary: "Canadian Review Engagement 2026"
      },
      {
        id: "/sections/inquiries/questions/12",
        sectionKey: "inquiries",
        sectionDisplayName: "Inquiries",
        changeType: "ADDED",
        title: "Describe any events after the reporting date that may require adjustment or disclosure.",
        description: "Added \"Describe any events after the reporting date that may require adjustment or disclosure.\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: null,
        newValueSummary: "Describe any events after the reporting date that may require adjustment or disclosure."
      },
      {
        id: "/sections/analytics/procedures/2/tolerance",
        sectionKey: "analytics",
        sectionDisplayName: "Analytics",
        changeType: "MODIFIED",
        title: "Tolerance",
        description: "Changed /sections/analytics/procedures/2/tolerance from \"0.15\" to \"0.1\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: "0.15",
        newValueSummary: "0.1"
      },
      {
        id: "/sections/inquiries/questions/4/helpText",
        sectionKey: "inquiries",
        sectionDisplayName: "Inquiries",
        changeType: "REMOVED",
        title: "Help Text",
        description: "Removed /sections/inquiries/questions/4/helpText.",
        impactLevel: "HIGH",
        actionRequired: true,
        oldValueSummary: "Ask management to describe changes in accounting policies since the prior year.",
        newValueSummary: null
      },
      {
        id: "/sections/completion/checklists/going-concern",
        sectionKey: "completion",
        sectionDisplayName: "Completion",
        changeType: "ADDED",
        title: "Going concern evaluation",
        description: "Added \"Going concern evaluation\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: null,
        newValueSummary: "Going concern evaluation"
      }
    ]
  },
  "ENG-1010": {
    engagementId: "ENG-1010",
    engagementName: "Greenfield Health Services 2026",
    templateId: "RISK-CA",
    templateDisplayName: "Canadian Risk Assessment",
    currentVersion: 11,
    targetVersion: 12,
    accumulatedVersions: [12],
    status: "PENDING_UPDATE",
    computedAt: "2026-09-16T12:00:00Z",
    totalChangesCount: 3,
    changes: [
      {
        id: "/sections/riskAssessment/scoring/highRiskThreshold",
        sectionKey: "riskAssessment",
        sectionDisplayName: "Risk Assessment",
        changeType: "MODIFIED",
        title: "High Risk Threshold",
        description: "Changed /sections/riskAssessment/scoring/highRiskThreshold from \"8\" to \"7\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: "8",
        newValueSummary: "7"
      },
      {
        id: "/sections/monitoring/checklists/control-changes",
        sectionKey: "monitoring",
        sectionDisplayName: "Monitoring",
        changeType: "ADDED",
        title: "Control changes since prior assessment",
        description: "Added \"Control changes since prior assessment\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: null,
        newValueSummary: "Control changes since prior assessment"
      },
      {
        id: "/sections/riskAssessment/guidance/reassessmentFrequencyMonths",
        sectionKey: "riskAssessment",
        sectionDisplayName: "Risk Assessment",
        changeType: "MODIFIED",
        title: "Reassessment Frequency Months",
        description: "Changed /sections/riskAssessment/guidance/reassessmentFrequencyMonths from \"12\" to \"9\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: "12",
        newValueSummary: "9"
      }
    ]
  },
  "ENG-1011": {
    engagementId: "ENG-1011",
    engagementName: "Stonebridge Construction 2026",
    templateId: "RISK-CA",
    templateDisplayName: "Canadian Risk Assessment",
    currentVersion: 10,
    targetVersion: 12,
    accumulatedVersions: [11, 12],
    status: "PENDING_UPDATE",
    computedAt: "2026-09-16T12:00:00Z",
    totalChangesCount: 6,
    changes: [
      {
        id: "/sections/riskAssessment/questions/2/label",
        sectionKey: "riskAssessment",
        sectionDisplayName: "Risk Assessment",
        changeType: "MODIFIED",
        title: "Label",
        description: "Changed /sections/riskAssessment/questions/2/label from \"Describe key business risks.\" to \"Describe key business risks and how management monitors them.\".",
        impactLevel: "LOW",
        actionRequired: false,
        oldValueSummary: "Describe key business risks.",
        newValueSummary: "Describe key business risks and how management monitors them."
      },
      {
        id: "/sections/riskAssessment/questions/6",
        sectionKey: "riskAssessment",
        sectionDisplayName: "Risk Assessment",
        changeType: "ADDED",
        title: "Which sources were used to identify emerging risks?",
        description: "Added \"Which sources were used to identify emerging risks?\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: null,
        newValueSummary: "Which sources were used to identify emerging risks?"
      },
      {
        id: "/sections/documentation/legacyRiskMatrix",
        sectionKey: "documentation",
        sectionDisplayName: "Documentation",
        changeType: "REMOVED",
        title: "Legacy risk matrix",
        description: "Removed \"Legacy risk matrix\".",
        impactLevel: "HIGH",
        actionRequired: true,
        oldValueSummary: "Legacy risk matrix",
        newValueSummary: null
      },
      {
        id: "/sections/riskAssessment/scoring/highRiskThreshold",
        sectionKey: "riskAssessment",
        sectionDisplayName: "Risk Assessment",
        changeType: "MODIFIED",
        title: "High Risk Threshold",
        description: "Changed /sections/riskAssessment/scoring/highRiskThreshold from \"8\" to \"7\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: "8",
        newValueSummary: "7"
      },
      {
        id: "/sections/monitoring/checklists/control-changes",
        sectionKey: "monitoring",
        sectionDisplayName: "Monitoring",
        changeType: "ADDED",
        title: "Control changes since prior assessment",
        description: "Added \"Control changes since prior assessment\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: null,
        newValueSummary: "Control changes since prior assessment"
      },
      {
        id: "/sections/riskAssessment/guidance/reassessmentFrequencyMonths",
        sectionKey: "riskAssessment",
        sectionDisplayName: "Risk Assessment",
        changeType: "MODIFIED",
        title: "Reassessment Frequency Months",
        description: "Changed /sections/riskAssessment/guidance/reassessmentFrequencyMonths from \"12\" to \"9\".",
        impactLevel: "MEDIUM",
        actionRequired: true,
        oldValueSummary: "12",
        newValueSummary: "9"
      }
    ]
  }
};
