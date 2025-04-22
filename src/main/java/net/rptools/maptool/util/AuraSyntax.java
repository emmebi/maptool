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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.model.GUID;
import net.rptools.maptool.model.Light;
import net.rptools.maptool.model.LightSource;
import net.rptools.maptool.model.ShapeType;
import net.rptools.maptool.model.drawing.DrawableColorPaint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuraSyntax {

  private static final int DEFAULT_LUMENS = 100;
  private static final Logger log = LogManager.getLogger(AuraSyntax.class);

  public Map<GUID, LightSource> parseAuras(String text, Iterable<LightSource> original) {
    final var aurasMap = new HashMap<GUID, LightSource>();
    final var reader = new LineNumberReader(new BufferedReader(new StringReader(text)));
    List<String> errlog = new LinkedList<>();

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();

        var source = parseAuraLine(line, reader.getLineNumber(), original, errlog);
        if (source != null) {
          aurasMap.put(source.getId(), source);
        }
      }
    } catch (IOException ioe) {
      MapTool.showError("msg.error.mtprops.aura.ioexception", ioe);
    }

    if (!errlog.isEmpty()) {
      MapTool.showFeedback(errlog.toArray());
      errlog.clear();
      throw new IllegalArgumentException(
          "msg.error.mtprops.aura.definition"); // Don't save lights...
    }

    return aurasMap;
  }

  public Map<String, Map<GUID, LightSource>> parseCategorizedAuras(
      String text, final Map<String, Map<GUID, LightSource>> originalAuraMap) {
    final var auraMap = new TreeMap<String, Map<GUID, LightSource>>();
    final var reader = new LineNumberReader(new BufferedReader(new StringReader(text)));
    List<String> errlog = new LinkedList<>();

    try {
      Collection<LightSource> currentGroupOriginalAuras = Collections.emptyList();
      Map<GUID, LightSource> auraGroupMap = null;

      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();

        // Blank lines
        if (line.isEmpty()) {
          auraGroupMap = null;
          continue;
        }
        // New group
        if (auraGroupMap == null) {
          final var currentGroupName = line;
          currentGroupOriginalAuras =
              originalAuraMap.getOrDefault(currentGroupName, Collections.emptyMap()).values();
          auraGroupMap = new HashMap<>();
          auraMap.put(currentGroupName, auraGroupMap);
          continue;
        }

        var source = parseAuraLine(line, reader.getLineNumber(), currentGroupOriginalAuras, errlog);
        if (source != null) {
          auraGroupMap.put(source.getId(), source);
        }
      }
      auraMap.values().removeIf(Map::isEmpty);
    } catch (IOException ioe) {
      MapTool.showError("msg.error.mtprops.aura.ioexception", ioe);
    }

    if (!errlog.isEmpty()) {
      MapTool.showFeedback(errlog.toArray());
      errlog.clear();
      throw new IllegalArgumentException(
          "msg.error.mtprops.aura.definition"); // Don't save auras...
    }

    return auraMap;
  }

  public String stringifyAuras(Iterable<LightSource> auras) {
    StringBuilder builder = new StringBuilder();
    writeAuraLines(builder, auras);
    return builder.toString();
  }

  public String stringifyCategorizedAuras(Map<String, Map<GUID, LightSource>> auras) {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, Map<GUID, LightSource>> entry : auras.entrySet()) {
      builder.append(entry.getKey());
      builder.append("\n----\n");

      writeAuraLines(builder, entry.getValue().values());
      builder.append('\n');
    }
    return builder.toString();
  }

  private void writeAuraLines(StringBuilder builder, Iterable<LightSource> auras) {
    for (LightSource lightSource : auras) {
      builder.append(lightSource.getName()).append(":");

      if (lightSource.getType() != LightSource.Type.AURA) {
        log.error("A non-aura light was provided for aura stringification. Skipping.");
        continue;
      }

      if (lightSource.isScaleWithToken()) {
        builder.append(" scale");
      }

      final var lastParameters = new LinkedHashMap<String, Object>();
      lastParameters.put("", null);
      lastParameters.put("width", 0.);
      lastParameters.put("arc", 0.);
      lastParameters.put("offset", 0.);
      lastParameters.put("GM", false);
      lastParameters.put("OWNER", false);
      lastParameters.put("IGNORES-VBL", false);

      for (Light light : lightSource.getLightList()) {
        final var parameters = new HashMap<>();

        if (lightSource.getType() == LightSource.Type.AURA) {
          parameters.put("GM", light.isGM());
          parameters.put("OWNER", light.isOwnerOnly());
        }
        parameters.put("IGNORES-VBL", lightSource.isIgnoresVBL());

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
      }
      builder.append('\n');
    }
  }

  private LightSource parseAuraLine(
      String line, int lineNumber, Iterable<LightSource> originalInCategory, List<String> errlog) {
    // Blank lines, comments
    if (line.isEmpty() || line.charAt(0) == '-') {
      return null;
    }

    // Item
    int split = line.indexOf(':');
    if (split < 1) {
      return null;
    }

    // region Aura properties.
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
    boolean gmOnly = false;
    boolean ownerOnly = false;
    String distance;
    // endregion

    for (String arg : line.substring(split + 1).split("\\s+")) {
      arg = arg.trim();
      if (arg.isEmpty()) {
        continue;
      }
      if (arg.equalsIgnoreCase("GM")) {
        gmOnly = true;
        ownerOnly = false;
        continue;
      }
      if (arg.equalsIgnoreCase("OWNER")) {
        gmOnly = false;
        ownerOnly = true;
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
          errlog.add(I18N.getText("msg.error.mtprops.aura.offset", lineNumber, arg));
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
            errlog.add(I18N.getText("msg.error.mtprops.aura.arc", lineNumber, value));
          }
        }
        if ("width".equalsIgnoreCase(key)) {
          try {
            width = StringUtil.parseDecimal(value);
            shape = ShapeType.BEAM; // If the user specifies a width, force the shape to BEAM
          } catch (ParseException pe) {
            errlog.add(I18N.getText("msg.error.mtprops.aura.width", lineNumber, value));
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
            errlog.add(I18N.getText("msg.error.mtprops.aura.zerolumens", lineNumber));
            perRangeLumens = DEFAULT_LUMENS;
          }
        }
      }

      ownerOnly = !gmOnly && ownerOnly;
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
                gmOnly,
                ownerOnly);
        lights.add(t);
      } catch (ParseException pe) {
        errlog.add(I18N.getText("msg.error.mtprops.aura.distance", lineNumber, distance));
      }
    }

    // Keep ID the same if modifying existing auras. This avoids tokens losing their auras when
    // the light definition is modified.
    for (LightSource ls : originalInCategory) {
      if (ls.getType() == LightSource.Type.AURA && name.equalsIgnoreCase(ls.getName())) {
        assert ls.getId() != null;
        id = ls.getId();
        break;
      }
    }

    return LightSource.createRegular(
        name, id, LightSource.Type.AURA, scaleWithToken, ignoresVBL, lights);
  }

  private String toHex(Color color) {
    return String.format("#%06x", color.getRGB() & 0x00FFFFFF);
  }
}
