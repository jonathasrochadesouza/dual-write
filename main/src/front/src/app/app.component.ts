import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ExperimentService } from './experiment.service';
import {
  ExperimentHistoryItem,
  ExperimentReport,
  SCENARIOS,
  ScenarioCard,
  ScenarioId,
  VERDICT_CONFIG,
  Verdict
} from './models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private readonly experimentService = inject(ExperimentService);

  readonly scenarios = SCENARIOS;
  readonly dualWriteScenarios = SCENARIOS.filter((s) => s.pattern === 'dual-write');
  readonly outboxScenarios = SCENARIOS.filter((s) => s.pattern === 'outbox');

  readonly selected = signal<ScenarioId | null>(null);
  readonly running = signal(false);
  readonly error = signal<string | null>(null);
  readonly report = signal<ExperimentReport | null>(null);
  readonly history = signal<ExperimentHistoryItem[]>([]);

  readonly canRun = computed(() => this.selected() !== null && !this.running());

  readonly ordersInDb = computed(() => {
    let count = 0;
    for (const h of this.history()) {
      if (h.verdict === 'ATOMICO' || h.scenario === 'DUAL_WRITE_BOTH_OK' || h.scenario === 'DUAL_WRITE_DB_ONLY' || h.scenario === 'OUTBOX_COMMIT') {
        count++;
      }
    }
    return count;
  });

  readonly eventsInKafka = computed(() => {
    let count = 0;
    for (const h of this.history()) {
      if (h.verdict === 'ATOMICO' || h.scenario === 'DUAL_WRITE_BOTH_OK' || h.scenario === 'DUAL_WRITE_KAFKA_ONLY') {
        count++;
      }
    }
    return count;
  });

  readonly totalExperiments = computed(() => this.history().length);

  ngOnInit(): void {
    this.reloadHistory();
  }

  select(scenario: ScenarioCard): void {
    this.selected.set(scenario.id);
    this.error.set(null);
  }

  run(): void {
    const scenario = this.selected();
    if (!scenario) {
      this.error.set('Select a scenario before running.');
      return;
    }

    this.running.set(true);
    this.error.set(null);

    this.experimentService.run(scenario).subscribe({
      next: (result) => {
        this.report.set(result);
        this.running.set(false);
        this.reloadHistory();
      },
      error: (err) => {
        this.running.set(false);
        this.error.set(err?.error?.message ?? 'Failed to run experiment.');
      }
    });
  }

  reloadHistory(): void {
    this.experimentService.history().subscribe({
      next: (items) => this.history.set(items),
      error: () => this.history.set([])
    });
  }

  verdictLabel(verdict: Verdict): string {
    return VERDICT_CONFIG[verdict]?.label ?? verdict;
  }

  verdictColor(verdict: Verdict): string {
    return VERDICT_CONFIG[verdict]?.color ?? '#6b7280';
  }

  verdictBg(verdict: Verdict): string {
    return VERDICT_CONFIG[verdict]?.bg ?? '#f3f4f6';
  }

  verdictBorder(verdict: Verdict): string {
    return VERDICT_CONFIG[verdict]?.border ?? '#e5e7eb';
  }

  scenarioCode(scenarioId: ScenarioId): string {
    return SCENARIOS.find((s) => s.id === scenarioId)?.code ?? '';
  }

  scenarioTitle(scenarioId: ScenarioId): string {
    return SCENARIOS.find((s) => s.id === scenarioId)?.title ?? scenarioId;
  }

  observedCount(report: ExperimentReport, kind: 'orders' | 'outbox' | 'kafka'): number {
    if (kind === 'orders') {
      return report.observed.orders?.length ?? report.observed.orderCount ?? 0;
    }
    if (kind === 'outbox') {
      return report.observed.outboxEvents?.length ?? report.observed.outboxCount ?? 0;
    }
    return report.observed.kafkaPayloads?.length ?? report.observed.kafkaCount ?? 0;
  }
}
