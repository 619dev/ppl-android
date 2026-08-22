import { Capacitor, registerPlugin } from '@capacitor/core'

interface NativeTorResponse {
  status: number
  body: string
  headers?: Record<string, string>
}

interface TorHttpPlugin {
  request(options: {
    url: string
    method: string
    headers: Record<string, string>
    body?: string
  }): Promise<NativeTorResponse>
}

interface TorRecoveryPlugin {
  recoverWebTunnel(): Promise<unknown>
}

const TorHttp = registerPlugin<TorHttpPlugin>('TorHttp')
const TorRecovery = registerPlugin<TorRecoveryPlugin>('TorPlugin')
// Includes one direct attempt, automatic WebTunnel bootstrap, and one retry.
const NATIVE_TOR_REQUEST_TIMEOUT_MS = 145_000

export interface HttpResponse {
  status: number
  ok: boolean
  text(): Promise<string>
  headers: Headers
}

export async function torAwareFetch(url: string, options: RequestInit = {}): Promise<HttpResponse> {
  const parsed = new URL(url)
  const useNativeTor = Capacitor.getPlatform() === 'android' && parsed.hostname.endsWith('.onion')
  if (!useNativeTor) return fetch(url, options)

  if (options.body != null && typeof options.body !== 'string') {
    throw new Error('Native Tor requests currently require a text body')
  }

  const headers: Record<string, string> = {}
  new Headers(options.headers).forEach((value, name) => { headers[name] = value })
  let timeoutId: ReturnType<typeof setTimeout> | undefined
  try {
    const response = await Promise.race([
      (async () => {
        const requestOptions = {
          url,
          method: options.method || 'GET',
          headers,
          body: options.body as string | undefined,
        }
        try {
          return await TorHttp.request(requestOptions)
        } catch (firstError) {
          await TorRecovery.recoverWebTunnel()
          try {
            return await TorHttp.request(requestOptions)
          } catch (retryError) {
            const detail = retryError instanceof Error ? retryError.message : String(retryError)
            throw new Error(`Tor request still failed after WebTunnel recovery: ${detail}`)
          }
        }
      })(),
      new Promise<never>((_, reject) => {
        timeoutId = setTimeout(
          () => reject(new Error('Tor request timed out after automatic WebTunnel recovery')),
          NATIVE_TOR_REQUEST_TIMEOUT_MS,
        )
      }),
    ])
    const responseHeaders = Object.fromEntries(
      Object.entries(response.headers || {}).map(([name, value]) => [name.toLowerCase(), value]),
    )
    return {
      status: response.status,
      ok: response.status >= 200 && response.status < 300,
      text: async () => response.body || '',
      headers: new Headers(responseHeaders),
    }
  } finally {
    if (timeoutId !== undefined) clearTimeout(timeoutId)
  }
}
