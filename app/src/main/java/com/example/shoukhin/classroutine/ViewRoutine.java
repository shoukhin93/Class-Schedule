package com.example.shoukhin.classroutine;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class ViewRoutine extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    ListView viewRoutine;
    private static BaseAdapter adapter;

    private static ArrayList<RoutineStructure> allData;
    private static ArrayList<RoutineStructure> currentDayData;

    private static DatabaseReference mFirebaseDatabase, updatedVersion;

    public static String[] dayArray = {"Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    public static final String LOGTAG = "tag";

    public static int cuurrentDayPosition;
    ImageButton nextDay, previousDay;

    private static TextView currentDayTbx;


    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_routine);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        initialize();
        checkUpdate();

        //getting all routine data from firebase database
        mFirebaseDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //clear all data and get new one
                allData.clear();
                currentDayData.clear();
                //loop through the child
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    RoutineStructure routine = postSnapshot.getValue(RoutineStructure.class);
                    routine.setKey(postSnapshot.getKey());

                    //storing all data into a arraylist
                    allData.add(routine);

                }
                //showing only selected day's routine
                showCurrentDayRoutine();

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //to edit or delete by CR
        viewRoutine.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                auth = FirebaseAuth.getInstance();
                if (auth.getCurrentUser() != null) {
                    // User is not logged in, he can modify data
                    Intent intent = new Intent(ViewRoutine.this, WriteRoutine.class);

                    RoutineStructure routine = currentDayData.get(position);
                    intent.putExtra("routine", routine);
                    intent.putExtra("day", cuurrentDayPosition);
                    startActivity(intent);
                } else {
                    RoutineStructure routine = currentDayData.get(position);
                    Intent intent = new Intent(ViewRoutine.this, ViewDates.class);
                    intent.putExtra("allData", allData);
                    intent.putExtra("selectedRoutine", routine);
                    startActivity(intent);
                }
            }
        });

        nextDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cuurrentDayPosition++;
                showCurrentDayRoutine();
            }
        });

        previousDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cuurrentDayPosition--;
                showCurrentDayRoutine();
            }
        });
    }

    private void checkUpdate() {

        PackageInfo pInfo;
        long tempVerCode = 0;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tempVerCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        final long verCode = tempVerCode;

        //Log.d("tag", verCode + "");

        if (updatedVersion == null) {
            updatedVersion = FirebaseDatabase.getInstance().getReference("update");
        }

        updatedVersion.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long dbVersionCode = (long) dataSnapshot.child("versionCode").getValue();
                String downloadLink = (String) dataSnapshot.child("downloadLink").getValue();
                String downloadMessage = (String) dataSnapshot.child("downloadMessage").getValue();

                if (verCode < dbVersionCode) {
                    showDownloadDialogue(downloadMessage, downloadLink);
                }

                //  Log.d(LOGTAG, dbVersionCode + " " + verCode);
                //  Log.d(LOGTAG, downloadMessage);
                //  Log.d(LOGTAG, downloadLink);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static void showCurrentDayRoutine() {

        if (cuurrentDayPosition >= dayArray.length)
            cuurrentDayPosition = 0;

        else if (cuurrentDayPosition < 0)
            cuurrentDayPosition = dayArray.length - 1;

        currentDayData.clear();
        for (int i = 0; i < allData.size(); i++) {
            //if current selected day is matched then only display that day's data
            if (allData.get(i).getDay().equals(dayArray[cuurrentDayPosition])) {
                currentDayData.add(allData.get(i));
            }
        }

        //sorting routine data by time
        sortByTime();

        currentDayTbx.setText(dayArray[cuurrentDayPosition]);
        adapter.notifyDataSetChanged();

    }

    private void showDownloadDialogue(String downloadMessage, final String dbDownloadLink) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ViewRoutine.this);
        builder.setTitle("Download new version");
        builder.setMessage(downloadMessage);
        builder.setCancelable(false);
        builder.setPositiveButton("download", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dbDownloadLink));
                if (browserIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(browserIntent);
                }
                finish();
            }
        });
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void initialize() {

        //setting offline storage
        if (mFirebaseDatabase == null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.setPersistenceEnabled(true);
            mFirebaseDatabase = FirebaseDatabase.getInstance().getReference("routine");

        }

        //getting today's day number of the week
        Calendar calendar = Calendar.getInstance();
        cuurrentDayPosition = calendar.get(Calendar.DAY_OF_WEEK);

        //Day will be saturday
        if (cuurrentDayPosition == 7)
            cuurrentDayPosition = 0;

        currentDayTbx = (TextView) findViewById(R.id.viewDayTBx);
        currentDayTbx.setText(dayArray[cuurrentDayPosition]);

        nextDay = (ImageButton) findViewById(R.id.viewNextDayIbtn);
        previousDay = (ImageButton) findViewById(R.id.viewPreviousDayIbtn);

        viewRoutine = (ListView) findViewById(R.id.viewRoutine);
        viewRoutine.setOnTouchListener(new OnSwipeTouchListener(this)); //adding swipe listener to listview

        allData = new ArrayList<>();
        currentDayData = new ArrayList<>();

        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return currentDayData.size();
            }

            @Override
            public Object getItem(int position) {
                return currentDayData.get(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.listview, null);
                }

                TextView courseName = (TextView) convertView.findViewById(R.id.listviewCourseName);
                TextView courseCode = (TextView) convertView.findViewById(R.id.listviewCourseCode);
                TextView roomNumber = (TextView) convertView.findViewById(R.id.listviewRoomNumbertbx);
                TextView fromTime = (TextView) convertView.findViewById(R.id.listviewFromtimetbx);
                TextView toTime = (TextView) convertView.findViewById(R.id.listviewTotimetbx);

                String tempRoomNo = "Room No : " + currentDayData.get(position).getRoomNumber();

                courseName.setText(currentDayData.get(position).getCourseName());
                courseCode.setText(currentDayData.get(position).getCourseCode());
                roomNumber.setText(tempRoomNo);
                fromTime.setText(currentDayData.get(position).getStartTime());
                toTime.setText(currentDayData.get(position).getEndTime());

                return convertView;
            }
        };

        viewRoutine.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.view_notification) {
            startActivity(new Intent(ViewRoutine.this, ViewNotification.class));

        } else if (id == R.id.admin_zone) {
            startActivity(new Intent(ViewRoutine.this, AdminAuth.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //sorting the routine according to time
    private static void sortByTime() {

        Collections.sort(currentDayData, new Comparator<RoutineStructure>() {
            @Override
            public int compare(RoutineStructure lhs, RoutineStructure rhs) {
                SimpleDateFormat format = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);

                String timeLHS = lhs.getStartTime();
                String timeRHS = rhs.getStartTime();

                Calendar calendarRHD = Calendar.getInstance();
                Calendar calendarLHS = Calendar.getInstance();
                try {
                    calendarLHS.setTime(format.parse(timeLHS));
                    calendarRHD.setTime(format.parse(timeRHS));

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return calendarLHS.compareTo(calendarRHD);
            }

        });

    }
}
