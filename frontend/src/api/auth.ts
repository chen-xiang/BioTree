/**
 * 前端认证 API（openapi-fetch）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 切换 openapi-fetch
 */
import { getCsrfHeaders } from '@/utils/csrf'
import { apiClient, unwrap } from './client'

export async function login(username: string, password: string): Promise<string> {
  const result = await apiClient.POST('/api/admin/auth/login', {
    body: { username, password },
  })
  const data = unwrap<{ username: string }>(result)
  return data.username
}

export async function fetchMe(): Promise<string | null> {
  try {
    const result = await apiClient.GET('/api/admin/auth/me')
    if (!result.response.ok) return null
    const data = unwrap<{ username: string }>(result)
    return data.username || null
  } catch {
    return null
  }
}

export async function logout(): Promise<void> {
  const result = await apiClient.POST('/api/admin/auth/logout', {
    headers: { ...getCsrfHeaders() },
  })
  unwrap(result)
}
