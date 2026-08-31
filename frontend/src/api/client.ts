/**
 * HTTP 客户端骨架：credentials 携带 Session Cookie。
 * 后端 OpenAPI 就绪后，用 openapi-typescript 生成类型并替换 paths。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import createClient from 'openapi-fetch'

/** 占位 Paths，待 `pnpm openapi:generate` 后替换为生成类型。 */
export type paths = Record<string, never>

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
