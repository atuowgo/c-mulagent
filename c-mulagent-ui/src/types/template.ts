export interface TaskTemplate {
  id: string;
  name: string;
  description: string;
  category?: string;
  planTemplate: string;
  agentBindings?: unknown;
  skillBindings?: unknown;
  toolBindings?: unknown;
  version: string;
  enabled: boolean;
  createdAt: string;
  updatedAt?: string;
}