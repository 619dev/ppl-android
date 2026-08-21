import { useEffect, useRef, useState } from 'react'
import QRCode from 'qrcode'
import jsQR from 'jsqr'

/**
 * Renders a QR code from the given data string.
 */
export function QRCodeCanvas({ data, size = 200 }: { data: string; size?: number }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    if (canvasRef.current && data) {
      QRCode.toCanvas(canvasRef.current, data, {
        width: size,
        margin: 2,
        color: { dark: '#000000', light: '#ffffff' },
      })
    }
  }, [data, size])

  return <canvas ref={canvasRef} style={{ borderRadius: 12, display: 'block' }} />
}

/**
 * Full-screen QR code display modal.
 */
export function QRCodeModal({
  data,
  title,
  subtitle,
  onClose,
}: {
  data: string
  title: string
  subtitle?: string
  onClose: () => void
}) {
  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 9999,
      background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(6px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      animation: 'fade-in .2s ease',
    }} onClick={onClose}>
      <div style={{
        background: '#fff', borderRadius: 24, padding: '32px 28px',
        display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16,
        boxShadow: '0 12px 48px rgba(0,0,0,0.3)', maxWidth: '85vw',
      }} onClick={e => e.stopPropagation()}>
        <div style={{ fontSize: 18, fontWeight: 700, color: '#111' }}>{title}</div>
        <QRCodeCanvas data={data} size={220} />
        {subtitle && (
          <div style={{ fontSize: 13, color: '#666', textAlign: 'center', maxWidth: 240, wordBreak: 'break-all' }}>
            {subtitle}
          </div>
        )}
        <button onClick={onClose} style={{
          marginTop: 4, padding: '8px 32px', borderRadius: 12, border: 'none',
          background: '#f0f0f0', color: '#333', fontSize: 14,
          fontWeight: 500, cursor: 'pointer',
        }}>✕</button>
      </div>
    </div>
  )
}

/**
 * QR code scanner using device camera.
 * Scans for QR codes containing paperphonelite:// URIs.
 */
export function QRScanner({ onScan, onClose }: { onScan: (data: string) => void; onClose: () => void }) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const [error, setError] = useState('')
  const [isStarting, setIsStarting] = useState(false)

  const stopCamera = () => {
    try {
      streamRef.current?.getTracks().forEach(track => track.stop())
    } catch {}
    streamRef.current = null
    try {
      if (videoRef.current) {
        videoRef.current.pause()
        videoRef.current.srcObject = null
      }
    } catch {}
  }

  const handleClose = () => {
    stopCamera()
    onClose()
  }

  useEffect(() => {
    let active = true
    let animFrame = 0
    const canUseMediaDevices =
      typeof navigator !== 'undefined' &&
      typeof navigator.mediaDevices === 'object' &&
      typeof navigator.mediaDevices.getUserMedia === 'function'

    const acquireStream = async () => {
      if (!canUseMediaDevices) {
        throw new Error('Camera API is not available on this device')
      }

      const candidates = [
        { video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 }, }, audio: false },
        { video: { facingMode: 'environment', width: { ideal: 640 }, height: { ideal: 480 }, }, audio: false },
        { video: true, audio: false },
      ] as const

      let lastErr: unknown
      for (const constraints of candidates) {
        try {
          return await navigator.mediaDevices.getUserMedia(constraints as MediaStreamConstraints)
        } catch (err) {
          lastErr = err
        }
      }

      throw lastErr ?? new Error('Unable to access camera')
    }

    const createDetector = () => {
      try {
        const CandidateDetector = (window as any).BarcodeDetector
        if (typeof CandidateDetector !== 'function') return null
        return new CandidateDetector({ formats: ['qr_code'] })
      } catch {
        return null
      }
    }

    const decodeWithDetector = async (detector: any | null, video: HTMLVideoElement) => {
      if (!detector) return null
      try {
        const barcodes = await detector.detect(video)
        const qrValue = barcodes?.find((bc: any) => Boolean(bc?.rawValue))?.rawValue
        return qrValue ?? null
      } catch {
        return null
      }
    }

    const decodeWithCanvas = (video: HTMLVideoElement, canvas: HTMLCanvasElement) => {
      try {
        const width = video.videoWidth
        const height = video.videoHeight
        if (!width || !height || video.readyState < 2) return null

        canvas.width = width
        canvas.height = height

        const context = canvas.getContext('2d', { willReadFrequently: true })
        if (!context) return null

        context.drawImage(video, 0, 0, width, height)
        const pixels = context.getImageData(0, 0, width, height)
        const result = jsQR(pixels.data, width, height, { inversionAttempts: 'dontInvert' })
        return result?.data ?? null
      } catch {
        return null
      }
    }

    const start = async () => {
      setIsStarting(true)
      try {
        const stream = await acquireStream()

        // The scanner may have been closed while the permission prompt was open.
        if (!active) {
          stream.getTracks().forEach(track => track.stop())
          return
        }

        streamRef.current = stream
        if (!videoRef.current) {
          throw new Error('Camera view is unavailable')
        }

        videoRef.current.srcObject = stream
        videoRef.current.setAttribute('playsinline', 'true')
        videoRef.current.muted = true
        try {
          await videoRef.current.play()
        } catch {
          setError('Please tap again to allow camera permission and playback')
        }

        const detector = createDetector()
        const finalizeScan = (value: string) => {
          active = false
          stopCamera()
          Promise.resolve().then(() => onScan(value)).catch(() => {})
        }

        const scan = async () => {
          try {
            if (!active || !videoRef.current || !canvasRef.current) {
              if (active) animFrame = requestAnimationFrame(scan)
              return
            }

            const detected = await decodeWithDetector(detector, videoRef.current)
            if (detected) {
              finalizeScan(detected)
              return
            }

            const result = decodeWithCanvas(videoRef.current, canvasRef.current)
            if (result) {
              finalizeScan(result)
              return
            }
          } catch {
            // keep scanning on transient frame errors
          }

          if (active) {
            animFrame = requestAnimationFrame(scan)
          }
        }

        scan()
      } catch (err: any) {
        setError(err?.message || 'Cannot access camera')
      } finally {
        setIsStarting(false)
      }
    }

    start()

    return () => {
      active = false
      cancelAnimationFrame(animFrame)
      stopCamera()
    }
  }, [])

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 9999,
      background: '#000', display: 'flex', flexDirection: 'column',
    }}>
      <div style={{
        padding: '12px 16px', display: 'flex', alignItems: 'center',
        background: 'rgba(0,0,0,0.8)', position: 'relative', zIndex: 2,
      }}>
        <button type="button" aria-label="Close scanner" onClick={handleClose} style={{
          border: 'none', background: 'none', color: '#fff',
          fontSize: 24, cursor: 'pointer', padding: 4, touchAction: 'manipulation',
        }}>←</button>
        <span style={{ flex: 1, textAlign: 'center', color: '#fff', fontWeight: 600, fontSize: 16 }}>
          Scan QR Code
        </span>
        <div style={{ width: 32 }} />
      </div>

      <div style={{ flex: 1, position: 'relative', overflow: 'hidden' }}>
        <video ref={videoRef} style={{
          width: '100%', height: '100%', objectFit: 'cover',
        }} playsInline muted />
        <canvas ref={canvasRef} style={{ display: 'none' }} />

        {/* Scan frame overlay */}
        <div style={{
          position: 'absolute', inset: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'center', pointerEvents: 'none',
        }}>
          <div style={{
            width: 240, height: 240, border: '3px solid rgba(255,255,255,0.7)',
            borderRadius: 24, boxShadow: '0 0 0 9999px rgba(0,0,0,0.5)',
          }}>
            {/* Animated scan line */}
            <div style={{
              width: '100%', height: 2, background: 'var(--accent, #00d4ff)',
              boxShadow: '0 0 12px var(--accent, #00d4ff)',
              animation: 'scan-line 2s ease-in-out infinite',
            }} />
          </div>
        </div>

        {error && (
          <div style={{
            position: 'absolute', inset: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: '#fff', fontSize: 14, textAlign: 'center', padding: 32,
          }}>
            <div>
              <div style={{ fontSize: 48, marginBottom: 12 }}>📷</div>
              <div>{error}</div>
              {isStarting ? <div style={{ marginTop: 8, opacity: 0.9 }}>Loading camera…</div> : null}
            </div>
          </div>
        )}
      </div>

      <style>{`
        @keyframes scan-line {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(234px); }
        }
      `}</style>
    </div>
  )
}
