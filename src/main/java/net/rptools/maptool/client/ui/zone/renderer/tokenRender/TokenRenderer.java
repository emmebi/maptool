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
import java.util.HashMap;
import java.util.Map;
import net.rptools.lib.CodeTimer;
import net.rptools.lib.image.ImageUtil;
import net.rptools.maptool.client.ui.zone.renderer.TokenPosition;
import net.rptools.maptool.client.ui.zone.renderer.ZoneRenderer;
import net.rptools.maptool.events.MapToolEventBus;
import net.rptools.maptool.model.*;
import net.rptools.maptool.model.zones.GridChanged;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TokenRenderer {
  public static final String GRAPHICS = "graphics";
  public static final String POSITION = "position";
  public static final String OPACITY = "opacity";
  public static final String CLIP = "clip";
  private static final Logger log = LogManager.getLogger(TokenRenderer.class);
  private final CodeTimer timer;
  private final Map<Token, BufferedImage> renderImageMap = new HashMap<>();
  private final Map<Token, TokenState> tokenStateMap = new HashMap<>();
  private Graphics2D g2d;
  private TokenPosition position;
  private Token token;
  private float opacity = 1f;
  private Area clip = null;
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

  public boolean isInitialised() {
    return initialised;
  }

  public void setRenderer(ZoneRenderer zoneRenderer) {
    timer.start("TokenRenderer-init");
    renderer = zoneRenderer;
    Zone zone = renderer.getZone();
    grid = zone.getGrid();

    scale = renderer.getScale();
    initialised = true;
    timer.stop("TokenRenderer-init");
  }

  public void renderToken(Token token, Map<String, Object> renderInfo) {
    timer.start("TokenRenderer-renderToken");
    this.token = token;
    if (haveSufficientRenderInfo(renderInfo)) {
      compareStates();
      paintTokenImage();
    } else {
      log.debug("Not enough render info in " + renderInfo);
    }
    timer.stop("TokenRenderer-renderToken");
  }

  private boolean haveSufficientRenderInfo(Map<String, Object> renderInfo) {
    try {
      // has minimum keys required
      if (!(renderInfo.containsKey(GRAPHICS) && renderInfo.containsKey(POSITION))) {
        return false;
      }
      g2d = (Graphics2D) renderInfo.get(GRAPHICS);
      position = (TokenPosition) renderInfo.get(POSITION);
      if (renderInfo.containsKey(OPACITY)) {
        opacity = (float) renderInfo.get(OPACITY);
      } else {
        opacity = 1f;
      }
      if (renderInfo.containsKey(CLIP)) {
        clip = (Area) renderInfo.get(CLIP);
      } else {
        clip = null;
      }
    } catch (ClassCastException cce) {
      log.debug(cce.getLocalizedMessage(), cce);
      return false;
    }
    return true;
  }

  private void compareStates() {
    TokenState currentState = createRecord(token);
    isoFigure =
        grid.isIsometric()
            && !currentState.flippedIso
            && currentState.shape.equals(Token.TokenShape.FIGURE);
    canSpin = currentState.shape.equals(Token.TokenShape.TOP_DOWN);
    boolean updateStoredImage;
    if (!tokenStateMap.containsKey(token)) {
      updateStoredImage = true;
    } else {
      updateStoredImage = !tokenStateMap.get(token).equals(currentState);
    }
    tokenStateMap.put(token, currentState);
    if (updateStoredImage || !renderImageMap.containsKey(token)) {
      renderImage = ImageUtil.getTokenRenderImage(token, renderer);
      renderImageMap.put(token, renderImage);
    } else {
      renderImage = renderImageMap.get(token);
    }
  }

  private void paintTokenImage() {
    timer.start("TokenRenderer-paintTokenImage");
    Shape oldClip = g2d.getClip();
    AffineTransform oldAT = g2d.getTransform();
    Composite oldComposite = g2d.getComposite();
    // centre image
    double imageCx = -renderImage.getWidth() / 2d;
    double imageCy = -renderImage.getHeight() / (isoFigure ? 4d / 3d : 2d);
    AffineTransform imageTransform =
        AffineTransform.getTranslateInstance(
            imageCx + token.getAnchorX() * scale, imageCy + token.getAnchorY() * scale);

    if (clip != null) {
      g2d.setClip(clip);
    }
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
    g2d.setClip(oldClip);
    timer.stop("TokenRenderer-paintTokenImage");
  }

  private TokenState createRecord(Token token) {
    return new TokenState(
        token.getFacing(),
        token.getImageRotation(),
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

  public void zoomChanged() {
    if (initialised) {
      renderImageMap.clear();
      scale = renderer.getScale();
      facingArrowRenderer.setScale(scale);
    }
  }

  @Subscribe
  private void onGridChanged(GridChanged event) {
    if (this.initialised) {
      setRenderer(this.renderer);
      facingArrowRenderer.setRenderer(this.renderer);
    }
  }

  public void paintFacingArrow(
      Graphics2D tokenG, Token token, Rectangle footprintBounds, TokenPosition position) {
    if (!facingArrowRenderer.isInitialised()) {
      facingArrowRenderer.setRenderer(renderer);
    }
    facingArrowRenderer.paintArrow(tokenG, token, footprintBounds, position, renderer);
  }

  private record TokenState(
      int facing,
      double imageRotation,
      double scaleX,
      double scaleY,
      double sizeScale,
      boolean flippedIso,
      boolean flippedX,
      boolean flippedY,
      Token.TokenShape shape,
      boolean snapToScale,
      TokenFootprint footprint) {}
}
