package com.puchinike.etsitnotas;

import java.util.Locale;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.puchinike.etsitnotas.model.Asignatura;
import com.puchinike.etsitnotas.model.NotasWrapper;

public class MainActivity extends Activity {
	

	ListView list;
	NotasReader nr;
	String rawHTML;
	String lastUpdate;
	String usr;
	String pwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        list = (ListView)findViewById(R.id.listview);
        if (savedInstanceState != null) {
        	this.rawHTML = savedInstanceState.getString("RawHTML");
        	this.lastUpdate = savedInstanceState.getString("LastUpdate");
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        this.rawHTML = sharedPref.getString("RawHTML",null);
        this.lastUpdate = sharedPref.getString("LastUpdate",null);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	this.usr = prefs.getString("usuario", "");
    	this.pwd = prefs.getString("password", "");
    	if (this.usr.equals("") || this.pwd.equals("")){
    		firstTimeInit();
    	} else {
    		loadNotasLocal();
    		loadNotasRemote();
    	}
    }
    
    @Override
    public void onStop(){
    	super.onStop();
    	SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
    	SharedPreferences.Editor editor = sharedPref.edit();
    	editor.putString("RawHTML", this.rawHTML);
    	editor.putString("LastUpdate", this.lastUpdate);
    	editor.commit();
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	this.nr.cancel(true);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
            	startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_refresh:
            	loadNotasRemote();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    private void firstTimeInit() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Bienvenido");
        builder.setMessage("Para comenzar, configura tu usuario y contraseña de la web de notas provisionales. Puedes cambiar posteriormente estos datos desde el menú de Ajustes");  
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {  
             @Override  
             public void onClick(DialogInterface dialog, int which) {  
                  dialog.cancel();
                  startActivity(new Intent(MainActivity.this, SettingsActivity.class));
             }  
        });  
        AlertDialog alert = builder.create();  
        alert.show();
    	
    }
    
    private void loadNotasRemote()
    {
    	if (isNetworkAvailable()){
    		this.nr = new NotasReader();
	        nr.execute(this.usr,this.pwd);
    	} else {
    		errorDialog("No hay conexión", "Se necesita conexión a internet para acceder a las notas");
    	}
    }
    
    private void loadNotasLocal() {
    	if (this.rawHTML != null) {
    		Log.d("APPLY HTML", "Applying new HTML from saved state");
    		applyRawHTML(this.rawHTML);
    	}
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager 
              = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }
    
    private void errorDialog(String title, String message) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(title);
        builder.setMessage(message);  
        builder.setNegativeButton("Aceptar", new DialogInterface.OnClickListener() {  
             @Override  
             public void onClick(DialogInterface dialog, int which) {  
                  dialog.cancel();  
             }  
        });  
        AlertDialog alert = builder.create();  
        alert.show();
    }
    
    private void updateList(NotasWrapper notas) {
    	NotasListViewAdapter notasAdapter = new NotasListViewAdapter(this, R.layout.tablerow, notas.getAsignaturas());
        list.setAdapter(notasAdapter);
        TextView lastUpdate = (TextView) findViewById(R.id.last_update);
        lastUpdate.setText(notas.getLastupdate());
        TextView userName = (TextView) findViewById(R.id.user_name);
        userName.setText(notas.getName());
    }
    
    public Asignatura[] parseNotas(String source) {
		source = source.replaceAll("<b>", "");
		source = source.replaceAll("</b><br>", "<SEPARATE>");
		source = source.replaceAll("<br><br>", "<BREAK>");
		source = source.replaceAll("<br>", "\r\n");
		source = source.replaceAll("&nbsp;&nbsp;&nbsp;&nbsp;", "\r\n");
		source = source.replaceAll("&nbsp;", " ");
		Log.d("PARSENOTAS" , source);
		String[] notas = source.split("<span class=\"Estilo25\">");
        notas = notas[1].split("</span>");
        notas = notas[0].split("<BREAK>");
        
        Asignatura[] asignaturas = new Asignatura[notas.length];
        for (int i = 0; i < notas.length; i++) {
        	asignaturas[i] = notaToAsignatura(notas[i]);
        }
        return asignaturas;
	}
    
    public Asignatura notaToAsignatura(String line) {
		String[] nota = line.split("<SEPARATE>");
		nota[1] = nota[1].replaceAll("\r\n$", "");
		Log.d("PARSENOTAS" , nota[1]);
		return new Asignatura(nota[0],nota[1]);
	}
    
    public void applyRawHTML(String html){
    	applyRawHTML(html,null);
    }
    
    public void applyRawHTML(String html, String lastupdate) {
    	if (html != null) {
	    	Asignatura[] asignaturas = parseNotas(html);
	        String name = parseName(html);
	        NotasWrapper nw = new NotasWrapper(asignaturas,name);
	        if (lastupdate != null){
	        	nw.setLastupdate(lastupdate);
	        }
	        this.rawHTML = html;
	        this.lastUpdate = nw.getLastupdate();
	        updateList(nw);
    	} else {
    		errorDialog("Error", "No se han podido cargar las notas");
    	}
    }
    
    public String parseName(String source) {
		// This could be fancier but not better
		source = source.split("<div align=\"justify\" class=\"12puntos\">")[1];
		source = source.replaceAll("<br>", "");
		source = source.replaceAll("</div>", "");
		source = source.replaceAll("\\s{2,}", " ");
		source = source.toLowerCase(Locale.US);
		String[] words = source.split(" ");
		String name = "";
		for (String word : words) {
			char[] stringArray = word.toCharArray();
			if(stringArray.length > 1) {
    			stringArray[0] = Character.toUpperCase(stringArray[0]);
    			name += new String(stringArray) + " ";
			}
		}
		return name;
	}
    
    class NotasReader extends AsyncTask<String, Void, String> {
    	
    	private HttpGet request;
    	
    	@Override
    	protected String doInBackground(String... params) {
    		try {
        	    // Create a URL for the desired page
    			String usr = params[0];
    			String pwd = params[1];
    			Log.w("LOGIN", "User: " + usr);
    			Log.w("LOGIN", "Password: " + pwd);
        	    //String url = "http://" + usr + ":" + pwd + "@www-app.etsit.upm.es/notas/";
    			String url = "http://www-app.etsit.upm.es/notas/";

                DefaultHttpClient client = new DefaultHttpClient();
                this.request = new HttpGet(url);
                Credentials creds = new UsernamePasswordCredentials(usr, pwd);
                client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), creds);
                // Get the response
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String response_str = client.execute(this.request, responseHandler);
                return response_str;
                
        	} catch (Exception e) {
            	return null;
        	}
    	}
    	@Override
    	protected void onPreExecute() {
    		setProgressBarIndeterminateVisibility(true);
    	}
    	
    	@Override
    	protected void onPostExecute(String html) {
    		setProgressBarIndeterminateVisibility(false);
    		Toast.makeText(getApplicationContext(), "Notas actualizadas", Toast.LENGTH_SHORT).show();
    		Log.d("APPLY HTML", "Applying new HTML from remote page");
    		applyRawHTML(html);
    	}

    }
    
    

    
}

