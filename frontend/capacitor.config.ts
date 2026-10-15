import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.matchcats.app',
  appName: 'MatchCats',
  webDir: 'capacitor-web',
  server: {
    cleartext: false,
  },
};

export default config;
