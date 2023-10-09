/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite;

import com.keenwrite.constants.Constants;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;

import static com.keenwrite.events.StatusEvent.clue;

/**
 * Responsible for loading the bootstrap.properties file, which is
 * tactically located outside the standard resource reverse domain name
 * namespace to avoid hard-coding the application name in many places.
 * Instead, the application name is located in the bootstrap file, which is
 * then used to look up the remaining settings.
 * <p>
 * See {@link Constants#PATH_PROPERTIES_SETTINGS} for details.
 * </p>
 */
public final class Bootstrap {
  private static final String PATH_BOOTSTRAP = "/bootstrap.properties";

  /**
   * Must be populated before deriving the app title (order matters).
   */
  private static final Properties sP = new Properties();

  public static String APP_TITLE;
  public static String APP_TITLE_LOWERCASE;
  public static String APP_VERSION;
  public static String APP_YEAR;

  static {
    try( final var in = openResource( PATH_BOOTSTRAP ) ) {
      sP.load( in );

      APP_TITLE = sP.getProperty( "application.title" );
    } catch( final Exception ex ) {
      APP_TITLE = "KeenWrite";

      // Bootstrap properties cannot be found, use a default value.
      final var fmt = "Unable to load %s resource, applying defaults.%n";
      clue( ex, fmt, PATH_BOOTSTRAP );
    }

    APP_TITLE_LOWERCASE = APP_TITLE.toLowerCase();

    try {
      APP_VERSION = Launcher.getVersion();
    } catch( final Exception ex ) {
      APP_VERSION = "0.0.0";

      // Application version cannot be found, use a default value.
      final var fmt = "Unable to determine application version.";
      clue( ex, fmt );
    }

    APP_YEAR = getYear();

    // This also sets the user agent for the SVG rendering library.
    System.setProperty( "http.agent", APP_TITLE + " " + APP_VERSION );
  }

  @SuppressWarnings( "SameParameterValue" )
  private static InputStream openResource( final String path ) {
    return Constants.class.getResourceAsStream( path );
  }

  private static String getYear() {
    return Integer.toString( Calendar.getInstance().get( Calendar.YEAR ) );
  }
}
