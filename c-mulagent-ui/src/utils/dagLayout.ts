import type { Subtask } from '../types/task';
import type { Node, Edge } from 'reactflow';

const NODE_WIDTH = 200;
const NODE_HEIGHT = 80;
const HORIZONTAL_GAP = 40;
const VERTICAL_GAP = 60;

/**
 * Layered DAG layout based on topological ordering.
 * Each node's layer = max depth from any root node.
 * Nodes within a layer are evenly distributed horizontally.
 */
export function layoutDAG(subtasks: Subtask[]): { nodes: Node[]; edges: Edge[] } {
  if (subtasks.length === 0) return { nodes: [], edges: [] };

  // Build adjacency: name -> subtask
  const byName = new Map<string, Subtask>();
  for (const st of subtasks) byName.set(st.name, st);

  // Compute layer (depth) for each node via DFS
  const layer = new Map<string, number>();

  function getLayer(name: string, visited: Set<string>): number {
    if (layer.has(name)) return layer.get(name)!;
    if (visited.has(name)) return 0; // cycle guard
    visited.add(name);

    const st = byName.get(name);
    if (!st || st.dependencies.length === 0) {
      layer.set(name, 0);
      return 0;
    }

    let maxDep = 0;
    for (const dep of st.dependencies) {
      maxDep = Math.max(maxDep, getLayer(dep, new Set(visited)) + 1);
    }
    layer.set(name, maxDep);
    return maxDep;
  }

  for (const st of subtasks) {
    getLayer(st.name, new Set());
  }

  // Group nodes by layer
  const layerGroups = new Map<number, Subtask[]>();
  let maxLayer = 0;
  for (const st of subtasks) {
    const l = layer.get(st.name) ?? 0;
    if (!layerGroups.has(l)) layerGroups.set(l, []);
    layerGroups.get(l)!.push(st);
    if (l > maxLayer) maxLayer = l;
  }

  // Generate nodes with positions
  const nodes: Node[] = [];
  for (let l = 0; l <= maxLayer; l++) {
    const group = layerGroups.get(l) ?? [];
    const totalWidth = group.length * NODE_WIDTH + (group.length - 1) * HORIZONTAL_GAP;
    const startX = -totalWidth / 2;

    for (let i = 0; i < group.length; i++) {
      const st = group[i];
      nodes.push({
        id: st.id,
        type: 'subtaskNode',
        position: {
          x: startX + i * (NODE_WIDTH + HORIZONTAL_GAP),
          y: l * (NODE_HEIGHT + VERTICAL_GAP),
        },
        data: { subtask: st },
      });
    }
  }

  // Generate edges from dependency names -> subtask IDs
  const edges: Edge[] = [];
  const nameToId = new Map<string, string>();
  for (const st of subtasks) nameToId.set(st.name, st.id);

  for (const st of subtasks) {
    for (const depName of st.dependencies) {
      const sourceId = nameToId.get(depName);
      if (sourceId) {
        edges.push({
          id: `e-${sourceId}-${st.id}`,
          source: sourceId,
          target: st.id,
          type: 'smoothstep',
          animated: st.status === 'running',
          style: { stroke: edgeColor(st.status), strokeWidth: 2 },
        });
      }
    }
  }

  return { nodes, edges };
}

function edgeColor(status: string): string {
  switch (status) {
    case 'running': return '#3b82f6';
    case 'done': return '#22c55e';
    case 'failed': return '#ef4444';
    default: return '#64748b';
  }
}