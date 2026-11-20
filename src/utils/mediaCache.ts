import { normalizeFileUrl } from '../api/http'

export const MEDIA_CACHE_NAME = 'paperphone-media-v3'
const pending = new Map<string, Promise<Blob>>()
const refreshed = new Set<string>()
window.addEventListener?.('online', () => refreshed.clear())

export function mediaCacheAccount(): string {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')?.id || 'anonymous'
  } catch {
    return 'anonymous'
  }
}

/** Cache original bytes, including encrypted attachments, never decrypted chat data. */
export async function cachedMediaBlob(raw: string, download?: () => Promise<Blob>): Promise<Blob> {
  const url = new URL(normalizeFileUrl(raw), window.location.href || window.location.origin)
  url.hash = ''
  const account = mediaCacheAccount()
  const token = localStorage.getItem('token')
  const serverUrl = localStorage.getItem('serverUrl') || import.meta.env.VITE_API_URL || window.location.origin
  const key = new Request(`${url.href}${url.search ? '&' : '?'}__pp_account=${encodeURIComponent(account)}`)
  const id = key.url
  const existing = pending.get(id)
  if (existing) return existing
  const task = (async () => {
    let cache: Cache | undefined
    try { cache = await caches.open(MEDIA_CACHE_NAME) } catch { /* Storage unavailable. */ }
    let cached: Response | undefined
    try { cached = await cache?.match(key) } catch { /* Storage disabled. */ }
    const fetchBlob = async () => {
      if (mediaCacheAccount() !== account) throw new Error('Media account changed')
      if (download) return download()
      const base = new URL(serverUrl, window.location.href || window.location.origin)
      const response = await fetch(url.href, {
        headers: url.origin === base.origin && url.pathname.startsWith('/api/files/') && token
          ? { Authorization: `Bearer ${token}` } : undefined,
      })
      if (!response.ok) throw new Error(`Media download failed: ${response.status}`)
      return response.blob()
    }
    const update = async () => {
      const blob = await fetchBlob()
      try { await cache?.put(key, new Response(blob)) } catch { /* Quota failure must not prevent display. */ }
      refreshed.add(id)
      window.dispatchEvent?.(new CustomEvent('pp-media-updated', { detail: { url: url.href, account, blob } }))
      return blob
    }
    if (cached) {
      // Revalidate once per session/reconnect. Navigation itself never downloads again.
      if (!refreshed.has(id)) {
        refreshed.add(id)
        void update().catch(() => refreshed.delete(id))
      }
      return cached.blob()
    }
    return update()
  })()
  pending.set(id, task)
  try { return await task } finally { pending.delete(id) }
}
