
## Architecture:
- Single Activity + Compose UI gate (no navigation trampoline)
- System SplashScreen API (androidx.core.splashscreen)
- StartupCoordinator (ViewModel) as single source of truth
- Requirement enum  → hard gates  (blocks isReady)
- Advisory enum     → soft hints  (never blocks isReady)
- ListenerEnabledMonitor → callbackFlow/ContentObserver for instant mid-session revocation detection (no ON_RESUME polling needed for NOTIFICATION_LISTENER specifically)
- DisposableEffect + ON_RESUME still re-checks POST_NOTIFICATIONS and BATTERY_OPTIMIZATION which have no observable Settings key
