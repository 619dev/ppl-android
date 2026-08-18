import { Capacitor, registerPlugin, type PluginListenerHandle } from '@capacitor/core'

export type TorStatus = 'OFF' | 'STARTING' | 'ON' | 'STOPPING' | 'FETCHING_WEBTUNNEL' | 'STARTING_WEBTUNNEL' | 'WEBTUNNEL_ERROR'

export interface TorState {
  status: TorStatus
  host: string
  port: number
  ready: boolean
  transport: 'direct' | 'webtunnel'
}

interface TorPluginInterface {
  start(): Promise<TorState>
  getStatus(): Promise<TorState>
  addListener(eventName: 'statusChange', listener: (state: TorState) => void): Promise<PluginListenerHandle>
}

const TorPlugin = registerPlugin<TorPluginInterface>('TorPlugin')

export const isNativeAndroid = Capacitor.getPlatform() === 'android'

export async function getTorStatus(): Promise<TorState> {
  if (!isNativeAndroid) return { status: 'ON', host: '', port: 0, ready: true, transport: 'direct' }
  return TorPlugin.getStatus()
}

export async function startTor(): Promise<TorState> {
  if (!isNativeAndroid) return { status: 'ON', host: '', port: 0, ready: true, transport: 'direct' }
  return TorPlugin.start()
}

export async function onTorStatusChange(listener: (state: TorState) => void): Promise<PluginListenerHandle | null> {
  if (!isNativeAndroid) return null
  return TorPlugin.addListener('statusChange', listener)
}
