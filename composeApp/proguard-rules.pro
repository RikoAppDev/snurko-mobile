# WorkManager, reached through the Glance widget, instantiates its Room database
# implementation class by name, so the no-argument constructor has to survive.
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
