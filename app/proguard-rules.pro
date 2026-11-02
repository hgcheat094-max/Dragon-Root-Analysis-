# Room and WorkManager ship their own consumer rules.
-keep class com.dragonroot.app.update.UpdateWorker { *; }
-dontwarn org.json.**
