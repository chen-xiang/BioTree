/**
 * HTTP 客户端：credentials 携带 Session Cookie；类型来自 schema.d.ts。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 接入 OpenAPI paths 类型
 */
import createClient from 'openapi-fetch'
import type { paths } from './schema'

export type { paths }

export const apiClient = createClient<paths>({
  baseUrl: '/',
  credentials: 'include',
})

export type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

export async function fetchHealth(): Promise<ApiResponse<{ status: string }>> {
  const response = await fetch('/api/health', { credentials: 'include' })
  if (!response.ok) {
    throw new Error(`Health check failed: ${response.status}`)
  }
  return response.json()
}
