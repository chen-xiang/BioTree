/**
 * HTTP 客户端：credentials 携带 Session Cookie；类型来自 schema.d.ts。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 接入 OpenAPI paths 类型与统一解包
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

/** 带业务码的 API 错误，供 i18n 映射 */
export class ApiError extends Error {
  readonly code: number
  readonly status: number

  constructor(code: number, message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

type Envelope<T> = {
  code?: number
  message?: string
  data?: T
}

/**
 * 将 openapi-fetch 结果解包为 data；非 0 code 或非 2xx 抛 ApiError。
 */
export function unwrap<T>(result: {
  data?: unknown
  error?: unknown
  response: Response
}): T {
  const body = (result.data ?? result.error) as Envelope<T> | undefined
  const code = body?.code ?? -1
  const message = body?.message || `Request failed: ${result.response.status}`
  if (!result.response.ok || code !== 0) {
    throw new ApiError(code, message, result.response.status)
  }
  return body!.data as T
}

export async function fetchHealth(): Promise<ApiResponse<{ status: string }>> {
  const result = await apiClient.GET('/api/health')
  const data = unwrap<{ status: string }>(result)
  return { code: 0, message: 'OK', data }
}
