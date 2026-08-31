/**
 * ApiError → i18n 映射单测。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { describe, expect, it } from 'vitest'
import { ApiError } from '@/api/client'
import { messageFromApiError } from '@/utils/apiError'

describe('messageFromApiError', () => {
  it('maps TAXON_HAS_CHILDREN code', () => {
    const msg = messageFromApiError(new ApiError(40901, 'x', 409))
    expect(msg).toContain('子')
  })

  it('maps unauthorized status', () => {
    const msg = messageFromApiError(new ApiError(40100, 'x', 401))
    expect(msg.length).toBeGreaterThan(0)
  })
})
