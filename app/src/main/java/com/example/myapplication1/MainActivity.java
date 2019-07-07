package com.example.myapplication1;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    Context mContext;
    RecyclerView recyclerView;
    MyRecyclerAdapter adapter;
    RecyclerView.LayoutManager layoutManager;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

    String deviceID = null;
    String deviceKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mContext = getApplicationContext();
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);

        deviceID = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);

        final ArrayList<Procedure> items = new ArrayList<>();



        DatabaseReference ref = FirebaseDatabaseEngine.getFreshLocalDB().getReference("Devices");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean searched = false;
                if(dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        User user = snapshot.getValue(User.class);
                        String idCheck = snapshot.child("id").getValue().toString();
                        if (idCheck.equals(deviceID)) {
                            registerKey(snapshot.getKey());
                            searched = true;
                            break;
                        }
                    }
                }
                if(!searched){
                    //사용자 추가
                    Log.d("ID_CHECK", "ID 없음, 등록");
                    User user = new User(deviceID);
                    String key = FirebaseDatabaseEngine.getFreshLocalDBref().child("Devices").push().getKey();
                    registerKey(key);

                    Map<String, Object> postVal = user.toMap();
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("/Devices/"+key, postVal);

                    FirebaseDatabaseEngine.getFreshLocalDBref().updateChildren(childUpdates);
                }else {
                    DatabaseReference sref = FirebaseDatabaseEngine.getFreshLocalDB().getReference("Devices/"+deviceKey+"/procedures");
                    sref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                                String procName = snapshot.child("procName").getValue().toString();
                                Calendar regFl, stFl, edFl;
                                regFl = Calendar.getInstance();
                                stFl = Calendar.getInstance();
                                edFl = Calendar.getInstance();
                                regFl.setTimeInMillis(Long.parseLong(snapshot.child("registerFlag").getValue().toString()));
                                stFl.setTimeInMillis(Long.parseLong(snapshot.child("startFlag").getValue().toString()));
                                edFl.setTimeInMillis(Long.parseLong(snapshot.child("endFlag").getValue().toString()));
                                items.add(new Procedure(procName, regFl, stFl, edFl));
                                Log.e("right", regFl.getTimeInMillis()+" "+stFl.getTimeInMillis()+" "+edFl.getTimeInMillis());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new MyRecyclerAdapter(items, MainActivity.this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View popupView = View.inflate(MainActivity.this, R.layout.pop_up, null);
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                final ConstraintLayout container = new ConstraintLayout(MainActivity.this);
                final ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.leftMargin = getResources().getDimensionPixelSize(R.dimen.checkbox_margin);
                params.rightMargin =getResources().getDimensionPixelSize(R.dimen.checkbox_margin);
                popupView.setLayoutParams(params);
                container.addView(popupView);

                final Switch mySwitch = popupView.findViewById(R.id.switch1);
                final TextView startFlagView = popupView.findViewById(R.id.startFlag);
                final TextView endFlagView = popupView.findViewById(R.id.endFlag);
                final TextView procName = popupView.findViewById(R.id.procName);
                final TextView errorText = popupView.findViewById(R.id.error);
                mySwitch.setChecked(true);
                startFlagView.setText(sdf.format(new Date()));
                startFlagView.setEnabled(false);

                mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(isChecked) {
                            Date date = new Date();
                            date.setTime(System.currentTimeMillis());
                            startFlagView.setText(sdf.format(new Date()));
                            startFlagView.setEnabled(false);
                        }else
                            startFlagView.setEnabled(true);
                    }
                });

                builder.setTitle("Procedure 추가하기");
                builder.setView(container);
                builder.setCancelable(true);

                builder.setPositiveButton("추가",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                builder.setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ((ViewGroup)container.getParent()).removeView(container);
                            }
                        });
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        ((ViewGroup)container.getParent()).removeView(container);
                    }
                });
                final AlertDialog dialog = builder.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        if (procName.getText().length() == 0) {
                            errorText.setText("프로시저 이름을 입력해주세요.");
                            return;
                        }
                        if (startFlagView.getText().length() == 0) {
                            errorText.setText("시작 날짜/시간을 입력해주세요.");
                            return;
                        }
                        if(!isValidDate(startFlagView.getText().toString())){
                            errorText.setText("시작 날짜/시간의 형식이 올바르지 않습니다.");
                            return;
                        }
                        if (endFlagView.getText().length() == 0) {
                            errorText.setText("종료 날짜/시간을 입력해주세요.");
                            return;
                        }
                        if(!isValidDate(endFlagView.getText().toString())){
                            errorText.setText("종료 날짜/시간의 형식이 올바르지 않습니다.");
                            return;
                        }

                        Procedure proc = null;
                        try {
                            if (sdf.parse(startFlagView.getText().toString()).getTime() > sdf.parse(endFlagView.getText().toString()).getTime()) {
                                errorText.setText("종료 시점이 시작 시점보다 앞서 있습니다.");
                                return;
                            }

                            proc = new Procedure(
                                    procName.getText().toString(),
                                    newCalendar(new Date(System.currentTimeMillis())),
                                    newCalendar(sdf.parse(startFlagView.getText().toString())),
                                    newCalendar(sdf.parse(endFlagView.getText().toString()))
                            );
                            items.add(proc);
                        }catch(Exception e){}

                        String procKey = FirebaseDatabaseEngine.getFreshLocalDB().getReference("Devices/"+deviceKey).child("procedures").push().getKey();
                        Map<String, Object> postVal = proc.toMap();

                        Map<String, Object> childUpdates = new HashMap<>();
                        childUpdates.put("/Devices/"+deviceKey+"/procedures/"+ procKey, postVal);

                        FirebaseDatabaseEngine.getFreshLocalDBref().updateChildren(childUpdates);

                        dialog.dismiss();
                    }
                });
            }
        });
    }

    private Calendar newCalendar(Date parsed){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parsed);
        return calendar;
    }

    private boolean isValidDate(String str){
        try{
            sdf.setLenient(false);
            sdf.parse(str);
            return  true;
        }catch (ParseException e){
            return  false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void registerKey(String key){
        this.deviceKey = key;
    }
}
