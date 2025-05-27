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

import com.formdev.flatlaf.util.UIScale;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Objects;

public class FontData {
  private static final Font BASE_FONT = ThemeTools.UNSCALED_BASE_FONT;
  private static final float BASE_SIZE = UIScale.unscale(BASE_FONT.getSize());
  private float size = Float.NaN;
  private boolean bold = false;
  private boolean italic = false;
  private String name = null;

  public FontData() {}

  public FontData(float size, boolean bold, boolean italic, String name) {
    this.size = size;
    this.bold = bold;
    this.italic = italic;
    this.name = name;
  }

  public FontData(float size) {
    this.size = size;
  }

  public FontData(Font f) {
    this();
    if (f != null) {
      setSize(Math.min(f.getSize2D(), 1))
          .setBold(f.isBold())
          .setItalic(f.isItalic())
          .setName(f.getName());
    }
  }

  FontData(String[] sArray) {
    this(
        Float.parseFloat(sArray[0]),
        sArray[1].equalsIgnoreCase("bold"),
        sArray[2].equalsIgnoreCase("italic"),
        sArray[3]);
  }

  public boolean isBold() {
    return bold;
  }

  public boolean isItalic() {
    return italic;
  }

  public String getName() {
    return name;
  }

  public float getSize() {
    return size;
  }

  public FontData setBold(boolean bold) {
    this.bold = bold;
    return this;
  }

  public FontData setItalic(boolean italic) {
    this.italic = italic;
    return this;
  }

  public FontData setName(String name) {
    this.name = name;
    return this;
  }

  public FontData setSize(float size) {
    this.size = size;
    return this;
  }

  public Font toFont() {
    Font font =
        ThemeTools.FONT_MAP.get(
            ThemeTools.FONT_MAP.keySet().stream()
                .filter(name -> name.equalsIgnoreCase(name()))
                .findFirst()
                .orElse(null));
    if (font == null) {
      font = ThemeTools.getFontByName.apply(name);
    }
    if (font != null) {
      return font.deriveFont(
          (bold() ? Font.BOLD : Font.PLAIN) + (italic() ? Font.ITALIC : Font.PLAIN),
          UIScale.scale(BASE_SIZE + size));
    }
    return null;
  }

  @Override
  public String toString() {
    DecimalFormat nf = new DecimalFormat("+0;-0");
    return MessageFormat.format(
        "[\"{0}\",\"{1}\",\"{2}\",\"{3}\"]",
        nf.format(size), bold() ? "bold" : "", italic ? "italic" : "", name == null ? "" : name);
  }

  public String toPropertyString(String propertyKey) {
    if (ThemeTools.FONT_STYLE_FAMILIES.contains(propertyKey)) {
      DecimalFormat nf = new DecimalFormat("+0;-0");
      return MessageFormat.format(
          "{0}{1}{2}{3}",
          nf.format(size),
          bold ? " bold" : "",
          italic ? " italic" : "",
          name == null ? "" : " \"" + name + "\"");
    }
    return "";
  }

  public boolean bold() {
    return bold;
  }

  public boolean italic() {
    return italic;
  }

  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (FontData) obj;
    return this.bold == that.bold
        && this.italic == that.italic
        && Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bold, italic, name);
  }
}
