/**
 * 校验 scripts 下 bat/sh 不含中文，避免 Windows cmd 把 UTF-8 rem 拆成伪命令。
 *
 * Author: chen-xiang
 * Created: 2026-09-03
 */
import { readdirSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

const scriptsDir = path.resolve(import.meta.dirname, '../../../scripts')
const CJK = /[\u3400-\u9fff]/

describe('scripts language', () => {
  const names = readdirSync(scriptsDir).filter((name) => /\.(bat|sh)$/.test(name))

  it('lists scripts', () => {
    expect(names.length).toBeGreaterThan(5)
  })

  it('contains no CJK characters', () => {
    for (const name of names) {
      const text = readFileSync(path.join(scriptsDir, name), 'utf8')
      const line = text.split(/\r?\n/).find((row) => CJK.test(row))
      expect(line, name).toBeUndefined()
    }
  })
})
