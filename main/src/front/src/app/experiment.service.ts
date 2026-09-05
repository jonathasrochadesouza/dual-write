import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ExperimentHistoryItem, ExperimentReport, ScenarioId } from './models';

@Injectable({ providedIn: 'root' })
export class ExperimentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/experiments';

  run(scenario: ScenarioId): Observable<ExperimentReport> {
    return this.http.post<ExperimentReport>(`${this.baseUrl}/run`, { scenario });
  }

  report(id: string): Observable<ExperimentReport> {
    return this.http.get<ExperimentReport>(`${this.baseUrl}/${id}/report`);
  }

  history(): Observable<ExperimentHistoryItem[]> {
    return this.http.get<ExperimentHistoryItem[]>(this.baseUrl);
  }

  reset(): Observable<{ kafkaReset: boolean }> {
    return this.http.delete<{ kafkaReset: boolean }>(this.baseUrl);
  }

  health(): Observable<{ status: string }> {
    return this.http.get<{ status: string }>(`${this.baseUrl}/health`);
  }
}
