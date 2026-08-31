/**
 * 导入状态 API。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { apiClient, unwrap } from './client'
import type { components } from './schema'

export type ImportStatus = components['schemas']['ImportStatus']

export async function fetchImportStatus(): Promise<ImportStatus> {
  const result = await apiClient.GET('/api/admin/import/status')
  return unwrap(result)
}
