/**
 * 分类树子节点分页单测。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
import { describe, expect, it, vi } from 'vitest'
import type { TaxonListItem } from '@/api/taxon'
import { TREE_CHILD_PAGE_SIZE, useTaxonChildren } from './useTaxonChildren'

function item(id: number): TaxonListItem {
  return {
    id,
    rank: 'PHYLUM',
    scientificName: `Taxon ${id}`,
    commonName: null,
    childCount: 0,
    hasChildren: false,
    directChildCount: 0,
  }
}

function page(ids: number[], total: number, pageNo: number) {
  return {
    items: ids.map(item),
    total,
    page: pageNo,
    size: TREE_CHILD_PAGE_SIZE,
  }
}

describe('useTaxonChildren', () => {
  it('loads the first page and reports remaining siblings', async () => {
    const load = vi.fn().mockResolvedValue(page([1, 2], 5, 0))
    const pager = useTaxonChildren({
      parentId: () => 10,
      locale: () => 'zh-CN',
      view: () => 'simple',
      load,
    })

    await pager.loadFirstPage()

    expect(load).toHaveBeenCalledWith(10, 'zh-CN', 0, TREE_CHILD_PAGE_SIZE, undefined, 'simple')
    expect(pager.children.value.map((row) => row.id)).toEqual([1, 2])
    expect(pager.hasMore.value).toBe(true)
  })

  it('appends the next page and clears hasMore when complete', async () => {
    const load = vi
      .fn()
      .mockResolvedValueOnce(page([1, 2], 3, 0))
      .mockResolvedValueOnce(page([3], 3, 1))
    const pager = useTaxonChildren({
      parentId: () => 10,
      locale: () => 'en',
      view: () => 'full',
      load,
    })

    await pager.loadFirstPage()
    const added = await pager.loadMore()

    expect(added).toBe(1)
    expect(pager.children.value.map((row) => row.id)).toEqual([1, 2, 3])
    expect(pager.hasMore.value).toBe(false)
  })

  it('stops when a page returns no items so auto-load cannot loop', async () => {
    const load = vi
      .fn()
      .mockResolvedValueOnce(page([1], 8, 0))
      .mockResolvedValueOnce(page([], 8, 1))
    const pager = useTaxonChildren({
      parentId: () => 10,
      locale: () => 'zh-CN',
      view: () => 'simple',
      load,
    })

    await pager.loadFirstPage()
    await pager.loadMore()

    expect(pager.hasMore.value).toBe(false)
    expect(pager.children.value).toHaveLength(1)
  })

  it('reset clears cached children so a later expand refetches', async () => {
    const load = vi.fn().mockResolvedValue(page([1], 1, 0))
    const pager = useTaxonChildren({
      parentId: () => 10,
      locale: () => 'zh-CN',
      view: () => 'simple',
      load,
    })

    await pager.loadFirstPage()
    pager.reset()

    expect(pager.children.value).toEqual([])
    expect(pager.loaded.value).toBe(false)
    expect(pager.hasMore.value).toBe(false)
  })
})
