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
package net.rptools.maptool.client.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import net.rptools.maptool.client.AppPreferences;

/**
 * @author trevor
 */
public class StatusPanel extends JPanel {
  private final StatusMarquee statusLabel = new StatusMarquee();

  public StatusPanel() {
    statusLabel.setMinimumSize(new Dimension(0, 0));

    setLayout(new GridBagLayout());

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.BOTH;

    add(wrap(statusLabel), constraints);
    addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(ComponentEvent e) {
            super.componentResized(e);
            repaint();
          }
        });
  }

  public void setStatus(String status) {
    statusLabel.setText(status);
  }

  public void addPanel(JComponent component) {

    int nextPos = getComponentCount();

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.fill = GridBagConstraints.BOTH;

    constraints.gridx = nextPos;

    add(wrap(component), constraints);

    invalidate();
    doLayout();
  }

  private JComponent wrap(JComponent component) {
    component.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    return component;
  }

  private static class StatusMarquee extends JLabel implements ActionListener {
    private static boolean allowScroll = AppPreferences.scrollStatusMessages.get();
    private static final int TICK_INTERVAL = 1000 / AppPreferences.frameRateCap.get();
    private static final int START_DELAY =
        (int) (1000 * AppPreferences.scrollStatusStartDelay.get());
    private static final int END_HOLD = (int) (1000 * AppPreferences.scrollStatusEndPause.get());
    private static float scrollSpeed = AppPreferences.scrollStatusSpeed.get();
    private float scrollPosition = 0;
    private String labelText = ""; // keep a copy as super truncates string excess
    private final Rectangle innerArea =
        new Rectangle(); // region inside border modified to become clip bounds
    private float overflow; // amount that string overflows container
    private FontRenderContext fontRenderContext; // for calculating text size
    private TextLayout textLayout; // for calculating text size
    private float y; // paint vertical position
    private Timer timer;

    StatusMarquee() {
      super();
      // Reverse direction for RTL scripts
      scrollSpeed = this.getComponentOrientation().isLeftToRight() ? -scrollSpeed : scrollSpeed;

      textLayout = null;

      this.addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              super.mouseClicked(e);
              // RMB toggle preference - LMB pause/resume scrolling
              if (SwingUtilities.isRightMouseButton(e)) {
                allowScroll = !allowScroll;
                if (!allowScroll && timer.isRunning()) {
                  timer.stop();
                  scrollPosition = 0;
                } else if (allowScroll && !timer.isRunning()) {
                  sizeCheck();
                }
              } else {
                if (timer.isRunning()) {
                  timer.setActionCommand("pause");
                } else {
                  timer.start();
                }
              }
            }
          });
    }

    private void initTimer() {
      if (timer == null) {
        timer = new Timer(TICK_INTERVAL, this);
        timer.setRepeats(true);
      }
      resetTimer();
    }

    private void resetTimer() {
      timer.setDelay(TICK_INTERVAL);
      timer.setInitialDelay(START_DELAY);
      timer.setActionCommand("start_delay");
    }

    @Override
    public void setText(String text) {
      initTimer();
      scrollPosition = 0;
      labelText = text;
      super.setToolTipText(
          "<html><p width=\""
              + Toolkit.getDefaultToolkit().getScreenSize().width / 3
              + "\">"
              + labelText
              + "</p></html>");
      sizeCheck();
      if (!allowScroll) {
        super.setText(text);
      }
    }

    private void sizeCheck() {
      SwingUtilities.calculateInnerArea(this, innerArea);
      if (innerArea == null || fontRenderContext == null || labelText.isEmpty()) { // avoid NPEs
        return;
      }

      // calculates text bounds
      textLayout = new TextLayout(labelText, getFont(), fontRenderContext);
      // calculate vertical offset to centre text
      y =
          (float)
              (innerArea.getHeight()
                  - (innerArea.getHeight() - textLayout.getBounds().getHeight()) / 2);

      // add margin to text render area
      innerArea.setRect(
          innerArea.x + super.getIconTextGap(),
          innerArea.y,
          innerArea.width - 2 * super.getIconTextGap(),
          innerArea.height);

      overflow = (float) (textLayout.getBounds().getWidth() - innerArea.width);

      markDirty();

      if (overflow > 0) {
        overflow += innerArea.width / 8f;
        timer.start();
      } else {
        timer.stop();
        scrollPosition = 0;
      }
    }

    private void markDirty() {
      RepaintManager.currentManager(this)
          .addDirtyRegion(this, innerArea.x, innerArea.y, innerArea.width, innerArea.height);
    }

    @Override
    protected void paintComponent(Graphics g) {
      if (!allowScroll) {
        super.paintComponent(g);
      } else {
        super.paintBorder(g);
        Graphics2D g2d = (Graphics2D) g;
        if (fontRenderContext == null) {
          fontRenderContext = g2d.getFontRenderContext();
          sizeCheck();
        } else {
          g2d.setClip(innerArea);
          g2d.drawString(labelText, scrollPosition, y);
        }
        g2d.dispose();
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      switch (e.getActionCommand()) {
        case "start_delay" -> timer.setActionCommand("scroll"); // initial delay completed
        case "end_hold" -> { // end hold period completed
          timer.stop();
          resetTimer();
          scrollPosition = 0; // go back to start
          timer.start();
          markDirty();
        }
        case "pause" -> {
          timer.setInitialDelay(0); // start scrolling immediately on resume
          timer.setActionCommand("scroll");
          timer.stop();
        }
        case "scroll" -> {
          scrollPosition += scrollSpeed;
          markDirty();
        }
      }

      if ((scrollSpeed > 0 && scrollPosition > overflow)
          || (scrollSpeed < 0 && scrollPosition < -overflow)) {
        timer.stop();
        timer.setInitialDelay(END_HOLD); // start end hold period
        timer.setActionCommand("end_hold");
        timer.start();
      }
    }
  }
}
