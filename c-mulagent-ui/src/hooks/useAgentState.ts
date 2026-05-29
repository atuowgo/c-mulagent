import { useAgentStore } from '../stores/agentStore';
import { useMemo } from 'react';
import { AgentState } from '../types/agent';

export function useAgentState(agentId?: string) {
  const agents = useAgentStore((s) => s.agents);
  const selectedAgentId = useAgentStore((s) => s.selectedAgentId);
  const selectAgent = useAgentStore((s) => s.selectAgent);

  const selectedAgent = useMemo(
    () => agents.find((a) => a.id === (agentId ?? selectedAgentId)) ?? null,
    [agents, agentId, selectedAgentId]
  );

  const idleCount = agents.filter((a) => a.state === AgentState.Idle).length;
  const runningCount = agents.filter((a) => a.state === AgentState.Running).length;
  const doneCount = agents.filter((a) => a.state === AgentState.Done).length;
  const errorCount = agents.filter((a) => a.state === AgentState.Error).length;

  return {
    agents,
    selectedAgent,
    selectAgent,
    stats: { idleCount, runningCount, doneCount, errorCount, total: agents.length },
  };
}