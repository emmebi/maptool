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
package net.rptools.maptool.client.ui.zone.renderer;

import com.github.weisj.jsvg.util.ColorUtil;
import java.awt.*;
import net.rptools.lib.image.ImageUtil;
import net.rptools.maptool.client.AppState;
import net.rptools.maptool.client.ui.zone.PlayerView;
import net.rptools.maptool.events.MapToolEventBus;
import net.rptools.maptool.model.*;

public class GridRenderer {
  private ZoneRenderer renderer;
  private Zone zone;
  private static float gridLineWeight;
  private static float scale;
  private static float baseWidth = 2f;
  private static Color[] gridColours;

  GridRenderer(ZoneRenderer renderer) {
    new MapToolEventBus().getMainEventBus().register(this);
    setRenderer(renderer);
  }

  public void setRenderer(ZoneRenderer renderer) {
    this.renderer = renderer;
    if (renderer.getZone() == null) {
      return;
    }
    this.zone = renderer.getZone();
  }

  private void setGridColours() {
    Color gc = new Color(zone.getGridColor());
    Color contrast = new Color(ImageUtil.negativeColourInt(zone.getGridColor()));
    gridColours =
        new Color[] {
          gc,
          ColorUtil.withAlpha(gc, 0.14f),
          ColorUtil.withAlpha(contrast, 0.04f),
          ColorUtil.withAlpha(contrast, 0.05f)
        };
  }

  public static void drawGridShape(Graphics2D g, Shape shape) {
    if (scale > 0.49f) {
      for (int i = 3; i > -1; i--) {
        g.setColor(gridColours[i]);
        g.setStroke(
            new BasicStroke(
                baseWidth * (i + 1) * 0.5f * gridLineWeight * scale,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_MITER));
        g.draw(shape);
      }
    } else {
      g.setColor(gridColours[0]);
      g.setStroke(
          new BasicStroke(
              Math.clamp(
                  baseWidth * gridLineWeight * scale,
                  baseWidth * gridLineWeight * 0.15f,
                  baseWidth * gridLineWeight * 0.25f),
              BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_MITER));
      g.draw(shape);
    }
  }

  public void renderGrid(Graphics2D g, PlayerView view) {
    if (zone == null
        || !AppState.isShowGrid()
        || zone.getGrid().getSize() * renderer.getScale() < ZoneRendererConstants.MIN_GRID_SIZE) {
      if (renderer != null) {
        setRenderer(renderer);
      }
      return;
    }
    gridLineWeight = AppState.getGridLineWeight();
    scale = (float) renderer.getScale();
    baseWidth = zone.getGrid().getSize() / 50f;
    if (gridColours == null) {
      setGridColours();
    }
    zone.getGrid().draw(renderer, g, g.getClipBounds());
  }

  public void renderCoordinates(Graphics2D g, PlayerView view) {
    if (AppState.isShowCoordinates()) {
      zone.getGrid().drawCoordinatesOverlay(g, this.renderer);
    }
  }
}
