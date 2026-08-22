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

const TorHttp = registerPlugin<TorHttpPlugin>('TorHttp')

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
  const response = await TorHttp.request({
    url,
    method: options.method || 'GET',
    headers,
    body: options.body as string | undefined,
  })
  const responseHeaders = Object.fromEntries(
    Object.entries(response.headers || {}).map(([name, value]) => [name.toLowerCase(), value]),
  )
  return {
    status: response.status,
    ok: response.status >= 200 && response.status < 300,
    text: async () => response.body || '',
    headers: new Headers(responseHeaders),
  }
}
