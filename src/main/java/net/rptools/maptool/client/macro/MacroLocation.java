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
package net.rptools.maptool.client.macro;

import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.rptools.maptool.model.Token;

// TODO: CDW needs comments
public class MacroLocation {

  @Nonnull private final String name;
  @Nonnull private final MacroSource source;
  @Nonnull private final String location;
  @Nullable private final URI uri;

  private static final MacroLocationFactory factory = MacroLocationFactory.getInstance();

  MacroLocation(
      @Nonnull String name,
      @Nonnull MacroSource source,
      @Nonnull String location,
      @Nullable URI uri) {
    this.name = name;
    this.source = source;
    this.location = location;
    this.uri = uri;
  }

  /** Enumeration to represent the source of the macro. */
  public enum MacroSource {
    chat("chat", false),
    token("token", true),
    gm("gm", true),
    campaign("campaign", true),
    global("global", true),
    library("libToken", true),
    uri("uri", true),
    execFunction("execFunction", false),
    sentryIoLogging("sentryIoLogging", false),
    tooltip("tooltip", true),
    macroLink("macroLink", false),
    event("event", false),
    unknown("unknown", false);

    /**
     * Creates a new {code MacroSource} with the given source name.
     *
     * @param sourceName the source name.
     */
    MacroSource(@Nonnull String sourceName, boolean allowsAtThis) {
      this.sourceName = sourceName;
      this.allowsAtThis = allowsAtThis;
    }

    /** The source name. */
    private final String sourceName;

    private final boolean allowsAtThis;

    /**
     * Returns the source name.
     *
     * @return the source name.
     */
    public String getSourceName() {
      return sourceName;
    }

    /**
     * Returns true if the source allows @this to refer to other macros at the same location.
     *
     * @return true if the source allows @this to refer to other macros at the same location.
     */
    public boolean allowsAtThis() {
      return allowsAtThis;
    }
  }

  public static MacroLocation parseMacroName(
      @Nonnull String qMacroName, @Nullable MacroLocation calledFrom, @Nullable Token token) {
    String qMacroNameLower = qMacroName.toLowerCase();

    if (qMacroNameLower.contains("@campaign")) {
      return new MacroLocation(
          qMacroName.substring(0, qMacroName.indexOf("@")),
          MacroSource.campaign,
          MacroSource.campaign.getSourceName(),
          null);
    }

    if (qMacroNameLower.contains("@gm")) {
      return new MacroLocation(
          qMacroName.substring(0, qMacroName.indexOf("@")),
          MacroSource.gm,
          MacroSource.gm.getSourceName(),
          null);
    }

    if (qMacroNameLower.contains("@global")) {
      return new MacroLocation(
          qMacroName.substring(0, qMacroName.indexOf("@")),
          MacroSource.global,
          MacroSource.global.getSourceName(),
          null);
    }

    if (qMacroNameLower.contains("@token")) {
      if (token == null) {
        return factory.createUnknownLocation(qMacroName);
      }
      return new MacroLocation(
          qMacroName.substring(0, qMacroName.indexOf("@")),
          MacroSource.token,
          token.getName(),
          null);
    }

    if (qMacroNameLower.contains("@lib:")) {
      String libName = qMacroName.substring(qMacroName.indexOf("@") + 1);
      return new MacroLocation(
          qMacroName.substring(0, qMacroName.indexOf("@")), MacroSource.library, libName, null);
    }

    if (qMacroNameLower.contains("@this")) {
      var name = qMacroName.substring(0, qMacroName.indexOf("@"));
      var cfrom = calledFrom;
      if (cfrom == null || cfrom.getSource() == MacroSource.tooltip) { // tooltip is special
        if (token != null) {
          cfrom = factory.createTokenLocation(name, token);
        }
        if (cfrom == null || !cfrom.getSource().allowsAtThis()) {
          return factory.createUnknownLocation(qMacroName);
        }
      }
      return new MacroLocation(name, cfrom.getSource(), cfrom.getLocation(), null);
    }

    // If none of the above then assume it is a URI
    URI uri;
    try {
      uri = new URI(qMacroName);
    } catch (URISyntaxException e) {
      return factory.createUnknownLocation(qMacroName);
    }

    if (uri.getHost() == null) {
      if (calledFrom != null && calledFrom.getSource() == MacroSource.uri) {
        uri = calledFrom.getUri().resolve(uri);
      } else {
        return factory.createUnknownLocation(qMacroName);
      }
    }

    if (uri.getScheme() == null || !uri.getScheme().toLowerCase().equals("lib")) {
      return factory.createUnknownLocation(qMacroName);
    }

    return new MacroLocation(uri.getPath().substring(1), MacroSource.uri, uri.getHost(), uri);
  }

  public String getName() {
    return name;
  }

  public MacroSource getSource() {
    return source;
  }

  public String getLocation() {
    return location;
  }

  public URI getUri() {
    return uri;
  }

  public static MacroLocationFactory getFactory() {
    return factory;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{name='");
    sb.append(name);
    sb.append(", location='");
    sb.append(location);
    sb.append(", source='");
    sb.append(source);
    if (uri != null) {
      sb.append(", uri='");
      sb.append(uri);
    }
    sb.append("'}");

    return sb.toString();
  }
}
