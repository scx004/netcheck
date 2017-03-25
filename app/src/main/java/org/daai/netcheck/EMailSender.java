package org.daai.netcheck;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class EMailSender {
    public static class MailSenderInfo {
       // 发送邮件的服务器的IP和端口    
        private String mailServerHost = "smtp.126.com";
        private String mailServerPort = "25";
        
       // 邮件发送者的地址    
       public String fromAddress = "scx004@126.com";
       // 登陆邮件发送服务器的用户名和密码    
       public String userName = "scx004";
       public String password = "qwer1234";
       
       // 邮件接收者的地址    
       public String toAddress = "sendmail.netcheck@daai.org";
       
       // 是否需要身份验证    
       public boolean validate = true;    
       // 邮件主题    
       public String mSubject = "UncaughtException Info";    
       // 邮件的文本内容    
       public String mContent = "Only for test!";    
       
       public Properties mProperties = null;
       
       public MailSenderInfo(){
          mProperties = new Properties();
          mProperties.put("mail.smtp.host",mailServerHost);  //设置smtp的服务器地址是smtp.126.com
          mProperties.put("mail.smtp.port", mailServerPort);
          mProperties.put("mail.smtp.auth",validate);
       }
    }
    
    private static class MyAuthenticator extends Authenticator {
       private String mUserName = null;
       private String mPassword = null;
       
       public MyAuthenticator(String username,String password){
          mUserName = username;
          mPassword = password;
       }
       
       protected PasswordAuthentication  getPasswordAuthentication( ) {
            return new PasswordAuthentication(mUserName, mPassword);
        }
    }

    public interface Callback{
       public void onResult(boolean success);
    };
    
    public static void sendTextMail(final MailSenderInfo mailInfo, final Callback callback) {
       /*new Thread(new Runnable(){

         @Override
         public void run() {*/
            // TODO Auto-generated method stub
            // 判断是否需要身份认证    
             MyAuthenticator authenticator = null;    
              if( mailInfo.validate ) 
              {    
                  // 如果需要身份认证，则创建一个密码验证器    
                  authenticator = new MyAuthenticator(mailInfo.userName, mailInfo.password);    
              }   
              // 根据邮件会话属性和密码验证器构造一个发送邮件的session    
              Session sendMailSession = Session.getDefaultInstance(mailInfo.mProperties,authenticator);    
              try 
              {    
                  // 根据session创建一个邮件消息    
                  Message mailMessage = new MimeMessage(sendMailSession);    
                  // 创建邮件发送者地址    
                  Address from = new InternetAddress(mailInfo.fromAddress);    
                  // 设置邮件消息的发送者    
                  mailMessage.setFrom(from);    
                  // 创建邮件的接收者地址，并设置到邮件消息中    
                  Address to = new InternetAddress(mailInfo.toAddress);    
                  mailMessage.setRecipient(Message.RecipientType.TO,to);    
                  // 设置邮件消息的主题    
                  mailMessage.setSubject(mailInfo.mSubject);    
                  // 设置邮件消息发送的时间    
                  mailMessage.setSentDate(new Date());    
                  // 设置邮件消息的主要内容       
                  mailMessage.setText( mailInfo.mContent );    
                  // 发送邮件    
                  Transport.send(mailMessage); 
                  
                  if( callback != null )
                     callback.onResult( true );
                  
                  return;
              } 
              catch (MessagingException ex) 
              {    
                  ex.printStackTrace();    
              }    


              if( callback != null )
                  callback.onResult( false );
         /*}
           
        }).start(); */
    }
    
    public static void sendUncaughtExceptionMail(Context context,Thread thread, Throwable ex, final Callback callback) {
      // TODO Auto-generated method stub    
      String exceptionInfo = "";
      
      exceptionInfo += "=====================UncaughtExceptionHandler===================Start\n\n";
      
      if( ex != null ){
          exceptionInfo += "Exception: " + ex.getLocalizedMessage() + "\n";
          
          StackTraceElement[] stackTraceElements = ex.getStackTrace();
          for( StackTraceElement stackTraceElement : stackTraceElements ){
             exceptionInfo += stackTraceElement.toString() + "\n";
          }
      }
      
      exceptionInfo += "\n------------------------------------------------------------------\n\n";
      
      exceptionInfo += "\n============================UI======================================\n\n";

      
      exceptionInfo += "\n==================================================================\n\n";
      
      if( thread != null ){
          exceptionInfo += "Thread info: \n";
          exceptionInfo += " Thread Id = " + thread.getId() +"; Thread Name = " + thread.getName() + "\n";
      }
      
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy MM-dd HH:mm:ss");
      Date curDate = new Date(System.currentTimeMillis());//获取当前时间             

      
      exceptionInfo += "Date & Time : " + formatter.format(curDate) + "\n";
      
      PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = null;
      try {
         packInfo = packageManager.getPackageInfo(context.getPackageName(),0);
      } catch (NameNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

        
        exceptionInfo += " App version: " + packInfo.versionName + "\n";

      
      exceptionInfo += "System info: \n";
      exceptionInfo += " Android SDK= " + android.os.Build.VERSION.SDK_INT + "\n";
      exceptionInfo += " Build ID= " + android.os.Build.DISPLAY + "\n";
      exceptionInfo += " Build Info= " + android.os.Build.FINGERPRINT + "\n";
      exceptionInfo += " Manufacturer= " + android.os.Build.MANUFACTURER + "\n";

      
      exceptionInfo += "\n=====================UncaughtExceptionHandler===================End\n";
      
      EMailSender.MailSenderInfo mailSenderInfo = new EMailSender.MailSenderInfo();
      mailSenderInfo.mContent = exceptionInfo;
      
      sendTextMail(mailSenderInfo,callback);
   }

   public static void sendUncaughtExceptionMail(Context context,String log, final Callback callback) {
      // TODO Auto-generated method stub
      String exceptionInfo = "";

      exceptionInfo += "=====================UncaughtExceptionHandler===================Start\n\n";

      exceptionInfo += "\n------------------------------------------------------------------\n\n";
      if( log != null ){
         exceptionInfo += "log info: \n";
         exceptionInfo += log + "\n";
      }
      exceptionInfo += "\n============================UI======================================\n\n";


      exceptionInfo += "\n==================================================================\n\n";

      SimpleDateFormat formatter = new SimpleDateFormat("yyyy MM-dd HH:mm:ss");
      Date curDate = new Date(System.currentTimeMillis());//获取当前时间


      exceptionInfo += "Date & Time : " + formatter.format(curDate) + "\n";

      PackageManager packageManager = context.getPackageManager();
      // getPackageName()是你当前类的包名，0代表是获取版本信息
      PackageInfo packInfo = null;
      try {
         packInfo = packageManager.getPackageInfo(context.getPackageName(),0);
      } catch (NameNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }


      exceptionInfo += " App version: " + packInfo.versionName + "\n";


      exceptionInfo += "System info: \n";
      exceptionInfo += " Android SDK= " + android.os.Build.VERSION.SDK_INT + "\n";
      exceptionInfo += " Build ID= " + android.os.Build.DISPLAY + "\n";
      exceptionInfo += " Build Info= " + android.os.Build.FINGERPRINT + "\n";
      exceptionInfo += " Manufacturer= " + android.os.Build.MANUFACTURER + "\n";


      exceptionInfo += "\n=====================UncaughtExceptionHandler===================End\n";

      EMailSender.MailSenderInfo mailSenderInfo = new EMailSender.MailSenderInfo();
      mailSenderInfo.mContent = exceptionInfo;

      sendTextMail(mailSenderInfo,callback);
   }
    
    
    public static void sendFeedbackMail(Context context,final String content, final Callback callback) {
      new Thread(new Runnable() {

         @Override
         public void run() {
            EMailSender.MailSenderInfo mailSenderInfo = new EMailSender.MailSenderInfo();
            mailSenderInfo.mContent = content;
            mailSenderInfo.mSubject = "Feed back!";

            sendTextMail(mailSenderInfo, callback);
         }
      }).start();

   }

   public static void sendNetworkTestMail(Context context,final String content, final Callback callback) {
      new Thread(new Runnable() {

         @Override
         public void run() {
            EMailSender.MailSenderInfo mailSenderInfo = new EMailSender.MailSenderInfo();
            mailSenderInfo.mContent = content;
            mailSenderInfo.mSubject = "NetworkTest!";

            sendTextMail(mailSenderInfo, callback);
         }
      }).start();

   }
}
