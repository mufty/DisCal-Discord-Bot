package com.cloudcraftgaming.discal.api.utils;

import com.cloudcraftgaming.discal.api.calendar.CalendarAuth;
import com.cloudcraftgaming.discal.api.database.DatabaseManager;
import com.cloudcraftgaming.discal.api.object.GuildSettings;
import com.cloudcraftgaming.discal.api.object.calendar.CalendarData;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.Calendar;

import java.io.IOException;

/**
 * Created by Nova Fox on 11/10/17.
 * Website: www.cloudcraftgaming.com
 * For Project: DisCal-Discord-Bot
 */
public class CalendarUtils {
	/**
	 * Deletes a calendar from Google Calendar and the Db
	 *
	 * @param data The BotData of the Guild whose deleting their calendar.
	 * @return <code>true</code> if successful, else <code>false</code>.
	 */
	public static Boolean deleteCalendar(CalendarData data, GuildSettings settings) {
		try {
			//Only delete if the calendar is stored on DisCal's account.
			if (!data.getCalendarAddress().equalsIgnoreCase("primary") && !settings.useExternalCalendar()) {
				Calendar service = CalendarAuth.getCalendarService();
				service.calendars().delete(data.getCalendarAddress()).execute();
			}
		} catch (IOException e) {
			//Fail silently.
			ExceptionHandler.sendException(null, "Failed to delete calendar", e, CalendarUtils.class);
		}
		if (settings.useExternalCalendar()) {
			//Update settings.
			settings.setUseExternalCalendar(false);
			settings.setEncryptedAccessToken("N/a");
			settings.setEncryptedRefreshToken("N/a");
			DatabaseManager.getManager().updateSettings(settings);
		}

		//Delete everything that is specific to the calendar...
		DatabaseManager.getManager().deleteCalendar(data);
		DatabaseManager.getManager().deleteAllEventData(data.getGuildId());
		DatabaseManager.getManager().deleteAllRSVPData(data.getGuildId());
		DatabaseManager.getManager().deleteAllAnnouncementData(data.getGuildId());

		return true;
	}

	public static boolean calendarExists(CalendarData data, GuildSettings settings) {
		try {
			if (settings.useExternalCalendar()) {
				return CalendarAuth.getCalendarService(settings).calendars().get(data.getCalendarAddress()).execute() != null;
			} else {
				return CalendarAuth.getCalendarService().calendars().get(data.getCalendarAddress()).execute() != null;
			}
		} catch (GoogleJsonResponseException ge) {
			if (ge.getStatusCode() == 410 || ge.getStatusCode() == 404) {
				//Calendar does not exist... remove from db...
				settings.setUseExternalCalendar(false);
				settings.setEncryptedRefreshToken("N/a");
				settings.setEncryptedAccessToken("N/a");
				DatabaseManager.getManager().updateSettings(settings);

				DatabaseManager.getManager().deleteCalendar(data);
				DatabaseManager.getManager().deleteAllEventData(data.getGuildId());
				DatabaseManager.getManager().deleteAllRSVPData(data.getGuildId());
				DatabaseManager.getManager().deleteAllAnnouncementData(data.getGuildId());

				return false;
			} else {
				ExceptionHandler.sendException(null, "Unknown google error when checking for calendar exist", ge, CalendarUtils.class);
				return true;
			}
		} catch (Exception e) {
			ExceptionHandler.sendException(null, "Unknown error when checking for calendar exist", e, CalendarUtils.class);
			return true;
		}
	}
}