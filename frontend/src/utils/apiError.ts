/**
 * 将后端 ApiError.code 映射为 i18n 文案。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { i18n } from '@/locales'
import { ApiError } from '@/api/client'

const CODE_KEYS: Record<number, string> = {
  40000: 'errors.validation',
  40001: 'errors.INVALID_PARENT',
  40002: 'errors.INVALID_MOVE',
  40003: 'errors.INVALID_QUERY',
  40004: 'errors.INVALID_UPLOAD',
  40100: 'errors.unauthorized',
  40300: 'errors.forbidden',
  40400: 'errors.notFound',
  40401: 'errors.TAXON_NOT_FOUND',
  40402: 'errors.MEDIA_NOT_FOUND',
  40900: 'errors.conflict',
  40901: 'errors.TAXON_HAS_CHILDREN',
  40902: 'errors.DUPLICATE_NAME',
  50000: 'errors.server',
}

export function messageFromApiError(err: unknown, fallbackKey = 'errors.server'): string {
  const t = i18n.global.t
  if (err instanceof ApiError) {
    const key = CODE_KEYS[err.code]
    if (key) return String(t(key))
    if (err.status === 401) return String(t('errors.unauthorized'))
    if (err.status === 403) return String(t('errors.forbidden'))
    if (err.status === 404) return String(t('errors.notFound'))
    if (err.status === 409) return String(t('errors.conflict'))
    if (err.status === 400) return String(t('errors.validation'))
  }
  if (err instanceof Error && err.message) {
    return err.message
  }
  return String(t(fallbackKey))
}

export function rankLabel(rank: string): string {
  const key = `rank.${rank}`
  const translated = i18n.global.t(key)
  return translated === key ? rank : String(translated)
}
