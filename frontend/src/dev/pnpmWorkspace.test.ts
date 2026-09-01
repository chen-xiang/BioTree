/**
 * 校验 pnpm 构建策略，避免 esbuild 再次被写成未审批占位值。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

const workspaceYaml = readFileSync(
  path.resolve(import.meta.dirname, '../../pnpm-workspace.yaml'),
  'utf8',
)

describe('pnpm-workspace allowBuilds', () => {
  it('approves esbuild native build scripts', () => {
    expect(workspaceYaml).not.toMatch(/set this to true or false/)
    expect(workspaceYaml).toMatch(/allowBuilds:\s*\n\s+esbuild:\s*true\b/)
  })
})
