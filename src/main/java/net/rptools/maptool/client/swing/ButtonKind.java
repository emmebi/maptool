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

import com.jidesoft.dialog.ButtonPanel;
import java.awt.event.KeyEvent;
import net.rptools.maptool.language.I18N;

public enum ButtonKind {
  //  @spotless:off
  ACCEPT         ("ACCEPT"         , ButtonPanel.AFFIRMATIVE_BUTTON, "Button.accept"        , "Button.accept.mnemonic"        ),
  ADD            ("ADD"            , ButtonPanel.OTHER_BUTTON      , "Button.add"           , "Button.add.mnemonic"           ),
  APPLY          ("APPLY"          , ButtonPanel.AFFIRMATIVE_BUTTON, "Button.apply"         , "Button.apply.mnemonic"         ),
  BACK           ("BACK"           , ButtonPanel.OTHER_BUTTON      , "Button.back"          , "Button.back.mnemonic"          ),
  BROWSE         ("BROWSE"         , ButtonPanel.OTHER_BUTTON      , "Button.browse"        , "Button.browse.mnemonic"        ),
  CANCEL         ("CANCEL"         , ButtonPanel.CANCEL_BUTTON     , "Button.cancel"        , "Button.cancel.mnemonic"        ),
  CLEAR          ("CLEAR"          , ButtonPanel.OTHER_BUTTON      , "Button.clear"         , "Button.clear.mnemonic"         ),
  CLEAR_ALL      ("CLEAR_ALL"      , ButtonPanel.OTHER_BUTTON      , "Button.clearAll"      , "Button.clearAll.mnemonic"),
  CLOSE          ("CLOSE"          , ButtonPanel.CANCEL_BUTTON     , "Button.close"         , "Button.close.mnemonic"         ),
  CONTINUE       ("CONTINUE"       , ButtonPanel.AFFIRMATIVE_BUTTON, "Button.continue"      , "Button.continue.mnemonic"      ),
  DELETE         ("DELETE"         , ButtonPanel.OTHER_BUTTON      , "Button.delete"        , "Button.delete.mnemonic"        ),
  DETAILS        ("DETAILS"        , ButtonPanel.OTHER_BUTTON      , "Button.details"       , "Button.details.mnemonic"       ),
  DISABLE        ("DISABLE"        , ButtonPanel.OTHER_BUTTON      , "Button.disable"       , "Button.disable.mnemonic"       ),
  EDIT           ("EDIT"           , ButtonPanel.OTHER_BUTTON      , "Button.edit"          , "Button.edit.mnemonic"          ),
  ENABLE         ("ENABLE"         , ButtonPanel.OTHER_BUTTON      , "Button.enable"        , "Button.enable.mnemonic"        ),
  EXIT           ("EXIT"           , ButtonPanel.CANCEL_BUTTON     , "Button.exit"          , "Button.exit.mnemonic"          ),
  EXPORT         ("EXPORT"         , ButtonPanel.OTHER_BUTTON      , "Button.export"        , "Button.export.mnemonic"        ),
  FIND           ("FIND"           , ButtonPanel.OTHER_BUTTON      , "Button.find"          , "Button.find.mnemonic"          ),
  FIND_NEXT      ("FIND_NEXT"      , ButtonPanel.OTHER_BUTTON      , "Button.findNext"      , "Button.findNext.mnemonic"      ),
  FINISH         ("FINISH"         , ButtonPanel.AFFIRMATIVE_BUTTON, "Button.finish"        , "Button.finish.mnemonic"        ),
  FORWARD        ("FORWARD"        , ButtonPanel.OTHER_BUTTON      , "Button.forward"       , "Button.forward.mnemonic"       ),
  HELP           ("HELP"           , ButtonPanel.HELP_BUTTON       , "Button.help"          , "Button.help.mnemonic"          ),
  HIDE_DETAILS   ("HIDE_DETAILS"   , ButtonPanel.OTHER_BUTTON      , "Button.hideDetails"   , "Button.hideDetails.mnemonic"   ),
  INSTALL        ("INSTALL"        , ButtonPanel.AFFIRMATIVE_BUTTON, "Button.install"       , "Button.install.mnemonic"       ),
  IMPORT         ("IMPORT"         , ButtonPanel.OTHER_BUTTON      , "Button.import"        , "Button.import.mnemonic"        ),
  NEW            ("NEW"            , ButtonPanel.OTHER_BUTTON      , "Button.new"           , "Button.new.mnemonic"           ),
  NEXT           ("NEXT"           , ButtonPanel.OTHER_BUTTON      , "Button.next"          , "Button.next.mnemonic"          ),
  NETWORKING_HELP("NETWORKING_HELP", ButtonPanel.HELP              , "Button.networkingHelp", "Button.networkingHelp.mnemonic"),
  NO             ("NO"             , ButtonPanel.CANCEL_BUTTON     , "Button.no"            , "Button.no.mnemonic"            ),
  OK             ("OK"             , ButtonPanel.AFFIRMATIVE_BUTTON, "Button.ok"            , "Button.ok.mnemonic"            ),
  OPEN           ("OPEN"           , ButtonPanel.AFFIRMATIVE_BUTTON, "Button.open"          , "Button.open.mnemonic"          ),
  PRINT          ("PRINT"          , ButtonPanel.OTHER_BUTTON      , "Button.print"         , "Button.print.mnemonic"         ),
  REFRESH        ("REFRESH"        , ButtonPanel.OTHER_BUTTON      , "Button.refresh"       , "Button.refresh.mnemonic"       ),
  REPLACE        ("REPLACE"        , ButtonPanel.OTHER_BUTTON      , "Button.replace"       , "Button.replace.mnemonic"       ),
  RESET          ("RESET"          , ButtonPanel.OTHER_BUTTON      , "Button.reset"         , "Button.reset.mnemonic"         ),
  RETRY          ("RETRY"          , ButtonPanel.OTHER_BUTTON      , "Button.retry"         , "Button.retry.mnemonic"         ),
  REVERT         ("REVERT"         , ButtonPanel.OTHER_BUTTON      , "Button.revert"        , "Button.revert.mnemonic"        ),
  SAVE           ("SAVE"           , ButtonPanel.AFFIRMATIVE_BUTTON, "Button.save"          , "Button.save.mnemonic"          ),
  SAVE_AS        ("SAVE_AS"        , ButtonPanel.AFFIRMATIVE_BUTTON, "Button.saveAs"        , "Button.saveAs.mnemonic"        ),
  SHOW_DETAILS   ("SHOW_DETAILS"   , ButtonPanel.OTHER_BUTTON      , "Button.showDetails"   , "Button.showDetails.mnemonic"   ),
  STOP           ("STOP"           , ButtonPanel.OTHER_BUTTON      , "Button.stop"          , "Button.stop.mnemonic"          ),
  UPDATE         ("UPDATE"         , ButtonPanel.OTHER_BUTTON      , "Button.update"        , "Button.update.mnemonic"        ),
  YES            ("YES"            , ButtonPanel.AFFIRMATIVE_BUTTON, "Button.yes"           , "Button.yes.mnemonic"           ),
  MOVE_UP        ("MOVE_UP"        , ButtonPanel.OTHER_BUTTON      , "Button.moveUp"        , "Button.moveUp.mnemonic"        ),
  MOVE_DOWN      ("MOVE_DOWN"      , ButtonPanel.OTHER_BUTTON      , "Button.moveDown"      , "Button.moveDown.mnemonic"      );
  //  @spotless:on
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

  public int getMnemonicKey() {
    String keyString = I18N.getText(this.i18nMnemonicKey).trim();
    if (keyString.length() == 1) {
      return KeyEvent.getExtendedKeyCodeForChar(keyString.charAt(0));
    } else {
      // Literal strings, should not be translated.
      switch (keyString) {
        case "{esc}" -> {
          return KeyEvent.VK_ESCAPE;
        }
        case "{f1}" -> {
          return KeyEvent.VK_F1;
        }
        case "{left}" -> {
          return KeyEvent.VK_LEFT;
        }
        case "{right}" -> {
          return KeyEvent.VK_RIGHT;
        }
        case "{up}" -> {
          return KeyEvent.VK_UP;
        }
        case "{down}" -> {
          return KeyEvent.VK_DOWN;
        }
        default -> {
          return KeyEvent.VK_ENTER;
        }
      }
    }
  }
}
