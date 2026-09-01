/**
 * 阶元脊柱与台账排序单测。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
import { describe, expect, it } from 'vitest'
import { buildRankSpine, ledgerRanks, LINNAEAN_RANKS } from './ranks'

describe('buildRankSpine', () => {
  it('always returns the seven Linnaean ranks in order', () => {
    const spine = buildRankSpine()
    expect(spine.map((item) => item.rank)).toEqual([...LINNAEAN_RANKS])
    expect(spine.every((item) => item.count === null)).toBe(true)
  })

  it('fills known counts and leaves missing ranks null', () => {
    const spine = buildRankSpine({ KINGDOM: 2, SPECIES: 40, OTHER: 9 })
    expect(spine[0]).toEqual({ rank: 'KINGDOM', count: 2 })
    expect(spine[1]).toEqual({ rank: 'PHYLUM', count: null })
    expect(spine[6]).toEqual({ rank: 'SPECIES', count: 40 })
    expect(spine.some((item) => item.rank === 'OTHER')).toBe(false)
  })
})

describe('ledgerRanks', () => {
  it('orders known ranks and appends unknown keys', () => {
    const rows = ledgerRanks({ SPECIES: 10, KINGDOM: 2, CLADE: 3 })
    expect(rows.map((row) => row.rank)).toEqual(['KINGDOM', 'SPECIES', 'CLADE'])
  })
})
