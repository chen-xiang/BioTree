/**
 * vue-i18n 初始化。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { createI18n } from 'vue-i18n'
import en from './en'
import zhCN from './zh-CN'

export type AppLocale = 'zh-CN' | 'en'

export const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'en',
  messages: {
    'zh-CN': zhCN,
    en,
  },
})
