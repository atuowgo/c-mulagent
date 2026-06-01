export interface AgentEvent {
  id: string;
  type: string;
  source: string;
  data: Record<string, unknown>;
  timestamp: string;
}