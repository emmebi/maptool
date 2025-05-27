/*
 * This software Copyright by the RPTools.net development team, and
 * licensed under the Affero GPL Version 3 or, at your option, any later
 * version.
 *
 * MapTool Source Code is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public
 * License * along with this source Code.  If not, please visit
 * <http://www.gnu.org/licenses/> and specifically the Affero license
 * text at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.maptool.client.ui.theme;

import com.formdev.flatlaf.FlatLaf;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.*;
import net.rptools.maptool.client.AppPreferences;
import net.rptools.maptool.client.AppUtil;
import net.rptools.maptool.client.MapTool;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThemeTools {
  private static final Logger log = LogManager.getLogger(ThemeTools.class);
  public static final Properties MODEL_FLAT_PROPERTIES = new Properties();
  public static final List<String> MODEL_FLAT_PROPERTY_NAMES = new ArrayList<>();
  public static final Properties USER_FLAT_PROPERTIES = new Properties();
  public static final List<String> USER_FLAT_PROPERTY_NAMES = new ArrayList<>();

  static {
    if (AppPreferences.useCustomThemeFontProperties.get()) {
      try (InputStream is =
          new FileInputStream(AppUtil.getAppHome("config/FlatLaf.properties").toPath().toFile())) {
        USER_FLAT_PROPERTIES.load(is);
        USER_FLAT_PROPERTY_NAMES.addAll(USER_FLAT_PROPERTIES.stringPropertyNames());
      } catch (IOException ignored) {
      }
      try (InputStream is =
          ThemeTools.class.getResourceAsStream(
              "/net/rptools/maptool/client/ui/themes/modelUserTheme.properties")) {
        MODEL_FLAT_PROPERTIES.load(is);
        MODEL_FLAT_PROPERTY_NAMES.addAll(MODEL_FLAT_PROPERTIES.stringPropertyNames());
      } catch (IOException ignored) {
      }
    }
  }

  private static final int WIN_DEFAULT_FONT_SIZE =
      ((Font) Toolkit.getDefaultToolkit().getDesktopProperty("win.messagebox.font")).getSize();
  public static final Font UNSCALED_BASE_FONT = UIManager.getFont("Label.font");
  public static final float OS_DEFAULT_FONT_SIZE =
      SystemUtils.IS_OS_WINDOWS ? WIN_DEFAULT_FONT_SIZE : SystemUtils.IS_OS_MAC ? 13 : 12;
  public static final Map<String, Float> FLAT_LAF_DEFAULT_FONT_SIZES =
      new HashMap<>() {
        {
          for (String key : MODEL_FLAT_PROPERTY_NAMES) {
            put(key, Float.parseFloat((String) MODEL_FLAT_PROPERTIES.get(key)));
          }
        }
      };
  public static final Set<String> GENERAL_FONT_KEYS = FLAT_LAF_DEFAULT_FONT_SIZES.keySet();
  public static final List<String> FONT_STYLE_FAMILIES =
      new ArrayList<>(List.of("defaultFont", "light.font", "semibold.font", "monospaced.font"));
  private static final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
  public static final List<Font> FONT_LIST =
      Arrays.stream(ge.getAllFonts()).sorted(Comparator.comparing(Font::getName)).toList();
  public static final Map<String, Font> FONT_MAP =
      FONT_LIST.stream().collect(Collectors.toMap(Font::getName, font -> font));

  public static void flatusInterruptus() {
    FlatLaf.registerCustomDefaultsSource(AppUtil.getAppHome("config"));
    if (USER_FLAT_PROPERTY_NAMES.contains("defaultFont")) {
      Font font = parseString(USER_FLAT_PROPERTIES.getProperty("defaultFont"));
      if (font != null) {
        FlatLaf.setPreferredFontFamily(font.getFamily());
      }
    }
    if (USER_FLAT_PROPERTY_NAMES.contains("light.font")) {
      Font font = parseString(USER_FLAT_PROPERTIES.getProperty("light.font"));
      if (font != null) {
        FlatLaf.setPreferredLightFontFamily(font.getFamily());
      }
    }
    if (USER_FLAT_PROPERTY_NAMES.contains("semibold.font")) {
      Font font = parseString(USER_FLAT_PROPERTIES.getProperty("semibold.font"));
      if (font != null) {
        FlatLaf.setPreferredSemiboldFontFamily(font.getFamily());
      }
    }
    if (USER_FLAT_PROPERTY_NAMES.contains("monospaced.font")) {
      Font font = parseString(USER_FLAT_PROPERTIES.getProperty("monospaced.font"));
      if (font != null) {
        FlatLaf.setPreferredMonospacedFontFamily(font.getFamily());
      }
    }
  }

  public static final Function<String, Font> getFontByName =
      name ->
          FONT_LIST.stream()
              .dropWhile(
                  font ->
                      !(font.getName().equalsIgnoreCase(name)
                          || font.getFontName().equalsIgnoreCase(name)
                          || font.getPSName().equalsIgnoreCase(name)))
              .findAny()
              .orElse(
                  FONT_LIST.stream()
                      .dropWhile(font -> !font.getFamily().equalsIgnoreCase(name))
                      .findAny()
                      .orElse(null));

  protected static Font parseString(String s) {
    if (s != null && !s.isBlank()) {
      s = s.replaceAll(" {2,}", " ");
      // split on spaces not contained within quotes
      String[] split = s.split(" (?=(([^\"]*\"){2})*[^\"]*$)");
      // remove th quotes
      for (int i = 0; i < split.length; i++) {
        split[i] = split[i].replaceAll("\"", "");
      }
      if (split.length > 0) {
        if (split.length == 4) {
          return new FontData(split).toFont();
        }
        Font font = null;
        boolean bold = false;
        boolean italic = false;
        float size = Float.NaN;
        String name = null;
        for (String part : split) {
          bold = bold ? bold : part.equalsIgnoreCase("bold");
          italic = italic ? italic : part.equalsIgnoreCase("italic");
          if (Float.isNaN(size)) {
            try {
              size = Float.parseFloat(part);
            } catch (NumberFormatException ignored) {
            }
          }
          if (name == null) {
            font = getFontByName.apply(part);
            if (font != null) {
              name = font.getName();
            }
          }
        }
        if (font != null) {
          font =
              font.deriveFont(
                  (bold ? Font.BOLD : Font.PLAIN) + (italic ? Font.ITALIC : Font.PLAIN),
                  Float.isNaN(size) ? 1f : size);
          return font;
        }
      }
    }
    return null;
  }

  public static boolean writeCustomProperties(Map<String, FontData> properties) {
    for (String key : MODEL_FLAT_PROPERTY_NAMES) {
      if (properties.containsKey(key)) {
        String entry = properties.get(key).toPropertyString(key);
        if (entry.isBlank()) {
          USER_FLAT_PROPERTIES.remove(key);
        } else {
          USER_FLAT_PROPERTIES.put(key, entry);
        }
      } else {
        USER_FLAT_PROPERTIES.remove(key);
      }
    }
    try {
      USER_FLAT_PROPERTIES.store(
          new FileOutputStream(AppUtil.getAppHome("config/FlatLaf.properties").toPath().toFile()),
          "User properties");
      log.info("User font preferences written to config directory.");
      return true;
    } catch (IOException e) {
      log.info("Could not write user font preferences to config directory.");
      MapTool.showError("msg.error.cantSaveTheme", e);
      return false;
    }
  }
}
