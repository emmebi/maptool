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
package net.rptools.maptool.client.ui.zone.renderer.tokenRender;

import com.google.common.eventbus.Subscribe;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import net.rptools.lib.CodeTimer;
import net.rptools.lib.MD5Key;
import net.rptools.lib.image.ImageUtil;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.ui.zone.renderer.TokenPosition;
import net.rptools.maptool.client.ui.zone.renderer.ZoneRenderer;
import net.rptools.maptool.events.MapToolEventBus;
import net.rptools.maptool.model.*;
import net.rptools.maptool.model.zones.GridChanged;
import net.rptools.maptool.model.zones.TokensChanged;
import net.rptools.maptool.util.ImageManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TokenRenderer {
  private static final Logger log = LogManager.getLogger(TokenRenderer.class);
  private final CodeTimer timer;
  private static final Map<String, Map<Integer, BufferedImage>> imageTableMap =
      Collections.synchronizedMap(new HashMap<>());
  private final Map<Token, BufferedImage> renderImageMap = new HashMap<>();
  private final Map<Token, TokenState> tokenStateMap = new HashMap<>();
  private TokenPosition position;
  private Token token;
  private float opacity = 1f;
  private ZoneRenderer renderer;
  private Grid grid;
  private double scale;
  private boolean isoFigure = false;
  private boolean canSpin = false;
  private BufferedImage renderImage;
  private boolean initialised = false;
  public FacingArrowRenderer facingArrowRenderer;

  public TokenRenderer() {
    facingArrowRenderer = new FacingArrowRenderer();
    new MapToolEventBus().getMainEventBus().register(this);
    timer = CodeTimer.get();
  }

  public boolean notInitialised() {
    return !initialised;
  }

  public void setRenderer(ZoneRenderer zoneRenderer) {
    timer.start("TokenRenderer-init");
    renderer = zoneRenderer;
    Zone zone = renderer.getZone();
    grid = zone.getGrid();

    scale = renderer.getScale();
    initialised = grid != null && renderer != null;
    timer.stop("TokenRenderer-init");
  }

  public void renderToken(Token token, TokenPosition position, Graphics2D g2d, Float opacity) {
    if (!initialised) {
      log.info("TokenRenderer: Not yet initialised.");
      return;
    }
    timer.start("TokenRenderer-renderToken");
    this.token = token;
    this.position = position;
    this.opacity = opacity * token.getTokenOpacity();
    compareStates();
    paintTokenImage(g2d);
    timer.stop("TokenRenderer-renderToken");
  }

  private void compareStates() {
    TokenState currentState = createRecord(token); // for comparing apples to apples
    boolean updateStoredImage = true;
    if (token.getHasImageTable() && !imageTableMap.containsKey(token.getImageTableName())) {
      (new CacheTableImagesWorker(token.getImageTableName())).execute();
    }
    if (tokenStateMap.containsKey(token)) {
      // exists, is it equal
      if (!tokenStateMap.get(token).equals(currentState)) {
        // not equal, replace
        tokenStateMap.put(token, currentState);
      } else {
        // is equal, only update if we have nothing stored.
        updateStoredImage = !renderImageMap.containsKey(token);
      }
    }
    if (updateStoredImage) {
      renderImage = getRenderImage();
      renderImageMap.put(token, renderImage);
    } else {
      renderImage = renderImageMap.get(token);
    }
    // set these whilst we have the information handy
    isoFigure =
        grid.isIsometric()
            && !currentState.flippedIso
            && currentState.shape.equals(Token.TokenShape.FIGURE);
    canSpin = currentState.shape.equals(Token.TokenShape.TOP_DOWN);
  }

  private BufferedImage getRenderImage() {
    BufferedImage bi = ImageManager.BROKEN_IMAGE;
    if (token.getHasImageTable() && imageTableMap.containsKey(token.getImageTableName())) {
      Map<Integer, BufferedImage> imageTable = imageTableMap.get(token.getImageTableName());
      int max = imageTable.keySet().stream().max(Integer::compareTo).orElse(Integer.MAX_VALUE);
      if (max != Integer.MAX_VALUE) {
        int useValue = (360 + token.getFacingInDegrees()) % max;
        bi =
            imageTable.get(
                imageTable.keySet().stream()
                    .sorted()
                    .filter(integer -> integer >= useValue)
                    .toList()
                    .getFirst());
        if (bi != null) {
          bi = ImageUtil.getTokenRenderImage(token, bi, renderer);
        }
      }
    } else {
      bi = ImageUtil.getTokenRenderImage(token, renderer);
    }
    return bi;
  }

  private void paintTokenImage(Graphics2D g2d) {
    timer.start("TokenRenderer-paintTokenImage");
    AffineTransform oldAT = g2d.getTransform();
    Composite oldComposite = g2d.getComposite();
    // centre image
    double imageCx = -renderImage.getWidth() / 2d;
    double imageCy = -renderImage.getHeight() / (isoFigure ? 4d / 3d : 2d);
    AffineTransform imageTransform =
        AffineTransform.getTranslateInstance(
            imageCx + token.getAnchorX() * scale, imageCy + token.getAnchorY() * scale);

    g2d.translate(position.x + position.scaledWidth / 2d, position.y + position.scaledHeight / 2d);

    if (token.hasFacing() && canSpin) {
      g2d.rotate(Math.toRadians(token.getFacingInDegrees()));
    }
    if (opacity < 1.0f) {
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
    }

    g2d.drawImage(renderImage, imageTransform, renderer);
    g2d.setStroke(new BasicStroke(1f));

    g2d.setComposite(oldComposite);
    g2d.setTransform(oldAT);
    timer.stop("TokenRenderer-paintTokenImage");
  }

  public void paintFacingArrow(
      Graphics2D tokenG, Token token, Rectangle footprintBounds, TokenPosition position) {
    if (!facingArrowRenderer.isInitialised()) {
      facingArrowRenderer.setRenderer(renderer);
    }
    facingArrowRenderer.paintArrow(tokenG, token, footprintBounds, position, renderer);
  }

  public void zoomChanged() {
    if (initialised) {
      renderImageMap.clear();
      scale = renderer.getScale();
      facingArrowRenderer.setScale(scale);
    }
  }

  @Subscribe
  private void onGridChanged(GridChanged event) {
    if (initialised && event.zone() != null) {
      renderImageMap.clear();
      this.grid = event.zone().getGrid();
    }
  }

  @Subscribe
  private void onTokensChanged(TokensChanged event) {
    if (initialised) {
      for (Token t : event.tokens()) {
        if (t != null) {
          tokenStateMap.remove(t);
        }
      }
    }
  }

  protected TokenState createRecord(Token token) {
    return new TokenState(
        token.getFacing(),
        token.getScaleX(),
        token.getScaleY(),
        token.getSizeScale(),
        token.getIsFlippedIso(),
        token.isFlippedX(),
        token.isFlippedY(),
        token.getShape(),
        token.isSnapToScale(),
        token.getFootprint(grid));
  }

  protected record TokenState(
      int facing,
      double scaleX,
      double scaleY,
      double sizeScale,
      boolean flippedIso,
      boolean flippedX,
      boolean flippedY,
      Token.TokenShape shape,
      boolean snapToScale,
      TokenFootprint footprint) {}

  private static Map<Integer, BufferedImage> cacheImageTable(String tableName) {
    LookupTable lookupTable = MapTool.getCampaign().getLookupTableMap().get(tableName);
    if (lookupTable != null) {
      BufferedImage broken = ImageManager.BROKEN_IMAGE;
      Map<Integer, BufferedImage> tmp = new HashMap<>();
      List<LookupTable.LookupEntry> entries = lookupTable.getEntryList();
      for (LookupTable.LookupEntry entry : entries) {
        MD5Key asset = entry.getImageId();
        if (asset != null) {
          BufferedImage bi = ImageManager.getImageAndWait(asset);
          if (!bi.equals(broken)) {
            tmp.put(entry.getMax(), bi);
          }
        }
      }
      if (!tmp.isEmpty()) {
        return tmp;
      }
    }
    return null;
  }

  private static class CacheTableImagesWorker
      extends SwingWorker<Map<Integer, BufferedImage>, String> {
    String tableName;

    public CacheTableImagesWorker(String tableName) {
      this.tableName = tableName;
    }

    @Override
    public Map<Integer, BufferedImage> doInBackground() {
      return cacheImageTable(tableName);
    }

    @Override
    protected void done() {
      try {
        imageTableMap.put(tableName, get());
      } catch (Exception ignore) {
      }
    }
  }
}
