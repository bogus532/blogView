package com.androidtest.blogView;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.widget.ProgressBar;

class WheelProgressDialog extends Dialog { 
	 
	 
	 public static WheelProgressDialog show(Context context, CharSequence title, 
	         CharSequence message) { 
	     return show(context, title, message, false); 
	 } 
	  
	 public static WheelProgressDialog show(Context context, CharSequence title, 
	         CharSequence message, boolean indeterminate) { 
	     return show(context, title, message, indeterminate, false, null); 
	 } 
	  
	 public static WheelProgressDialog show(Context context, CharSequence title, 
	         CharSequence message, boolean indeterminate, boolean cancelable) { 
	     return show(context, title, message, indeterminate, cancelable, null); 
	 } 
	 
	  
	 public static WheelProgressDialog show(Context context, CharSequence title, 
	         CharSequence message, boolean indeterminate, 
	         boolean cancelable, OnCancelListener cancelListener) { 
		 WheelProgressDialog dialog = new WheelProgressDialog(context); 
	     dialog.setTitle(title); 
	     dialog.setCancelable(cancelable); 
	     dialog.setOnCancelListener(cancelListener); 
	     /* The next line will add the ProgressBar to the dialog. */ 
	     dialog.addContentView(new ProgressBar(context), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)); 
	     dialog.show(); 
	  
	     return dialog; 
	 } 
	  
	 public WheelProgressDialog(Context context) { 
	     super(context, R.style.NewDialog); 
	 } 
}
