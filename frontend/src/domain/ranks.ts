/**
 * 阶元顺序：林奈七级与完整阶元表，供脊柱图与统计台账使用。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
import type { TaxonRank } from '@/api/taxon'

/** 完整阶元（主级在前，中间级随后） */
export const TAXON_RANK_ORDER: TaxonRank[] = [
  'KINGDOM',
  'SUBKINGDOM',
  'PHYLUM',
  'SUBPHYLUM',
  'CLASS',
  'SUBCLASS',
  'ORDER',
  'SUBORDER',
  'SUPERFAMILY',
  'FAMILY',
  'SUBFAMILY',
  'TRIBE',
  'GENUS',
  'SUBGENUS',
  'SPECIES',
  'SUBSPECIES',
  'VARIETY',
  'FORM',
  'OTHER',
]

/** 公开浏览默认的林奈七级 */
export const LINNAEAN_RANKS: TaxonRank[] = [
  'KINGDOM',
  'PHYLUM',
  'CLASS',
  'ORDER',
  'FAMILY',
  'GENUS',
  'SPECIES',
]

export type RankSpineItem = {
  rank: TaxonRank
  count: number | null
}

export function buildRankSpine(byRank?: Record<string, number> | null): RankSpineItem[] {
  return LINNAEAN_RANKS.map((rank) => ({
    rank,
    count: byRank != null && Number.isFinite(byRank[rank]) ? byRank[rank] : null,
  }))
}

/** 将 byRank 按完整阶元表排序，未知键附于末尾 */
export function ledgerRanks(byRank: Record<string, number>): { rank: string; count: number }[] {
  const seen = new Set<string>()
  const ordered: { rank: string; count: number }[] = []
  for (const rank of TAXON_RANK_ORDER) {
    if (Object.prototype.hasOwnProperty.call(byRank, rank)) {
      ordered.push({ rank, count: byRank[rank] })
      seen.add(rank)
    }
  }
  for (const [rank, count] of Object.entries(byRank)) {
    if (!seen.has(rank)) {
      ordered.push({ rank, count })
    }
  }
  return ordered
}
