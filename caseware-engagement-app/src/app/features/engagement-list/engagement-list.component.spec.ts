import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { EngagementListComponent } from './engagement-list.component';
import { EngagementApiService, API_LATENCY_MS } from '../../core/services/engagement-api.service';
import { EngagementListResponse } from '../../core/models/engagement.model';
import { EngagementStore } from '../../core/store/engagement.store';

describe('EngagementListComponent', () => {
  let fixture: ComponentFixture<EngagementListComponent>;
  let store: InstanceType<typeof EngagementStore>;

  const rowFor = (engagementId: string): HTMLElement =>
    fixture.nativeElement.querySelector(`[data-engagement-id="${engagementId}"]`) as HTMLElement;

  const rows = (): HTMLElement[] => [
    ...fixture.nativeElement.querySelectorAll('[data-testid="engagement-row"]'),
  ];

  const textOf = (testId: string): string =>
    (fixture.nativeElement.querySelector(`[data-testid="${testId}"]`) as HTMLElement | null)
      ?.textContent?.trim() ?? '';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideZonelessChangeDetection(),
        { provide: API_LATENCY_MS, useValue: 0 },
      ],
    });
    fixture = TestBed.createComponent(EngagementListComponent);
    store = TestBed.inject(EngagementStore);
    // ngOnInit loads the engagements; with a zero latency the response arrives synchronously.
    fixture.detectChanges();
  });

  it('renders one row per engagement of the response, with its template version', () => {
    expect(rows().length).toBe(12);
    expect(textOf('total-count')).toContain('12');
    expect(textOf('pending-count')).toContain('6');

    const row = rowFor('ENG-1003');
    expect(row.textContent).toContain('Harbourview Logistics 2026');
    expect(row.textContent).toContain('Canadian Audit Engagement');
    expect(row.textContent).toContain('3');
    expect(row.textContent).toContain('5');
    expect(row.textContent).toContain('Update available');
  });

  it('loads the list through the store, which is the one calling the api service', () => {
    const api = TestBed.inject(EngagementApiService);
    const listRequest = spyOn(api, 'getEngagements').and.callThrough();

    store.loadEngagements();
    fixture.detectChanges();

    expect(listRequest).toHaveBeenCalledTimes(1);
    expect(store.engagements().length).toBe(12);
    expect(store.totalCount()).toBe(12);
    expect(store.pendingUpdatesCount()).toBe(6);
  });

  it('marks the engagements that are up to date', () => {
    expect(rowFor('ENG-1001').dataset['status']).toBe('UP_TO_DATE');
    expect(rowFor('ENG-1001').textContent).toContain('Up to date');
    expect(rowFor('ENG-1002').dataset['status']).toBe('PENDING_UPDATE');
  });

  it('shows an empty state when the response has no engagement', () => {
    const api = TestBed.inject(EngagementApiService);
    const emptyResponse: EngagementListResponse = {
      totalCount: 0,
      pendingUpdatesCount: 0,
      engagements: [],
    };
    spyOn(api, 'getEngagements').and.returnValue(of(emptyResponse));

    store.loadEngagements();
    fixture.detectChanges();

    expect(rows().length).toBe(0);
    expect(textOf('empty-state')).toContain('No engagements available');
  });

  it('only enables the review action for engagements with pending updates', () => {
    const reviewButton = (engagementId: string): HTMLButtonElement =>
      rowFor(engagementId).querySelector('[data-testid="review-button"]') as HTMLButtonElement;

    expect(reviewButton('ENG-1001').disabled).toBeTrue();
    expect(reviewButton('ENG-1002').disabled).toBeFalse();
  });

  it('opens the read only summary of a pending engagement', () => {
    (rowFor('ENG-1003').querySelector('[data-testid="review-button"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(store.selectedEngagementId()).toBe('ENG-1003');
    expect(textOf('modal-title')).toContain('Harbourview Logistics 2026');
    expect(textOf('modal-total-changes')).toContain('6');
  });

  it('ignores the engagements that are already up to date', () => {
    (rowFor('ENG-1004').querySelector('[data-testid="review-button"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(store.selectedEngagementId()).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="modal-title"]')).toBeNull();
  });

  it('closes the summary from the modal', () => {
    store.openSummary('ENG-1006');
    fixture.detectChanges();
    expect(textOf('modal-title')).toContain('Westmount Consulting 2026');

    (fixture.nativeElement.querySelector('[data-testid="modal-cancel"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(store.selectedEngagementId()).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="modal-title"]')).toBeNull();
  });
});
