package org.daai.netcheck;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.net.DefaultSocketFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


/**
 * Http请求工具类
 *
 * @author snowfigure
 * @version v1.0.1
 * @since 2014-8-24 13:30:56
 */
public class HttpRequestUtil {
    static boolean proxySet = false;
    static String proxyHost = "127.0.0.1";
    static int proxyPort = 8087;
    SSLSocketFactory netcheckSocketFactory;
    private static Context context;
    //private static final String VIDEO_SELF_CA="";​
    private static final String netca = "-----BEGIN CERTIFICATE-----\n" +
            "MIICXzCCAcgCCQD+5coq16MHnTANBgkqhkiG9w0BAQUFADBzMQswCQYDVQQGEwJD\n" +
            "TjELMAkGA1UECAwCQkoxCzAJBgNVBAcMAkJKMQswCQYDVQQKDAJCSjELMAkGA1UE\n" +
            "CwwCQkoxETAPBgNVBAMMCGRhYWkub3JnMR0wGwYJKoZIhvcNAQkBFg5hZG1pbkBk\n" +
            "YWFpLm9yZzAgFw0xNzAzMTgxNDI4NDlaGA8yMTE3MDIyMjE0Mjg0OVowczELMAkG\n" +
            "A1UEBhMCQ04xCzAJBgNVBAgMAkJKMQswCQYDVQQHDAJCSjELMAkGA1UECgwCQkox\n" +
            "CzAJBgNVBAsMAkJKMREwDwYDVQQDDAhkYWFpLm9yZzEdMBsGCSqGSIb3DQEJARYO\n" +
            "YWRtaW5AZGFhaS5vcmcwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALD9FqHR\n" +
            "Vq3xZT7fAiS9/4olA27mnasZZPSVlEuqhWUPgglSFqg+/w94aqAC3ijvlvtx1crV\n" +
            "GBtTvnR8yrbAtqj2VJQ9/aZRAO0gj5065w6+n0TrqBJpD2Y6ci9JX+KMZBNnRfP6\n" +
            "NSkt4UEhrn4IbA0drFrrxteqGw2gSTmxLlgdAgMBAAEwDQYJKoZIhvcNAQEFBQAD\n" +
            "gYEAjAPzfsPdYBSEhiNcDxWGCZKvlCvWZmGGQcRPP8sPDincv9ym6v3jNUCNqUES\n" +
            "aqS62WObSXX1SCue2voFAKrL526tiAdXQ5KHSXx7KfD2r/qr4U0XZXof8wMSEnPF\n" +
            "8CcnQYAcJqDJa/mkrh7xVkJc9Tx0i9WU+p3Ilv5aZjAU0Ww=\n" +
            "-----END CERTIFICATE-----";
    /**
     * 编码
     *
     * @param source
     * @return
     */
    public static void setContext(Context context) {
        HttpRequestUtil.context = context;
    }
    public static String urlEncode(String source, String encode) {
        String result = source;
        try {
            result = java.net.URLEncoder.encode(source, encode);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "0";
        }
        return result;
    }

    public static String urlEncodeGBK(String source) {
        String result = source;
        try {
            result = java.net.URLEncoder.encode(source, "GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "0";
        }
        return result;
    }

    /**
     * 发起http请求获取返回结果
     *
     * @param req_url 请求地址
     * @return
     */
    public static String httpRequest(String req_url) {

        StringBuffer buffer = new StringBuffer();
        try {
            URL url = new URL(req_url);
            HttpURLConnection httpUrlConn = (HttpURLConnection) url.openConnection();

            httpUrlConn.setDoOutput(false);
            httpUrlConn.setDoInput(true);
            httpUrlConn.setUseCaches(false);

            httpUrlConn.setRequestMethod("GET");
            httpUrlConn.connect();

            // 将返回的输入流转换成字符串
            InputStream inputStream = httpUrlConn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            bufferedReader.close();
            inputStreamReader.close();
            // 释放资源
            inputStream.close();
            inputStream = null;
            httpUrlConn.disconnect();

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
        return buffer.toString();
    }

    /**
     * 发送http请求取得返回的输入流
     *
     * @param requestUrl 请求地址
     * @return InputStream
     */
    public static InputStream httpRequestIO(String requestUrl) {
        InputStream inputStream = null;
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection httpUrlConn = (HttpURLConnection) url.openConnection();
            httpUrlConn.setDoInput(true);
            httpUrlConn.setRequestMethod("GET");
            httpUrlConn.connect();
            // 获得返回的输入流
            inputStream = httpUrlConn.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inputStream;
    }


    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url   发送请求的URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");

            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection safeConn = (HttpsURLConnection) connection;
                safeConn.setSSLSocketFactory(initcheckSSLSocketFactory());
                safeConn.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        System.out.println("麻烦什么，通通放行！");
                        return true;
                    }
                });
            }

            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url     发送请求的 URL
     * @param param   请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @param isproxy 是否使用代理模式
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String file_name, boolean isproxy) {
        //BufferedOutputStream out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            HttpURLConnection conn = null;
            if (isproxy) {//使用代理模式
                @SuppressWarnings("static-access")
                Proxy proxy = new Proxy(Proxy.Type.DIRECT.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                conn = (HttpURLConnection) realUrl.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) realUrl.openConnection();
            }
            // 打开和URL之间的连接

            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection safeConn = (HttpsURLConnection) conn;
                safeConn.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        System.out.println("麻烦什么，通通放行！");
                        return true;
                    }
                });
            }

            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");    // POST方法


            // 设置通用的请求属性

            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.addRequestProperty("file", "tvnettest_log.zip");
            conn.addRequestProperty("ptf", "206");
            conn.addRequestProperty("deviceid", "9098cff7029faae6dcf04d1669287221");
            conn.connect();

            // 获取URLConnection对象对应的输出流
            //out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
            // 发送请求参数
            //out.write(param);
            // flush输出流的缓冲
            //上传文件
            File file = new File(file_name);
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            int numReadByte = 0;
            while ((numReadByte = fileInputStream.read(bytes, 0, 1024)) > 0) {
                out.write(bytes, 0, numReadByte);
            }
            out.flush();
            fileInputStream.close();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        }

        return result;
    }
    public static String sendPostbuff(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection safeConn = (HttpsURLConnection) conn;
                safeConn.setSSLSocketFactory(initcheckSSLSocketFactory());
                safeConn.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        System.out.println("麻烦什么，通通放行！");
                        return true;
                    }
                });
            }
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }
    private static SSLSocketFactory initcheckSSLSocketFactory(){

        try {
            // Load CAs from an ByteArrayStream
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new BufferedInputStream(new ByteArrayInputStream(netca.getBytes()));
            //InputStream caInput = HttpRequestUtil.context.getResources().openRawResource(R.raw.crt);
            Certificate ca;
            ca = cf.generateCertificate(caInput);

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), new SecureRandom());
            //netcheckSocketFactory =  context.getSocketFactory();
            return context.getSocketFactory();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
