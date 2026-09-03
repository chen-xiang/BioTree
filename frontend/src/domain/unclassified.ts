/**
 * simple 视图未分类占位节点（负 id，不入库）。
 *
 * Author: chen-xiang
 * Created: 2026-09-03
 */

export function isUnclassifiedId(id: number | null | undefined): boolean {
  return id != null && id < 0
}
