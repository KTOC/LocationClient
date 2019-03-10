package work.koumakan.locationclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private Button btn;
    private EditText username;
    public static final String USERNAME = "username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //call UI components  by id
        btn = (Button)findViewById(R.id.connect);
        username = (EditText) findViewById(R.id.username);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            // username to the intent extra
            if(!username.getText().toString().isEmpty()){
                Intent i  = new Intent(MainActivity.this, ConnectedActivity.class);
                //retreive username from EditText and add it to intent extra
                i.putExtra(USERNAME,username.getText().toString());
                startActivity(i);
            }
            }
        });
    }
}
