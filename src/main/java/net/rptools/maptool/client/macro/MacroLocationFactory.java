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
import net.rptools.maptool.client.macro.MacroLocation.MacroSource;
import net.rptools.maptool.model.Token;

// TODO: CDW needs comments
public class MacroLocationFactory {

  private MacroLocationFactory() {}

  private static MacroLocationFactory instance = new MacroLocationFactory();

  public static MacroLocationFactory getInstance() {
    return instance;
  }

  public MacroLocation createUnknownLocation(@Nonnull String name) {
    return new MacroLocation(name, MacroSource.unknown, "", null);
  }

  public MacroLocation createGlobalLocation(@Nonnull String name) {
    return new MacroLocation(name, MacroSource.global, MacroSource.global.getSourceName(), null);
  }

  public MacroLocation createCampaignLocation(@Nonnull String name) {
    return new MacroLocation(
        name, MacroSource.campaign, MacroSource.campaign.getSourceName(), null);
  }

  public MacroLocation createTokenLocation(@Nonnull String name, @Nonnull Token token) {
    return createTokenLocation(name, token.getName());
  }

  public MacroLocation createTokenLocation(@Nonnull String name, @Nonnull String tokenName) {
    return new MacroLocation(name, MacroSource.token, tokenName, null);
  }

  public MacroLocation createLibTokenLocation(@Nonnull String name, @Nonnull Token libToken) {
    return createLibTokenLocation(name, libToken.getName());
  }

  public MacroLocation createLibTokenLocation(@Nonnull String name, @Nonnull String libTokenName) {
    return new MacroLocation(name, MacroSource.library, libTokenName.substring(4), null);
  }

  public MacroLocation createGmLocation(@Nonnull String name) {
    return new MacroLocation(name, MacroSource.gm, MacroSource.gm.getSourceName(), null);
  }

  public MacroLocation createExecFunctionLocation(@Nonnull String functionName) {
    return new MacroLocation(
        MacroSource.execFunction.getSourceName(), MacroSource.execFunction, functionName, null);
  }

  public MacroLocation createMacroLinkLocation(@Nonnull String name) {
    return new MacroLocation(
        MacroSource.macroLink.getSourceName(),
        MacroSource.macroLink,
        MacroSource.macroLink.getSourceName(),
        null);
  }

  public MacroLocation createEventLocation(@Nonnull String name) {
    return new MacroLocation(MacroSource.event.getSourceName(), MacroSource.event, name, null);
  }

  public MacroLocation createSentryIoLoggingLocation() {
    return new MacroLocation(
        MacroSource.sentryIoLogging.getSourceName(),
        MacroSource.sentryIoLogging,
        MacroSource.sentryIoLogging.getSourceName(),
        null);
  }

  public MacroLocation createUriLocation(@Nonnull String name, @Nullable URI calledFrom) {
    try {
      var uri = new URI(name);
      if (uri.getScheme() == null) {
        if (calledFrom == null) {
          return createUnknownLocation(name);
        }
        uri = calledFrom.resolve(uri);
      }
      return new MacroLocation(uri.getPath(), MacroSource.uri, uri.getHost(), uri);
    } catch (URISyntaxException e) {
      return createUnknownLocation(name);
    }
  }

  public MacroLocation createChatLocation() {
    return new MacroLocation(
        MacroSource.chat.getSourceName(), MacroSource.chat, MacroSource.chat.getSourceName(), null);
  }

  public MacroLocation createToolTipLocation(@Nullable Token token) {
    return new MacroLocation(
        MacroSource.tooltip.getSourceName(),
        MacroSource.tooltip,
        token != null ? token.getName() : "",
        null);
  }
}
