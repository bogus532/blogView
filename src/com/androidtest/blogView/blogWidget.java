package com.androidtest.blogView;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

public class blogWidget extends AppWidgetProvider {
	
	private static final String TAG = "blogView-widget";
	
	public void updateblog(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Cursor lastblog;
		ContentResolver cr = context.getContentResolver();
		lastblog = cr.query(blogProvider.CONTENT_URI, null, null, null, null);
		RemoteViews views;

		String details = "-- None --";
		String author = "";
		String uridata = "";
		
		Log.d(TAG,"updateWidget");
		
		context.startService(new Intent(context, blogService.class));
				
		if (lastblog != null) {
			try 
			{
				int temp = lastblog.getCount(); 
				Log.d("blogView-widget","getCount : "+temp);
				int index = (int)(Math.random()*temp);
				lastblog.moveToPosition(index);
				details = lastblog.getString(blogProvider.TITLE_COLUMN);
				author = lastblog.getString(blogProvider.AUTHOR_COLUMN);
				uridata = lastblog.getString(blogProvider.LINK_COLUMN);
				
				Log.d(TAG,"details : "+details+", author : "+author);
				
				/*
				if (lastblog.moveToFirst()) 
				{
					details = lastblog.getString(blogProvider.TITLE_COLUMN);
					Log.d("blogView-widget","widget : "+details);
				}
				*/
			} 
			finally 
			{
				lastblog.close();
			}
		}
		
		final int N = appWidgetIds.length;
	    for (int i = 0; i < N; i++) {
	        int appWidgetId = appWidgetIds[i];
	        views = new RemoteViews(context.getPackageName(), R.layout.blog_widget);
	        views.setTextViewText(R.id.widget_details, details);
	        views.setTextViewText(R.id.widget_author, author);
	        
	        Uri uri = Uri.parse(uridata);
			Intent detailIntent  = new Intent(Intent.ACTION_VIEW,uri);
			
			PendingIntent pending = PendingIntent.getActivity(context, 0, detailIntent, 0);

	        views.setOnClickPendingIntent(R.id.widget_layout, pending);
			
	        appWidgetManager.updateAppWidget(appWidgetId, views);
	    }
	}

	public void updateblog(Context context) {
	    ComponentName thisWidget = new ComponentName(context, blogWidget.class);
	    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
	    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
	    updateblog(context, appWidgetManager, appWidgetIds);
	}
 
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		updateblog(context, appWidgetManager, appWidgetIds);
	}
	
	@Override
	public void onReceive(Context context, Intent intent){
	    super.onReceive(context, intent);
	    
	    Log.d(TAG,"onReceive");
	    
	    if (intent.getAction().equals(blogService.BLOG_REFRESHED))
	        updateblog(context);
	}

}
