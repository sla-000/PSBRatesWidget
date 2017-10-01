package ru.sla000.psb_rates_widget.psbrates;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.text.Html;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.sla000.psb_rates_widget.psbrates.rest.CurrencyRate;
import ru.sla000.psb_rates_widget.psbrates.rest.Rates;

import static android.util.Log.DEBUG;
import static android.util.Log.VERBOSE;
import static android.util.Log.isLoggable;

/**
 * Created by sla on 01.10.17.
 * <p>
 * Download service
 */

public class UpdateService extends IntentService {
	private final static String TAG = "UpdateService";

	private static final String INTENT_EXTRA_WIDGET_ID = "widgetId";
	public static final String BASE_ADDR = "https://ib.psbank.ru/";
	private static final int DEFAULT_UPDATE_MIN_m = 5;

	private final Set<Integer> updatedIds = new ArraySet<>();

	private long lastDownloadTime;
	private Response<List<CurrencyRate>> response;

	public static Intent getIntent( final Context ctx, final int widgetId ) {
		final Intent intent = new Intent( ctx, UpdateService.class );
		intent.putExtra( INTENT_EXTRA_WIDGET_ID, widgetId );
		return intent;
	}

	public UpdateService() {
		super( "UpdateService" );

		if(isLoggable( TAG, VERBOSE )) Log.v( TAG, "UpdateService" );

	}

	public UpdateService( final String name ) {
		super( name );

		if(isLoggable( TAG, VERBOSE )) Log.v( TAG, "UpdateService: name=" + name );
	}

	@Override
	protected void onHandleIntent( @Nullable final Intent intent ) {
		if(isLoggable( TAG, VERBOSE )) Log.v( TAG, "onHandleIntent: intent=" + intent );

		if(intent == null) {
			if(isLoggable( TAG, DEBUG )) Log.d( TAG, "onHandleIntent: intent == null" );

			return;
		}

		final int widgetId = intent.getExtras().getInt( INTENT_EXTRA_WIDGET_ID, -1 );

		if(widgetId == -1) {
			if(isLoggable( TAG, DEBUG )) Log.d( TAG, "onHandleIntent: widgetId == -1" );

			return;
		}

		final RemoteViews updateViews = execUpdate( this, widgetId );

		if(updateViews == null) {
			if(isLoggable( TAG, DEBUG )) Log.d( TAG, "onHandleIntent: updateViews == null" );

			return;
		}

		final AppWidgetManager manager = AppWidgetManager.getInstance( this );
		manager.updateAppWidget( widgetId, updateViews );

		if(isLoggable( TAG, VERBOSE )) Log.v( TAG, "onHandleIntent: OK" );
	}

	@Nullable
	public RemoteViews execUpdate( @NonNull final Context context, final int widgetId ) {
		if(isLoggable( TAG, VERBOSE )) Log.v( TAG, "execUpdate: widgetId=" + widgetId );

		final RemoteViews views = new RemoteViews( context.getPackageName(), R.layout.psbrates_widget );

		final Resources res = context.getResources();

		final String prefCurrencyIso = PSBRatesWidgetConfigureActivity.loadPref( context, widgetId,
				PSBRatesWidgetConfigureActivity.KEY_PREF, "USD" );

		{
			final String fmt = res.getString( R.string.title );
			final String title = String.format( fmt, prefCurrencyIso, "RUB" );
			views.setTextViewText( R.id.title, Html.fromHtml( title ) );
		}

		Response<List<CurrencyRate>> tempResponse;

		final long timeLeft = System.nanoTime() - lastDownloadTime;

		if(response == null || //
				timeLeft > TimeUnit.MINUTES.toNanos( 10 )) {
			if(isLoggable( TAG, VERBOSE ) && response == null) {
				Log.v( TAG, "execUpdate: response == null" );
			}
			if(isLoggable( TAG, VERBOSE ) && timeLeft > TimeUnit.MINUTES.toNanos( DEFAULT_UPDATE_MIN_m )) {
				Log.v( TAG, "execUpdate: timeLeft=" + timeLeft / 1e9 + "s" );
			}

			final Retrofit retrofit = new Retrofit.Builder()
					.baseUrl( BASE_ADDR ) //Базовая часть адреса
					.addConverterFactory( GsonConverterFactory.create() ) //Конвертер, необходимый для преобразования JSON'а в объекты
					.build();

			final Rates mRates = retrofit.create( Rates.class ); //Создаем объект, при помощи которого будем выполнять запросы

			try {
				if(isLoggable( TAG, VERBOSE )) Log.v( TAG, "execUpdate: getData start" );
				tempResponse = mRates.getData().execute();
				if(isLoggable( TAG, VERBOSE )) Log.v( TAG, "execUpdate: getData end" );

				if(tempResponse == null) {
					Log.e( TAG, "onResponse: tempResponse == null" );

					return null;
				}

				response = tempResponse;
			}
			catch(final IOException e) {
				Log.e( TAG, "execUpdate: exc=" + e.getMessage() );

				if(!updatedIds.contains( widgetId )) {
					if(isLoggable( TAG, DEBUG )) {
						Log.d( TAG, "execUpdate: updatedIds not contain widgetId=" + widgetId );
					}

					final String text = getString( R.string.error );

					views.setTextViewText( R.id.textCurrency, Html.fromHtml( text ) );
				}

				return null;
			}
		}
		else {
			if(isLoggable( TAG, VERBOSE )) Log.v( TAG, "execUpdate: Skip download" );
		}

		if(response == null) {
			Log.e( TAG, "onResponse: response == null" );

			return null;
		}

		List<CurrencyRate> cur_list = response.body();

		if(cur_list == null) {
			Log.e( TAG, "onResponse: cur_list == null" );

			return null;
		}

		CurrencyRate cur_rate = null;

		for(final CurrencyRate cur : cur_list) {
			if(cur.currency.nameIso.equals( prefCurrencyIso )) {
				cur_rate = cur;
				break;
			}
		}

		if(cur_rate == null) {
			Log.e( TAG, "onResponse: cur_rate = null" );

			return null;
		}

		final float centralBankRate = cur_rate.centralBankRate;
		final float sell = cur_rate.sell;
		final float buy = cur_rate.buy;
		final int trend = cur_rate.centralBankRateTrend;
		final int mult = cur_rate.multiplier;

		final String trendStr;

		if(trend == 2) {
			trendStr = "<font color='#b71c1c'>▼</font>";
		}
		else if(trend == 1) {
			trendStr = "<font color='#2E7D32'>▲</font>";
		}
		else {
			trendStr = "";
		}

		final String cbr = String.format( res.getString( R.string.cbr ), //
				centralBankRate, trendStr ); //

		views.setTextViewText( R.id.cbr, Html.fromHtml( cbr ) );

		final String text = String.format( res.getString( R.string.text ), //
				sell, //
				buy ); //

		views.setTextViewText( R.id.textCurrency, Html.fromHtml( text ) );

		final SimpleDateFormat date_fmt = new SimpleDateFormat( "dd.MM.yyyy/HH:mm" );
		views.setTextViewText( R.id.date, date_fmt.format( new Date() ) );

		views.setTextViewText( R.id.mult, mult == 1 ? "" : String.format( "x%d", mult ) );

		updatedIds.add( widgetId );

		lastDownloadTime = System.nanoTime();

		if(isLoggable( TAG, VERBOSE )) Log.v( TAG, "execUpdate: OK" );

		final Intent updateIntent = new Intent( context, PSBRatesWidget.class );
		updateIntent.setAction( AppWidgetManager.ACTION_APPWIDGET_UPDATE );
//		AppWidgetManager man = AppWidgetManager.getInstance(context);
//		final int[] ids = man.getAppWidgetIds(new ComponentName(context, PSBRatesWidget.class)); // all ids
//		updateIntent.putExtra( AppWidgetManager.EXTRA_APPWIDGET_IDS, ids );
		final PendingIntent pIntent = PendingIntent.getBroadcast( context, widgetId, updateIntent, 0 );
		views.setOnClickPendingIntent( R.id.textCurrency, pIntent );

		return views;
	}

	@Nullable
	@Override
	public IBinder onBind( final Intent intent ) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		if(isLoggable( TAG, VERBOSE )) Log.v( TAG, "onCreate" );
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if(isLoggable( TAG, VERBOSE )) Log.v( TAG, "onDestroy" );
	}
}
