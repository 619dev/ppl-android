import { Capacitor, registerPlugin } from '@capacitor/core'

export type TorStatus = 'OFF' | 'STARTING' | 'ON' | 'STOPPING'

interface TorPluginApi {
  start(): Promise<{ status: TorStatus; host: string; port: number }>
  getStatus(): Promise<{ status: TorStatus; host: string; port: number }>
  addListener(
    event: 'statusChange',
    listener: (state: { status: TorStatus; host: string; port: number }) => void,
  ): Promise<{ remove(): Promise<void> }>
}

const TorPlugin = registerPlugin<TorPluginApi>('TorPlugin')

/** Starts the APK-bundled Tor service. Native networking has no direct fallback. */
export async function startEmbeddedTor(): Promise<void> {
  if (!Capacitor.isNativePlatform()) return
  const state = await TorPlugin.start()
  console.info('[Tor] Embedded client:', state.status)
}
