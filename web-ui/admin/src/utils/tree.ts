/** 带子节点的树节点 */
export type TreeNode<T> = T & { children?: TreeNode<T>[] }

/**
 * 把扁平列表构建成树（后端 /system/menu/list 返回的是扁平列表）。
 *
 * @param list      扁平数据
 * @param idKey     主键字段名
 * @param parentKey 父级字段名（0 或缺失视为根节点）
 */
export function buildTree<T extends object>(
  list: T[],
  idKey = 'menuId',
  parentKey = 'parentId',
): TreeNode<T>[] {
  const pick = (item: T, key: string): unknown => (item as Record<string, unknown>)[key]

  const nodes = new Map<number, TreeNode<T>>()
  for (const item of list) {
    nodes.set(Number(pick(item, idKey)), { ...item })
  }

  const roots: TreeNode<T>[] = []
  for (const item of list) {
    const node = nodes.get(Number(pick(item, idKey)))
    if (!node) continue
    const parentId = Number(pick(item, parentKey) ?? 0)
    const parent = parentId ? nodes.get(parentId) : undefined
    if (parent) {
      if (!parent.children) parent.children = []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  }

  // 无子节点时删除 children，避免表格出现无意义的展开箭头
  const tidy = (items: TreeNode<T>[]) => {
    for (const item of items) {
      if (item.children && item.children.length > 0) tidy(item.children)
      else delete item.children
    }
  }
  tidy(roots)

  return roots
}

/** 统计树节点数量 */
export function countTree<T extends { children?: T[] }>(nodes: T[]): number {
  return nodes.reduce((sum, node) => sum + 1 + (node.children?.length ? countTree(node.children) : 0), 0)
}
