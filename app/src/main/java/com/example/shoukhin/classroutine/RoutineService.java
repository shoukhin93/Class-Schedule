package com.example.shoukhin.classroutine;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RoutineService extends Service {

    private DatabaseReference mFirebaseDatabase, pinnedPost;
    public static boolean FIRST_TIME_OPEN = false;

    public RoutineService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mFirebaseDatabase = FirebaseDatabase.getInstance().getReference("Notification");
        pinnedPost = FirebaseDatabase.getInstance().getReference("Notice Board");


        mFirebaseDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //if (FIRST_TIME_OPEN == false)
                    showNotification("Notice about class!", "tap to view");


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });

        pinnedPost.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
               // if (FIRST_TIME_OPEN == false)
                    showNotification("Pinned post updated", "tap to view");

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        return START_STICKY;
    }

    private void showNotification(String message1, String messege2) {
        Intent intnt = new Intent(RoutineService.this, ViewNotification.class);

        PendingIntent pIntent = PendingIntent.getActivity(RoutineService.this, 0, intnt, 0);
        Notification n = new Notification.Builder(RoutineService.this)
                .setContentTitle(message1).setContentText(messege2)
                .setContentIntent(pIntent).setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, n);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("tag", "service stopped");
    }
}