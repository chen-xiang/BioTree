/**
 * 分类相关 API 调用（公开 + 管理），基于 openapi-fetch。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加配图上传与删除接口
 * Updated: 2026-08-31 支持 AbortSignal、节点移动
 * Updated: 2026-08-31 切换 openapi-fetch；配图分页
 */
import { getCsrfHeaders } from '@/utils/csrf'
import { apiClient, unwrap } from './client'
import type { components } from './schema'

export type TaxonRank = components['schemas']['TaxonRank']
export type TaxonListItem = components['schemas']['TaxonListItem']
export type PageResult<T> = {
  items: T[]
  total: number
  page: number
  size: number
}
export type TaxonBreadcrumb = components['schemas']['TaxonBreadcrumb']
export type TaxonMedia = components['schemas']['TaxonMedia']
export type TaxonSynonym = components['schemas']['TaxonSynonym']
export type TaxonDetail = components['schemas']['TaxonDetail']
export type CreateTaxonPayload = components['schemas']['CreateTaxonRequest']
export type UpdateTaxonPayload = components['schemas']['UpdateTaxonRequest']

export async function fetchChildren(
  parentId: number | null,
  locale: string,
  page = 0,
  size = 30,
  signal?: AbortSignal,
): Promise<PageResult<TaxonListItem>> {
  const result = await apiClient.GET('/api/taxa/children', {
    params: {
      query: {
        parentId: parentId ?? undefined,
        locale,
        page,
        size,
      },
    },
    signal,
  })
  return unwrap(result)
}

export async function fetchTaxonDetail(
  id: number,
  locale: string,
  signal?: AbortSignal,
): Promise<TaxonDetail> {
  const result = await apiClient.GET('/api/taxa/{id}', {
    params: { path: { id }, query: { locale } },
    signal,
  })
  return unwrap(result)
}

export async function fetchTaxonMedia(
  id: number,
  page = 0,
  size = 12,
  signal?: AbortSignal,
): Promise<PageResult<TaxonMedia>> {
  const result = await apiClient.GET('/api/taxa/{id}/media', {
    params: { path: { id }, query: { page, size } },
    signal,
  })
  return unwrap(result)
}

export async function searchTaxa(
  q: string,
  locale: string,
  page = 0,
  size = 30,
  signal?: AbortSignal,
): Promise<PageResult<TaxonListItem>> {
  const result = await apiClient.GET('/api/taxa/search', {
    params: { query: { q, locale, page, size } },
    signal,
  })
  return unwrap(result)
}

export async function createTaxon(payload: CreateTaxonPayload): Promise<TaxonDetail> {
  const result = await apiClient.POST('/api/admin/taxa', {
    body: payload,
    headers: { ...getCsrfHeaders() },
  })
  return unwrap(result)
}

export async function updateTaxon(id: number, payload: UpdateTaxonPayload): Promise<TaxonDetail> {
  const result = await apiClient.PUT('/api/admin/taxa/{id}', {
    params: { path: { id } },
    body: payload,
    headers: { ...getCsrfHeaders() },
  })
  return unwrap(result)
}

export async function moveTaxon(id: number, newParentId: number, locale?: string): Promise<TaxonDetail> {
  const result = await apiClient.POST('/api/admin/taxa/{id}/move', {
    params: { path: { id }, query: locale ? { locale } : undefined },
    body: { newParentId },
    headers: { ...getCsrfHeaders() },
  })
  return unwrap(result)
}

export async function deleteTaxon(id: number): Promise<void> {
  const result = await apiClient.DELETE('/api/admin/taxa/{id}', {
    params: { path: { id } },
    headers: { ...getCsrfHeaders() },
  })
  unwrap(result)
}

export async function uploadTaxonMedia(
  taxonId: number,
  file: File,
  options?: { locale?: string; caption?: string; license?: string; attribution?: string },
): Promise<TaxonMedia> {
  const form = new FormData()
  form.append('file', file)
  if (options?.locale) form.append('locale', options.locale)
  if (options?.caption) form.append('caption', options.caption)
  if (options?.license) form.append('license', options.license)
  if (options?.attribution) form.append('attribution', options.attribution)
  const result = await apiClient.POST('/api/admin/taxa/{taxonId}/media', {
    params: { path: { taxonId } },
    // openapi-fetch 对 multipart 接受 FormData
    body: form as unknown as {
      file: string
      locale?: string
      caption?: string
      license?: string
      attribution?: string
    },
    headers: { ...getCsrfHeaders() },
    bodySerializer: (body) => body as unknown as FormData,
  })
  return unwrap(result)
}

export async function deleteTaxonMedia(taxonId: number, mediaId: number): Promise<void> {
  const result = await apiClient.DELETE('/api/admin/taxa/{taxonId}/media/{mediaId}', {
    params: { path: { taxonId, mediaId } },
    headers: { ...getCsrfHeaders() },
  })
  unwrap(result)
}
