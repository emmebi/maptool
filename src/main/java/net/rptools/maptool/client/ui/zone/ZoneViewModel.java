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
package net.rptools.maptool.client.ui.zone;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.rptools.lib.CodeTimer;
import net.rptools.lib.image.ImageUtil;
import net.rptools.maptool.client.AppUtil;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.ScreenPoint;
import net.rptools.maptool.client.swing.label.FlatImageLabelFactory;
import net.rptools.maptool.client.ui.zone.renderer.TokenPosition;
import net.rptools.maptool.model.GUID;
import net.rptools.maptool.model.Token;
import net.rptools.maptool.model.Zone;
import net.rptools.maptool.model.player.Player;
import net.rptools.maptool.util.CollectionUtil;
import net.rptools.maptool.util.GraphicsUtil;

/**
 * Data used for rendering.
 *
 * <p>This is deliberately not much encapsulated.
 */
public class ZoneViewModel {
  private final Zone zone;
  // TODO Use array list
  private PlayerView view = new PlayerView(Player.Role.PLAYER);
  private final SelectionModel selectionModel;

  private final List<Token> selectedTokenList = new ArrayList<>();
  private final Map<Zone.Layer, List<TokenPosition>> tokenPositionMap =
      CollectionUtil.newFilledEnumMap(Zone.Layer.class, l -> new LinkedList<>());

  public ZoneViewModel(Zone zone) {
    this.zone = zone;
    this.selectionModel = new SelectionModel(zone);
  }

  public List<TokenPosition> getTokenPositionsForLayer(Zone.Layer layer) {
    return Collections.unmodifiableList(
        tokenPositionMap.getOrDefault(layer, Collections.emptyList()));
  }

  public List<Token> getSelectedTokenList() {
    return Collections.unmodifiableList(selectedTokenList);
  }

  public void update() {
    updateSelectedTokensList();
    updatePlayerView();
    updateTokenPositions();
  }

  private void updateSelectedTokensList() {
    selectedTokenList.clear();

    for (GUID g : selectionModel.getSelectedTokenIds()) {
      final var token = zone.getToken(g);
      if (token != null) {
        selectedTokenList.add(token);
      }
    }
  }

  // What follows are "systems".

  private void updatePlayerView() {
    var selected = true;
    var role = MapTool.getPlayer().getEffectiveRole();

    List<Token> selectedTokens = null;
    if (selected && selectionModel.isAnyTokenSelected()) {
      selectedTokens = new ArrayList<>(selectedTokenList);
      selectedTokens.removeIf(token -> !token.getHasSight() || !AppUtil.playerOwns(token));
    }
    if (selectedTokens == null || selectedTokens.isEmpty()) {
      // if no selected token qualifying for view, use owned tokens or player tokens with sight
      final boolean checkOwnership =
          MapTool.getServerPolicy().isUseIndividualViews() || MapTool.isPersonalServer();
      selectedTokens =
          checkOwnership
              ? zone.getOwnedTokensWithSight(MapTool.getPlayer())
              : zone.getPlayerTokensWithSight();
    }
    if (selectedTokens == null || selectedTokens.isEmpty()) {
      view = new PlayerView(role);
    } else {
      view = new PlayerView(role, selectedTokens);
    }
  }

  private void updateTokenPositions() {
    tokenPositionMap.values().forEach(List::clear);

    final var timer = CodeTimer.get();

    var imageLabelFactory = new FlatImageLabelFactory();

    boolean isGMView = view.isGMView(); // speed things up

    // This is in screen coordinates
    Rectangle viewport = new Rectangle(0, 0, getSize().width, getSize().height);

    Rectangle clipBounds = g.getClipBounds();
    double scale = zoneScale.getScale();
    Set<GUID> tempVisTokens = new HashSet<>();

    // calculations
    boolean calculateStacks =
        !tokenList.isEmpty()
            && tokenList.getFirst().getLayer().isTokenLayer()
            && tokenStackMap == null;
    if (calculateStacks) {
      tokenStackMap = new HashMap<>();
    }

    List<Token> tokenPostProcessing = new ArrayList<>(tokenList.size());
    for (Token token : tokenList) {
      if (token.getShape() != Token.TokenShape.FIGURE && figuresOnly && !token.isAlwaysVisible()) {
        continue;
      }

      timer.start("token-list-1");
      try {
        if (token.getLayer().isStampLayer() && isTokenMoving(token)) {
          continue;
        }
        // Don't bother if it's not visible
        // NOTE: Not going to use zone.isTokenVisible as it is very slow. In fact, it's faster
        // to just draw the tokens and let them be clipped
        if ((!token.isVisible() || !token.getLayer().isVisibleToPlayers()) && !isGMView) {
          continue;
        }
        if (token.isVisibleOnlyToOwner() && !AppUtil.playerOwns(token)) {
          continue;
        }
      } finally {
        // This ensures that the timer is always stopped
        timer.stop("token-list-1");
      }
      timer.start("token-list-1.1");
      TokenPosition position = tokenPositionCache.get(token);
      if (position != null && !position.maybeOnscreen(viewport)) {
        timer.stop("token-list-1.1");
        continue;
      }
      timer.stop("token-list-1.1");

      timer.start("token-list-1a");
      Rectangle footprintBounds = token.getBounds(zone);
      timer.stop("token-list-1a");

      timer.start("token-list-1b");
      // get token image, using image table if present
      BufferedImage image = ImageUtil.getTokenImage(token, this);
      timer.stop("token-list-1b");

      timer.start("token-list-1c");
      double scaledWidth = (footprintBounds.width * scale);
      double scaledHeight = (footprintBounds.height * scale);

      ScreenPoint tokenScreenLocation =
          ScreenPoint.fromZonePoint(this, footprintBounds.x, footprintBounds.y);
      timer.stop("token-list-1c");

      timer.start("token-list-1d");
      // Tokens are centered on the image center point
      double x = tokenScreenLocation.x;
      double y = tokenScreenLocation.y;

      Rectangle2D origBounds = new Rectangle2D.Double(x, y, scaledWidth, scaledHeight);
      Area tokenBounds = new Area(origBounds);
      if (token.hasFacing() && token.getShape() == Token.TokenShape.TOP_DOWN) {
        double sx = scaledWidth / 2 + x - (token.getAnchor().x * scale);
        double sy = scaledHeight / 2 + y - (token.getAnchor().y * scale);
        tokenBounds.transform(
            AffineTransform.getRotateInstance(Math.toRadians(token.getFacingInDegrees()), sx, sy));
        // facing defaults to down or -90 degrees
      }
      timer.stop("token-list-1d");

      timer.start("token-list-1e");
      try {
        position =
            new TokenPosition(
                this,
                tokenBounds,
                origBounds,
                token,
                x,
                y,
                footprintBounds.width,
                footprintBounds.height,
                scaledWidth,
                scaledHeight);
        tokenPositionCache.put(token, position);
        // Too small ?
        if (position.scaledHeight < 1 || position.scaledWidth < 1) {
          continue;
        }
        // Vision visibility
        if (!isGMView && token.getLayer().supportsVision() && zoneView.isUsingVision()) {
          if (!GraphicsUtil.intersects(visibleScreenArea, position.bounds)) {
            continue;
          }
        }
      } finally {
        // This ensures that the timer is always stopped
        timer.stop("token-list-1e");
      }
      // Markers
      timer.start("renderTokens:Markers");
      if (token.isMarker() && canSeeMarker(token)) {
        markerPositionList.add(position);
      }
      timer.stop("renderTokens:Markers");

      // Stacking check
      if (calculateStacks) {
        timer.start("tokenStack");

        Set<Token> tokenStackSet = null;
        for (TokenPosition currPosition : getTokenPositions(Zone.Layer.TOKEN)) {
          // Are we covering anyone ?
          if (position.boundsCache.contains(currPosition.boundsCache)) {
            if (tokenStackSet == null) {
              tokenStackSet = new HashSet<>();
              // Sometimes got NPE here
              if (tokenStackMap == null) {
                tokenStackMap = new HashMap<>();
              }
              tokenStackMap.put(token, tokenStackSet);
              tokenStackSet.add(token);
            }
            tokenStackSet.add(currPosition.token);

            if (tokenStackMap.get(currPosition.token) != null) {
              tokenStackSet.addAll(tokenStackMap.get(currPosition.token));
              tokenStackMap.remove(currPosition.token);
            }
          }
        }
        timer.stop("tokenStack");
      }

      // Keep track of the position on the screen
      // Note the order -- the top most token is at the end of the list
      Zone.Layer layer = token.getLayer();
      List<TokenPosition> locationList = tokenPositionMap.get(layer);
      locationList.add(position);
    }
  }
}
