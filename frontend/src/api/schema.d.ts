/**
 * OpenAPI paths 类型骨架（可由 `pnpm openapi:generate` 覆盖为服务端导出）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
export interface paths {
  '/api/health': {
    get: {
      responses: {
        200: {
          content: {
            'application/json': {
              code: number
              message: string
              data: { status: string }
            }
          }
        }
      }
    }
  }
  '/api/taxa/children': {
    get: {
      parameters: {
        query: {
          parentId?: number
          locale?: string
          page?: number
          size?: number
        }
      }
      responses: {
        200: {
          content: {
            'application/json': {
              code: number
              message: string
              data: {
                items: Array<{
                  id: number
                  rank: string
                  scientificName: string
                  commonName: string | null
                  childCount: number
                  hasChildren: boolean
                }>
                total: number
                page: number
                size: number
              }
            }
          }
        }
      }
    }
  }
  '/api/taxa/search': {
    get: {
      parameters: {
        query: {
          q: string
          locale?: string
          page?: number
          size?: number
        }
      }
      responses: {
        200: {
          content: {
            'application/json': {
              code: number
              message: string
              data: {
                items: Array<{
                  id: number
                  rank: string
                  scientificName: string
                  commonName: string | null
                  childCount: number
                  hasChildren: boolean
                }>
                total: number
                page: number
                size: number
              }
            }
          }
        }
      }
    }
  }
  '/api/taxa/{id}': {
    get: {
      parameters: {
        path: { id: number }
        query: { locale?: string }
      }
      responses: {
        200: {
          content: {
            'application/json': {
              code: number
              message: string
              data: Record<string, unknown>
            }
          }
        }
      }
    }
  }
  '/api/admin/taxa/{id}/move': {
    post: {
      parameters: {
        path: { id: number }
        query?: { locale?: string }
      }
      requestBody: {
        content: {
          'application/json': { newParentId: number }
        }
      }
      responses: {
        200: {
          content: {
            'application/json': {
              code: number
              message: string
              data: Record<string, unknown>
            }
          }
        }
      }
    }
  }
}

export type components = Record<string, never>
export type operations = Record<string, never>
