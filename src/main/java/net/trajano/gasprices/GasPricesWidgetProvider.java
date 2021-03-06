package net.trajano.gasprices;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.RemoteViews;

/**
 * Widget provider.
 * 
 * @author Archimedes
 * 
 */
public class GasPricesWidgetProvider extends AppWidgetProvider {
	private static final Paint BIG_WIDGET_TEXT;

	private static final Paint SMALL_WIDGET_TEXT;

	static {
		final Typeface rb = Typeface
				.create("Roboto Condensed", Typeface.NORMAL);

		SMALL_WIDGET_TEXT = new Paint();
		SMALL_WIDGET_TEXT.setColor(Color.LTGRAY);
		SMALL_WIDGET_TEXT.setAntiAlias(true);
		SMALL_WIDGET_TEXT.setTextAlign(Align.CENTER);
		SMALL_WIDGET_TEXT.setTypeface(rb);
		SMALL_WIDGET_TEXT.setTextSize(38);

		BIG_WIDGET_TEXT = new Paint();
		BIG_WIDGET_TEXT.setColor(Color.WHITE);
		BIG_WIDGET_TEXT.setAntiAlias(true);
		BIG_WIDGET_TEXT.setTextAlign(Align.CENTER);
		BIG_WIDGET_TEXT.setTextSize(78);
		BIG_WIDGET_TEXT.setTypeface(rb);
	}

	private static Intent getLaunchIntent(final Context context,
			final int appWidgetId) {
		final PackageManager manager = context.getPackageManager();
		final Intent intent = manager
				.getLaunchIntentForPackage("net.trajano.gasprices");
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.setData(new Uri.Builder().path(String.valueOf(appWidgetId))
				.build());
		return intent;

	}

	private static void setBlue(final RemoteViews remoteViews) {
		remoteViews.setInt(R.id.widget_background, "setBackgroundResource",
				R.drawable.default_bg);
	}

	private static void setGreen(final RemoteViews remoteViews) {
		remoteViews.setInt(R.id.widget_background, "setBackgroundResource",
				R.drawable.green_bg);
	}

	private static void setRed(final RemoteViews remoteViews) {
		remoteViews.setInt(R.id.widget_background, "setBackgroundResource",
				R.drawable.red_bg);
	}

	/**
	 * This will update the app widgets provided that an update is not needed.
	 * Because if an update is neded then there isn't a point of changing the UI
	 * until the updates have been completed.
	 * 
	 * @param context
	 * @param appWidgetManager
	 * @param appWidgetId
	 * @param preferences
	 * @param remoteViews
	 */
	public static void updateAppWidget(final Context context,
			final AppWidgetManager appWidgetManager, final int appWidgetId,
			final PreferenceAdaptor preferences, final RemoteViews remoteViews) {

		if (preferences.isUpdateNeeded()) {
			return;
		}

		final CityInfo city = preferences.getWidgetCityInfo(appWidgetId);
		final Bitmap bitmap = Bitmap.createBitmap(400, 180,
				Bitmap.Config.ARGB_8888);

		final Canvas c = new Canvas(bitmap);
		final float xPos = c.getWidth() / 2.0f;
		final float yPos1 = c.getHeight() / 8.0f
				- (SMALL_WIDGET_TEXT.descent() + SMALL_WIDGET_TEXT.ascent())
				/ 2.0f;
		final float yPos = c.getHeight() / 2.0f
				- (BIG_WIDGET_TEXT.descent() + BIG_WIDGET_TEXT.ascent()) / 2.0f;

		final float yPos2 = c.getHeight() * 7.0f / 8.0f
				- (SMALL_WIDGET_TEXT.descent() + SMALL_WIDGET_TEXT.ascent())
				/ 2.0f;

		c.drawText(city.getName(), xPos, yPos1, SMALL_WIDGET_TEXT);

		setBlue(remoteViews);
		if (city.isTomorrowsGasPriceAvailable()) {
			if (city.isTomorrowsGasPriceUp()) {
				setRed(remoteViews);
				c.drawText(
						context.getResources().getString(
								R.string.widget_price_change_up_format,
								city.getPriceDifferenceAbsoluteValue()), xPos,
						yPos2, SMALL_WIDGET_TEXT);

			} else if (city.isTomorrowsGasPriceDown()) {
				setGreen(remoteViews);
				c.drawText(
						context.getResources().getString(
								R.string.widget_price_change_down_format,
								city.getPriceDifferenceAbsoluteValue()), xPos,
						yPos2, SMALL_WIDGET_TEXT);
			} else {
				setBlue(remoteViews);
				c.drawText(
						context.getResources().getString(
								R.string.widget_price_unchanged), xPos, yPos2,
						SMALL_WIDGET_TEXT);
			}
		}

		c.drawText(
				context.getResources().getString(R.string.widget_price_format,
						city.getCurrentGasPrice()), xPos, yPos, BIG_WIDGET_TEXT);

		remoteViews.setImageViewBitmap(R.id.widget_image, bitmap);

		final PendingIntent pendingIntent = PendingIntent.getActivity(context,
				appWidgetId, getLaunchIntent(context, appWidgetId), 0);

		remoteViews.setOnClickPendingIntent(R.id.widget_background,
				pendingIntent);
		appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
	}

	/**
	 * This will remove all the preferences along with all the
	 * {@link PendingIntent} associated with the widget.
	 */
	@Override
	public void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		final PreferenceAdaptor preferences = new PreferenceAdaptor(context);
		final PreferenceAdaptorEditor editor = preferences.edit();
		editor.removeWidgetCityId(appWidgetIds);
		editor.apply();
		for (final int appWidgetId : appWidgetIds) {
			final PendingIntent pendingIntent = PendingIntent.getActivity(
					context, appWidgetId,
					getLaunchIntent(context, appWidgetId),
					PendingIntent.FLAG_NO_CREATE);
			if (pendingIntent != null) {
				pendingIntent.cancel();
			}
		}
	}

	@Override
	public void onEnabled(final Context context) {
		super.onEnabled(context);
		GasPricesUpdateService.scheduleUpdate(context);
	}

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		final RemoteViews remoteViews = new RemoteViews(
				context.getPackageName(), R.layout.widget_layout);

		final PreferenceAdaptor preferences = new PreferenceAdaptor(context);
		for (final int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId,
					preferences, remoteViews);
		}
		if (preferences.isUpdateNeeded()) {
			// Build the intent to call the service
			final Intent intent = GasPricesUpdateService_.intent(
					context.getApplicationContext()).get();
			context.startService(intent);
		}
	}

}
