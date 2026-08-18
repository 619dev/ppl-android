import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.fm619.paperphonelite',
  appName: 'PaperPhoneLite',
  webDir: 'dist',
  server: {
    // HTTPS scheme is required for WebRTC getUserMedia() and crypto.subtle
    androidScheme: 'https',
    iosScheme: 'https',
  },
  plugins: {
    SystemBars: {
      // Android 15+ is edge-to-edge. Capacitor derives these values from
      // WindowInsets (including Pixel display cutouts and navigation modes).
      insetsHandling: 'css',
    },
    SplashScreen: {
      launchAutoHide: true,
      launchShowDuration: 2000,
      androidScaleType: 'CENTER_CROP',
      splashFullScreen: true,
      splashImmersive: true,
      backgroundColor: '#1a1a2e',
    },
  },
  android: {
    // The secure Capacitor origin (https://localhost) must be allowed to call
    // http:// v3 onion services. Onion services are authenticated and
    // encrypted by Tor itself; production URL validation remains onion-only.
    allowMixedContent: true,
  },
  ios: {
    contentInset: 'automatic',
    allowsLinkPreview: false,
    scrollEnabled: false,
  },
}

export default config
