import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UpdateSummaryModalComponent } from './update-summary-modal.component';
import { EngagementApiService, API_LATENCY_MS } from '../../core/services/engagement-api.service';
import { EngagementStore } from '../../core/store/engagement.store';

describe('UpdateSummaryModalComponent', () => {
  let fixture: ComponentFixture<UpdateSummaryModalComponent>;
  let store: InstanceType<typeof EngagementStore>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideZonelessChangeDetection(),
        { provide: API_LATENCY_MS, useValue: 0 },
      ],
    });
    fixture = TestBed.createComponent(UpdateSummaryModalComponent);
    store = TestBed.inject(EngagementStore);
    store.loadEngagements();
    fixture.detectChanges();
  });

  const text = (testId: string): string =>
    (fixture.nativeElement.querySelector(`[data-testid="${testId}"]`) as HTMLElement | null)?.textContent?.trim() ?? '';

  it('renders nothing until an engagement with pending updates is selected', () => {
    expect(store.isSummaryOpen()).toBeFalse();
    expect(fixture.nativeElement.querySelector('[data-testid="modal-title"]')).toBeNull();
  });

  it('renders the upgrade route and the accumulated diffs of the engagement', () => {
    store.openSummary('ENG-1003');
    fixture.detectChanges();

    expect(text('modal-title')).toContain('Harbourview Logistics 2026');
    expect(text('modal-subtitle')).toContain('ENG-1003');
    expect(text('modal-subtitle')).toContain('Canadian Audit Engagement');
    expect(text('modal-version')).toContain('3');
    expect(text('modal-version')).toContain('5');
    expect(text('modal-accumulated')).toContain('v4');
    expect(text('modal-accumulated')).toContain('v5');
  });

  it('groups the changes by section, in backend order', () => {
    store.openSummary('ENG-1003');
    fixture.detectChanges();

    const sections = [...fixture.nativeElement.querySelectorAll('[data-testid="modal-section-name"]')].map(
      (element: Element) => element.textContent?.trim(),
    );
    expect(sections).toEqual(['Planning', 'Materiality', 'Completion']);
    expect(fixture.nativeElement.querySelectorAll('[data-testid="change-item"]').length).toBe(
      store.summaryChanges().length,
    );
  });

  it('groups the changes without a section under a general heading', () => {
    store.openSummary('ENG-1006');
    fixture.detectChanges();

    const sections = [...fixture.nativeElement.querySelectorAll('[data-testid="modal-section-name"]')].map(
      (element: Element) => element.textContent?.trim(),
    );
    expect(sections).toEqual(['General', 'Analytics', 'Completion']);
  });

  it('counts the changes per impact level and marks the ones requiring action', () => {
    store.openSummary('ENG-1003');
    fixture.detectChanges();

    expect(text('modal-stats')).toContain(`${store.summaryChanges().length} changes`);
    expect(text('modal-stats')).toContain(`${store.actionRequiredCount()} require action`);
    for (const level of ['HIGH', 'MEDIUM', 'LOW'] as const) {
      expect(text('modal-stats')).toContain(`${level === 'HIGH' ? 'High' : level === 'MEDIUM' ? 'Medium' : 'Low'}: ${store.summaryImpactCounts()[level]}`);
    }
  });

  it('loads the summary once and reuses it while the same engagement stays selected', () => {
    const api = TestBed.inject(EngagementApiService);
    const summaryRequest = spyOn(api, 'getPendingUpdateSummary').and.callThrough();

    store.openSummary('ENG-1003');
    fixture.detectChanges();

    expect(summaryRequest).toHaveBeenCalledOnceWith('ENG-1003');
    expect(store.summary()?.engagementId).toBe('ENG-1003');

    // Reopening the modal is served from the state, so the endpoint is not called again.
    store.openSummary('ENG-1003');
    expect(summaryRequest).toHaveBeenCalledTimes(1);
  });

  it('closes on the close button and on the backdrop', () => {
    store.openSummary('ENG-1003');
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="modal-close"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(store.selectedEngagementId()).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="modal-title"]')).toBeNull();

    store.openSummary('ENG-1003');
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('[data-testid="modal-backdrop"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(store.selectedEngagementId()).toBeNull();
  });
});
