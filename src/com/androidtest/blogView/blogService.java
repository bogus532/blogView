package com.androidtest.blogView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class blogService extends Service {
	
	private static final String TAG = "BlogView-service";
	public static String BLOG_REFRESHED = "com.androidtest.blogView.BLOG_REFRESHED";
	
	AlarmManager alarms;
	PendingIntent alarmIntent;
	
	private boolean autoUpdate = false;
	private int updateFreq = 0;
	
	blogWidgetLookupTask lastLookup =null;
	
	//private int appstate;
	
	@Override
	public void onCreate() {
	
		Log.d(TAG,"service onCreate");
		alarms = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

	    Intent intentToFire = new Intent(BLOG_REFRESHED);
	    alarmIntent = PendingIntent.getBroadcast(this, 0, intentToFire, 0);
	}

	public blogService() {

	}

	@Override
	public IBinder onBind(Intent arg0) {

		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		Log.d(TAG,"service onStartCommand");
	    
	    Context context = getApplicationContext();
	    SharedPreferences prefs =
	        PreferenceManager.getDefaultSharedPreferences(context);

	    autoUpdate = 
	    	prefs.getBoolean(Preferences.PREF_AUTO_UPDATE, false);
	    updateFreq = 
	    	Integer.parseInt(prefs.getString(Preferences.PREF_UPDATE_FREQ, "0"));
	    
	    Log.d(TAG,"onStartCommand - "+"autoUpdate : "+autoUpdate+", updateFreq : "+updateFreq);
	    
	        
	    if(autoUpdate) {
	    	
	    	int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
	        long timeToRefresh = SystemClock.elapsedRealtime() + updateFreq*60*1000;
	        alarms.setRepeating(alarmType, timeToRefresh, updateFreq*60*1000, alarmIntent);
	        
	        Log.d(TAG,"Widget Update.................. : ");
	    }
	    else
	    	alarms.cancel(alarmIntent);
	    
	    updateWidget();
	   
	    return Service.START_NOT_STICKY;
	};
	
	private void updateWidget()
	{
		if (lastLookup == null || lastLookup.getStatus().equals(AsyncTask.Status.FINISHED)) 
		{
			Log.d(TAG,"AsyncTask execute");
	        lastLookup = new blogWidgetLookupTask();
	        lastLookup.execute((Void[])null);
	    }
		else
		{
			Log.d(TAG,"AsyncTask NOT execute");
		}
	}
	
	public class blogWidgetLookupTask extends AsyncTask<Void, Void, Void> {
	    @Override
	    protected Void doInBackground(Void... params) {
	    	
			int totalIndex = 0;
			//if(appstate>0)
			{
				totalIndex = refreshblogInWidget();
				Log.d(TAG,"doInBackground : "+ totalIndex);
			}			
			
	    	Intent intent = new Intent(BLOG_REFRESHED);
	    	sendBroadcast(intent);
	        return null;
	    }

	    @Override
	    protected void onProgressUpdate(Void... values) {
	        super.onProgressUpdate(values);
	    }

	    @Override
	    protected void onPostExecute(Void result) {
	        super.onPostExecute(result);
	        stopSelf();
	    }
	}
	
	 private int refreshblogInWidget() {
		    // XML을 가져온다.
		    URL url;
		    int result=0;
		    try {
		    	String blogFeed = getString(R.string.blog_feed);
		        url = new URL(blogFeed);

		        URLConnection connection;
		        connection = url.openConnection();

		        HttpURLConnection httpConnection = (HttpURLConnection)connection;
		        int responseCode = httpConnection.getResponseCode();

		        if (responseCode == HttpURLConnection.HTTP_OK) {
		            InputStream in = httpConnection.getInputStream(); 
		            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		            DocumentBuilder db = dbf.newDocumentBuilder();

		            //  정보 피드를 파싱한다.
		            Document dom = db.parse(in);
		            Element docEle = dom.getDocumentElement();

		            // 정보로 구성된 리스트를 얻어온다.
		            NodeList nl = docEle.getElementsByTagName("item");
		            result = nl.getLength();
		            if (nl != null && nl.getLength() > 0) {
		                for (int i = 0 ; i < nl.getLength(); i++) {
		                    Element items = (Element)nl.item(i);
		                    Element title = (Element)items.getElementsByTagName("title").item(0);
		                    Element link = (Element)items.getElementsByTagName("link").item(0);
		                    Element date = (Element)items.getElementsByTagName("pubDate").item(0); 
		                    Element author = (Element)items.getElementsByTagName("author").item(0);
		                   
		                    String details = title.getFirstChild().getNodeValue();
		                    
		                    String linkString = link.getFirstChild().getNodeValue()+"&"+link.getLastChild().getNodeValue();
		                   
		                    String dt = date.getFirstChild().getNodeValue();
		                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		                    Date qdate = new GregorianCalendar(0,0,0).getTime();
		                    try {
		                        qdate = sdf.parse(dt);
		                    } catch (ParseException e) {
		                        e.printStackTrace();
		                    }
		                    
		                    String strAuthor = author.getFirstChild().getNodeValue();
		                    
		                    int readcheck = 0;

		                    blog blogData = new blog(details, linkString,qdate,strAuthor,readcheck);

		                    addNewBlogInWidget(blogData);
		                }
		            }
		        }
		    } catch (MalformedURLException e) {
		        e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    } catch (ParserConfigurationException e) {
		        e.printStackTrace();
		    } catch (SAXException e) {
		        e.printStackTrace();
		    } finally {
		    }
		    
		    return result;
		}
	 
	 private void addNewBlogInWidget(blog _blog) {
		    ContentResolver cr = getContentResolver();
		    String w = blogProvider.KEY_DATE + " = " + _blog.getDate().getTime();
		    	    
		    //Log.d(TAG,"addNewblog : "+w);
		    
		    if(cr.query(blogProvider.CONTENT_URI, null, w, null, null).getCount() == 0)
		    {
		    	ContentValues values = new ContentValues();
		    	
		    	values.put(blogProvider.KEY_TITLE,_blog.getTitle());
		    	values.put(blogProvider.KEY_LINK,_blog.getLink());
		    	values.put(blogProvider.KEY_DATE,_blog.getDate().getTime());
		    	values.put(blogProvider.KEY_AUTHOR,_blog.getAuthor());
		    	values.put(blogProvider.KEY_READCHECK,"0");
		    	
		    	cr.insert(blogProvider.CONTENT_URI, values);	
		    	
		    } 
		    
	   	}

}
