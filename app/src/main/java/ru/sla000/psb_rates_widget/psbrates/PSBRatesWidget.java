package ru.sla000.psb_rates_widget.psbrates;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.Html;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.sla000.psb_rates_widget.psbrates.rest.CurrencyRate;
import ru.sla000.psb_rates_widget.psbrates.rest.Rates;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link PSBRatesWidgetConfigureActivity PSBRatesWidgetConfigureActivity}
 */
public class PSBRatesWidget extends AppWidgetProvider {
	private final static String TAG = "PSBRatesWidget";

	public static void startUpdate(final Context context, final int widgetId) {
		context.startService( UpdateService.getIntent( context, widgetId ));
	}

	@Override
	public void onUpdate( final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds ) {
		super.onUpdate( context, appWidgetManager, appWidgetIds );

		// There may be multiple widgets active, so update all of them
		for(int widgetId : appWidgetIds) {
			startUpdate(context, widgetId);

			final RemoteViews views = new RemoteViews( context.getPackageName(), R.layout.psbrates_widget );
		}
	}

	@Override
	public void onDeleted( final Context context, final int[] appWidgetIds ) {
		// When the user deletes the widget, delete the preference associated with it.
		for(int appWidgetId : appWidgetIds) {
			PSBRatesWidgetConfigureActivity.deletePrefs( context, appWidgetId );
		}
	}

	@Override
	public void onReceive( Context context, Intent intent ) {
		super.onReceive( context, intent );
	}
}

