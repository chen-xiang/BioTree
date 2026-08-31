/**
 * Markdown 渲染（介绍字段）；XSS 用轻量消毒。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { marked } from 'marked'

marked.setOptions({ gfm: true, breaks: true })

function sanitize(html: string): string {
  // 允许常见排版标签，去掉 script/style/事件属性
  return html
    .replace(/<(script|style)[\s\S]*?>[\s\S]*?<\/\1>/gi, '')
    .replace(/\son\w+\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)/gi, '')
    .replace(/javascript:/gi, '')
}

export function renderMarkdown(source: string | null | undefined): string {
  if (!source) return ''
  const raw = marked.parse(source, { async: false }) as string
  return sanitize(raw)
}
