/**
 * debounce 单元测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { debounce } from './debounce'

describe('debounce', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('calls once after wait', () => {
    const fn = vi.fn()
    const d = debounce(fn, 300)
    d()
    d()
    d()
    expect(fn).not.toHaveBeenCalled()
    vi.advanceTimersByTime(300)
    expect(fn).toHaveBeenCalledTimes(1)
  })
})
