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
package net.rptools.maptool.client.ui.token.dialog.edit;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatButton;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import javax.swing.*;
import net.rptools.lib.MD5Key;
import net.rptools.lib.MathUtil;
import net.rptools.lib.image.ImageUtil;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.swing.AbeillePanel;
import net.rptools.maptool.client.swing.GenericDialog;
import net.rptools.maptool.client.swing.SpinnerSliderPaired;
import net.rptools.maptool.client.swing.VerticalLabel;
import net.rptools.maptool.client.ui.theme.Images;
import net.rptools.maptool.client.ui.theme.RessourceManager;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.model.*;
import net.rptools.maptool.util.GraphicsUtil;
import net.rptools.maptool.util.ImageManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Support class that does much of the heavy lifting for TokenLayoutPanel. Links control components
 * and the mouse events to property changes in the reference token. Stores a bunch of useful values.
 * Disables the default OK button click on "Enter" when editing spinner fields.
 *
 * @author 2024 - Reverend/Bubblobill
 */
public class TokenLayoutPanelHelper {
  private static final Logger log = LogManager.getLogger(TokenLayoutPanelHelper.class);

  public TokenLayoutPanelHelper(
      AbeillePanel parentAbeillePanel, TokenLayoutRenderPanel renderPane, AbstractButton okBtn) {
    parent = parentAbeillePanel;
    renderPanel = renderPane;
    renderPanel.setHelper(this);
    setOKButton((JButton) okBtn);
    init();

    parent.addComponentListener(
        new ComponentAdapter() {
          private void doStuff() {
            if (parentRoot == null) {
              setParentRoot(parent.getRootPane());
            }
            iFeelDirty();
          }

          @Override
          public void componentMoved(ComponentEvent e) {
            super.componentMoved(e);
            doStuff();
          }

          @Override
          public void componentShown(ComponentEvent e) {
            super.componentShown(e);
            doStuff();
          }

          @Override
          public void componentResized(ComponentEvent e) {
            super.componentResized(e);
            doStuff();
          }
        });
  }

  public void init() {
    getSizeCombo().addItemListener(sizeListener);
    initButtons();
    initSpinners();
    initSliders();
    pairControls();
  }

  enum FlipState {
    HORIZONTAL,
    ISOMETRIC,
    VERTICAL
  }

  EnumSet<FlipState> flipStates = EnumSet.noneOf(FlipState.class);
  private static final UIDefaults UI_DEFAULTS = UIManager.getDefaults();
  private static final double DEFAULT_FONT_SIZE = UI_DEFAULTS.getFont("defaultFont").getSize2D();
  private static final int ICON_SIZE = (int) (DEFAULT_FONT_SIZE * 2 + 4);

  Grid grid = MapTool.getFrame().getCurrentZoneRenderer().getZone().getGrid();
  RenderBits renderBits = new RenderBits();
  boolean isoGrid = grid.isIsometric();
  boolean noGrid = GridFactory.getGridType(grid).equals(GridFactory.NONE);
  boolean squareGrid = GridFactory.getGridType(grid).equals(GridFactory.SQUARE);
  boolean hexGrid = false;
  boolean isIsoFigure = false;
  int gridSize = grid.getSize();
  double cellHeight = grid.getCellHeight();
  double cellWidth = grid.getCellWidth();
  private static final CellPoint ORIGIN = new CellPoint(0, 0);
  private Token originalToken, mirrorToken;
  private BufferedImage tokenImage;
  TokenFootprint footprint;
  Rectangle2D footprintBounds;
  Set<CellPoint> occupiedCells;
  ArrayList<Point2D> cellCentres;
  /* controls/components */
  AbeillePanel parent;
  private JRootPane parentRoot;
  private final TokenLayoutRenderPanel renderPanel;
  private JComboBox sizeCombo;
  private JLabel scaleLabel;
  private JSpinner anchorXSpinner, anchorYSpinner, rotationSpinner, scaleSpinner, zoomSpinner;
  private JSlider anchorXSlider, anchorYSlider, rotationSlider, scaleSlider, zoomSlider;
  private AbstractButton scaleButton, okButton;

  public void setOKButton(JButton b) {
    okButton = b;
  }

  public void setParentRoot(JRootPane rp) {
    parentRoot = rp;
  }

  private final String helpText = assembleHelpText();

  /* Component Getters */
  public JComboBox getSizeCombo() {
    if (sizeCombo == null) sizeCombo = (JComboBox) parent.getComponent("size");
    return sizeCombo;
  }

  /* Labels */
  public JLabel getScaleLabel() {
    if (scaleLabel == null) scaleLabel = parent.getLabel("scaleLabel");
    return scaleLabel;
  }

  /* Spinners */
  public JSpinner getAnchorXSpinner() {
    if (anchorXSpinner == null) anchorXSpinner = parent.getSpinner("anchorXSpinner");
    return anchorXSpinner;
  }

  public JSpinner getAnchorYSpinner() {
    if (anchorYSpinner == null) anchorYSpinner = parent.getSpinner("anchorYSpinner");
    return anchorYSpinner;
  }

  public JSpinner getRotationSpinner() {
    if (rotationSpinner == null) rotationSpinner = parent.getSpinner("rotationSpinner");
    return rotationSpinner;
  }

  public JSpinner getScaleSpinner() {
    if (scaleSpinner == null) scaleSpinner = parent.getSpinner("scaleSpinner");
    return scaleSpinner;
  }

  public JSpinner getZoomSpinner() {
    if (zoomSpinner == null) zoomSpinner = parent.getSpinner("zoomSpinner");
    return zoomSpinner;
  }

  /* Sliders */
  public JSlider getAnchorXSlider() {
    if (anchorXSlider == null) anchorXSlider = (JSlider) parent.getComponent("anchorXSlider");
    return anchorXSlider;
  }

  public JSlider getAnchorYSlider() {
    if (anchorYSlider == null) anchorYSlider = (JSlider) parent.getComponent("anchorYSlider");
    return anchorYSlider;
  }

  public JSlider getRotationSlider() {
    if (rotationSlider == null) rotationSlider = (JSlider) parent.getComponent("rotationSlider");
    return rotationSlider;
  }

  public JSlider getScaleSlider() {
    if (scaleSlider == null) scaleSlider = (JSlider) parent.getComponent("scaleSlider");
    return scaleSlider;
  }

  public JSlider getZoomSlider() {
    if (zoomSlider == null) zoomSlider = (JSlider) parent.getComponent("zoomSlider");
    return zoomSlider;
  }

  /* Buttons */
  public AbstractButton getScaleButton() {
    if (scaleButton == null) scaleButton = parent.getButton("scaleButton");
    return scaleButton;
  }

  /* Panel */
  public TokenLayoutRenderPanel getRenderPanel() {
    return renderPanel;
  }

  /* Linked controls */
  SpinnerSliderPaired anchorXPair, anchorYPair, rotationPair, scalePair, zoomPair;

  /* Token value getters pointing to the mirror token */
  public double getTokenSizeScale() {
    return mirrorToken.getSizeScale();
  }

  public double getTokenScaleX() {
    return mirrorToken.getScaleX();
  }

  public double getTokenScaleY() {
    return mirrorToken.getScaleY();
  }

  public double getTokenImageRotation() {
    return mirrorToken.getImageRotation();
  }

  public int getTokenAnchorX() {
    return mirrorToken.getAnchorX();
  }

  public int getTokenAnchorY() {
    return mirrorToken.getAnchorY();
  }

  public boolean getTokenFlippedX() {
    return mirrorToken.isFlippedX();
  }

  public boolean getTokenFlippedY() {
    return mirrorToken.isFlippedY();
  }

  public boolean getTokenFlippedIso() {
    return mirrorToken.getIsFlippedIso();
  }

  /* Token value setters pointing to the mirror token */
  protected void setTokenScaleX(double scale) {
    mirrorToken.setScaleX(scale);
  }

  protected void setTokenScaleY(double scale) {
    mirrorToken.setScaleY(scale);
  }

  protected void setTokenSizeScale(double scale) {
    mirrorToken.setSizeScale(scale);
  }

  protected void setTokenAnchorX(Number x) {
    mirrorToken.setAnchorX(x.intValue());
  }

  protected void setTokenAnchorY(Number y) {
    mirrorToken.setAnchorY(y.intValue());
  }

  protected void setTokenImageRotation(Number value) {
    mirrorToken.setImageRotation(MathUtil.doublePrecision(value.doubleValue(), 4));
  }

  protected void setTokenFlipIso(Boolean b) {
    mirrorToken.setIsFlippedIso(b);
    if (flipStates.contains(FlipState.ISOMETRIC) && !b) {
      flipStates.remove(FlipState.ISOMETRIC);
    } else if (!flipStates.contains(FlipState.ISOMETRIC) && b) {
      flipStates.add(FlipState.ISOMETRIC);
    }
    iFeelDirty();
  }

  protected void setTokenFlipX(Boolean b) {
    mirrorToken.setFlippedX(b);
    if (flipStates.contains(FlipState.HORIZONTAL) && !b) {
      flipStates.remove(FlipState.HORIZONTAL);
    } else if (!flipStates.contains(FlipState.HORIZONTAL) && b) {
      flipStates.add(FlipState.HORIZONTAL);
    }
    iFeelDirty();
  }

  protected void setTokenFlipY(Boolean b) {
    mirrorToken.setFlippedY(b);
    if (flipStates.contains(FlipState.VERTICAL) && !b) {
      flipStates.remove(FlipState.VERTICAL);
    } else if (!flipStates.contains(FlipState.VERTICAL) && b) {
      flipStates.add(FlipState.VERTICAL);
    }
    iFeelDirty();
  }

  /**
   * There is one spinner/slider for three different scale settings. This sets the value for the
   * active axis/axes
   *
   * @param n (Number) the scale value being set
   */
  protected void setScaleByAxis(Number n) {
    double value = MathUtil.doublePrecision(n.doubleValue(), 4);
    log.debug("scaleAxis: " + scaleAxis + " -> " + value);
    switch (scaleAxis) {
      case 0 -> setTokenSizeScale(value);
      case 1 -> setTokenScaleX(value);
      case 2 -> setTokenScaleY(value);
    }
  }

  /**
   * There is one spinner/slider for three different scale settings. This returns the value for the
   * active axis/axes
   *
   * @return double value for appropriate scale
   */
  protected Number getScaleByAxis() {
    if (scaleAxis == 0) {
      return getTokenSizeScale();
    } else if (scaleAxis == 1) {
      return getTokenScaleX();
    } else {
      return getTokenScaleY();
    }
  }

  /* used to determine which scale is being edited: 0 is XY, 1 is X, 2 is Y */
  private int scaleAxis = 0;

  /**
   * These functions serve to link the scale and zoom sliders and spinners and allow a useful
   * representation of values < 100%. Slider ranges from -200 to 0 are for values below 100% and 0
   * to 200 for 100% to 300%
   */
  Function<Integer, Number> percentSliderToSpinner =
      i ->
          i <= 0
              ? MathUtil.mapToRange(((Number) i).doubleValue(), -200.0, 0.0, 0.0, 1.0)
              : MathUtil.mapToRange(((Number) i).doubleValue(), 0.0, 200.0, 1.0, 3.0);

  Function<Number, Integer> percentSpinnerToSlider =
      d ->
          (int)
              (d.doubleValue() <= 1
                  ? MathUtil.mapToRange(d.doubleValue(), 0.0, 1.0, -200, 0)
                  : MathUtil.mapToRange(d.doubleValue(), 1.0, 3.0, 0, 200));

  private void storeFlipDirections() {
    if (getTokenFlippedIso()) {
      flipStates.add(FlipState.ISOMETRIC);
    }
    if (getTokenFlippedX()) {
      flipStates.add(FlipState.HORIZONTAL);
    }
    if (getTokenFlippedY()) {
      flipStates.add(FlipState.VERTICAL);
    }
  }

  void resetPanel() {
    setToken(originalToken, false);
    renderPanel.setInitialScale(1d);
    renderPanel.calcZoomFactor();
  }

  void resetPanelToDefault() {
    setToken(originalToken, true);
  }

  /* Listeners */
  PropertyChangeListener controlListener =
      evt -> {
        log.debug("controlListener " + evt);
        if (evt.getPropertyName().toLowerCase().contains("spinnervalue")) {
          iFeelDirty();
        } else if (evt.getPropertyName().toLowerCase().contains("flip")) {
          storeFlipDirections();
        }
      };

  FocusListener focusListener =
      new FocusListener() {
        /* Toggle "Enter" closing the window. */
        @Override
        public void focusGained(FocusEvent e) {
          ((JComponent) e.getComponent()).getRootPane().setDefaultButton(null);
        }

        @Override
        public void focusLost(FocusEvent e) {
          ((JComponent) e.getComponent()).getRootPane().setDefaultButton((JButton) okButton);
        }
      };
  ItemListener sizeListener =
      new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          if (mirrorToken != null) {
            setFootprint((TokenFootprint) getSizeCombo().getSelectedItem());
          }
        }
      };

  /** Mark the rendering panel in need of repainting */
  void iFeelDirty() {
    Rectangle panelBounds = getRenderPanel().getBounds();
    RepaintManager.currentManager(getRenderPanel())
        .addDirtyRegion(
            getRenderPanel(), panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height);
  }

  public void setTokenImageId(MD5Key tokenImageKey) {
    tokenImage = ImageManager.getImage(tokenImageKey);
  }

  protected BufferedImage getTokenImage() {
    return tokenImage;
  }

  public void commitChanges(Token tok) {
    tok.setAnchor(getTokenAnchorX(), getTokenAnchorY());
    tok.setSizeScale(getTokenSizeScale());
    tok.setScaleX(getTokenScaleX());
    tok.setScaleY(getTokenScaleY());
    tok.setImageRotation(getTokenImageRotation());
    tok.setFlippedX(flipStates.contains(FlipState.HORIZONTAL));
    tok.setFlippedY(flipStates.contains(FlipState.VERTICAL));
    tok.setIsFlippedIso(flipStates.contains(FlipState.ISOMETRIC));
  }

  public void setToken(Token token, boolean useDefaults) {
    grid = MapTool.getFrame().getCurrentZoneRenderer().getZone().getGrid();
    isoGrid = grid.isIsometric();
    hexGrid = grid.isHex();
    noGrid = GridFactory.getGridType(grid).equals(GridFactory.NONE);
    squareGrid = GridFactory.getGridType(grid).equals(GridFactory.SQUARE);
    renderBits = new RenderBits();
    gridSize = grid.getSize();
    cellHeight = grid.getCellHeight();
    cellWidth = grid.getCellWidth();

    this.originalToken = new Token(token, true); // duplicate for resetting purposes
    /* The mirror token exists so we can write token changes without committing them prior to clicking OK */
    this.mirrorToken = new Token(token, false);

    if (useDefaults) {
      mirrorToken.setImageRotation(0);
      mirrorToken.setSizeScale(1d);
      mirrorToken.setScaleX(1d);
      mirrorToken.setScaleY(1d);
      mirrorToken.setAnchor(0, 0);
    }

    if (mirrorToken.getFootprint(grid) != getSizeCombo().getSelectedItem()) {
      TokenFootprint tmpFP = (TokenFootprint) getSizeCombo().getSelectedItem();
      if (tmpFP != null) {
        mirrorToken.setFootprint(grid, grid.getFootprint(tmpFP.getId()));
      }
    }

    isIsoFigure =
        isoGrid
            && mirrorToken.getShape() == Token.TokenShape.FIGURE
            && !mirrorToken.getIsFlippedIso();

    tokenImage = ImageManager.getImage(mirrorToken.getImageAssetId());
    setFootprint(mirrorToken.getFootprint(grid));

    getAnchorXSlider().setMinimum((int) -Math.ceil(footprintBounds.getWidth()));
    getAnchorXSlider().setMaximum((int) Math.ceil(footprintBounds.getWidth()));
    if (isIsoFigure) {
      /* Allow more vertical travel for iso figures */
      getAnchorYSlider().setMinimum((int) -Math.ceil(1.4 * footprintBounds.getHeight()));
      getAnchorYSlider().setMaximum((int) Math.ceil(1.4 * footprintBounds.getHeight()));
    } else {
      getAnchorYSlider().setMinimum((int) -Math.ceil(footprintBounds.getHeight()));
      getAnchorYSlider().setMaximum((int) Math.ceil(footprintBounds.getHeight()));
    }
    /* align mouse drag bounds with sliders */
    getRenderPanel().setMaxXoff(getAnchorXSlider().getMaximum());
    getRenderPanel().setMaxYoff(getAnchorYSlider().getMaximum());

    storeFlipDirections();

    /* Assign Suppliers and Consumers to the linked controls and add a PropertyChangeListener */
    anchorXPair.setPropertySetter(this::setTokenAnchorX);
    anchorXPair.setPropertyGetter(this::getTokenAnchorX);
    anchorXPair.setPropertyName("AnchorX");
    anchorXPair.addPropertyChangeListener(controlListener);

    anchorYPair.setPropertySetter(this::setTokenAnchorY);
    anchorYPair.setPropertyGetter(this::getTokenAnchorY);
    anchorYPair.setPropertyName("AnchorY");
    anchorYPair.addPropertyChangeListener(controlListener);

    rotationPair.setPropertySetter(this::setTokenImageRotation);
    rotationPair.setPropertyGetter(this::getTokenImageRotation);
    rotationPair.setPropertyName("Rotation");
    rotationPair.addPropertyChangeListener(controlListener);

    scalePair.setPropertySetter(this::setScaleByAxis);
    scalePair.setPropertyGetter(this::getScaleByAxis);
    scalePair.setPropertyName("Scale");
    scalePair.addPropertyChangeListener(controlListener);

    zoomPair.setPropertySetter(getRenderPanel().getZoomConsumer());
    zoomPair.setPropertyGetter(getRenderPanel().getZoomSupplier());
    zoomPair.setPropertyName("Zoom");
    zoomPair.addPropertyChangeListener(controlListener);

    setControlValues();
    getRenderPanel()
        .addComponentListener(
            new ComponentAdapter() {
              @Override
              public void componentShown(ComponentEvent e) {
                super.componentShown(e);
                renderBits.init();
              }
            });
  }

  private void setFootprint(TokenFootprint fp) {
    this.footprint = fp;
    setCentredFootprintBounds();
    occupiedCells = footprint.getOccupiedCells(ORIGIN);
    Rectangle2D aggregateBounds = new Rectangle2D.Double();
    cellCentres = new ArrayList<>(occupiedCells.size());
    for (CellPoint cp : occupiedCells) {
      cellCentres.add(grid.getCellCenter(cp));
      aggregateBounds.add(grid.getBounds(cp));
    }
    double xFix = -aggregateBounds.getCenterX();
    double yFix = -aggregateBounds.getCenterY();
    cellCentres.replaceAll(pt -> new Point2D.Double(pt.getX() + xFix, pt.getY() + yFix));
  }

  private void setCentredFootprintBounds() {
    if (grid == null) {
      return;
    }
    if (!noGrid) {
      footprintBounds = footprint.getBounds(grid, ORIGIN);
      footprintBounds =
          new Rectangle2D.Double(
              -footprintBounds.getWidth() / 2d,
              -footprintBounds.getHeight() / 2d,
              footprintBounds.getWidth(),
              footprintBounds.getHeight());
    } else {
      double factor = footprint.getScale();
      footprintBounds =
          new Rectangle2D.Double(
              -gridSize / 2d * factor,
              -gridSize / 2d * factor,
              gridSize * factor,
              gridSize * factor);
    }
  }

  /** Link the spinners to the sliders and join them in matrimonial bliss */
  private void pairControls() {
    anchorXPair = new SpinnerSliderPaired(getAnchorXSpinner(), getAnchorXSlider());
    anchorYPair = new SpinnerSliderPaired(getAnchorYSpinner(), getAnchorYSlider());

    rotationPair = new SpinnerSliderPaired(getRotationSpinner(), getRotationSlider());
    rotationPair.setSpinnerWraps(true);

    scalePair =
        new SpinnerSliderPaired(
            getScaleSpinner(),
            getScaleSlider(),
            null,
            percentSpinnerToSlider,
            percentSliderToSpinner);
    scalePair.addVetoableChangeListener(
        evt -> {
          if (evt.getPropertyName().toLowerCase().contains("value")
              && evt.getNewValue().getClass().isAssignableFrom(Double.class)
              && ((Number) evt.getNewValue()).doubleValue() < 0.1) {
            throw new PropertyVetoException("Minimum scale value reached", evt);
          }
        });
    zoomPair =
        new SpinnerSliderPaired(
            getZoomSpinner(),
            getZoomSlider(),
            null,
            percentSpinnerToSlider,
            percentSliderToSpinner);
    zoomPair.addVetoableChangeListener(
        evt -> {
          if (evt.getPropertyName().toLowerCase().contains("value")
              && evt.getNewValue().getClass().isAssignableFrom(Double.class)
              && ((Number) evt.getNewValue()).doubleValue() < 0.34) {
            throw new PropertyVetoException("Minimum zoom value reached", evt);
          }
        });
    anchorXPair.addPropertyChangeListener(evt -> iFeelDirty());
    anchorYPair.addPropertyChangeListener(evt -> iFeelDirty());
    rotationPair.addPropertyChangeListener(evt -> iFeelDirty());
    scalePair.addPropertyChangeListener(evt -> iFeelDirty());
    zoomPair.addPropertyChangeListener(evt -> iFeelDirty());
  }

  public void initSpinners() {
    /* models */
    getAnchorXSpinner().setModel(new SpinnerNumberModel(0d, -200d, 200d, 1d));
    getAnchorYSpinner().setModel(new SpinnerNumberModel(0d, -200d, 200d, 1d));
    getRotationSpinner().setModel(new SpinnerNumberModel(0d, 0d, 360d, 1d));
    getScaleSpinner().setModel(new SpinnerNumberModel(1d, 0d, 3d, 0.1));
    getZoomSpinner().setModel(new SpinnerNumberModel(1d, 0d, 3d, 0.1));
    getZoomSpinner().setVisible(false);
    /* editors */
    getAnchorXSpinner().setEditor(new JSpinner.NumberEditor(anchorXSpinner, "0"));
    getAnchorYSpinner().setEditor(new JSpinner.NumberEditor(anchorYSpinner, "0"));
    getRotationSpinner().setEditor(new JSpinner.NumberEditor(rotationSpinner, "0.0"));
    getScaleSpinner().setEditor(new JSpinner.NumberEditor(scaleSpinner, "0%"));

    ((JSpinner.NumberEditor) getAnchorXSpinner().getEditor()).getTextField().setColumns(3);
    ((JSpinner.NumberEditor) getAnchorYSpinner().getEditor()).getTextField().setColumns(3);
    ((JSpinner.NumberEditor) getRotationSpinner().getEditor()).getTextField().setColumns(3);
    ((JSpinner.NumberEditor) getScaleSpinner().getEditor()).getTextField().setColumns(4);

    /* listeners */
    ((JSpinner.NumberEditor) getAnchorXSpinner().getEditor())
        .getTextField()
        .addFocusListener(focusListener);
    ((JSpinner.NumberEditor) getAnchorYSpinner().getEditor())
        .getTextField()
        .addFocusListener(focusListener);
    ((JSpinner.NumberEditor) getRotationSpinner().getEditor())
        .getTextField()
        .addFocusListener(focusListener);
    ((JSpinner.NumberEditor) getScaleSpinner().getEditor())
        .getTextField()
        .addFocusListener(focusListener);
  }

  public void initButtons() {
    /* icons */
    createButtonIcons();

    scaleButton = new FlatButton();
    parent.replaceComponent("scalePanel", "scaleButton", scaleButton);
    getScaleButton().addActionListener(new ScaleButtonListener());
    getScaleButton().setIcon(scaleIcons[scaleAxis]);
    getScaleLabel().setText(I18N.getString("sightLight.optionLabel.scale") + "   ");

    FlatButton layoutHelpButton = new FlatButton();
    layoutHelpButton.setButtonType(FlatButton.ButtonType.help);
    parent.replaceComponent("layoutTabPanel", "layoutHelpButton", layoutHelpButton);
    layoutHelpButton.setToolTipText(helpText);
    layoutHelpButton.addActionListener(e -> showHelp());
    layoutHelpButton.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseExited(MouseEvent e) {
            super.mouseExited(e);
            iFeelDirty();
          }
        });
  }

  private String assembleHelpText() {
    String rowText = "<tr><th align='right'>%s<td>%s";
    String caption = "<html><table><caption color='white' bgcolor='navy'><b>%s</b></caption>";
    return caption.formatted(I18N.getString("EditTokenDialog.layout.help.caption"))
        + rowText.formatted(
            I18N.getString("Mouse.leftDrag"),
            I18N.getString("EditTokenDialog.layout.help.moveImage"))
        + rowText.formatted(
            I18N.getString("Mouse.rightDrag"),
            I18N.getString("EditTokenDialog.layout.help.moveView"))
        + rowText.formatted(
            I18N.getString("Mouse.leftDoubleClick"),
            I18N.getString("EditTokenDialog.layout.help.reset"))
        + rowText.formatted(
            I18N.getString("Mouse.rightDoubleClick"),
            I18N.getString("EditTokenDialog.layout.help.resetDefaults"))
        + rowText.formatted(
            I18N.getString("Mouse.wheel"), I18N.getString("EditTokenDialog.layout.help.scaleImage"))
        + rowText.formatted(
            I18N.getString("Mouse.shiftWheel"),
            I18N.getString("EditTokenDialog.layout.help.rotateImage"))
        + rowText.formatted(
            I18N.getString("Mouse.ctrlWheel"),
            I18N.getString("EditTokenDialog.layout.help.zoomView"));
  }

  private void showHelp() {
    JPanel helpPanel = new JPanel(new BorderLayout());
    GenericDialog gd = new GenericDialog("Layout Controls", MapTool.getFrame(), helpPanel, true);
    gd.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    JButton okayButton = new JButton();
    okayButton.setText(I18N.getString("Button.ok"));

    JLabel helpTextContainer = new JLabel();
    helpTextContainer.setText(helpText);
    helpPanel.add(new JScrollPane(helpTextContainer), BorderLayout.NORTH);
    helpPanel.add(okayButton, BorderLayout.SOUTH);
    okayButton.addActionListener(
        e -> {
          gd.closeDialog();
          iFeelDirty();
        });
    gd.showDialog();
  }

  public void initSliders() {
    getAnchorXSlider().setModel(new DefaultBoundedRangeModel(0, gridSize, -gridSize, gridSize));
    getAnchorYSlider().setModel(new DefaultBoundedRangeModel(0, gridSize, -gridSize, gridSize));
    getScaleSlider().setModel(new DefaultBoundedRangeModel(0, 0, -200, 200));
    getZoomSlider().setModel(new DefaultBoundedRangeModel(0, 0, -200, 200));
    getRotationSlider().setModel(new DefaultBoundedRangeModel(0, 360, 0, 360));

    class VertLabel extends VerticalLabel {
      public double divisor = 1;

      public VertLabel(String text) {
        super(text);
      }

      public void setDivisor(double divisor) {
        this.divisor = divisor;
      }

      protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (isRotated())
          g2d.rotate(
              Math.toRadians(-90 * getRotation() / divisor),
              getPreferredSize().getWidth(),
              getPreferredSize().getHeight());

        super.paintComponent(g2d);
        g2d.dispose();
      }
    }
    Dictionary<Integer, JLabel> offSetYLabels = new Hashtable<>();
    Dictionary<Integer, JLabel> offSetXLabels = new Hashtable<>();
    for (int i = -1200; i <= 1200; i += 50) {
      JLabel label1 = new JLabel(String.valueOf(i));
      offSetYLabels.put(i, label1);
      VertLabel label2 = new VertLabel(String.valueOf(i));
      label2.setRotation(VerticalLabel.ROTATE_RIGHT);
      label2.setDivisor(3d);
      offSetXLabels.put(i, label2);
    }
    getAnchorYSlider().setLabelTable(offSetYLabels);
    getAnchorXSlider().setLabelTable(offSetXLabels);

    DecimalFormat df = new DecimalFormat("##0%");
    Dictionary<Integer, JLabel> pctLabels = new Hashtable<>();
    pctLabels.put(-200, new JLabel(df.format(0)));
    pctLabels.put(-100, new JLabel(df.format(0.5)));
    pctLabels.put(0, new JLabel(df.format(1)));
    pctLabels.put(100, new JLabel(df.format(2)));
    pctLabels.put(200, new JLabel(df.format(3)));

    getScaleSlider().setLabelTable(pctLabels);
    getZoomSlider().setLabelTable(pctLabels);

    setupSlider(getAnchorXSlider(), 50, 25);
    setupSlider(getAnchorYSlider(), 50, 25);
    setupSlider(getRotationSlider(), 60, 12);
    setupSlider(getScaleSlider(), 100, 20);
    setupSlider(getZoomSlider(), 100, 20);
  }

  private void setupSlider(JSlider slider, int majorTick, int minorTick) {
    slider.setMajorTickSpacing(majorTick);
    slider.setMinorTickSpacing(minorTick);
    slider.setPaintTicks(true);
    slider.setPaintLabels(true);
  }

  /** Set controls to match token values */
  private void setControlValues() {
    getAnchorXSpinner().setValue(getTokenAnchorX());
    getAnchorYSpinner().setValue(getTokenAnchorY());
    scaleAxis = 0;
    getScaleButton().setIcon(scaleIcons[scaleAxis]);
    getScaleSpinner().setValue(getTokenSizeScale());
    getRotationSpinner().setValue(getTokenImageRotation());
    getZoomSpinner().setValue(1d);
  }

  ImageIcon[] scaleIcons = new ImageIcon[3];

  private void createButtonIcons() {
    String iconBase = "net/rptools/maptool/client/image/";
    scaleIcons[0] = new FlatSVGIcon(iconBase + "scale.svg", ICON_SIZE, ICON_SIZE);
    scaleIcons[1] = new FlatSVGIcon(iconBase + "scaleHor.svg", ICON_SIZE, ICON_SIZE);
    scaleIcons[2] = new FlatSVGIcon(iconBase + "scaleVert.svg", ICON_SIZE, ICON_SIZE);
  }

  private class ScaleButtonListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      scaleAxis = scaleAxis == 2 ? 0 : scaleAxis + 1;
      getScaleButton().setIcon(scaleIcons[scaleAxis]);
      switch (scaleAxis) {
        case 0 -> {
          getScaleSlider().setValue(percentSpinnerToSlider.apply(getTokenSizeScale()));
          getScaleLabel().setText(I18N.getString("sightLight.optionLabel.scale") + "  ");
        }
        case 1 -> {
          getScaleSlider().setValue(percentSpinnerToSlider.apply(getTokenScaleX()));
          getScaleLabel().setText(I18N.getString("sightLight.optionLabel.scale") + "-X");
        }
        case 2 -> {
          getScaleSlider().setValue(percentSpinnerToSlider.apply(getTokenScaleY()));
          getScaleLabel().setText(I18N.getString("sightLight.optionLabel.scale") + "-Y");
        }
      }
    }
  }

  /**
   * Just a convenient place to hold a bunch of stuff that is not token related but purely for the
   * rendering panel
   */
  class RenderBits {
    RenderBits() {
      if (FlatLaf.isLafDark()) {
        panelTexture = ImageUtil.negativeImage(panelTexture);
        for (int i = 0; i < colours.length; i++) {
          colours[i] = new Color(ImageUtil.negativeColourInt(colours[i].getRGB()));
        }
      }

      backgroundTexture =
          new TexturePaint(
              panelTexture, new Rectangle(0, 0, panelTexture.getWidth(), panelTexture.getHeight()));
      setStrokeArrays();
      gridShapeFill = createGridShape(false);
      gridShapeOutline = createGridShape(true);
    }

    void init() {
      Dimension size = getRenderPanel().getSize();
      viewBounds = getRenderPanel().getVisibleRect();
      viewOffset = getRenderPanel().getViewOffset();
      centrePoint =
          new Point2D.Double(
              size.width / 2d + viewOffset.getX(), size.height / 2d + viewOffset.getY());

      if (zoomFactor != getRenderPanel().getZoomFactor()) {
        zoomFactor = getRenderPanel().getZoomFactor();
        constrainedZoom = (float) Math.clamp(zoomFactor, 1, 1.6d);
        setStrokeArrays();
      }

      if (tokenImage == null) {
        /* just to avoid Div/0 if called before image loaded */
        tokenImage =
            new BufferedImage(
                (int) footprintBounds.getWidth(),
                (int) footprintBounds.getHeight(),
                BufferedImage.TYPE_4BYTE_ABGR_PRE);
      }
      iso_figure_ho = ImageUtil.getIsoFigureHeightOffset(mirrorToken, footprintBounds);

      workImage = ImageUtil.getScaledTokenImage(tokenImage, mirrorToken, grid, zoomFactor);
      workImage = getFlippedImage(workImage);
      workImage = ImageUtil.rotateImage(workImage, mirrorToken.getImageRotation());
    }

    private void setStrokeArrays() {
      if (solidStrokes == null) {
        solidStrokes = new BasicStroke[strokeModels.length];
        dashedStrokes = new BasicStroke[strokeModels.length];
      }
      float thickest = strokeModels[0].getLineWidth();
      float thinnest = strokeModels[3].getLineWidth();
      boolean mapValues = constrainedZoom != 1f;
      for (int i = 0; i < strokeModels.length; i++) {
        BasicStroke model = strokeModels[i];
        float useWidth;
        if (mapValues) {
          useWidth =
              (float)
                  MathUtil.mapToRange(
                      model.getLineWidth(),
                      thinnest,
                      thickest,
                      thinnest * constrainedZoom,
                      thickest * constrainedZoom);
          solidStrokes[i] = new BasicStroke(useWidth, model.getEndCap(), model.getLineJoin());
          dashedStrokes[i] =
              new BasicStroke(
                  useWidth,
                  model.getEndCap(),
                  model.getLineJoin(),
                  model.getMiterLimit(),
                  model.getDashArray(),
                  model.getDashPhase());
        }
      }
    }

    static final RenderingHints RENDERING_HINTS = ImageUtil.getRenderingHintsQuality();
    static final float LINE_SIZE = (float) (DEFAULT_FONT_SIZE / 12f);
    Rectangle2D viewBounds;
    Point2D viewOffset, centrePoint;
    double zoomFactor = 1, iso_figure_ho = 0;
    float constrainedZoom = 1;
    private static BufferedImage panelTexture = RessourceManager.getImage(Images.TEXTURE_PANEL);
    BufferedImage workImage;
    static TexturePaint backgroundTexture;
    Shape centreMark, gridShapeFill, gridShapeOutline;
    BasicStroke[] solidStrokes, dashedStrokes;
    BasicStroke[] strokeModels =
        new BasicStroke[] {
          new BasicStroke(
              LINE_SIZE * 2.35f,
              BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND,
              1f,
              new float[] {4f, 6f},
              2f),
          new BasicStroke(
              LINE_SIZE * 1.63f,
              BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND,
              1f,
              new float[] {3.5f, 6.5f},
              1.5f),
          new BasicStroke(
              LINE_SIZE * 1.45f,
              BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND,
              1f,
              new float[] {2f, 8f},
              1.5f),
          new BasicStroke(
              LINE_SIZE,
              BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND,
              1f,
              new float[] {2.5f, 7.5f},
              1.5f)
        };
    public Color[] colours =
        new Color[] {Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK, Color.DARK_GRAY};

    /**
     * Returns an image flipped according to the token's flip properties
     *
     * @param bi Image to flip
     * @return Flipped bufferedImage
     */
    protected BufferedImage getFlippedImage(BufferedImage bi) {
      log.debug("getFlippedImage - flipStates: " + flipStates);
      int direction =
          (flipStates.contains(FlipState.HORIZONTAL) ? 1 : 0)
              + (flipStates.contains(FlipState.VERTICAL) ? 2 : 0);
      if (direction != 0) {
        bi = ImageUtil.flipCartesian(bi, direction);
      }
      if (flipStates.contains(FlipState.ISOMETRIC)) {
        bi = ImageUtil.flipIsometric(bi, true);
      }
      return bi;
    }

    private Shape createGridShape(boolean trueSize) {
      return GraphicsUtil.createGridShape(
          GridFactory.getGridType(grid), (trueSize ? grid.getSize() : grid.getSize() - 8));
    }

    /** Itty bitty cross to show the dead-centre of the footprint */
    void createCentreMark() {
      double aperture = Math.max(cellHeight, cellWidth) / 7.0;
      double r = aperture / 4.0;
      Path2D path = new Path2D.Double();
      path.moveTo(-r, -r);
      path.lineTo(r, r);
      path.moveTo(-r, r);
      path.lineTo(r, -r);
      centreMark = path;
    }

    protected void paintCentreMark(Graphics g) {
      if (centreMark == null) {
        createCentreMark();
      }
      Graphics2D g2d = (Graphics2D) g;
      g2d.translate(centrePoint.getX(), centrePoint.getY());
      g2d.scale(constrainedZoom, constrainedZoom);
      g2d.setStroke(
          new BasicStroke(
              constrainedZoom * 2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 10f));
      g2d.setColor(colours[4]);
      Composite oldAc = g2d.getComposite();
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacities[0]));
      g2d.draw(centreMark);
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacities[2]));
      g2d.setStroke(
          new BasicStroke(
              constrainedZoom * 1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 10f));
      g2d.setColor(colours[3]);
      g2d.draw(centreMark);
      g2d.setComposite(oldAc);
      renderBits.paintShapeOutLine(g2d, centreMark, true, true);
      g2d.dispose();
    }

    /**
     * Used for gridless maps, paints concentric rings with an interval of the grid size. Paints a
     * ring of a different colour at the radius associated with the footprint scale. Also paints
     * some radial lines to assist with alignment
     *
     * @param g graphics object
     * @param zoomFactor zoom level applied to view
     */
    void paintRings(Graphics g, double zoomFactor) {
      Graphics2D g2d = (Graphics2D) g.create();
      /* used with no grid. A set of rings and radial lines. */
      TokenFootprint fp = mirrorToken.getFootprint(grid);
      Rectangle2D fpCellBounds = fp.getBounds(grid, ORIGIN);
      fpCellBounds =
          new Rectangle2D.Double(
              0,
              0,
              fpCellBounds.getWidth() * footprint.getScale(),
              fpCellBounds.getHeight() * footprint.getScale());

      double cx = viewBounds.getCenterX() + viewOffset.getX();
      double cy = viewBounds.getCenterY() + viewOffset.getY();

      double gap = grid.getSize() * zoomFactor;
      double maxRadius = Math.hypot(this.viewBounds.getWidth(), this.viewBounds.getHeight());
      double currentRadius = gap / 2d;
      double tokenRadius = fpCellBounds.getCenterX() * zoomFactor;
      Line2D lineLong = new Line2D.Double(cx + currentRadius / 2d, cy, maxRadius, cy);
      Line2D lineShort = new Line2D.Double(cx + gap * 2d, cy, maxRadius, cy);

      /* draw radial lines */
      for (double i = 0; i < 24; i++) {
        if (i % 6 == 0) {
          continue; /* skip cardinal lines */
        }
        paintShapeOutLine(
            g2d,
            AffineTransform.getRotateInstance(Math.TAU / 24 * i, cx, cy)
                .createTransformedShape(i % 2 == 1 ? lineShort : lineLong),
            false,
            true);
      }

      /* draw rings */
      while (currentRadius < maxRadius) {
        Ellipse2D e =
            new Ellipse2D.Double(
                cx - currentRadius, cy - currentRadius, 2 * currentRadius, 2 * currentRadius);
        paintShapeOutLine(g2d, e, true, true);
        currentRadius += gap;
      }
      paintShapeOutLine(
          g2d,
          new Ellipse2D.Double(
              cx - tokenRadius, cy - tokenRadius, tokenRadius * 2, tokenRadius * 2),
          true,
          false);
      g2d.dispose();
    }

    /**
     * Horizontal and vertical lines oriented on cx/cy
     *
     * @param g graphics object
     * @param rotation used to draw lines on rotated images
     * @param solid Draw as solid else dashed
     * @param colourSet1 Use colour set 1 else 2
     */
    void paintCentreLines(Graphics g, double rotation, boolean solid, boolean colourSet1) {
      /* create cross-hair with a central gap */
      double cx = centrePoint.getX();
      double cy = centrePoint.getY();
      Rectangle2D r = viewBounds;
      double x = r.getX() - 1,
          y = r.getY() - 1,
          w = x + r.getWidth() + 2,
          h = y + r.getHeight() + 2;

      double aperture = Math.max(cellHeight, cellWidth) / 5.0;
      Path2D lines = new Path2D.Double();
      lines.moveTo(cx, y - h);
      lines.lineTo(cx, cy - aperture);
      lines.moveTo(cx, cy + aperture);
      lines.lineTo(cx, h * 2);
      lines.moveTo(x - w, cy);
      lines.lineTo(cx - aperture, cy);
      lines.moveTo(cx + aperture, cy);
      lines.lineTo(2 * w, cy);
      Shape s;
      if (!MathUtil.inTolerance(rotation, 0, 0.009)) {
        s =
            AffineTransform.getRotateInstance(Math.toRadians(rotation), cx, cy)
                .createTransformedShape(lines);
        s =
            AffineTransform.getTranslateInstance(
                    getTokenAnchorX() * zoomFactor, getTokenAnchorY() * zoomFactor)
                .createTransformedShape(s);
      } else {
        s = lines;
      }
      paintShapeOutLine(g, s, solid, colourSet1);
    }

    /*
    Each line is drawn as a sequence of overlapping lines.
    The following arrays are used to define each stroke
     */
    float[] opacities = new float[] {0.15f, 0.85f, 0.6f, 1f};
    float[] strokeWidths =
        new float[] {2f * LINE_SIZE, 1.5f * LINE_SIZE, LINE_SIZE, 0.5f * LINE_SIZE};
    float[] dashPhases =
        new float[] {
          2f * LINE_SIZE + 2f, 2f * LINE_SIZE + 1.75f, 2f * LINE_SIZE + 1f, 2f * LINE_SIZE + 1.25f
        };
    float[][] dashes =
        new float[][] {
          {4f * LINE_SIZE + 4f, 6f},
          {4f * LINE_SIZE + 3.5f, 6.5f},
          {4f * LINE_SIZE + 2f, 8f},
          {4f * LINE_SIZE + 2.5f, 7.5f}
        };

    void paintShapeOutLine(Graphics g, Shape shp, boolean solid, boolean colourSet1) {
      Graphics2D g2d = (Graphics2D) g.create();
      Composite oldAc = g2d.getComposite();
      AlphaComposite ac;
      for (int i = 0; i < 4; i++) {
        switch (i) {
          case 0, 1 -> g2d.setColor(colourSet1 ? colours[2] : colours[3]);
          case 2, 3 -> g2d.setColor(colourSet1 ? colours[0] : colours[1]);
        }
        g2d.setStroke(
            solid
                ? new BasicStroke(strokeWidths[i])
                : new BasicStroke(
                    strokeWidths[i],
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    1f,
                    dashes[i],
                    dashPhases[i]));

        ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacities[i]);
        g2d.setComposite(ac);
        g2d.draw(shp);
      }
      g2d.setComposite(oldAc);
      g2d.dispose();
    }

    void paintFootprint(Graphics g, double zoomFactor) {
      if (noGrid) {
        paintRings(g, zoomFactor);
        return;
      }

      Graphics2D g2d = (Graphics2D) g.create();
      Shape oldClip = g2d.getClip();
      AffineTransform oldXform = g2d.getTransform();
      g2d.setRenderingHints(RENDERING_HINTS);

      g2d.translate(centrePoint.getX(), centrePoint.getY());
      g2d.scale(zoomFactor, zoomFactor);

      Shape tmpShape;

      Area clipArea = new Area(g2d.getClipBounds());
      Area tmpClip = new Area(), fpArea = new Area();
      g2d.setStroke(new BasicStroke(1f));
      g2d.setColor(colours[4]);

      double footprintScale = footprint.getScale();
      double yCorrection =
          isIsoFigure ? footprintScale * footprintBounds.getHeight() / 2d * zoomFactor : 0;

      Shape scaledOutline, scaledFill;
      /* for drawing sub-cell-sizes */
      if (footprintScale < 1d) {
        scaledOutline =
            AffineTransform.getScaleInstance(footprintScale, footprintScale)
                .createTransformedShape(gridShapeOutline);
        scaledFill =
            AffineTransform.getScaleInstance(footprintScale, footprintScale)
                .createTransformedShape(gridShapeFill);
      } else {
        scaledOutline = gridShapeOutline;
        scaledFill = gridShapeFill;
      }
      for (Point2D pt : cellCentres) {
        AffineTransform ptXform =
            AffineTransform.getTranslateInstance(pt.getX(), pt.getY() + yCorrection);
        if (footprintScale < 1) {
          g2d.draw(ptXform.createTransformedShape(gridShapeOutline));
        }
        g2d.draw(ptXform.createTransformedShape(scaledOutline));
        tmpShape = ptXform.createTransformedShape(scaledFill);
        fpArea.add(new Area(tmpShape));
      }
      tmpClip.subtract(fpArea);
      g2d.setClip(tmpClip);
      g2d.setPaint(FlatLaf.isLafDark() ? new Color(1f, 1f, 1f, 0.35f) : new Color(0, 0, 0, 0.35f));
      g2d.setClip(fpArea);
      g2d.fill(fpArea);

      g2d.setClip(clipArea);
      paintShapeOutLine(g2d, fpArea, true, true);
      g2d.setTransform(oldXform);
      g2d.setClip(oldClip);

      g2d.dispose();
      paintExtraGuides(g);
    }

    void paintExtraGuides(Graphics g) {
      if (noGrid) {
        return;
      }
      Graphics2D g2d = (Graphics2D) g.create();
      g2d.setRenderingHints(RENDERING_HINTS);

      g2d.translate(
          centrePoint.getX(),
          centrePoint.getY() + (isIsoFigure ? footprintBounds.getHeight() * zoomFactor / 2d : 0));

      g2d.setPaint(colours[4]);
      g2d.setStroke(
          new BasicStroke(
              0.5f,
              BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND,
              10f,
              new float[] {0.5f, 1.75f},
              0));

      double limit = Math.hypot(g2d.getClipBounds().getWidth(), g2d.getClipBounds().getHeight());
      double radius = footprintBounds.getHeight() * zoomFactor;
      Rectangle2D bounds = gridShapeOutline.getBounds2D();
      while (radius < limit) {
        radius += 2 * cellHeight * zoomFactor;
        Shape s =
            AffineTransform.getScaleInstance(
                    radius / bounds.getHeight(), radius / bounds.getHeight())
                .createTransformedShape(gridShapeOutline);
        g2d.draw(s);
      }
      g2d.dispose();
    }

    void paintToken(Graphics g, boolean translucent) {
      Graphics2D g2d = (Graphics2D) g.create();
      g2d.setRenderingHints(RENDERING_HINTS);
      if (centreMark == null) {
        createCentreMark();
      }

      g2d.translate(centrePoint.getX(), centrePoint.getY());
      g2d.translate(getTokenAnchorX() * zoomFactor, getTokenAnchorY() * zoomFactor);

      Composite oldAc = g2d.getComposite();

      AffineTransform imageXform =
          AffineTransform.getTranslateInstance(
              -workImage.getWidth() / 2d, -workImage.getHeight() / 2d);

      if (translucent) {
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
        g2d.setComposite(ac);
        g2d.drawImage(workImage, imageXform, null);
        g2d.setComposite(oldAc);
      } else {
        g2d.drawImage(workImage, imageXform, null);
      }
      g2d.dispose();
      double rotAngle = getTokenImageRotation();
      if (rotAngle > 0.01 && rotAngle < 359.99) {
        paintCentreLines(g, rotAngle, false, true);
      }
    }
  }
}
