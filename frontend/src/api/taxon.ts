/**
 * 分类相关 API 调用（公开 + 管理）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
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
  media: Array<{
    id: number
    url: string
    mimeType: string | null
    caption: string | null
  }>
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
): Promise<PageResult<TaxonListItem>> {
  const params = new URLSearchParams({
    locale,
    page: String(page),
    size: String(size),
  })
  if (parentId != null) {
    params.set('parentId', String(parentId))
  }
  const response = await fetch(`/api/taxa/children?${params}`, { credentials: 'include' })
  return (await parseJson<PageResult<TaxonListItem>>(response)).data
}

export async function fetchTaxonDetail(id: number, locale: string): Promise<TaxonDetail> {
  const params = new URLSearchParams({ locale })
  const response = await fetch(`/api/taxa/${id}?${params}`, { credentials: 'include' })
  return (await parseJson<TaxonDetail>(response)).data
}

export async function searchTaxa(
  q: string,
  locale: string,
  page = 0,
  size = 30,
): Promise<PageResult<TaxonListItem>> {
  const params = new URLSearchParams({
    q,
    locale,
    page: String(page),
    size: String(size),
  })
  const response = await fetch(`/api/taxa/search?${params}`, { credentials: 'include' })
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

export async function deleteTaxon(id: number): Promise<void> {
  const response = await fetch(`/api/admin/taxa/${id}`, {
    method: 'DELETE',
    credentials: 'include',
  })
  await parseJson<null>(response)
}
