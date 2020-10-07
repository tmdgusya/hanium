package com.example.smartbycicylelock.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;

import com.example.smartbycicylelock.InnerDB.DBOpenHelper;
import com.example.smartbycicylelock.R;

import java.util.List;

public class User_information extends AppCompatActivity {
    private DBOpenHelper dbHelper;
    private SQLiteDatabase database;
    private static String DATABASENAME= "Bicycle";
    private static int VERSION = 1;
    private List list;
    private TextView email;
    private TextView code;
    private TextView name;
    public void openDatabase()
    {
        database = openOrCreateDatabase("Bicycle", MODE_PRIVATE, null);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_information);
        openDatabase();
        dbHelper = new DBOpenHelper(this, DATABASENAME, null, VERSION);
        email = (TextView)findViewById(R.id.email);
        name = (TextView)findViewById(R.id.name);
        code = (TextView)findViewById(R.id.ID);
        email.setText(dbHelper.get_email());
        code.setText(dbHelper.get_name());
        name.setText(dbHelper.get_code());
    }
}
