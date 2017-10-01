package ru.sla000.psb_rates_widget.psbrates;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.sla000.psb_rates_widget.psbrates.rest.CurrencyRate;
import ru.sla000.psb_rates_widget.psbrates.rest.Rates;

/**
 * The configuration screen for the {@link PSBRatesWidget PSBRatesWidget} AppWidget.
 */
public class PSBRatesWidgetConfigureActivity extends Activity {
	private final static String TAG = "ConfigureActivity";

	private static final String PREFS_NAME = "ru.sla000.psb_rates_widget.psbrates.PSBRatesWidget";

	public static final String KEY_PREF = "currency_";

	int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	@BindView( R.id.spinnerPref )
	Spinner spinnerPref;

	@BindView( R.id.progressBar )
	ProgressBar mProgressBar;

	@BindView( R.id.add_button )
	Button mButtonAdd;

	private List<String> currencies_iso =  new ArrayList<>();

	@OnClick(R.id.add_button)
	public void onClick( View v ) {
		final Context context = PSBRatesWidgetConfigureActivity.this;

		SharedPreferences.Editor prefs = context.getSharedPreferences( PREFS_NAME, 0 ).edit();

		{
			final int spinnerPrefPos = spinnerPref.getSelectedItemPosition();
			prefs.putString( KEY_PREF + widgetId, currencies_iso.get( spinnerPrefPos ) );
			Log.d(TAG, "Add Widget: spinnerPrefPos=" + spinnerPrefPos);
		}

		prefs.apply();

		// It is the responsibility of the configuration activity to update the app widget
		PSBRatesWidget.startUpdate( context, widgetId );

		// Make sure we pass back the original appWidgetId
		Intent resultValue = new Intent();
		resultValue.putExtra( AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId );
		setResult( RESULT_OK, resultValue );

		Log.d(TAG, "Add Widget: widgetId=" + widgetId );
		finish();
	}

	public PSBRatesWidgetConfigureActivity() {
		super();
	}

	static void deletePrefs( Context context, int appWidgetId ) {
		SharedPreferences.Editor prefs = context.getSharedPreferences( PREFS_NAME, 0 ).edit();

		prefs.remove( KEY_PREF + appWidgetId );

		prefs.apply();
	}

	static int loadPref( final Context context, final int appWidgetId, final String key, final int def ) {
		SharedPreferences prefs = context.getSharedPreferences( PREFS_NAME, 0 );

		return prefs.getInt( key + appWidgetId, def);
	}

	static String loadPref( final Context context, final int appWidgetId, final String key, final String def ) {
		SharedPreferences prefs = context.getSharedPreferences( PREFS_NAME, 0 );

		return prefs.getString( key + appWidgetId, def);
	}

	@Override
	public void onCreate( Bundle icicle ) {
		super.onCreate( icicle );

		// Set the result to CANCELED.  This will cause the widget host to cancel
		// out of the widget placement if the user presses the back button.
		setResult( RESULT_CANCELED );

		setContentView( R.layout.psbrates_widget_configure );
		ButterKnife.bind(this);

		mProgressBar.setVisibility( ProgressBar.VISIBLE );

		// Find the widget id from the intent.
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if(extras != null) {
			widgetId = extras.getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID );
		}

		// If this activity was started with an intent without an app widget ID, finish with an error.
		if(widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
			return;
		}

		SharedPreferences prefs = this.getSharedPreferences( PREFS_NAME, 0 );

		{
			final int spinnerPrefPos = prefs.getInt( KEY_PREF + widgetId, 1 );
			spinnerPref.setSelection( spinnerPrefPos );
			Log.d(TAG, "onCreate: spinnerPrefPos=" + spinnerPrefPos);
		}

		final Retrofit retrofit = new Retrofit.Builder()
				.baseUrl( UpdateService.BASE_ADDR ) //Базовая часть адреса
				.addConverterFactory( GsonConverterFactory.create() ) //Конвертер, необходимый для преобразования JSON'а в объекты
				.build();

		final Rates mRates = retrofit.create( Rates.class ); //Создаем объект, при помощи которого будем выполнять запросы

		mRates.getData().enqueue( new Callback<List<CurrencyRate>>() {
			@Override
			public void onResponse( Call<List<CurrencyRate>> call, Response<List<CurrencyRate>> response ) {
				{
					mProgressBar.setVisibility( ProgressBar.INVISIBLE );

					if( response == null ) {
						Log.e(TAG, "onResponse: responce == null");
						return;
					}

					List<CurrencyRate> cur_list = response.body();

					if( cur_list == null ) {
						Log.e(TAG, "onResponse: cur_list == null");
						return;
					}

					List<String> spinnerArray =  new ArrayList<>();

					for( CurrencyRate cur : cur_list ) {
						spinnerArray.add(cur.currency.nameIso + " - " + cur.currency.name);
						currencies_iso.add( cur.currency.nameIso );
					}

					ArrayAdapter<String> adapter = new ArrayAdapter<>(
							PSBRatesWidgetConfigureActivity.this, android.R.layout.simple_spinner_item, spinnerArray);

					adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					Spinner sItems = (Spinner) findViewById(R.id.spinnerPref);
					sItems.setAdapter(adapter);
				}
			}

			@Override
			public void onFailure( Call<List<CurrencyRate>> call, Throwable t ) {
				mProgressBar.setVisibility( ProgressBar.INVISIBLE );

				Log.e( TAG, "onFailure: call=" + call + ", t=" + t );
			}
		} );

		Log.d(TAG, "onCreate: widgetId=" + widgetId );
	}
}

