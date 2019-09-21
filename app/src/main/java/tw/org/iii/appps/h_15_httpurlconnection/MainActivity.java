package tw.org.iii.appps.h_15_httpurlconnection;
//目的:HTTP資料交換
//1.加入網路權限
//2.要做HTTP資料交換,利用http交換資料,要拿別人網頁的原始碼

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {
    private TextView msg ;
    private MyReceiver myReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //5.廣播註冊送出
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter("brad");//過濾完只收brad廣播
        registerReceiver(myReceiver,intentFilter);//廣播註冊,(1.接收廣播,2.過濾的接收東西)

        msg = findViewById(R.id.msg);//取得顯示畫面
    }

    //1.按下按鈕取的資策會url網頁原始碼
    public void test1(View view) {
       new Thread(){
           @Override
           public void run() {
               try {
                   //1.取得資策會連線http頁面
                    URL url = new URL("https://www.iii.org.tw/");//抓取URL("要連接的網址")
                    HttpURLConnection conn = (HttpURLConnection ) url.openConnection();//URL網址開始連線,傳到HttpURLConnection根頁面做互動
                    conn.connect();//URL連線

                   //2.把取得的資料一行一行灌到log上
                   BufferedReader reader =
                           new BufferedReader(
                                   new InputStreamReader(conn.getInputStream()));
                   String line = null; StringBuffer sb = new StringBuffer();
                   while((line = reader.readLine()) != null){
                       Log.v("brad",line);
                       sb.append(line); //一列一列讀,並且增加在後面
                   }
                   reader.close(); //關閉串流

                   //3.發出廣播
                   Intent intent = new Intent("brad");//自己定義的intent送出action brad
                   //6.把讀取在url的資料用append方式加好後,送到intente掛上去,送給廣播接收
                   intent.putExtra("data",sb.toString());//intent.掛上資料(1."自己定義的名字" 2.value因為有兩張所以toSring)

                   sendBroadcast(intent); //發出廣播(自己定義的inttent)這招是Context本身的招 Context: Activity,Service,Application 這幾個都可以發出廣播

               }catch (Exception e){
                   Log.v("brad","取得url原始碼錯誤:" + e.toString());
               }
           }
       }.start();
    }

    //4.廣播接受Context context, Intent intent,其中intent有一個sb是網頁資料
    private class MyReceiver extends BroadcastReceiver{
        @Override
        //當廣播有進來時
        public void onReceive(Context context, Intent intent) {
            Log.v("brad","廣播有接受成功");
            String data = intent.getStringExtra("data");//取得intente設置的String參數
            msg.setText(data);//抓到的url頁面原始碼,顯示出來

        }
    }
}
