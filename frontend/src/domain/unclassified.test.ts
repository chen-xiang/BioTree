/**
 * 未分类 id 判定测试。
 *
 * Author: chen-xiang
 * Created: 2026-09-03
 */
import { describe, expect, it } from 'vitest'
import { isUnclassifiedId } from './unclassified'

describe('isUnclassifiedId', () => {
  it('treats negative ids as unclassified buckets', () => {
    expect(isUnclassifiedId(-8)).toBe(true)
    expect(isUnclassifiedId(12)).toBe(false)
    expect(isUnclassifiedId(null)).toBe(false)
  })
})
