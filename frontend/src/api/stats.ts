/**
 * 统计 API（openapi-fetch）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 切换 openapi-fetch
 */
import { apiClient, unwrap } from './client'
import type { components } from './schema'

export type StatsSummary = components['schemas']['StatsSummary']

export async function fetchStatsSummary(): Promise<StatsSummary> {
  const result = await apiClient.GET('/api/stats/summary')
  return unwrap(result)
}
