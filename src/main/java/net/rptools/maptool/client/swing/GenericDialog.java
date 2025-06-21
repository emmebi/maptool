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

import com.formdev.flatlaf.ui.FlatRootPaneUI;
import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.ScrollableButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import com.jidesoft.swing.JideBoxLayout;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.language.I18N;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GenericDialog extends StandardDialog {
  private boolean hasPositionedItself;
  private JComponent content = new JPanel();
  private ButtonPanel buttonPanel;
  private AbstractAction onCloseAction;
  public enum ButtonKind {
    ACCEPT("ACCEPT",ButtonPanel.AFFIRMATIVE_BUTTON,"Button.accept","Button.accept.mnemonic"),
    ADD("ADD",ButtonPanel.OTHER_BUTTON,"Button.add","Button.add.mnemonic"),
    APPLY("APPLY",ButtonPanel.AFFIRMATIVE_BUTTON,"Button.apply","Button.apply.mnemonic"),
    BACK("BACK",ButtonPanel.OTHER_BUTTON,"Button.back","Button.back.mnemonic"),
    BROWSE("BROWSE",ButtonPanel.OTHER_BUTTON,"Button.browse","Button.browse.mnemonic"),
    CANCEL("CANCEL",ButtonPanel.CANCEL_BUTTON,"Button.cancel","Button.cancel.mnemonic"),
    CLEAR("CLEAR",ButtonPanel.OTHER_BUTTON,"Button.clear","Button.clear.mnemonic"),
    CLEAR_ALL("CLEAR_ALL",ButtonPanel.OTHER_BUTTON,"Button.clearall","Button.clearall.mnemonic"),
    CLOSE("CLOSE",ButtonPanel.CANCEL_BUTTON,"Button.close","Button.close.mnemonic"),
    CONTINUE("CONTINUE",ButtonPanel.AFFIRMATIVE_BUTTON,"Button.continue","Button.continue.mnemonic"),
    DELETE("DELETE",ButtonPanel.OTHER_BUTTON,"Button.delete","Button.delete.mnemonic"),
    DETAILS("DETAILS",ButtonPanel.OTHER_BUTTON,"Button.details","Button.details.mnemonic"),
    DISABLE("DISABLE",ButtonPanel.OTHER_BUTTON,"Button.disable","Button.disable.mnemonic"),
    EDIT("EDIT",ButtonPanel.OTHER_BUTTON,"Button.edit","Button.edit.mnemonic"),
    ENABLE("ENABLE",ButtonPanel.OTHER_BUTTON,"Button.enable","Button.enable.mnemonic"),
    EXIT("EXIT",ButtonPanel.CANCEL_BUTTON,"Button.exit","Button.exit.mnemonic"),
    EXPORT("EXPORT",ButtonPanel.OTHER_BUTTON,"Button.export","Button.export.mnemonic"),
    FIND("FIND",ButtonPanel.OTHER_BUTTON,"Button.find","Button.find.mnemonic"),
    FIND_NEXT("FIND_NEXT",ButtonPanel.OTHER_BUTTON,"Button.findNext","Button.findNext.mnemonic"),
    FINISH("FINISH",ButtonPanel.AFFIRMATIVE_BUTTON,"Button.finish","Button.finish.mnemonic"),
    FORWARD("FORWARD",ButtonPanel.OTHER_BUTTON,"Button.forward","Button.forward.mnemonic"),
    HELP("HELP",ButtonPanel.HELP_BUTTON,"Button.help","Button.help.mnemonic"),
    HIDE_DETAILS("HIDE_DETAILS",ButtonPanel.OTHER_BUTTON,"Button.hideDetails","Button.hideDetails.mnemonic"),
    IMPORT("IMPORT",ButtonPanel.OTHER_BUTTON,"Button.import","Button.import.mnemonic"),
    NEW("NEW",ButtonPanel.OTHER_BUTTON,"Button.new","Button.new.mnemonic"),
    NEXT("NEXT",ButtonPanel.OTHER_BUTTON,"Button.next","Button.next.mnemonic"),
    NO("NO",ButtonPanel.CANCEL_BUTTON,"Button.no","Button.no.mnemonic"),
    OK("OK",ButtonPanel.AFFIRMATIVE_BUTTON,"Button.ok","Button.ok.mnemonic"),
    OPEN("OPEN",ButtonPanel.AFFIRMATIVE_BUTTON,"Button.open","Button.open.mnemonic"),
    PRINT("PRINT",ButtonPanel.OTHER_BUTTON,"Button.print","Button.print.mnemonic"),
    REFRESH("REFRESH",ButtonPanel.OTHER_BUTTON,"Button.refresh","Button.refresh.mnemonic"),
    REPLACE("REPLACE",ButtonPanel.OTHER_BUTTON,"Button.replace","Button.replace.mnemonic"),
    RESET("RESET",ButtonPanel.OTHER_BUTTON,"Button.reset","Button.reset.mnemonic"),
    RETRY("RETRY",ButtonPanel.OTHER_BUTTON,"Button.retry","Button.retry.mnemonic"),
    REVERT("REVERT",ButtonPanel.OTHER_BUTTON,"Button.revert","Button.revert.mnemonic"),
    SAVE("SAVE",ButtonPanel.AFFIRMATIVE_BUTTON,"Button.save","Button.save.mnemonic"),
    SAVE_AS("SAVE_AS",ButtonPanel.AFFIRMATIVE_BUTTON,"Button.saveAs","Button.saveAs.mnemonic"),
    SHOW_DETAILS("SHOW_DETAILS",ButtonPanel.OTHER_BUTTON,"Button.showDetails","Button.showDetails.mnemonic"),
    STOP("STOP",ButtonPanel.OTHER_BUTTON,"Button.stop","Button.stop.mnemonic"),
    UPDATE("UPDATE",ButtonPanel.OTHER_BUTTON,"Button.update","Button.update.mnemonic"),
    YES("YES",ButtonPanel.AFFIRMATIVE_BUTTON,"Button.yes","Button.yes.mnemonic"),
    MOVE_UP("MOVE_UP",ButtonPanel.OTHER_BUTTON,"Button.up","Button.up.mnemonic"),
    MOVE_DOWN("MOVE_DOWN",ButtonPanel.OTHER_BUTTON,"Button.down","Button.down.mnemonic")
;
    final String name;
    final String buttonPanelButtonType;
    final String i18nKey;
    final String i18nMnemonicKey;
    ButtonKind(String name, String buttonPanelButtonType, String i18nKey, String i18nMnemonicKey) {
      this.name = name;
      this.buttonPanelButtonType = buttonPanelButtonType;
      this.i18nKey = i18nKey;
      this.i18nMnemonicKey = i18nMnemonicKey;
    }
  }
  public GenericDialog(){
    super(MapTool.getFrame());
    super.setResizable(true);
    getRootPane().setBorder(
                    BorderFactory.createCompoundBorder(
                            new FlatRootPaneUI.FlatWindowBorder(),
                            BorderFactory.createEmptyBorder(0, 4, 4, 3)));
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
  }
  public GenericDialog(String title, Frame parent, JPanel panel) {
    this(title, parent, panel, true);
  }

  public GenericDialog(String title, Frame parent, JPanel panel, boolean modal) {
    this();
    setTitle(title);
    setModal(modal);
    setContent(panel);
  }

  @Override
  public ButtonPanel createButtonPanel() {
    buttonPanel = new ScrollableButtonPanel();
    return buttonPanel;
  }

  @Override
  public JComponent createBannerPanel() {
    return null;
  }

  @Override
  public JComponent createContentPanel() {
    return content;
  }

  @SuppressWarnings("UnusedReturnValue")
  public GenericDialog addButton(ButtonKind buttonKind){
    if(buttonPanel == null){
      createButtonPanel();
    }
    AbstractButton b = new JButton(I18N.getText(buttonKind.i18nKey));
    b.setName(buttonKind.name);
    b.setMnemonic(I18N.getText(buttonKind.i18nMnemonicKey).charAt(0));
    if(buttonKind.buttonPanelButtonType.equals(ButtonPanel.AFFIRMATIVE_BUTTON)){
      b.setAction(new AbstractAction(I18N.getText(buttonKind.i18nKey)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          GenericDialog.this.setDialogResult(RESULT_AFFIRMED);
          closeDialog();
        }
      });
    } else if(buttonKind.buttonPanelButtonType.equals(ButtonPanel.CANCEL_BUTTON)){
      b.setAction(new AbstractAction(I18N.getText(buttonKind.i18nKey)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          GenericDialog.this.setDialogResult(RESULT_CANCELLED);
          closeDialog();
        }
      });
    }
    buttonPanel.addButton(b, buttonKind.buttonPanelButtonType);
    return this;
  }
  @SuppressWarnings("UnusedReturnValue")
  public GenericDialog makeModal(boolean modal){
    setModal(modal);
    return this;
  }
  @SuppressWarnings("UnusedReturnValue")
  public GenericDialog addOkCancelButtons(){
    addButton(ButtonKind.OK);
    addButton(ButtonKind.CANCEL);
    return this;
  }
  public AbstractButton getButton(ButtonKind buttonKind){
    return (AbstractButton) buttonPanel.getButtonByName(buttonKind.name);
  }
  @SuppressWarnings("UnusedReturnValue")
  public GenericDialog onClose(AbstractAction action){
    onCloseAction = action;
    return this;
  }
  @SuppressWarnings("UnusedReturnValue")
  public GenericDialog setDialogTitle(String title){
    super.setTitle(title);
    return this;
  }
  @SuppressWarnings("UnusedReturnValue")
  public GenericDialog setContent(JComponent content){
    this.content = content;
    JideBoxLayout layout = new JideBoxLayout(getContentPane(), JideBoxLayout.PAGE_AXIS);
    getContentPane().setLayout(layout);
    JScrollPane scrollPane = new JScrollPane(content);
    getContentPane().add(scrollPane, JideBoxLayout.FLEXIBLE);

    getRootPane().validate();
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        closeDialog();
      }
    });

    // ESCAPE cancels the window without committing
    content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    content.getActionMap().put("cancel", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        closeDialog();
      }
    });
    return this;
  }



  public void closeDialog() {
    dispose();
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

  public void showDialog() {
    // We want to center over our parent, but only the first time.
    // If this dialog is reused, we want it to show up where it was last.
    if (!hasPositionedItself) {
      pack();
      positionInitialView();
      hasPositionedItself = true;
    }
    setVisible(true);
  }

  protected void positionInitialView() {
    SwingUtil.centerOver(this, getOwner());
  }
}
