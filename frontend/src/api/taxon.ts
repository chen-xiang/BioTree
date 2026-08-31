/**
 * 分类相关 API 调用（公开 + 管理）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加配图上传与删除接口
 * Updated: 2026-08-31 支持 AbortSignal、节点移动
 */
import type { ApiResponse } from './client'

export type TaxonRank =
  | 'KINGDOM'
  | 'PHYLUM'
  | 'CLASS'
  | 'ORDER'
  | 'FAMILY'
  | 'GENUS'
  | 'SPECIES'

export type TaxonListItem = {
  id: number
  rank: TaxonRank
  scientificName: string
  commonName: string | null
  childCount: number
  hasChildren: boolean
}

export type PageResult<T> = {
  items: T[]
  total: number
  page: number
  size: number
}

export type TaxonBreadcrumb = {
  id: number
  rank: TaxonRank
  scientificName: string
  commonName: string | null
}

export type TaxonMedia = {
  id: number
  url: string
  mimeType: string | null
  width?: number | null
  height?: number | null
  sortOrder?: number
  locale?: string | null
  caption: string | null
  license?: string | null
  attribution?: string | null
}

export type TaxonDetail = {
  id: number
  parentId: number | null
  rank: TaxonRank
  scientificName: string
  commonName: string | null
  summary: string | null
  description: string | null
  locale: string
  childCount: number
  accepted: boolean
  breadcrumbs: TaxonBreadcrumb[]
  media: TaxonMedia[]
}

export type CreateTaxonPayload = {
  parentId?: number | null
  rank: TaxonRank
  scientificName: string
  locale?: string
  commonName?: string
  summary?: string
  description?: string
}

export type UpdateTaxonPayload = {
  scientificName: string
  accepted?: boolean
  locale?: string
  commonName?: string
  summary?: string
  description?: string
}

async function parseJson<T>(response: Response): Promise<ApiResponse<T>> {
  const body = (await response.json()) as ApiResponse<T>
  if (!response.ok || body.code !== 0) {
    throw new Error(body.message || `Request failed: ${response.status}`)
  }
  return body
}

export async function fetchChildren(
  parentId: number | null,
  locale: string,
  page = 0,
  size = 30,
  signal?: AbortSignal,
): Promise<PageResult<TaxonListItem>> {
  const params = new URLSearchParams({
    locale,
    page: String(page),
    size: String(size),
  })
  if (parentId != null) {
    params.set('parentId', String(parentId))
  }
  const response = await fetch(`/api/taxa/children?${params}`, { credentials: 'include', signal })
  return (await parseJson<PageResult<TaxonListItem>>(response)).data
}

export async function fetchTaxonDetail(
  id: number,
  locale: string,
  signal?: AbortSignal,
): Promise<TaxonDetail> {
  const params = new URLSearchParams({ locale })
  const response = await fetch(`/api/taxa/${id}?${params}`, { credentials: 'include', signal })
  return (await parseJson<TaxonDetail>(response)).data
}

export async function searchTaxa(
  q: string,
  locale: string,
  page = 0,
  size = 30,
  signal?: AbortSignal,
): Promise<PageResult<TaxonListItem>> {
  const params = new URLSearchParams({
    q,
    locale,
    page: String(page),
    size: String(size),
  })
  const response = await fetch(`/api/taxa/search?${params}`, { credentials: 'include', signal })
  return (await parseJson<PageResult<TaxonListItem>>(response)).data
}

export async function createTaxon(payload: CreateTaxonPayload): Promise<TaxonDetail> {
  const response = await fetch('/api/admin/taxa', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  return (await parseJson<TaxonDetail>(response)).data
}

export async function updateTaxon(id: number, payload: UpdateTaxonPayload): Promise<TaxonDetail> {
  const response = await fetch(`/api/admin/taxa/${id}`, {
    method: 'PUT',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  return (await parseJson<TaxonDetail>(response)).data
}

export async function moveTaxon(id: number, newParentId: number, locale?: string): Promise<TaxonDetail> {
  const params = new URLSearchParams()
  if (locale) params.set('locale', locale)
  const qs = params.toString()
  const response = await fetch(`/api/admin/taxa/${id}/move${qs ? `?${qs}` : ''}`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ newParentId }),
  })
  return (await parseJson<TaxonDetail>(response)).data
}

export async function deleteTaxon(id: number): Promise<void> {
  const response = await fetch(`/api/admin/taxa/${id}`, {
    method: 'DELETE',
    credentials: 'include',
  })
  await parseJson<null>(response)
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
  const response = await fetch(`/api/admin/taxa/${taxonId}/media`, {
    method: 'POST',
    credentials: 'include',
    body: form,
  })
  return (await parseJson<TaxonMedia>(response)).data
}

export async function deleteTaxonMedia(taxonId: number, mediaId: number): Promise<void> {
  const response = await fetch(`/api/admin/taxa/${taxonId}/media/${mediaId}`, {
    method: 'DELETE',
    credentials: 'include',
  })
  await parseJson<null>(response)
}
