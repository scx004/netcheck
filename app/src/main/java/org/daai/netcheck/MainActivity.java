package org.daai.netcheck;

import android.app.Activity;
import android.content.Context;
import android.location.Geocoder;
import android.location.Location;
import android.location.Address;

import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Selection;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.thin.downloadmanager.DefaultRetryPolicy;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListenerV1;
import com.thin.downloadmanager.ThinDownloadManager;

import java.io.InputStream;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.lang.Thread;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.http.params.HttpParams;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.net.telnet.TelnetClient;

import bolts.Continuation;
import bolts.Task;

public class MainActivity extends Activity {

    Button button;
    EditText checkdomain;
    TextView textView;
    TextView textView2;
    EditText textViewPort;
    Button clean_text;
    Button btn_sendmail;
    private Spinner spinner;
    private ArrayAdapter<String> adapter;
    final String urlKey = "c4abdc8bbc72cb07f9f5b961b2";
    private static final String[] m = {"域名解析+PING", "出口IP+DNSIP", "TranceRoute", "端口检测TCP", "端口检测UDP", "Curl结果", "下载文件","测试任务"};
    private ThinDownloadManager downloadManager;
    private long totalBytes;
    private LocationManager locationManager;
    private String provider;
    private String opt_s;
    private String opt_r_s = "Return begin : \n";
    private String dl_r_s = "";
    private int run_num = 0;
    private static SSLSocketFactory socketFactory;

    private static CountDownLatch countdownLatch = new CountDownLatch(11);

    //String serviceName = Context.LOCATION_SERVICE;
    //locationManager =(LocationManager）getSystemService(serviceName);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        downloadManager = new ThinDownloadManager();
        button = (Button) findViewById(R.id.button);
        clean_text = (Button) findViewById(R.id.clean_text);
        btn_sendmail = (Button) findViewById(R.id.btn_sendmail);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start_button(v);
            }
        });
        clean_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clean_button(v);
            }
        });
        btn_sendmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendmail(v);
            }
        });
        checkdomain = (EditText) findViewById(R.id.check_domain);
        textViewPort = (EditText) findViewById(R.id.textViewPort);
        textViewPort.setVisibility(View.GONE);
        textView = (TextView) findViewById(R.id.textView);
        textView2 = (TextView) findViewById(R.id.textView2);
        spinner = (Spinner) findViewById(R.id.spinner);
        //将可选内容与ArrayAdapter连接起来
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m);
        //设置下拉列表的风格
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //将adapter 添加到spinner中
        spinner.setAdapter(adapter);
        //添加事件Spinner事件监听
        spinner.setOnItemSelectedListener(new SpinnerSelectedListener());
        //设置默认值
        spinner.setVisibility(View.VISIBLE);
    }

    public void download() {
        final long startTime = System.currentTimeMillis();
        final String  dladdr = checkdomain.getText().toString();
        //Uri downloadUri = Uri.parse("http://package.cdn.box.xiaomi.com/mitv/10009/3/3b05e1a6cb9d1b412e696ba7d3bb0610.apk");
        Uri downloadUri = Uri.parse(dladdr);
        Uri destinationUri = Uri.parse(this.getExternalCacheDir().toString() + "/tmp_file");
        final String filestr = this.getExternalCacheDir().toString() + "/tmp_file";
        DownloadRequest downloadRequest = new DownloadRequest(downloadUri)
                .addCustomHeader("clent", "daai-netcheck")
                .setRetryPolicy(new DefaultRetryPolicy())
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
                .setStatusListener(new DownloadStatusListenerV1() {
                    @Override
                    public void onDownloadComplete(DownloadRequest downloadRequest) {
                        long endTime = System.currentTimeMillis();
                        float bpsM = (float) totalBytes/((endTime - startTime)/1000)/1024/1024;
                        try {
                            textView.setText(textView.getText().toString() + "\n文件MD5 ：" + fileMD5(filestr) + "\n文件长度 ：" + totalBytes + "\n下载速度 ：" + String.format("%.2f",bpsM) + " MB/秒" );
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), "MD5 Fail:" + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onDownloadFailed(DownloadRequest downloadRequest, int errorCode, String errorMessage) {
                        textView.setText("下载失败 ： " + errorMessage);
                    }
                    @Override
                    public void onProgress(DownloadRequest downloadRequest, long totalBytes, long downloadedBytes, int progress) {
                        MainActivity.this.totalBytes = totalBytes;
                        textView.setText("下载进度  ： " + progress + " %");

                    }
                });
        downloadManager.add(downloadRequest);
    }

    public synchronized void run_num_add () {
        run_num ++;
    }
    public synchronized void task_str_add (String taskstr) {
        opt_r_s = opt_r_s  + taskstr;
    }

    public String downloadopt(String url) {
        final long startTime = System.currentTimeMillis();
        final String  dladdr = url;
        Uri downloadUri = Uri.parse(dladdr);
        Uri destinationUri = Uri.parse(this.getExternalCacheDir().toString() + "/tmp_file");
        final String filestr = this.getExternalCacheDir().toString() + "/tmp_file";
        DownloadRequest downloadRequest = new DownloadRequest(downloadUri)
                .addCustomHeader("clent", "daai-netcheck")
                .setRetryPolicy(new DefaultRetryPolicy())
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
                .setStatusListener(new DownloadStatusListenerV1() {
                    @Override
                    public  void onDownloadComplete(DownloadRequest downloadRequest) {
                        long endTime = System.currentTimeMillis();
                        float bpsM = (float) totalBytes/((endTime - startTime)/1000)/1024/1024;
                        try {
                            dl_r_s= dl_r_s + "\n文件MD5 ：" + fileMD5(filestr) + "\n文件长度 ：" + totalBytes + "\n下载速度 ：" + String.format("%.2f",bpsM) + " MB/秒";

                        } catch (Exception e) {
                            //Toast.makeText(getApplicationContext(), "MD5 Fail:" + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onDownloadFailed(DownloadRequest downloadRequest, int errorCode, String errorMessage) {
                        //textView.setText("下载失败 ： " + errorMessage);
                    }
                    @Override
                    public void onProgress(DownloadRequest downloadRequest, long totalBytes, long downloadedBytes, int progress) {
                        MainActivity.this.totalBytes = totalBytes;
                        //textView.setText("下载进度  ： " + progress + " %");

                    }
                });
        downloadManager.add(downloadRequest);
        return dl_r_s;
    }

    public String ping(String host) {
        String result = "PING结果：\n";
        try {
            String ip = host;
            Process p = Runtime.getRuntime().exec("ping -c 5 -w 100 " + ip);
            // 读取ping的内容，可不加。
            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuffer stringBuffer = new StringBuffer();
            String content = "";
            while ((content = in.readLine()) != null) {
                stringBuffer.append(content + "\n");
                final String ping_content = content;
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(textView.getText().toString() + "\n" + ping_content);
                    }
                });
            }
            result = stringBuffer.toString();
            // PING的状态
            int status = p.waitFor();
            if (status == 0) {
                result = result + "\n" + "successful~";
                return result;
            } else {
                result = result + "\n" + "failed~ cannot reach the IP address";
            }
        } catch (IOException e) {
            result = result + "\n" + "failed~ IOException";
        } catch (InterruptedException e) {
            result = result + "\n" + "failed~ InterruptedException";
        } finally {
            return result;
        }
    }

    public String nslookup(String host) {
        String result = "DNS解析结果:\n";
        try {
            Lookup lookup = new Lookup(host, Type.A);
            lookup.run();
            if (lookup.getResult() != Lookup.SUCCESSFUL) {
                System.out.println("ERROR: " + lookup.getErrorString());
                result = "ERROR: " + lookup.getErrorString();
                System.out.println("22" + result);
                return result;
            }
            result = result + host + "\n";
            Record[] answers = lookup.getAnswers();
            for (Record rec : answers) {
                result = result + rec.toString() + "\n";

            }
        } catch (Exception e) {
            Toast.makeText(this, "DNS lookup Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            return result;
        }
    }
/*    public Task<String> nslookuptask(String host) {
        String result = "DNS解析结果:\n";
        try {
            Lookup lookup = new Lookup(host, Type.A);
            lookup.run();
            if (lookup.getResult() != Lookup.SUCCESSFUL) {
                System.out.println("ERROR: " + lookup.getErrorString());
                result = "ERROR: " + lookup.getErrorString();
                System.out.println("22" + result);
            }
            result = result + host + "\n";
            Record[] answers = lookup.getAnswers();
            for (Record rec : answers) {
                result = result + rec.toString() + "\n";

            }
        } catch (Exception e) {
            Toast.makeText(this, "DNS lookup Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }*/

    public static String fileMD5(String inputFile) throws IOException {
        // 缓冲区大小（这个可以抽出一个参数）
        int bufferSize = 256 * 1024;
        FileInputStream fileInputStream = null;
        DigestInputStream digestInputStream = null;
        try {
            // 拿到一个MD5转换器（同样，这里可以换成SHA1）
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            // 使用DigestInputStream
            fileInputStream = new FileInputStream(inputFile);
            digestInputStream = new DigestInputStream(fileInputStream, messageDigest);
            // read的过程中进行MD5处理，直到读完文件
            byte[] buffer = new byte[bufferSize];
            while (digestInputStream.read(buffer) > 0) ;
            // 获取最终的MessageDigest
            messageDigest = digestInputStream.getMessageDigest();
            // 拿到结果，也是字节数组，包含16个元素
            byte[] resultByteArray = messageDigest.digest();
            // 同样，把字节数组转换成字符串
            return byteArrayToHex(resultByteArray);
        } catch (NoSuchAlgorithmException e) {
            System.out.print(e.getMessage());
            return null;
        } finally {
            try {
                digestInputStream.close();
            } catch (Exception e) {
            }
            try {
                fileInputStream.close();
            } catch (Exception e) {
            }
        }
    }

    public static String byteArrayToHex(byte[] byteArray) {

        // 首先初始化一个字符数组，用来存放每个16进制字符
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
        char[] resultCharArray = new char[byteArray.length * 2];
        // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        // 字符数组组合成字符串返回
        return new String(resultCharArray);
    }

    public static String mark_signature(String url, String urlKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            byte[] secretByte = urlKey.getBytes("UTF-8");
            byte[] dataBytes = url.getBytes("UTF-8");
            SecretKey secret = new SecretKeySpec(secretByte, "HmacSHA1");
            mac.init(secret);
            byte[] doFinal = mac.doFinal(dataBytes);
            return byteArrayToHex(doFinal);
        } catch (Exception e) {
            System.out.print("nettest error:" + e.getMessage());
        }
        return null;
    }

    public String getuniqueId() {
        TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        String simSerialNumber = tm.getSimSerialNumber();
        String androidId = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) imei.hashCode() << 32) | simSerialNumber.hashCode());
        String uniqueId = deviceUuid.toString();
        return uniqueId;
    }

    private void delay(int ms){
        try {
            Thread.currentThread();
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface Callback {
        public void onGetIp(String ip);
        //public void onGetdnsIp(String ip);

    }
    public interface Callbackpost {
        public void onPoststr(String str);
        //public void onGetdnsIp(String ip);

    }
    public interface Callbackdns {
        public void onGetdnsIp(String ip);
    }

    public interface Callback1 {
        public void telnetTest(String msgtext);
    }

    public interface Callback2 {
        public void telnetudpTest(String msgtext);
    }
    public interface Callback4 {
        public void getoptid(String optid);
    }

    public interface Callback3 {
        public void Curl(String curl_r);
    }

    public void getipAsync(final Callback callback) {
        new Thread() {
            public void run() {
                //final String deviceid = getuniqueId();
                //final String uri = deviceid + "." + System.currentTimeMillis() + ".d.daai.org/getdnsip";
                //final String opaque = mark_signature(uri, urlKey);
                //final String url = uri + "?deviceid=" + deviceid + "&opaque=" + opaque;
                final String oip = HttpRequestUtil.sendGet("https://www.daai.org/ip","");
                if (callback != null)
                    callback.onGetIp(oip);
                //System.out.println(oip);
                //String dnsip = HttpRequestUtil.sendGet("http://" + uri, "deviceid=" + deviceid + "&opaque=" + opaque);
                //if (dnsip.length() < 4) {
                //    dnsip = HttpRequestUtil.sendGet("http://" + uri, "deviceid=" + deviceid + "&opaque=" + opaque);
                //}
                //if (callback != null)
                //    callback.onGetdnsIp(dnsip);
                //System.out.println("Client DNS ips:" + dnsip);
            }
        }.start();
    }
    public void onPostsyncstr(final  String str,final Callbackpost callback) {
        new Thread() {
            public void run() {
                HttpRequestUtil.sendPostbuff("https://www.daai.org/revtask",str);
                if (callback != null)
                    callback.onPoststr("ok");
            }
        }.start();
    }
    public void getdnsipAsync(final Callbackdns callback) {
        new Thread() {
            public void run() {
                final String deviceid = getuniqueId();
                final String uri = deviceid + "." + System.currentTimeMillis() + ".d.daai.org/getdnsip";
                final String opaque = mark_signature(uri, urlKey);
                final String url = uri + "?deviceid=" + deviceid + "&opaque=" + opaque;
                //final String oip = HttpRequestUtil.httpRequest("http://www.daai.org/ip");
                //if (callback != null)
                //    callback.onGetIp(oip);
                //System.out.println(oip);
                String dnsip = HttpRequestUtil.sendGet("https://" + uri, "deviceid=" + deviceid + "&opaque=" + opaque);
                if (dnsip.length() < 4) {
                    dnsip = HttpRequestUtil.sendGet("https://" + uri, "deviceid=" + deviceid + "&opaque=" + opaque);
                }
                if (callback != null)
                    callback.onGetdnsIp(dnsip);
                //System.out.println("Client DNS ips:" + dnsip);
            }
        }.start();
    }

    public void getoptidAsync(final Callback4 callback) {
        new Thread() {
            public void run() {
                final String deviceid = getuniqueId();
                final String optid = checkdomain.getText().toString();
                final String uri = "/getopt?optid=" + optid;
                final String opaque = mark_signature(uri, urlKey);
                //final String url = uri + "&deviceid=" + deviceid + "&opaque=" + opaque;
                final String url = "/getopt";
                String getopt_http_r = HttpRequestUtil.sendGet("https://www.daai.org" + url, "optid=" + optid+ "&deviceid=" + deviceid + "&opaque=" + opaque);
                if (getopt_http_r.length() < 4) {
                    System.out.println("return status error!");
                }
                if (callback != null)
                    callback.getoptid(getopt_http_r);

                System.out.println("http getopt :" + getopt_http_r);
            }
        }.start();
    }

    public void curlAsync(final Callback3 callback) {
        new Thread() {
            public void run() {
                String curl_r = HttpRequestUtil.sendGet(checkdomain.getText().toString(), "");
                if (callback != null)
                    callback.Curl(curl_r);
                System.out.println("Client DNS ips:" + curl_r);
            }
        }.start();
    }
    public void curlAsyncf(final String url_s,final Callback3 callback) {
        new Thread() {
            public void run() {
                String curl_r = HttpRequestUtil.sendGet(url_s, "");
                if (callback != null)
                    callback.Curl(curl_r);
                //System.out.println("Client DNS ips:" + curl_r);
            }
        }.start();
    }
    public void telAsync(final Callback1 callback) {
        new Thread() {
            public void run() {
                InetAddress ip = null;
                int port = 80;
                String msgtext = null;
                String sport = textViewPort.getText().toString();
                if (!sport.isEmpty())
                    port = Integer.parseInt(sport);
                try {
                    ip = InetAddress.getByName(checkdomain.getText().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                TelnetClient telnet;
                telnet = new TelnetClient();
                try {
                    telnet.setConnectTimeout(5000);
                    telnet.connect(ip, port);
                    telnet.disconnect();
                    Date time = new Date();
                    msgtext = time + "\n" + ip + ":" + port + " is reachable!";
                } catch (Exception e) {
                    e.printStackTrace();
                    Date time = new Date();
                    msgtext = time + "\n" + ip + ":" + port + " is not reachable!";
                    System.out.println(msgtext);
                }
                if (callback != null)
                    callback.telnetTest(msgtext);
            }
        }.start();
    }
    public void telAsyncf(final String host, final  int port,final Callback1 callback) {
        new Thread() {
            public void run() {
                InetAddress ip = null;
                String msgtext = null;
                try {
                    ip = InetAddress.getByName(host);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                TelnetClient telnet;
                telnet = new TelnetClient();
                try {
                    telnet.setConnectTimeout(5000);
                    telnet.connect(ip, port);
                    telnet.disconnect();
                    Date time = new Date();
                    msgtext = time + "\n" + ip + ":" + port + " is reachable!";
                } catch (Exception e) {
                    e.printStackTrace();
                    Date time = new Date();
                    msgtext = time + "\n" + ip + ":" + port + " is not reachable!";
                    System.out.println(msgtext);
                }
                if (callback != null)
                    callback.telnetTest(msgtext);
            }
        }.start();
    }
    public void teludpAsync(final Callback2 callback) {
        new Thread() {
            public void run() {
                String msgtext = null;
                DatagramSocket ds = null;
                int port = 53;
                String sport = textViewPort.getText().toString();
                if (!sport.isEmpty())
                    port = Integer.parseInt(sport);
                try {
                    ds = new DatagramSocket();
                    byte[] buf = "UDP Demo".getBytes();
                    //DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(checkdomain.getText().toString()), port);
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    dp.setSocketAddress(new InetSocketAddress(InetAddress.getByName(checkdomain.getText().toString()), port));
                    ds.send(dp);
                //} catch (Exception ex) {
                  //  ex.printStackTrace();
                    //System.out.println("upd port is not reachable");
                    //msgtext = "upd port is not reachable";
                //}
                } catch (UnknownHostException e) {
                    System.out.println("Cannot find host");
                    e.printStackTrace();
                } catch (SocketException e) {
                     System.out.println("Can't open socket");
                     e.printStackTrace();
                }catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    ds.close();
                    System.out.println("upd port ds close!");
                    msgtext = "upd port is reachable";
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (callback != null)
                    callback.telnetudpTest(msgtext);
            }
        }.start();

    }

    class SpinnerSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {
            textView2.setText(m[arg2]);
            textView2.setTag(arg2);
            if (arg2 == 3 || arg2 == 4)
                textViewPort.setVisibility(View.VISIBLE);
            else
                textViewPort.setVisibility(View.GONE);
            if (arg2 == 1)
                checkdomain.setVisibility(View.GONE);
            else
                checkdomain.setVisibility(View.VISIBLE);
            if (arg2 == 5) {
                checkdomain.setText("http://");
                Editable etext = checkdomain.getText();
                Selection.setSelection(etext, etext.length());
            }
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    public void clean_button(View view) {
        textView.setText("");
    }

    public String getgbs(){

        Geocoder geocoder=new Geocoder(this);
        //获取定位服务
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //获取当前可用的位置控制器
        List<String> list = locationManager.getProviders(true);

        if (list.contains(LocationManager.GPS_PROVIDER)) {
            //是否为GPS位置控制器
            provider = LocationManager.GPS_PROVIDER;
        }
        else if (list.contains(LocationManager.NETWORK_PROVIDER)) {
            //是否为网络位置控制器
            provider = LocationManager.NETWORK_PROVIDER;

        } else {
            Toast.makeText(this, "请检查网络或GPS是否打开",
                    Toast.LENGTH_LONG).show();
            return "请检查网络或GPS是否打开";
        }
        try {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                //获取当前位置，这里只用到了经纬度
                //String string = "纬度为：" + location.getLatitude() + ",经度为：" + location.getLongitude();
                List places = null;
                String placename = "";
                places = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 5);
                placename = ((Address) places.get(0)).getAddressLine(0);
                return placename;
            }
        } catch ( Exception e) {
            e.printStackTrace();
        }
        return "无法获得位置信息";
    }

    public void sendmail(View view) {

        EMailSender.sendNetworkTestMail(this, textView.getText().toString(), new EMailSender.Callback() {
            @Override
            public void onResult(boolean success) {
                if (success) {
                    System.out.println("send mail success!");
                }
            }
        });
    }

    public void start_button(View view) {

        final String domain_name = checkdomain.getText().toString();
        final int text_name_no = (Integer) textView2.getTag();
        switch (text_name_no) {
            case 0: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String name_a = nslookup(domain_name);
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textView.setText(textView.getText().toString() + "\n" + name_a);
                                    textView.setText(textView.getText().toString() + "\n" + "Ping 结果:");
                                }
                            });
                            ping(domain_name);
                            //System.out.print(fileMD5(Environment.getExternalStorageDirectory().getPath() + "/share_pax.png"));
                        } catch (Exception e) {
                            System.out.print("netcheck error:" + e.getMessage());
                        }
                    }
                }).start();

            }
            break;
            case 1: {
                textView.setText("地理位置: " + getgbs());
                getipAsync(new Callback() {
                    @Override
                    public void onGetIp(final String ip) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(textView.getText().toString() + "\n出口IP和端口：\n" + ip);
                                System.out.print("netcheck oip :" + ip);
                            }
                        });
                    }
                });
                getdnsipAsync(new Callbackdns() {
                    @Override
                    public void onGetdnsIp(final String dnsip) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(textView.getText().toString() + "\nDnsIP:\n" + dnsip);
                            }
                        });
                    }
                });

            }
            break;
            case 2: {
                textView.setMovementMethod(new ScrollingMovementMethod());
                TracerouteWithPing traceRoute = new TracerouteWithPing(MainActivity.this);
                traceRoute.executeTraceroute(domain_name, 30, new TracerouteWithPing.TraceRouteCallback() {
                    @Override
                    public void onExecStart() {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText("Traceroute: ==========Start()\n");
                            }
                        });
                    }

                    @Override
                    public void onTraceRouteLog(final String log) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(textView.getText().toString() + "Traceroute: " + log);
                            }
                        });
                    }

                    @Override
                    public void onExecCompleted() {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(textView.getText().toString() + "\nTraceroute: =================onCompleted()");
                            }
                        });
                    }
                });
            }
            break;
            case 3: {
                telAsync(new Callback1() {
                    @Override
                    public void telnetTest(final String msgtext) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(textView.getText().toString() + "\n" + msgtext);
                            }
                        });
                    }
                });
            }
            break;
            case 4: {
                teludpAsync(new Callback2() {
                    @Override
                    public void telnetudpTest(final String msgtext) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(textView.getText().toString() + "\n" + msgtext);
                            }
                        });
                    }
                });
            }
            break;
            case 5: {
                MainActivity.this.findViewById(R.id.btn_sendmail).setVisibility(View.GONE);
                textView.setMovementMethod(new ScrollingMovementMethod());
                curlAsync(new Callback3() {
                    @Override
                    public void Curl(final String curl_r) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(curl_r);
                                MainActivity.this.findViewById(R.id.btn_sendmail).setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }
            break;
            case 6: {
                System.out.print("netcheck download begin!");
                download();
            }
            break;
            case 7: {
                getoptidAsync(new Callback4() {
                    @Override
                    public void getoptid(final String msgtext) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(textView.getText().toString() + "\n" + msgtext);
                            }
                        });
                        opt_s = msgtext;
                        JSONObject jsonObject = JSONObject.parseObject(opt_s);
                        JSONObject jsnetcheck = JSONObject.parseObject(jsonObject.get("netcheck").toString());
                        for (String key : jsnetcheck.keySet()) {
                            Object v =jsnetcheck.get(key);
                            switch (key) {
                                case "nslookup" : {
                                    System.out.println("nslookup begin : "+v+"\n");
                                    JSONArray list = (JSONArray) v;
                                    for (int i=0;i<list.size();i++) {
                                        String ns_host = (String) list.get(i);
                                        run_num_add();
                                        //opt_r_s = opt_r_s +"nslookup task:"+run_num+ "\n"+nslookup(ns_host)+"\n";
                                        task_str_add("nslookup task:"+run_num+ "\n"+nslookup(ns_host)+"\n");

                                    }
                                }
                                break;
                                case "ping" : {
                                    System.out.println("ping begin : "+v+"\n");
                                    JSONArray list = (JSONArray) v;
                                    for (int i=0;i<list.size();i++) {
                                        String ns_host = (String) list.get(i);
                                        run_num_add();
                                        task_str_add("ping task:"+run_num+"\n"+ping(ns_host)+"\n");
                                        //opt_r_s = opt_r_s + "ping task:"+run_num+"\n"+ping(ns_host)+"\n";

                                    }
                                }
                                break;
                                case "tcpcheck" : {
                                    System.out.println("tcpcheck begin : "+v+"\n");
                                    JSONArray list = (JSONArray) v;
                                    int list_num = list.size();
                                    for (int i=0;i<list_num;i++) {
                                        JSONObject kk = (JSONObject) list.get(i);
                                        String vhost = kk.get("host").toString();
                                        int vport = Integer.parseInt(kk.get("port").toString());
                                        telAsyncf(vhost, vport, new Callback1() {
                                            @Override
                                            public  void telnetTest(final String msgtext) {
                                                //opt_r_s = opt_r_s + "tcpcheck task:"+run_num +"\n" + msgtext+"\n";
                                                //System.out.println("tcpcheck task:"+run_num +"\n" + msgtext);
                                                run_num_add();
                                                task_str_add("tcpcheck task:"+run_num +"\n" + msgtext+"\n");
                                            }
                                        });
                                    }
                                    System.out.println("tcpcheck end\n");
                                }
                                break;
                                case "curl" : {
                                    System.out.println("curl begin : "+v+"\n");
                                    JSONArray list = (JSONArray) v;
                                    for (int i=0;i<list.size();i++) {
                                        String curl_r = (String) list.get(i);
                                        curlAsyncf(curl_r, new Callback3() {
                                            @Override
                                            public  void Curl(final String msgtext) {
                                                //opt_r_s = opt_r_s + "curl task:"+run_num +"\n"+ msgtext+"\n";
                                                run_num_add();
                                                task_str_add("curl task:"+run_num +"\n"+ msgtext+"\n");
                                                //System.out.println("curl task:"+run_num +"\n"+ msgtext);
                                            }
                                        });
                                    }
                                }
                                break;
                                case "oip" : {
                                    System.out.println("oip begin : "+v+"\n");
                                    getipAsync(new Callback() {
                                        @Override
                                        public  void onGetIp(final String ip) {
                                            //opt_r_s = opt_r_s +"oip task:"+run_num +"\n"+ ip+"\n";
                                            run_num_add();
                                            task_str_add("oip task:"+run_num +"\n"+ ip+"\n");
                                            //System.out.println("oip task:"+run_num +"\n"+ ip);
                                        }
                                    });
                                    System.out.println("oip end\n");
                                }
                                break;
                                case "dns" : {
                                    System.out.println("dns begin : "+v+"\n");
                                    getdnsipAsync(new Callbackdns() {
                                        @Override
                                        public  void onGetdnsIp(final String dnsip) {
                                            //opt_r_s = opt_r_s + "dns task:"+run_num +"\n"+dnsip+"\n";
                                            run_num_add();
                                            task_str_add("dns task:"+run_num +"\n"+dnsip+"\n");
                                            //System.out.println("dns task:"+run_num +"\n"+dnsip);
                                        }
                                    });
                                    System.out.println("dns end\n");
                                }
                                break;
                                case "pos" : {
                                    System.out.println("pos begin : "+v+"\n");
                                    run_num_add();
                                    task_str_add("pos task:"+run_num +"\n"+ getgbs()+"\n");
                                    //opt_r_s = opt_r_s + "pos task:"+run_num +"\n"+ getgbs()+"\n";


                                }
                                break;
                                case "download" : {
                                    System.out.println("download begin : "+v +"\n");
                                    JSONArray list = (JSONArray) v;
                                    for (int i=0;i<list.size();i++) {
                                        String curl_r = (String) list.get(i);
                                        //opt_r_s = opt_r_s + "download task:"+run_num +"\n"+downloadopt(curl_r)+"\n";
                                        run_num_add();
                                        task_str_add("download task:"+run_num +"\n"+downloadopt(curl_r)+"\n");
                                        //System.out.println("download task:"+run_num +"\n"+downloadopt(curl_r));
                                    }
                                    //System.out.println("download end\n");
                                }
                                break;
                            }
                        }
                        while (true) {
                            System.out.println("while wait : "+run_num);
                            if (run_num >= 11) {
                                System.out.println("main thread is back to work");
                                opt_r_s = opt_r_s + "\nReturn end";
                                System.out.println(opt_r_s);
                                onPostsyncstr(opt_r_s,new Callbackpost() {
                                    @Override
                                    public void onPoststr(final String r_str) {
                                        System.out.println(r_str);
                                    }
                                });
                                break;
                            }
                            delay(3000);
                        }

                    }

                });

            }
            break;
        }
    }
}
