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

import com.jidesoft.dialog.*;
import com.jidesoft.swing.*;
import com.jidesoft.utils.PortingUtils;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.language.I18N;

public class GenericDialog extends JDialog {
  public static final String AFFIRM = ButtonPanel.AFFIRMATIVE_BUTTON;
  public static final String DENY = ButtonPanel.CANCEL_BUTTON;

  private boolean hasPositionedItself;
  private String _dialogResult = ButtonPanel.CANCEL_BUTTON;
  protected final Resizable _resizable;
  private final JComponent _contentPane = new JPanel();
  private JComponent _content = new JPanel();
  private final JScrollPane _scrollPane = new JScrollPane();
  private ButtonPanel _buttonPanel;
  private boolean _usingAbeillePanel = false;
  private ActionListener _onCloseAction;
  private ActionListener _onShowAction;

  public static GenericDialogFactory getFactory() {
    return new GenericDialogFactory();
  }

  public GenericDialog() {
    super(MapTool.getFrame());
    super.setContentPane(_contentPane);
    initComponents();

    _resizable =
        new Resizable(getRootPane()) {
          public void resizing(int resizeDir, int newX, int newY, int newW, int newH) {
            Container container = GenericDialog.this.getContentPane();
            PortingUtils.setPreferredSize(container, new Dimension(newW, newH));
            if (GenericDialog.this.isUndecorated()) {
              GenericDialog.this.setBounds(newX, newY, newW, newH);
            }
          }
        };
    _resizable.setResizeCornerSize(18);
    _resizable.setResizableCorners(Resizable.LOWER_LEFT | Resizable.LOWER_RIGHT);
    super.setResizable(true);

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            closeDialog();
          }
        });
  }

  public GenericDialog(String title, JPanel panel) {
    this(title, panel, false);
  }

  public GenericDialog(String title, JComponent panel, boolean modal) {
    this();
    setDialogTitle(title);
    setModal(modal);
    setContent(panel);
  }

  protected void initComponents() {
    JideBoxLayout layout = new JideBoxLayout(this.getContentPane(), JideBoxLayout.PAGE_AXIS);
    this.getContentPane().setLayout(layout);
    this._scrollPane.setViewportView(getContentPanel());
    this.getContentPane().add(this._scrollPane, JideBoxLayout.VARY);
    this.getContentPane().add(this.getButtonPanel(), JideBoxLayout.FIX);
  }

  @Override
  public JComponent getContentPane() {
    return _contentPane;
  }

  public JComponent getContentPanel() {
    return _content;
  }

  @SuppressWarnings("UnusedReturnValue")
  public ButtonPanel getButtonPanel() {
    if (_buttonPanel == null) {
      _buttonPanel = new ScrollableButtonPanel();
      _buttonPanel.setSizeConstraint(ButtonPanel.NO_LESS_THAN);
      _buttonPanel.setBorder(UIManager.getDefaults().getBorder("DesktopIcon.border"));
    }
    return _buttonPanel;
  }

  @SuppressWarnings("UnusedReturnValue")
  public void setDefaultButton(ButtonKind buttonKind) {
    JButton button = (JButton) _buttonPanel.getButtonByName(buttonKind.name);
    if (button == null) {
      addButton(buttonKind);
      button = (JButton) _buttonPanel.getButtonByName(buttonKind.name);
    }
    getRootPane().setDefaultButton(button);
  }

  public void addButton(ButtonKind buttonKind) {
    addButton(buttonKind, null, null);
  }

  public void addButton(ButtonKind buttonKind, Action action) {
    addButton(buttonKind, action, null);
  }

  public void addButton(ButtonKind buttonKind, ActionListener listener) {
    addButton(buttonKind, null, listener);
  }

  public void addButton(ButtonKind buttonKind, Action action, ActionListener listener) {
    // check button exists
    AbstractButton b = (AbstractButton) _buttonPanel.getButtonByName(buttonKind.name);
    boolean needNewButton = b == null;
    if (needNewButton) {
      b = new JButton(I18N.getText(buttonKind.i18nKey));
      b.setName(buttonKind.name);
      b.setMnemonic(buttonKind.getMnemonicKey());
    }
    if (action != null) {
      b.setAction(action);
    } else {
      if (buttonKind.buttonPanelButtonType.equals(ButtonPanel.AFFIRMATIVE_BUTTON)) {
        b.setAction(
            new AbstractAction(I18N.getText(buttonKind.i18nKey)) {
              @Override
              public void actionPerformed(ActionEvent e) {
                setDialogResult(AFFIRM);
                closeDialog();
              }
            });
      } else if (buttonKind.buttonPanelButtonType.equals(ButtonPanel.CANCEL_BUTTON)) {
        b.setAction(
            new AbstractAction(I18N.getText(buttonKind.i18nKey)) {
              @Override
              public void actionPerformed(ActionEvent e) {
                setDialogResult(DENY);
                closeDialog();
              }
            });
      }
    }
    if (listener != null) {
      b.addActionListener(listener);
    }
    if (needNewButton) {
      this._buttonPanel.addButton(b, buttonKind.buttonPanelButtonType);
    }
  }

  public void createOkCancelButtons() {
    addButton(ButtonKind.OK);
    addButton(ButtonKind.CANCEL);
  }

  public void onBeforeShow(ActionListener listener) {
    _onShowAction = listener;
  }

  public void onBeforeClose(ActionListener listener) {
    _onCloseAction = listener;
  }

  public void setDialogTitle(String title) {
    super.setTitle(title);
  }

  public void setContent(JComponent content) {
    this._content = content;
    _usingAbeillePanel = content instanceof AbeillePanel;
    this._scrollPane.setViewportView(content);

    // ESCAPE cancels the window without committing
    content
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    content
        .getActionMap()
        .put(
            "cancel",
            new AbstractAction() {
              public void actionPerformed(ActionEvent e) {
                closeDialog();
              }
            });
  }

  public AbstractButton getOKButton() {
    return getButton(ButtonKind.OK);
  }

  public AbstractButton getCancelButton() {
    return getButton(ButtonKind.CANCEL);
  }

  public void closeDialog() {
    if (_usingAbeillePanel && ((AbeillePanel<?>) _content).getModel() != null) {
      if (getDialogResult().equals(AFFIRM)) {
        ((AbeillePanel<?>) _content).commit();
      }
      ((AbeillePanel<?>) _content).unbind();
    }
    if (_onCloseAction != null) {
      _onCloseAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "close"));
    }
    if (getDefaultCloseOperation() == DISPOSE_ON_CLOSE) {
      dispose();
    }
  }

  public AbstractButton getButton(ButtonKind buttonKind) {
    return (AbstractButton) _buttonPanel.getButtonByName(buttonKind.name);
  }

  protected void setDialogResult(String result) {
    _dialogResult = result;
  }

  public String getDialogResult() {
    return _dialogResult;
  }

  private Dimension getMaxScreenSize() {
    GraphicsConfiguration gc = getOwner().getGraphicsConfiguration();
    Insets insets = getOwner().getToolkit().getScreenInsets(gc);
    Rectangle bounds = gc.getDevice().getDefaultConfiguration().getBounds();
    return new Dimension(
        bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension superPref = super.getPreferredSize();
    Dimension screenMax = getMaxScreenSize();
    return new Dimension(
        Math.min(superPref.width, screenMax.width), Math.min(superPref.height, screenMax.height));
  }

  @Override
  public Dimension getMaximumSize() {
    Dimension superMax = super.getMaximumSize();
    Dimension screenMax = getMaxScreenSize();
    return new Dimension(
        Math.min(superMax.width, screenMax.width), Math.min(superMax.height, screenMax.height));
  }

  @Override
  public void setMaximumSize(Dimension maximumSize) {
    Dimension screenMax = getMaxScreenSize();
    super.setMaximumSize(
        new Dimension(
            Math.min(maximumSize.width, screenMax.width),
            Math.min(maximumSize.height, screenMax.height)));
  }

  public String showDialogWithReturnValue() {
    if (!isModal()) {
      setModal(true);
    }
    setVisible(true);
    return this.getDialogResult();
  }

  public void showDialog() {
    setVisible(true);
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      // We want to center over our parent, but only the first time.
      // If this dialog is reused, we want it to show up where it was last.
      pack();
      if (!hasPositionedItself) {
        positionInitialView();
        hasPositionedItself = true;
      }
      if (_onShowAction != null) {
        _onShowAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "show"));
      }
      if (getRootPane().getDefaultButton() == null && _buttonPanel.getComponents().length == 1) {
        getRootPane().setDefaultButton((JButton) _buttonPanel.getComponents()[0]);
      }
      super.setVisible(true);
    } else {
      super.setVisible(visible);
    }
  }

  protected void positionInitialView() {
    SwingUtil.centerOver(this, getOwner());
  }
}
