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
package net.rptools.maptool.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.model.CategorizedLights;
import net.rptools.maptool.model.GUID;
import net.rptools.maptool.model.Light;
import net.rptools.maptool.model.LightSource;
import net.rptools.maptool.model.Lights;
import net.rptools.maptool.model.ShapeType;
import net.rptools.maptool.model.drawing.DrawableColorPaint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LightSyntax {

  private static final int DEFAULT_LUMENS = 100;
  private static final Logger log = LogManager.getLogger(LightSyntax.class);

  public Lights parseLights(String text, Lights original) {
    final var lightSourceMap = new Lights();
    final var reader = new LineNumberReader(new BufferedReader(new StringReader(text)));
    List<String> errlog = new LinkedList<>();

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();

        var source = parseLightLine(line, reader.getLineNumber(), original, errlog);
        if (source != null) {
          lightSourceMap.add(source);
        }
      }
    } catch (IOException ioe) {
      MapTool.showError("msg.error.mtprops.light.ioexception", ioe);
    }

    if (!errlog.isEmpty()) {
      MapTool.showFeedback(errlog.toArray());
      errlog.clear();
      throw new IllegalArgumentException(
          "msg.error.mtprops.light.definition"); // Don't save lights...
    }

    return lightSourceMap;
  }

  public CategorizedLights parseCategorizedLights(
      String text, CategorizedLights originalLightSourcesMap) {
    final var categorized = new CategorizedLights();
    final var reader = new LineNumberReader(new BufferedReader(new StringReader(text)));
    List<String> errlog = new LinkedList<>();

    try {
      Lights currentGroupOriginalLightSources = new Lights();
      CategorizedLights.Category currentGroup = null;

      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();

        // Blank lines
        if (line.isEmpty()) {
          if (currentGroup != null) {
            categorized.addAllToCategory(currentGroup.name(), currentGroup.lights());
          }
          currentGroup = null;
          continue;
        }
        // New group
        if (currentGroup == null) {
          currentGroup = new CategorizedLights.Category(line, new Lights());
          currentGroupOriginalLightSources =
              originalLightSourcesMap
                  .getCategory(currentGroup.name())
                  .map(CategorizedLights.Category::lights)
                  .orElse(new Lights());
          continue;
        }

        var source =
            parseLightLine(line, reader.getLineNumber(), currentGroupOriginalLightSources, errlog);
        if (source != null) {
          currentGroup.lights().add(source);
        }
      }

      if (currentGroup != null) {
        categorized.addAllToCategory(currentGroup.name(), currentGroup.lights());
      }
    } catch (IOException ioe) {
      MapTool.showError("msg.error.mtprops.light.ioexception", ioe);
    }

    if (!errlog.isEmpty()) {
      MapTool.showFeedback(errlog.toArray());
      errlog.clear();
      throw new IllegalArgumentException(
          "msg.error.mtprops.light.definition"); // Don't save lights...
    }

    return categorized;
  }

  public String stringifyLights(Lights lights) {
    StringBuilder builder = new StringBuilder();
    writeLightLines(builder, lights);
    return builder.toString();
  }

  public String stringifyCategorizedLights(CategorizedLights lightSources) {
    StringBuilder builder = new StringBuilder();
    for (var category : lightSources.getCategories()) {
      builder.append(category.name());
      builder.append("\n----\n");

      writeLightLines(builder, category.lights());
      builder.append('\n');
    }
    return builder.toString();
  }

  private void writeLightLines(StringBuilder builder, Lights lights) {
    for (LightSource lightSource : lights) {
      if (lightSource.getType() != LightSource.Type.NORMAL) {
        log.error("A non-normal light was provided for lightt stringification. Skipping.");
        continue;
      }

      builder.append(lightSource.getName()).append(":");

      if (lightSource.isScaleWithToken()) {
        builder.append(" scale");
      }
      if (lightSource.isIgnoresVBL()) {
        builder.append(" ignores-vbl");
      }

      final var lastParameters = new LinkedHashMap<String, Object>();
      lastParameters.put("", null);
      lastParameters.put("width", 0.);
      lastParameters.put("arc", 0.);
      lastParameters.put("offset", 0.);

      for (Light light : lightSource.getLightList()) {
        final var parameters = new HashMap<>();

        parameters.put("", light.getShape().name().toLowerCase());
        switch (light.getShape()) {
          default:
            throw new RuntimeException(
                "Unrecognized shape: " + light.getShape().toString().toLowerCase());
          case SQUARE, GRID, CIRCLE, HEX:
            break;
          case BEAM:
            parameters.put("width", light.getWidth());
            parameters.put("offset", light.getFacingOffset());
            break;
          case CONE:
            parameters.put("arc", light.getArcAngle());
            parameters.put("offset", light.getFacingOffset());
            break;
        }

        for (final var parameterEntry : lastParameters.entrySet()) {
          final var key = parameterEntry.getKey();
          final var oldValue = parameterEntry.getValue();
          final var newValue = parameters.get(key);

          if (newValue != null && !newValue.equals(oldValue)) {
            lastParameters.put(key, newValue);

            // Special case: booleans are flags that are either present or not.
            if (newValue instanceof Boolean b) {
              if (b) {
                builder.append(" ").append(key);
              }
            } else {
              builder.append(" ");
              if (!"".equals(key)) {
                // Special case: don't include a key= for shapes.
                builder.append(key).append("=");
              }
              builder.append(
                  switch (newValue) {
                    case Double d -> StringUtil.formatDecimal(d);
                    default -> newValue.toString();
                  });
            }
          }
        }

        builder.append(' ').append(StringUtil.formatDecimal(light.getRadius()));
        if (light.getPaint() instanceof DrawableColorPaint) {
          Color color = (Color) light.getPaint().getPaint();
          builder.append(toHex(color));
        }

        final var lumens = light.getLumens();
        if (lumens != DEFAULT_LUMENS) {
          if (lumens >= 0) {
            builder.append('+');
          }
          builder.append(Integer.toString(lumens, 10));
        }
      }
      builder.append('\n');
    }
  }

  private LightSource parseLightLine(
      String line, int lineNumber, Lights originalInCategory, List<String> errlog) {
    // Blank lines, comments
    if (line.isEmpty() || line.charAt(0) == '-') {
      return null;
    }

    // Item
    int split = line.indexOf(':');
    if (split < 1) {
      return null;
    }

    // region Light source properties.
    String name = line.substring(0, split).trim();
    GUID id = new GUID();
    boolean scaleWithToken = false;
    boolean ignoresVBL = false;
    List<Light> lights = new ArrayList<>();
    // endregion
    // region Individual light properties
    ShapeType shape = ShapeType.CIRCLE;
    double width = 0;
    double arc = 0;
    double offset = 0;
    String distance;
    // endregion

    for (String arg : line.substring(split + 1).split("\\s+")) {
      arg = arg.trim();
      if (arg.isEmpty()) {
        continue;
      }
      // Scale with token designation
      if (arg.equalsIgnoreCase("SCALE")) {
        scaleWithToken = true;
        continue;
      }
      // pass through vbl designation
      if (arg.equalsIgnoreCase("IGNORES-VBL")) {
        ignoresVBL = true;
        continue;
      }

      // Shape designation ?
      try {
        shape = ShapeType.valueOf(arg.toUpperCase());
        continue;
      } catch (IllegalArgumentException iae) {
        // Expected when not defining a shape
      }

      // Facing offset designation
      if (arg.toUpperCase().startsWith("OFFSET=")) {
        try {
          offset = Integer.parseInt(arg.substring(7));
          continue;
        } catch (NullPointerException noe) {
          errlog.add(I18N.getText("msg.error.mtprops.light.offset", lineNumber, arg));
        }
      }

      // Parameters
      split = arg.indexOf('=');
      if (split > 0) {
        String key = arg.substring(0, split);
        String value = arg.substring(split + 1);

        if ("arc".equalsIgnoreCase(key)) {
          try {
            arc = StringUtil.parseDecimal(value);
            shape = ShapeType.CONE; // If the user specifies an arc, force the shape to CONE
          } catch (ParseException pe) {
            errlog.add(I18N.getText("msg.error.mtprops.light.arc", lineNumber, value));
          }
        }
        if ("width".equalsIgnoreCase(key)) {
          try {
            width = StringUtil.parseDecimal(value);
            shape = ShapeType.BEAM; // If the user specifies a width, force the shape to BEAM
          } catch (ParseException pe) {
            errlog.add(I18N.getText("msg.error.mtprops.light.width", lineNumber, value));
          }
        }

        continue;
      }

      Color color = null;
      int perRangeLumens = DEFAULT_LUMENS;
      distance = arg;

      final var rangeRegex = Pattern.compile("([^#+-]*)(#[0-9a-fA-F]+)?([+-]\\d*)?");
      final var matcher = rangeRegex.matcher(arg);
      if (matcher.find()) {
        distance = matcher.group(1);
        final var colorString = matcher.group(2);
        final var lumensString = matcher.group(3);
        // Note that Color.decode() _wants_ the leading "#", otherwise it might not treat the
        // value as a hex code.
        if (colorString != null) {
          color = Color.decode(colorString);
        }
        if (lumensString != null) {
          perRangeLumens = Integer.parseInt(lumensString, 10);
          if (perRangeLumens == 0) {
            errlog.add(I18N.getText("msg.error.mtprops.light.zerolumens", lineNumber));
            perRangeLumens = DEFAULT_LUMENS;
          }
        }
      }

      try {
        Light t =
            new Light(
                shape,
                offset,
                StringUtil.parseDecimal(distance),
                width,
                arc,
                color == null ? null : new DrawableColorPaint(color),
                perRangeLumens,
                false,
                false);
        lights.add(t);
      } catch (ParseException pe) {
        errlog.add(I18N.getText("msg.error.mtprops.light.distance", lineNumber, distance));
      }
    }

    // Keep ID the same if modifying existing light. This avoids tokens losing their lights when
    // the light definition is modified.
    for (LightSource ls : originalInCategory) {
      if (ls.getType() == LightSource.Type.NORMAL && name.equalsIgnoreCase(ls.getName())) {
        assert ls.getId() != null;
        id = ls.getId();
        break;
      }
    }

    return LightSource.createRegular(
        name, id, LightSource.Type.NORMAL, scaleWithToken, ignoresVBL, lights);
  }

  private String toHex(Color color) {
    return String.format("#%06x", color.getRGB() & 0x00FFFFFF);
  }
}
