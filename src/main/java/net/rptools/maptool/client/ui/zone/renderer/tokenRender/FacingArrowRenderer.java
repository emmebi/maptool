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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.rptools.lib.CodeTimer;
import net.rptools.maptool.client.AppPreferences;
import net.rptools.maptool.client.ui.zone.ZoneViewModel;
import net.rptools.maptool.client.ui.zone.renderer.RenderHelper;
import net.rptools.maptool.model.Token;
import net.rptools.maptool.model.Zone;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class FacingArrowRenderer {
  private static final Logger log = LogManager.getLogger(FacingArrowRenderer.class);
  private final CodeTimer timer;
  private final Map<ArrowType, Map<Double, Shape>> quivers = new HashMap<>();

  private final RenderHelper renderHelper;
  private final Zone zone;

  private ArrayList<Color> fillColours = new ArrayList<>();
  private Color fillColour = Color.YELLOW;
  private Color borderColour = Color.DARK_GRAY;

  public FacingArrowRenderer(RenderHelper renderHelper, Zone zone) {
    this.renderHelper = renderHelper;
    this.zone = zone;
    for (int i = 0; i <= 90; i++) {
      fillColours.add(new Color(1 - 0.5f / 90f * i, 1 - 0.5f / 90f * i, 0));
    }
    for (int i = 89; i >= 0; i--) {
      fillColours.add(fillColours.get(i));
    }
    timer = CodeTimer.get();
  }

  public void paintArrow(Graphics2D tokenG, ZoneViewModel.TokenPosition position) {
    renderHelper.render(tokenG, worldG -> paintArrowWorld(worldG, position));
  }

  private void paintArrowWorld(Graphics2D tokenG, ZoneViewModel.TokenPosition position) {
    try {
      var token = position.token();

      var tokenType = token.getShape();
      if (tokenType.equals(Token.TokenShape.TOP_DOWN) && !AppPreferences.forceFacingArrow.get()) {
        return;
      }
      if (tokenType.equals(Token.TokenShape.FIGURE)
          && token.getHasImageTable()
          && !AppPreferences.forceFacingArrow.get()) {
        return;
      }

      final var isIsometric = zone.getGrid().isIsometric();

      timer.start("ArrowRenderer-paintArrow");
      AffineTransform oldAT = tokenG.getTransform();
      double facing = token.getFacing();
      facing = isIsometric ? facing + 45 : facing;
      while (facing < 0) {
        facing += 360;
      }
      if (facing > 360) {
        facing %= 360;
      }
      double radFacing = Math.toRadians(facing);
      double cx = position.footprintBounds().getX() + position.footprintBounds().getWidth() / 2d;
      double cy = position.footprintBounds().getY() + position.footprintBounds().getHeight() / 2d;

      AffineTransform transform = AffineTransform.getRotateInstance(-radFacing);
      if (isIsometric) {
        transform.preConcatenate(AffineTransform.getScaleInstance(1.0, 0.5));
      }
      Shape facingArrow = transform.createTransformedShape(getArrow(position, isIsometric));

      if (tokenType.equals(Token.TokenShape.SQUARE) && !isIsometric) {
        double xp = position.footprintBounds().getWidth() / 2;
        double yp = position.footprintBounds().getHeight() / 2;
        if (facing >= 45 && facing <= 135 || facing >= 225 && facing <= 315) {
          xp = yp / Math.tan(Math.toRadians(facing));
          if (facing > 180) {
            xp = -xp;
            yp = -yp;
          }
        } else {
          yp = xp * Math.tan(Math.toRadians(facing));
          if (facing > 90 && facing < 270) {
            xp = -xp;
            yp = -yp;
          }
        }
        cx += xp;
        cy -= yp;
      }
      tokenG.translate(cx, cy);

      if (tokenType.equals(Token.TokenShape.FIGURE) && facing <= 180) {
        tokenG.setColor(fillColours.get((int) facing));
      } else {
        tokenG.setColor(fillColour);
      }

      tokenG.fill(facingArrow);
      tokenG.setColor(borderColour);
      tokenG.draw(facingArrow);

      tokenG.setTransform(oldAT);
    } catch (Exception e) {
      log.error("Failed to paint facing arrow.");
    }
    timer.stop("ArrowRenderer-paintArrow");
  }

  private Shape getArrow(ZoneViewModel.TokenPosition position, boolean isIsometric) {
    var tokenType = position.token().getShape();
    if ((!AppPreferences.forceFacingArrow.get() && tokenType.equals(Token.TokenShape.TOP_DOWN))
        || (!AppPreferences.forceFacingArrow.get() && position.token().getHasImageTable())) {
      return null;
    }
    Shape arrow = new Path2D.Double();
    final ArrowType arrowType;
    if (isIsometric) {
      arrowType = ArrowType.ISOMETRIC;
    } else {
      switch (tokenType) {
        case FIGURE, CIRCLE -> arrowType = ArrowType.CIRCLE;
        case SQUARE -> arrowType = ArrowType.SQUARE;
        default -> arrowType = ArrowType.NONE;
      }
    }
    double size = position.footprintBounds().getWidth() / 2d;
    Map<Double, Shape> quiver;
    if (quivers.containsKey(arrowType)) {
      quiver = quivers.get(arrowType);
    } else {
      quiver = new HashMap<>();
    }
    if (quiver.containsKey(size)) {
      return quiver.get(size);
    } else {
      switch (arrowType) {
        case CIRCLE -> arrow = getCircleFacingArrow(size);
        case ISOMETRIC -> arrow = getFigureFacingArrow(size);
        case SQUARE -> arrow = getSquareFacingArrow(size);
      }
      quiver.put(size, arrow);
      quivers.put(arrowType, quiver);
    }
    return arrow;
  }

  protected Shape getCircleFacingArrow(double size) {
    double base = size * .75;
    double y = size * .35;
    Path2D facingArrow = new Path2D.Double();
    facingArrow.moveTo(base, -y);
    facingArrow.lineTo(size, 0);
    facingArrow.lineTo(base, y);
    facingArrow.lineTo(base, -y);
    return facingArrow;
  }

  protected Shape getFigureFacingArrow(double size) {
    double base = size * .75;
    double y = size * .35;
    Path2D facingArrow = new Path2D.Double();
    facingArrow.moveTo(base, -y);
    facingArrow.lineTo(size, 0);
    facingArrow.lineTo(base, y);
    facingArrow.lineTo(base, -y);
    return facingArrow;
  }

  protected Shape getSquareFacingArrow(double size) {
    double base = size * .75;
    double y = size * .35;
    Path2D facingArrow = new Path2D.Double();
    facingArrow.moveTo(0, 0);
    facingArrow.lineTo(-(size - base), -y);
    facingArrow.lineTo(-(size - base), y);
    facingArrow.lineTo(0, 0);
    return facingArrow;
  }

  private enum ArrowType {
    CIRCLE,
    ISOMETRIC,
    SQUARE,
    NONE
  }
}
