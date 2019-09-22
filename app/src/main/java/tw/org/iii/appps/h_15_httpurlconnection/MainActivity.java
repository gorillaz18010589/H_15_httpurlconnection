package tw.org.iii.appps.h_15_httpurlconnection;
//目的:HTTP資料交換
//1.加入網路權限
//2.要做HTTP資料交換,利用http交換資料,要拿別人網頁的原始碼
//3.廣播接收httpurl資訊,傳到sb.append在送出廣播,讓廣播收到顯示在頁面上
//4.android butterknife:因為有時抓id太麻煩,用這個api抓比較快,類jqery抓標籤
//5.用了http無法連接url,解決辦法: android:usesCleartextTraffic="true" //使用明碼傳送 (是/否)
//6.抓取url影像資料並且顯示出來 用handler根bimtap處理
//7.把檔案存到手機上,權限必須打開
//8.引用別人app,intent叫出pdf來看//https://stackoverflow.com/questions/10530416/how-to-open-a-pdf-via-intent-from-sd-card
//9.pdf => res =>android Res Die=>選擇型態為xml
//android level查看版本看資料能不能套,在檔案總冠加上provider取得授權
//*   <provider
//            android:name="androidx.core.content.FileProvider" //授權檔案
//            android:authorities="${applicationId}.provider"//這個頁面的provider
//            android:exported="false"
//            android:grantUriPermissions="true">//允許權限
//            <meta-data
//                android:name="android.support.FILE_PROVIDER_PATHS"
//                android:resource="@xml/provider_paths"/>//這邊要自己創xml
//        </provider>
//<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
//<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

// decodeStream(InputStream is): 取得圖片(用串流方式取得)(回傳到Bitmap)
//setImageBitmap(Bitmap bm) :設定圖片(Bitmap)

//// getExternalStoragePublicDirectory(String type):取得外部公開檔案位置(檔案種類)

//ProgressDialog(Context context):////下載中跑圈圈圖物件實體(this Content)
//progressDialog = new ProgressDialog(this);//下載中跑圈圈圖物件實體(this Content)
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);//設置為跑圈圈型態
//        progressDialog.setMessage("下載中...");//設置訊息("自己定義string")
//        progressDialog.setTitle("下載中");//設定標題

//Uri getUriForFile(1.Context context, 2.String authority,3.File file)
//Uri pdfUri = FileProvider.getUriForFile(this,getPackageName() + ".provider",file);//1.這個contient 2.先取得你自己的檔案位置+剛provider制定的命字3.要放的檔案位置


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {
    private TextView msg ;
    private MyReceiver myReceiver;
    private ImageView img;
    private Bitmap bmp;
    private UIHandler uiHandler;
    private boolean isSaveStorage; //是否有允許存取權限,一開始是false
    private File downloadDir;
    private ProgressDialog progressDialog;//下載中跑圈圈圖物件實體

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //10.詢問用戶權限設定,有沒有寫出權限,沒有的話去要回應馬123
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    123);
        }else{
            isSaveStorage = true;
            initdownloadDir();//已經同意了,所以讓你玩


        }


        //5.廣播註冊送出
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter("brad");//過濾完只收brad廣播
        registerReceiver(myReceiver,intentFilter);//廣播註冊,(1.接收廣播,2.過濾的接收東西)

        uiHandler = new UIHandler();//handle呼叫處理圖片傳送
        msg = findViewById(R.id.msg);//取得顯示畫面
        img = findViewById(R.id.img);//取得圖像顯示位置

        //ProgressDialog(Context context):////下載中跑圈圈圖物件實體(this Content)
        progressDialog = new ProgressDialog(this);//下載中跑圈圈圖物件實體(this Content)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);//設置為跑圈圈型態
        progressDialog.setMessage("下載中...");//設置訊息("自己定義string")
        progressDialog.setTitle("下載中");//設定標題

        Log.v("brad","有走完Creat");
    }

    //10.存在sdcard/download底下的方法
    private  void initdownloadDir(){
        // getExternalStoragePublicDirectory(String type):取得外部公開檔案位置(檔案種類)
        downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);//取得sdcard/downolad位置

        Log.v("brad",downloadDir.getAbsolutePath());//顯示一下downloadDir的絕對路徑
    }

    //9.權限初始化設定,這次玩的方式是如果不同意還可以玩其他按鈕,但test3不行玩
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {  //如果是有同意的話
            isSaveStorage = true;//權限true為同意打開權限
            initdownloadDir();//有同意授權我才允許你寫下檔案
        }
    }

    //1.按下按鈕取的資策會url網頁原始碼,m
    public void test1(View view) {
       new Thread(){
           @Override
           public void run() {
               try {
                   //1.取得資策會連線http頁面
//                    URL url = new URL("https://www.iii.org.tw/");//抓取URL("要連接的網址")
                    URL url = new URL("http://www.bradchao.com");//http本來無法連接,加了名馬傳送後可以
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
    //6.取得url圖片檔,道奇摩複製圖片地址
    public void test2(View view) {
        new Thread(){
            @Override
            public void run() {
                try {
                    //*連線圖片url
                    URL url = new URL("https://s1.yimg.com/uu/api/res/1.2/xM8.AZtUc6OBTAi58eVlwA--~B/Zmk9dWxjcm9wO2N3PTkwMDtkeD0wO2NoPTU0NTtkeT00OTt3PTM5MjtoPTMwODtjcj0xO2FwcGlkPXl0YWNoeW9u/https://media-mbst-pub-ue1.s3.amazonaws.com/creatr-uploaded-images/2019-09/284adf80-dcce-11e9-97e5-5be965365f3d");
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.connect();//url連線

                    //*直接把串流用decodeStream灌到Bmp,再用UIhandle傳給handlemessge處理
                    bmp = BitmapFactory.decodeStream(conn.getInputStream());//取得圖片(用串流方式取得)(回傳到Bitmap)
                    uiHandler.sendEmptyMessage(0);//把這個訊息給handleMessage接收


                }catch (Exception e){
                    Log.v("brad","取得url圖片出現錯誤" + e.toString());
                }
            }
        }.start();
    }
    //8.把檔案存在手機上
    public void test3(View view) {
        if(!isSaveStorage) return; //如果你的允許權限為false直接true不給你玩
        progressDialog.show();//把剛設定的log顯示出來
        new Thread(){
            @Override
            public void run() {
                try {
                    URL url = new URL("https://pdfmyurl.com/?url=https://www.gamer.com.tw");
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.connect();//url連線

                    //輸出到sdcard/downloadDire
                    downloadDir = new File(downloadDir,"c.pdf");
                    FileOutputStream fout = new FileOutputStream(downloadDir);

                    //一邊讀黨一邊寫出
                    BufferedInputStream bin = new BufferedInputStream(conn.getInputStream());
                    int len; byte[] buf = new byte[4096*4006];
                    while( (len =bin.read()) != -1){
                        fout.write(buf,0,len);
                    }
                    fout.flush();
                    fout.close();
                    bin.close();
                    Log.v("brad","讀寫檔案成功,成功下載到download區");
                    uiHandler.sendEmptyMessage(1);//下載完成傳1訊息


                }catch (Exception e){
                    Log.v("brad","pdf下載失敗" + e.toString());
                    uiHandler.sendEmptyMessage(2);//下載失敗船2訊息
                }

            }
        }.start();
    }




    //7.UIHandler來處理圖片,接收你船的圖片訊息,做操作
    private class  UIHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what){ //如果是收到0訊息設定圖片()
                case 0:
                    img.setImageBitmap(bmp);
                    break;
                case 1: //如果訊息為1,
                    progressDialog.dismiss();
                    showPDF();//秀出pdf方法
                    break;
                case 2: //如果訊息為2 ,訊息要顯示時顯示不要時miss
                    progressDialog.dismiss();
                    break;
            }
//          if(msg.what == 0)  img.setImageBitmap(bmp);到
//          if(msg.what ==1 || msg.what ==2){ progressDialog.dismiss();}//如果訊息為1,2 ,訊息要顯示時顯示不要時miss

        }
    }
    //11.使用別人人寫的方法intente pdf
    private  void showPDF(){
        File file = new File(downloadDir,"gamer.pfd");
        //Uri getUriForFile(1.Context context, 2.String authority,3.File file)

        //呼叫你內部城市的intente去看pdf
       Uri pdfUri = FileProvider.getUriForFile(this,getPackageName() + ".provider",file);//1.這個contient 2.先取得你自己的檔案位置+剛provider制定的命字3.要放的檔案位置

        Intent intent = new Intent(Intent.ACTION_VIEW);//取得intent去看(String action要看的物件)
        intent.setDataAndType(pdfUri,"application/pdf");
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//跳转到的activity不压在栈中。
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//給予url臨時的讀取權限
        startActivity(intent);
        Log.v("brad","有盡到showpdf");

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
