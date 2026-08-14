# WorkManager, reached through the Glance widget, instantiates its Room database
# implementation class by name, so the no-argument constructor has to survive.
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}

# WorkManager instantiates the input merger of every work request by class name.
# Without the constructor the merge throws and the worker never runs, which leaves
# the Glance widget stuck on its loading layout.
-keep class * extends androidx.work.InputMerger {
    <init>();
}

# The Glance session runs as a WorkManager worker that is also created by name.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
