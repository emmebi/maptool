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
package net.rptools.maptool.client.ui.theme;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.components.FlatLabel;
import com.formdev.flatlaf.util.UIScale;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.font.TextAttribute;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import net.rptools.maptool.client.AppPreferences;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.swing.AbeillePanel;
import net.rptools.maptool.language.I18N;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("rawtypes")
public class ThemeFontPreferences extends AbeillePanel {
  public ThemeFontPreferences() {
    super(new ThemeFontPreferencesPanel().getRootComponent());
    panelInit();
    validate();
  }

  private static final Logger log = LogManager.getLogger(ThemeFontPreferences.class);

  private static final Icon cross = RessourceManager.getSmallIcon(Icons.ACTION_DELETE);

  private static final Map<String, FontData> FONT_DATA_MAP = new ConcurrentHashMap<>();
  private static final Map<String, Font> FONT_MAP = ThemeTools.FONT_MAP;
  private static final String[] FONT_NAME_ARRAY =
      FONT_MAP.keySet().stream().sorted().toList().toArray(new String[0]);
  private static final List<String> FONT_STYLE_KEYS = ThemeTools.FONT_STYLE_FAMILIES;
  private static final float SYSTEM_FONT_SIZE =
      UIScale.getUserScaleFactor() * ThemeTools.OS_DEFAULT_FONT_SIZE;
  private static final LookAndFeel CURRENT_LAF = UIManager.getLookAndFeel();
  private static final UIDefaults LAF_DEFAULTS = CURRENT_LAF.getDefaults();
  private static final float DEFAULT_ACTUAL_SIZE =
      UIScale.unscale(LAF_DEFAULTS.getFont("defaultFont").getSize());
  private static final float DEFAULT_RELATIVE_SIZE =
      DEFAULT_ACTUAL_SIZE
          + ThemeTools.FLAT_LAF_DEFAULT_FONT_SIZES.get("defaultFont")
          - ThemeTools.OS_DEFAULT_FONT_SIZE;

  private static final Map<String, List<JComponent>> COMPONENT_MAP = new HashMap<>();
  private static Map<String, FontData> fontDataMapBackup = new HashMap<>();
  private float currentRelativeSize = Integer.MIN_VALUE;

  private final Function<String, Boolean> getEnabled =
      string -> getComponent(string, JCheckBox.class).isSelected();

  private final BasicComboBoxRenderer bcr =
      new BasicComboBoxRenderer.UIResource() {
        @Override
        public Component getListCellRendererComponent(
            JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
          } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
          }
          if (value == null) {
            setText("---");
          } else {
            setText(String.valueOf(value));
            setFont(
                FONT_MAP
                    .get(String.valueOf(value))
                    .deriveFont(UIScale.scale(SYSTEM_FONT_SIZE + DEFAULT_RELATIVE_SIZE + 1)));
          }
          return this;
        }
      };

  private <T extends Component> T getComponent(String key, Class<T> clazz) {
    return clazz.cast(
        COMPONENT_MAP.get(key).stream()
            .filter(jComponent -> jComponent.getClass().isAssignableFrom(clazz))
            .findFirst()
            .orElse(null));
  }

  private Component getComponentByName(String name) {
    for (String key : COMPONENT_MAP.keySet()) {
      if (name.contains(key)) {
        for (Component c : COMPONENT_MAP.get(key)) {
          if (c.getName().equalsIgnoreCase(name)) {
            return c;
          }
        }
      }
    }
    return null;
  }

  private JToggleButton getToggleButton(String key, int index) {
    return (JToggleButton)
        COMPONENT_MAP.get(key).stream()
            .filter(jComponent -> jComponent.getClass().isAssignableFrom(JToggleButton.class))
            .toList()
            .get(index);
  }

  private String getComponentKey(Component c) {
    int end = c.getName().lastIndexOf(".");
    end = end == -1 ? c.getName().length() : end;
    return c.getName().substring(0, end);
  }

  @SuppressWarnings("unused")
  public void initComponents() {
    if (!COMPONENT_MAP.isEmpty()) {
      // we've been here before. Use the components from the last time
      Collection components = super.getAllComponents();
      List componentList = List.copyOf(components);
      for (Object o : componentList) {
        Component c = (Component) o;
        String key = getComponentKey(c);
        if (COMPONENT_MAP.containsKey(key)) {
          Component replacement = getComponentByName(c.getName());
          super.replaceComponent(c.getParent().getName(), c.getName(), replacement);
        }
      }
      return;
    }

    // Keys for general font sizes as well as generic font styles
    List<String> allKeys =
        Stream.of(ThemeTools.GENERAL_FONT_KEYS, ThemeTools.FONT_STYLE_FAMILIES)
            .flatMap(Collection::stream)
            .toList();

    currentRelativeSize = DEFAULT_RELATIVE_SIZE;

    FONT_DATA_MAP.put("defaultFont", new FontData(DEFAULT_RELATIVE_SIZE));
    for (String s : allKeys) {
      // initialise properties map with the current Laf defaults
      if (!s.equalsIgnoreCase("defaultFont")) {
        float relativetoDefaultSize =
            UIScale.unscale(CURRENT_LAF.getDefaults().getFont(s).getSize()) - DEFAULT_ACTUAL_SIZE;
        FONT_DATA_MAP.put(s, new FontData(relativetoDefaultSize));
      }
      // update it with stored properties
      if (ThemeTools.USER_FLAT_PROPERTIES.containsKey(s)) {
        String entry = ThemeTools.USER_FLAT_PROPERTIES.get(s).toString();
        if (entry != null && !entry.isBlank()) {
          FONT_DATA_MAP.put(s, new FontData(ThemeTools.parseString(entry)));
        }
      }
    }

    // app preference checkbox to use alternate font settings
    JCheckBox useCustomUIProperties = getCheckBox("useCustomUIProperties");
    useCustomUIProperties.setSelected(AppPreferences.useCustomThemeFontProperties.get());
    useCustomUIProperties.addChangeListener(
        change ->
            AppPreferences.useCustomThemeFontProperties.set(useCustomUIProperties.isSelected()));

    // Start with the controls every key has in common.
    // An enable checkbox, reset button, bold toggle, italic toggle, and an exemplar
    for (String key : allKeys) {
      // whilst we are here we will initialise the component map for each key
      COMPONENT_MAP.put(key, new ArrayList<>());

      FontData fontData = FONT_DATA_MAP.get(key);
      boolean enabled = ThemeTools.USER_FLAT_PROPERTIES.containsKey(key);

      // the enable checkbox
      JCheckBox cb = getCheckBox(key + ".enable");
      cb.setSelected(enabled);
      cb.addItemListener(enableListener);
      COMPONENT_MAP.get(key).add(cb);

      // the size spinner uses relative size
      float val = key.equalsIgnoreCase("defaultFont") ? DEFAULT_RELATIVE_SIZE : fontData.getSize();
      JSpinner sp = getSpinner(key + ".size");
      if (sp != null) {
        sp.setEnabled(enabled);
        sp.setModel(new SpinnerNumberModel(val, 1 - DEFAULT_ACTUAL_SIZE, 200f, 1f));
        sp.addChangeListener(spinnerListener);
        COMPONENT_MAP.get(key).add(sp);
      }
      // change the reset button to use an icon
      JButton btn = (JButton) getButton(key + ".reset");
      btn.setEnabled(enabled);
      btn.setText("");
      btn.setIcon(cross);
      btn.addActionListener(buttonListener);
      COMPONENT_MAP.get(key).add(btn);

      // bold style button
      JToggleButton bold = (JToggleButton) getButton(key + ".bold");
      bold.addActionListener(buttonListener);
      bold.setSelected(fontData.isBold());
      bold.setEnabled(enabled);
      COMPONENT_MAP.get(key).add(bold);

      // italic style button
      JToggleButton italic = (JToggleButton) getButton(key + ".italic");
      italic.addActionListener(buttonListener);
      italic.setSelected(fontData.isItalic());
      italic.setEnabled(enabled);
      COMPONENT_MAP.get(key).add(italic);

      // set up the labels to display the various fonts and sizes
      FlatLabel label = new FlatLabel();
      label.setText(getLabel(key + ".label").getText());
      label.setName(key + ".label");
      replaceComponent(getComponent(key + ".label").getParent().getName(), key + ".label", label);
      COMPONENT_MAP.get(key).add(label);

      if (FONT_STYLE_KEYS.contains(key)) {
        // if it is one of the three principal fonts, we want a little more in our exemplar
        if (key.equalsIgnoreCase("defaultFont")) {
          // defaultFont breaks the naming syntax so it gets its own fork
          label.setLabelType(FlatLabel.LabelType.regular);
          FONT_DATA_MAP.put(key, new FontData(label.getFont()));
        } else {
          Font lafFont = LAF_DEFAULTS.getFont(key);
          FONT_DATA_MAP.put(key, new FontData(lafFont));
          label.setFont(lafFont);
        }
        label.setText(
            MessageFormat.format(
                "{0} > {1}",
                I18N.getText("Preferences.label." + key), LAF_DEFAULTS.getFont(key).getFamily()));
      } else {
        // all the generic sizes plus headings
        label.setLabelType(
            FlatLabel.LabelType.valueOf(key.substring(0, key.indexOf(".")).toLowerCase()));
        FONT_DATA_MAP.put(key, new FontData(label.getFont()));
      }
    }
    // should have initial fonts for everything now - copy to the backup and remove inactive ones
    if (fontDataMapBackup.isEmpty()) {
      fontDataMapBackup = Map.copyOf(FONT_DATA_MAP); // immutable copy
    }

    // for the four main font styles, we want a font selection combo box
    for (String key : FONT_STYLE_KEYS) {
      boolean enabled = getEnabled.apply(key);
      FontData fontData = FONT_DATA_MAP.get(key);

      // font selection combo box
      JComboBox<String> replacement = new JComboBox<>(FONT_NAME_ARRAY);
      replacement.setRenderer(bcr);
      replaceComponent("main", key + ".family", replacement);
      replacement.setName(key + ".family");
      replacement.setSelectedItem(fontData.getName());
      replacement.setEnabled(enabled);
      replacement.addItemListener(comboListener);
      COMPONENT_MAP.get(key).add(replacement);
      replacement.invalidate();
    }
  }

  /**
   * Update the relevant label. All listeners point to here
   *
   * @param c component related to label
   */
  private void updateExemplar(JComponent c) {
    String key = getComponentKey(c);
    boolean enabled = getEnabled.apply(key);
    JLabel exemplar = getComponent(key, FlatLabel.class);
    if (exemplar != null) { // just in case it all happens too fast, check for null
      float size = currentRelativeSize;
      FontData fontData;
      if (enabled) {
        // get the font currently on the label
        fontData = new FontData(exemplar.getFont());

        fontData = fontData.setBold(getToggleButton(key, 0).isSelected());
        fontData = fontData.setItalic(getToggleButton(key, 1).isSelected());
        if (FONT_STYLE_KEYS.contains(key)) {
          // has a combo box for font selection
          JComboBox combo =
              (JComboBox)
                  COMPONENT_MAP.get(key).stream()
                      .filter(jComponent -> jComponent.getClass().isAssignableFrom(JComboBox.class))
                      .toList()
                      .getFirst();
          fontData = fontData.setName((String) combo.getSelectedItem());
          // add font name to label text
          log.info(key);
          exemplar.setText(
              MessageFormat.format(
                  "{0} > {1}", I18N.getText("Preferences.label." + key), combo.getSelectedItem()));
        }
        if (ThemeTools.FLAT_LAF_DEFAULT_FONT_SIZES.containsKey(key)) {
          size +=
              ((SpinnerNumberModel) getComponent(key, JSpinner.class).getModel())
                  .getNumber()
                  .floatValue();
        }
      } else {
        fontData = fontDataMapBackup.get(key);
        log.info(key);
        size = DEFAULT_RELATIVE_SIZE;
        log.info(fontData + ":" + size);
      }
      exemplar.setFont(fontData.setSize(size).toFont());
      exemplar.repaint();
      FONT_DATA_MAP.put(key, new FontData(exemplar.getFont()));
    }
  }

  private final ActionListener buttonListener =
      change -> {
        JComponent c1 = (JComponent) change.getSource();
        if (c1.getName().endsWith("reset")) {
          String key = getComponentKey(c1);
          Font f = fontDataMapBackup.get(key).toFont();
          List<JComponent> components = COMPONENT_MAP.get(key);
          for (JComponent c : components) {
            switch (c) {
              case JCheckBox ignored -> {
                // skip the enable checkbox
              }
              case JSpinner jSpinner ->
                  jSpinner.setValue(f.getSize2D() - SYSTEM_FONT_SIZE - DEFAULT_RELATIVE_SIZE);
              case JToggleButton toggleButton -> {
                if (toggleButton.getName().endsWith("bold")) {
                  toggleButton.setSelected(f.isBold());
                } else {
                  toggleButton.setSelected(f.isItalic());
                }
              }
              case JComboBox<?> comboBox -> comboBox.setSelectedItem(f.getFontName());
              case null, default -> {}
            }
          }
        } else {
          // is a toggle button
          JToggleButton toggleButton = (JToggleButton) c1;
          Map<TextAttribute, Integer> fontAttributes = new HashMap<>();
          if (toggleButton.isSelected()) {
            fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
          } else {
            fontAttributes.put(TextAttribute.UNDERLINE, -1);
          }
          toggleButton.setFont(toggleButton.getFont().deriveFont(fontAttributes));
        }
        updateExemplar(c1);
      };
  private final ChangeListener spinnerListener =
      event -> {
        String key = getComponentKey((Component) event.getSource());
        if (key.equalsIgnoreCase("defaultFont")) {
          currentRelativeSize =
              ((SpinnerNumberModel) getComponent(key, JSpinner.class).getModel())
                  .getNumber()
                  .floatValue();
          for (String k : COMPONENT_MAP.keySet()) {
            updateExemplar(COMPONENT_MAP.get(k).getLast());
          }
        } else {
          updateExemplar((JComponent) event.getSource());
        }
      };
  private final ItemListener comboListener =
      item -> {
        if (item.getStateChange() == ItemEvent.SELECTED) {
          updateExemplar((JComponent) item.getSource());
        }
      };

  private final ItemListener enableListener =
      e -> {
        JCheckBox cb = (JCheckBox) e.getSource();
        boolean enabled = cb.isSelected();
        String key = getComponentKey(cb);
        if (key.equalsIgnoreCase("defaultFont")) {
          if (enabled) {
            currentRelativeSize =
                ((SpinnerNumberModel) getComponent(key, JSpinner.class).getModel())
                    .getNumber()
                    .floatValue();
          } else {
            currentRelativeSize = DEFAULT_RELATIVE_SIZE;
          }
        }

        List<JComponent> components = COMPONENT_MAP.get(key);
        for (JComponent c : components) {
          if (!(c instanceof JCheckBox) && !(c instanceof JLabel)) {
            c.setEnabled(enabled);
          }
        }
        updateExemplar(cb);
      };

  @Override
  public boolean commit() {
    String[] keys = FONT_DATA_MAP.keySet().toArray(new String[0]);
    for (int i = keys.length - 1; i > -1; i--) {
      if (!getEnabled.apply(keys[i])) {
        FONT_DATA_MAP.remove(keys[i]);
      } else {
        UIManager.getDefaults().put(keys[i], getComponent(keys[i], FlatLabel.class).getFont());
      }
    }

    boolean write = ThemeTools.writeCustomProperties(FONT_DATA_MAP);
    if (write) {
      MapTool.showMessage(
          "PreferencesDialog.themeChangeWarning",
          "PreferencesDialog.themeChangeWarningTitle",
          JOptionPane.WARNING_MESSAGE);
      FlatLaf.updateUI();
      FlatLaf.revalidateAndRepaintAllFramesAndDialogs();
    }
    return write;
  }
}
