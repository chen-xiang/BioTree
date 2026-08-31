/**
 * 读取 Cookie CSRF token，供管理端写请求使用。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
export function getCsrfHeaders(): Record<string, string> {
  if (typeof document === 'undefined') {
    return {}
  }
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)
  if (!match?.[1]) {
    return {}
  }
  return { 'X-XSRF-TOKEN': decodeURIComponent(match[1]) }
}

/** 触发一次 GET 以确保 Spring 下发 XSRF-TOKEN Cookie。 */
export async function ensureCsrfCookie(): Promise<void> {
  await fetch('/api/health', { credentials: 'include' })
}
