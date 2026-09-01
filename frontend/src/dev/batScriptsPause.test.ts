/**
 * 校验 Windows bat 结束时 pause，避免双击窗口立刻关闭。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
import { readdirSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

const scriptsDir = path.resolve(import.meta.dirname, '../../../scripts')

describe('scripts/*.bat pause on finish', () => {
  const bats = readdirSync(scriptsDir).filter((name) => name.endsWith('.bat'))

  it('lists bat scripts', () => {
    expect(bats.includes('finish.bat')).toBe(true)
    expect(bats.length).toBeGreaterThan(5)
  })

  it('every bat ends via finish.bat or is finish.bat itself', () => {
    for (const name of bats) {
      const text = readFileSync(path.join(scriptsDir, name), 'utf8')
      if (name === 'finish.bat') {
        expect(text).toMatch(/^\s*pause\s*$/m)
        continue
      }
      expect(text, name).toMatch(/call "%~dp0finish\.bat" %EXITCODE%/)
      expect(text, name).toMatch(/:finish/)
    }
  })
})
