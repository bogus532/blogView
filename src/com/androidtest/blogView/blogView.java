package com.androidtest.blogView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class blogView extends Activity {
	private static final String TAG = "blogView";
	public static String BLOG_REFRESHED = "com.androidtest.blogView.BLOG_REFRESHED";
	
	private static final int MENU_UPDATE = Menu.FIRST;
	private static final int MENU_PREFERENCES = Menu.FIRST+1;
	private static final int MENU_DELETE = Menu.FIRST+2;
	private static final int MENU_DELETE_SEEN = Menu.FIRST+3;
	private static final int SHOW_PREFERENCES = 1;
	
	static final private int blog_DIALOG = 1;
	static final int PROGRESS_DIALOG = 0;
	
	ListView blogListView;
	blogAdapter aa;
	
	ArrayList<blog> blogArray = new ArrayList<blog>();
		
	blog selectedblog;
	
	ProgressDialog progressDialog;
	public WheelProgressDialog wheelprogressDialog;
	
	boolean myappstate;
	
	boolean dialog_orientation = false;
	
	boolean autoUpdate = false;
	int updateFreq = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        myappstate = false;
        
        blogListView = (ListView)this.findViewById(R.id.blogListView);
        
        blogListView.setOnItemClickListener(new OnItemClickListener () {
        	
 			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index,
					long arg3) {
				
 				selectedblog = blogArray.get(index);
        		//showDialog(blog_DIALOG);
 				if(selectedblog != null)
 				{
 					String uridata = selectedblog.getLink();
 					if(uridata == null)
 					{
 						Toast.makeText(blogView.this, R.string.data_null, Toast.LENGTH_SHORT).show();
 						return;
 					}
 					updateblogDB(selectedblog);
	 				Uri uri = Uri.parse(uridata);
	 				Intent intent  = new Intent(Intent.ACTION_VIEW,uri);
	 				startActivity(intent);
 				}
				
			}
        	
        });
  
        int layoutID = R.layout.row;
        
        aa = new blogAdapter(this,layoutID,blogArray);
        
        blogListView.setAdapter(aa);
        
        loadblogFromProvider();
        
        updateFromPreferences();
        
        showDialog(PROGRESS_DIALOG);
        
        serviceStartFunc();
    }
    
    @Override
    protected void onResume()
    {
        Log.d(TAG, "onResume");
        super.onResume();

        showDialog(PROGRESS_DIALOG);
    }
    
    @Override
    protected void onDestroy()
    {
    	Log.d(TAG, "onDestroy");
    	super.onDestroy();
    	
    	myappstate = true;
    }
    
    @Override
    protected void onStop()
    {
    	Log.d(TAG, "onStop");
    	super.onStop();
    	
    	myappstate = true;
    }
    
        
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);

	    menu.add(0, MENU_UPDATE, Menu.NONE, R.string.menu_update);
	    menu.add(0, MENU_PREFERENCES, Menu.NONE,R.string.preferences_name);
	    menu.add(0, MENU_DELETE, Menu.NONE,R.string.menu_delete);
	    menu.add(0, MENU_DELETE_SEEN, Menu.NONE,R.string.menu_delete_seen);

	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    super.onOptionsItemSelected(item);

	    switch (item.getItemId()) {
	        case (MENU_UPDATE): {
	        	showDialog(PROGRESS_DIALOG);
	            return true;
	        }
	        case (MENU_PREFERENCES) : {
	        	Intent i = new Intent(this, Preferences.class);
	            startActivityForResult(i, SHOW_PREFERENCES);
	            return true;
	        }
	        case (MENU_DELETE): {
	        	deleteblogFromProvider(0);
	        	showDialog(PROGRESS_DIALOG);
	            return true;
	        }
	        case (MENU_DELETE_SEEN): {
	        	deleteblogFromProvider(1);
	        	showDialog(PROGRESS_DIALOG);
	            return true;
	        }
	        
	    }
	    return false;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);

	    if (requestCode == SHOW_PREFERENCES) {
	        updateFromPreferences();

	        showDialog(PROGRESS_DIALOG);
	        //serviceStartFunc();
	    }
	}
    
    @Override
	public Dialog onCreateDialog(int id) {
	    switch(id) {
	        case (blog_DIALOG) :
	        	if(dialog_orientation){
	        		removeDialog(blog_DIALOG);
	        		return null;
	        	}
	            LayoutInflater li = LayoutInflater.from(this);
	            View blogDetailsView = li.inflate(R.layout.blog_details, null);

	            AlertDialog.Builder blogDialog = new AlertDialog.Builder(this);
	            blogDialog.setTitle("게시판");
	            blogDialog.setView(blogDetailsView);
	            dialog_orientation = false;
	            return blogDialog.create();
			case PROGRESS_DIALOG:
				
				wheelprogressDialog = WheelProgressDialog.show(this,"","",true,true,null);

				new readblog().execute();
				
				return wheelprogressDialog;
	    }
	    return null;
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
	    switch(id) {
	        case (blog_DIALOG) :
	            
	        	if(selectedblog == null)
	        	{
	        		dialog_orientation = true;
	        		return;
	        	}
	        		
	            String blogText = selectedblog.getTitle()+"\n"+selectedblog.getLink();
	        	
	            String dateString = "Contents";
	            AlertDialog blogDialog = (AlertDialog)dialog;
	            blogDialog.setTitle(dateString);
	            TextView tv = (TextView)blogDialog.findViewById(R.id.textView1);
	            tv.setText(blogText);

	            break;
	    }
	}
	
	private void updateFromPreferences() {
	    Context context = getApplicationContext();
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

	    autoUpdate = prefs.getBoolean(Preferences.PREF_AUTO_UPDATE, false);
	    updateFreq = Integer.parseInt(prefs.getString(Preferences.PREF_UPDATE_FREQ, "0"));
	}
	
	private void serviceStartFunc()
	{
		startService(new Intent(this, blogService.class));
	}
    
    private int refreshblog() {
	    // XML을 가져온다.
	    URL url;
	    int result=0;
	    try {
	        //String blogFeed = blog_feed;
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

	            // 이전에 있던 정보들을 모두 삭제한다.
	            blogArray.clear();
	            loadblogFromProvider();

	            // 정보로 구성된 리스트를 얻어온다.
	            NodeList nl = docEle.getElementsByTagName("item");
	            result = nl.getLength();
	            if (nl != null && nl.getLength() > 0) {
	                for (int i = 0 ; i < nl.getLength(); i++) {
	                    Element items = (Element)nl.item(i);
	                    Element title = (Element)items.getElementsByTagName("title").item(0);
	                    Element link = (Element)items.getElementsByTagName("link").item(0);
	                    //Element date = (Element)items.getElementsByTagName("dc:date").item(0);
	                    Element date = (Element)items.getElementsByTagName("pubDate").item(0); 
	                    Element author = (Element)items.getElementsByTagName("author").item(0);
	                   // Element des = (Element)items.getElementsByTagName("description").item(0);

	                    String details = title.getFirstChild().getNodeValue();
	                    
	                    String linkString = link.getFirstChild().getNodeValue();
	                    	                    
	                    ///RFC2822
	                    ///"Wed, 02 Apr 2008 03:45:32 +0900"
	                    String dt = date.getFirstChild().getNodeValue();
	                    SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	                    Date tmpdate = new Date(dt);
	                    dt = sdf2.format(tmpdate);
	                    Log.d(TAG,"date : "+dt);
	                    ////RFC2822 
	                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	                    Date qdate = new GregorianCalendar(0,0,0).getTime();
	                    try {
	                        qdate = sdf.parse(dt);
	                    } catch (ParseException e) {
	                        e.printStackTrace();
	                    }
	                    
	                    String strAuthor = author.getFirstChild().getNodeValue();
	                    	                    
	                    //Log.d(TAG,linkString);
	                    
	                    //String strDes = des.getFirstChild().getNodeValue();
	                    //Log.d(TAG,strDes);
	                    
	                    int readcheck = 0;

	                    blog blogData = new blog(details, linkString,qdate,strAuthor,readcheck);

	                    addNewblog(blogData);
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
    
    private void addNewblog(blog _blog) {
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
	    	
	    	blogArray.add(_blog);
	    } 
	    
   	}
    
    private void updateblogDB(blog _blog) {
	    ContentResolver cr = getContentResolver();
	    String w = blogProvider.KEY_DATE + " = " + _blog.getDate().getTime();
	    	    
	    //Log.d(TAG,"updateblogDB : "+w);
	    
	    ContentValues values = new ContentValues();
	    	
	    values.put(blogProvider.KEY_READCHECK,"1");
	    	
	    cr.update(blogProvider.CONTENT_URI, values, w, null);
	    
   	}
    
    private void addblogToArray(blog _blog)
    {
    	blogArray.add(_blog);
    }
    
    private void updateListView()
    {
    	int index = aa.getCount();
    	Log.d(TAG,"ListArray count : "+index);
    	aa.notifyDataSetChanged();
    	
    	boolean backup_result = backupDB();
    	Log.d(TAG,"backup restult : "+backup_result);
    }
    
    void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
           inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
           if (inChannel != null)
              inChannel.close();
           if (outChannel != null)
              outChannel.close();
        }
     }
    
    private boolean backupDB()
    {
        File dbFile =
                 new File(Environment.getDataDirectory() + "/data/com.androidtest.blogView/databases/blog.db");

        File exportDir = new File(Environment.getExternalStorageDirectory(), "exampledata");
        if (!exportDir.exists()) {
           exportDir.mkdirs();
        }
        File file = new File(exportDir, dbFile.getName());

        try {
           file.createNewFile();
           this.copyFile(dbFile, file);
           return true;
        } catch (IOException e) {
           Log.e(TAG, e.getMessage(), e);
           return false;
        }
     }
    
    private void deleteblogFromProvider(int deletetype)
    {
    	blogArray.clear();
    	ContentResolver cr = getContentResolver();
    	
    	String where = null;
    	
    	if(deletetype >0)
    	{
    		where = blogProvider.KEY_READCHECK + " = " + "1";
    	}
    	
     	cr.delete(blogProvider.CONTENT_URI, where, null);
    }
    
    private void loadblogFromProvider() {
	    
	    blogArray.clear();

	    ContentResolver cr = getContentResolver();

	    // 저장된 정보를 모두 가져온다.
	    Cursor c = cr.query(blogProvider.CONTENT_URI, null, null, null, null);
	    
	    if(c==null)
	    {
	    	return;
	    }
	    
	    if (c.moveToFirst()) {
	        do {
	            // 세부정보를 얻어온다.
	            
	            String title = c.getString(blogProvider.TITLE_COLUMN);
	            String link = c.getString(blogProvider.LINK_COLUMN);
	            Long datems = c.getLong(blogProvider.DATE_COLUMN);
	            String author = c.getString(blogProvider.AUTHOR_COLUMN);
	            int readcheck = c.getInt(blogProvider.READCHECK_COLUMN);
	            
	            Date date = new Date(datems);
	            
	            blog q = new blog(title, link,date,author,readcheck);
	            addblogToArray(q);
	        } while(c.moveToNext());
	    }
	    
	    c.close();
	}
    
    private class readblog extends AsyncTask<Void, Integer, Integer> {    	
    	   
 
		@Override
		protected void onPreExecute() {
				
			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(Void... arg0) {
			int totalIndex = 0;
			totalIndex = refreshblog();
			Log.d(TAG,"doInBackground : "+ totalIndex);
			return totalIndex;
		}  
    	
    	@Override
    	protected void onProgressUpdate(Integer... progress) {
 
    	}
    	
     	@Override
    	protected void onPostExecute(Integer result) {
    		blogView.this.removeDialog(PROGRESS_DIALOG);
    		
    		Log.d(TAG,"onPostExecute : "+ result);
			
			if(result == 0)
			{
				Toast.makeText(blogView.this, R.string.data_null, Toast.LENGTH_SHORT).show();
				return;
			}
			
			updateListView(); 
			sendBroadcast(new Intent(BLOG_REFRESHED));
    	}
    	
     	@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		  	
    }
    
    
}