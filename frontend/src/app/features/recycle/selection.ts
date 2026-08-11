export interface RecycleTreeNode {
  key: React.Key;
  submitKey?: string | null;
  children?: RecycleTreeNode[];
}

export function normalizeSelectedRoots(
  selectedKeys: React.Key[],
  nodes: RecycleTreeNode[],
): string[] {
  const selected = new Set(selectedKeys.map(String));
  const roots: string[] = [];

  const collectSubmitKeys = (node: RecycleTreeNode): string[] => {
    if (node.submitKey !== null) {
      return [node.submitKey || String(node.key)];
    }
    return (node.children || []).flatMap(collectSubmitKeys);
  };

  const visit = (node: RecycleTreeNode, ancestorSelected: boolean) => {
    const key = String(node.key);
    const currentSelected = selected.has(key);
    if (currentSelected && !ancestorSelected) {
      roots.push(...collectSubmitKeys(node));
      return;
    }
    node.children?.forEach(child =>
      visit(child, ancestorSelected || currentSelected),
    );
  };

  nodes.forEach(node => visit(node, false));
  return roots;
}

export function flattenTreeKeys(nodes: RecycleTreeNode[]): string[] {
  return nodes.flatMap(node => [
    String(node.key),
    ...flattenTreeKeys(node.children || []),
  ]);
}
