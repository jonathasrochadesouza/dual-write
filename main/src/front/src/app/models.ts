export type ScenarioId =
  | 'DUAL_WRITE_BOTH_OK'
  | 'DUAL_WRITE_NONE'
  | 'DUAL_WRITE_DB_ONLY'
  | 'DUAL_WRITE_KAFKA_ONLY'
  | 'OUTBOX_COMMIT'
  | 'OUTBOX_ROLLBACK';

export type Verdict = 'ATOMICO' | 'INCONSISTENTE' | 'OPERACAO_PERDIDA';

export interface ScenarioCard {
  id: ScenarioId;
  code: string;
  title: string;
  shortTitle: string;
  pattern: 'dual-write' | 'outbox';
  summary: string;
  dbWrites: boolean;
  kafkaPublishes: boolean;
  expectedVerdict: Verdict;
}

export interface ExpectedOutcome {
  orderPresent: boolean;
  outboxPresent: boolean;
  kafkaPresent: boolean;
  kafkaEmptyExpected: boolean;
  expectedVerdict: Verdict;
  description: string;
}

export interface ObservedState {
  orders: unknown[];
  outboxEvents: unknown[];
  kafkaPayloads: string[];
  orderCount?: number;
  outboxCount?: number;
  kafkaCount?: number;
}

export interface ExperimentReport {
  experimentId: string;
  scenario: ScenarioId;
  executedAt: string;
  expected: ExpectedOutcome;
  observed: ObservedState;
  verdict: Verdict;
  message: string;
}

export interface ExperimentHistoryItem {
  id: string;
  scenario: ScenarioId;
  executedAt: string;
  verdict: Verdict | null;
}

export const SCENARIOS: ScenarioCard[] = [
  {
    id: 'DUAL_WRITE_BOTH_OK',
    code: 'C1',
    title: 'Dual Write — Both OK',
    shortTitle: 'Both OK',
    pattern: 'dual-write',
    summary: 'DB and Kafka both succeed — correct by chance, no atomicity',
    dbWrites: true,
    kafkaPublishes: true,
    expectedVerdict: 'ATOMICO'
  },
  {
    id: 'DUAL_WRITE_NONE',
    code: 'C2',
    title: 'Dual Write — None',
    shortTitle: 'None',
    pattern: 'dual-write',
    summary: 'Failure before commit — lost operation',
    dbWrites: false,
    kafkaPublishes: false,
    expectedVerdict: 'OPERACAO_PERDIDA'
  },
  {
    id: 'DUAL_WRITE_DB_ONLY',
    code: 'C3',
    title: 'Dual Write — DB Only',
    shortTitle: 'DB Only',
    pattern: 'dual-write',
    summary: 'Phantom order — publish fails after commit',
    dbWrites: true,
    kafkaPublishes: false,
    expectedVerdict: 'INCONSISTENTE'
  },
  {
    id: 'DUAL_WRITE_KAFKA_ONLY',
    code: 'C4',
    title: 'Dual Write — Kafka Only',
    shortTitle: 'Kafka Only',
    pattern: 'dual-write',
    summary: 'Phantom event — DB fails after publish',
    dbWrites: false,
    kafkaPublishes: true,
    expectedVerdict: 'INCONSISTENTE'
  },
  {
    id: 'OUTBOX_COMMIT',
    code: 'C5',
    title: 'Outbox — Commit',
    shortTitle: 'Commit',
    pattern: 'outbox',
    summary: 'order + outbox_event committed atomically',
    dbWrites: true,
    kafkaPublishes: false,
    expectedVerdict: 'ATOMICO'
  },
  {
    id: 'OUTBOX_ROLLBACK',
    code: 'C6',
    title: 'Outbox — Rollback',
    shortTitle: 'Rollback',
    pattern: 'outbox',
    summary: 'Atomic rollback of both tables',
    dbWrites: false,
    kafkaPublishes: false,
    expectedVerdict: 'ATOMICO'
  }
];

export const VERDICT_CONFIG: Record<Verdict, { label: string; color: string; bg: string; border: string }> = {
  ATOMICO:          { label: 'Atomic',      color: '#16a34a', bg: '#dcfce7', border: '#bbf7d0' },
  INCONSISTENTE:    { label: 'Inconsistent', color: '#dc2626', bg: '#fef2f2', border: '#fecaca' },
  OPERACAO_PERDIDA: { label: 'Lost Op',      color: '#d97706', bg: '#fefce8', border: '#fef08a' }
};
