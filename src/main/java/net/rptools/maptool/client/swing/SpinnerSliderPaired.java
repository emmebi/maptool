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

import java.beans.*;
import java.text.ParseException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;
import net.rptools.lib.MathUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for linking a numeric JSpinner, JSlider, and a variable. The slider and spinner are
 * tied together with the class NumericModel. It holds delegates for the two component models which
 * update each other.The default relationship between spinner value and slider value is 1:1 and
 * utilises a Functional Interface to translate values between the two. This can be customised by
 * setting your own Functions, e.g. an example for 1:100
 *
 * <pre>
 * Function&lt;Number, Integer> spinnerToSlider =
 * number -> ((Number) number.doubleValue * 100).intValue<br>
 * Function&lt;Integer, Number> sliderToSpinner =
 * number -> ((Number) i).doubleValue()/100d
 * </pre>
 *
 * <p>Similarly:<br>
 * Providing a <code>Consumer&lt;Number> </code>for a linked property will result in the property
 * being updated with the spinner value.<br>
 * Providing a <code>Supplier&lt;Number></code> for a linked property can be used to update the
 * spinner value by calling <code>update()</code>. PropertyChangeSupport and VetoableChangeSupport
 * have been implemented on the spinner and slider models for the methods;<br>
 * setMinimum,<br>
 * setMaximum, and<br>
 * setValue.
 */
public class SpinnerSliderPaired {
  // constructors
  public SpinnerSliderPaired(JSpinner spinner, JSlider slider) {
    this(spinner, slider, null);
  }

  public SpinnerSliderPaired(JSpinner spinner, JSlider slider, Consumer<Number> propertySetter) {
    this(spinner, slider, propertySetter, (n) -> n.intValue(), (i) -> ((Number) i).doubleValue());
    log.debug("spinner-slider pair using default relationship.");
  }

  public SpinnerSliderPaired(
      JSpinner spinner,
      JSlider slider,
      Consumer<Number> propertySetter,
      Function<Number, Integer> spinnerToSlider,
      Function<Integer, Number> sliderToSpinner) {
    // set funcitional relationship
    setSpinnerToSlider(spinnerToSlider);
    setSliderToSpinner(sliderToSpinner);
    // set the controls
    setLinkedSpinner(spinner);
    setLinkedSlider(slider);
    // set the property setter
    setPropertySetter(propertySetter);

    commonModel.setDelegateValues();
    getLinkedSlider().setModel(commonModel.sliderModelDelegate);
    getLinkedSpinner().setModel(commonModel.spinnerModelDelegate);

    log.debug("new spinner-slider pair: " + this.toString());
  }

  // @formatter:off
  // spotless:off
    private static final Logger log = LogManager.getLogger(SpinnerSliderPaired.class);
    // Property Change Support
    protected SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    protected VetoableChangeSupport vcs = new VetoableChangeSupport(this);
    public void addVetoableChangeListener(VetoableChangeListener listener) { vcs.addVetoableChangeListener(listener); }
    protected void removeVetoableChangeListener(VetoableChangeListener listener){ vcs.addVetoableChangeListener(listener); }
    public void addPropertyChangeListener(PropertyChangeListener listener) { pcs.addPropertyChangeListener(listener); }
    protected void removePropertyChangeListener(PropertyChangeListener listener) { pcs.removePropertyChangeListener(listener); }

    // controls
    private transient JSpinner linkedSpinner;
    private transient JSlider linkedSlider;
    public JSpinner getLinkedSpinner(){ return linkedSpinner; }
    public JSlider getLinkedSlider(){ return linkedSlider; }
    private void setLinkedSlider(@NotNull JSlider slider){
        this.linkedSlider = slider;
        getLinkedSlider().addMouseWheelListener(
            e -> { incrementValue((int) Math.round(e.getPreciseWheelRotation()));
        });
    }
    private void setLinkedSpinner(JSpinner spinner){
        this.linkedSpinner = spinner;
        getLinkedSpinner().addChangeListener(spinnerEditListener);
        getLinkedSpinner().addMouseWheelListener(
            e -> { incrementValue( e.getPreciseWheelRotation());
        });
    }
    // option to set the spinner to loop/wrap at end values
    private boolean spinnerWraps = false;
    public void setSpinnerWraps(boolean b){ this.spinnerWraps = b; }

    // property setter
    private Consumer<Number> propertySetter;
    private Supplier<Number> propertyGetter;
    private String propertyName = "Property";
    public Consumer<Number> getPropertySetter() { return propertySetter; }
    public Supplier<Number> getPropertyGetter() { return propertyGetter; }
    public void setPropertySetter(Consumer<Number> propertySetter) {this.propertySetter = propertySetter; }
    public void setPropertyGetter(Supplier<Number> propertyGetter) {this.propertyGetter = propertyGetter; }
    public void setPropertyName(String s){ this.propertyName = s; }
    private void setProperty(Number n){
        if(propertyGetter != null){
            if(propertyGetter.get().doubleValue() != n.doubleValue() && propertySetter != null){
                propertySetter.accept(n);
                log.debug(propertyName + " set to " + n);
            }
        }
    }
    public void update(){ if(propertyGetter != null){ setValue(propertyGetter.get()); } }
    // Instance of NumericModel that ties the two controls together
    private NumericModel commonModel = new NumericModel();
    // functions that define the relationship between spinner and slider values
    private Function<Integer, Number> sliderToSpinner;
    private Function<Number, Integer> spinnerToSlider;
    public Function getSpinnerToSlider() { return spinnerToSlider; }
    public void setSpinnerToSlider(Function function) { spinnerToSlider = function; }
    public Function getSliderToSpinner() { return sliderToSpinner; }
    public void setSliderToSpinner(Function function) { sliderToSpinner = function; }

    // public methods pointing to model methods
    public Number getNextValue(){ return commonModel.getNextValue(); }
    public Number getPreviousValue(){ return commonModel.getPreviousValue(); }
    public int getSliderValue(){ return commonModel.getNumber(true).intValue(); }
    public double getSpinnerValue(){ return commonModel.getNumber(false).doubleValue(); }
    public void incrementValue(Number n){ commonModel.incrementValue(n);}
    public void setMaximum(Number n){ commonModel.setMaximum(n);}
    public void setMinimum(Number n){ commonModel.setMinimum(n);}
    public void setValue(Number n){ commonModel.setValue(n);}
    //@formatter:on
    // spotless:on
  // The shared model - two delegates controlled from above
  public class NumericModel {
    public NumericModel() {}

    // initialise delegates with linked spinner values
    private void setDelegateValues() {
      if (getLinkedSpinner() == null) {
        return;
      }
      if (spinnerToSlider == null) { // default 1:1 relationship
        sliderToSpinner = i -> ((Number) i).doubleValue();
        spinnerToSlider = n -> n.intValue();
      }
      SpinnerNumberModel spinModel = (SpinnerNumberModel) getLinkedSpinner().getModel();
      spinnerModelDelegate =
          new NumericSpinnerModel(
              spinModel.getNumber().doubleValue(),
              ((Number) spinModel.getMinimum()).doubleValue(),
              ((Number) spinModel.getMaximum()).doubleValue(),
              spinModel.getStepSize().doubleValue());
      sliderModelDelegate =
          new NumericSliderModel(
              spinnerToSlider.apply(spinnerModelDelegate.getNumber()),
              0,
              spinnerToSlider.apply(((Number) spinnerModelDelegate.getMinimum())),
              spinnerToSlider.apply(((Number) spinnerModelDelegate.getMaximum())));
    }

    // a method for every flavour
    private void incrementValue(Number delta) {
      PropertyChangeEvent pce = new PropertyChangeEvent(this, "increment", getNumber(false), delta);
      try {
        vcs.fireVetoableChange(pce);
        spinnerModelDelegate.increment(delta);
        pcs.firePropertyChange(pce);
      } catch (PropertyVetoException e) {
        log.info(e.getMessage(), e);
      }
    }

    private void setStepSize(Number n) {
      PropertyChangeEvent pce = new PropertyChangeEvent(this, "stepsize", getNumber(false), n);
      try {
        vcs.fireVetoableChange(pce);
        spinnerModelDelegate.setStepSize(n);
        pcs.firePropertyChange(pce);
      } catch (PropertyVetoException e) {
        log.info(e.getMessage(), e);
      }
    }

    private void setValue(Number n) {
      PropertyChangeEvent pce =
          new PropertyChangeEvent(this, "value", getNumber(MathUtil.isInt(n)), n);
      try {
        vcs.fireVetoableChange(pce);
        if (MathUtil.isInt(n)) {
          sliderModelDelegate.setValue(n.intValue());
        } else {
          spinnerModelDelegate.setValue(n);
        }
        pcs.firePropertyChange(pce);
      } catch (PropertyVetoException e) {
        log.info(e.getMessage(), e);
      }
    }

    private void setMaximum(Number n) {
      PropertyChangeEvent pce =
          new PropertyChangeEvent(this, "value", getNumber(MathUtil.isInt(n)), n);
      try {
        vcs.fireVetoableChange(pce);
        if (MathUtil.isInt(n)) {
          sliderModelDelegate.setMaximum(n.intValue());
        } else {
          spinnerModelDelegate.setMaximum((Comparable<?>) n);
        }
        pcs.firePropertyChange(pce);
      } catch (PropertyVetoException e) {
        log.info(e.getMessage(), e);
      }
    }

    private void setMinimum(Number n) {
      PropertyChangeEvent pce =
          new PropertyChangeEvent(this, "value", getNumber(MathUtil.isInt(n)), n);
      try {
        vcs.fireVetoableChange(pce);
        if (MathUtil.isInt(n)) {
          sliderModelDelegate.setMinimum(n.intValue());
        } else {
          spinnerModelDelegate.setMinimum((Comparable<?>) n);
        }
        pcs.firePropertyChange(pce);
      } catch (PropertyVetoException e) {
        log.info(e.getMessage(), e);
      }
    }

    private Number getNextValue() {
      return (Number) spinnerModelDelegate.getNextValue();
    }

    private Number getPreviousValue() {
      return (Number) spinnerModelDelegate.getPreviousValue();
    }

    private <T extends Number> T getNumber(boolean asInt) {
      if (asInt) {
        return (T) spinnerToSlider.apply(spinnerModelDelegate.getNumber());
      } else {
        return (T) spinnerModelDelegate.getNumber();
      }
    }

    // Delegate classes with aditional setters and normal setters redirected
    public NumericSpinnerModel spinnerModelDelegate = new NumericSpinnerModel(0d, 0d, 100d, 1d);

    private class NumericSpinnerModel extends SpinnerNumberModel {
      public NumericSpinnerModel(double value, double minimum, double maximum, double stepSize) {
        super(value, minimum, maximum, stepSize);
      }

      public void setMax(@NotNull Number n) {
        super.setMaximum(n.doubleValue());
        sliderModelDelegate.setMaximum(spinnerToSlider.apply((Number) super.getMaximum()));
      }

      public void setMin(@NotNull Number n) {
        super.setMinimum(n.doubleValue());
        sliderModelDelegate.setMinimum(spinnerToSlider.apply((Number) super.getMinimum()));
      }

      public void setVal(@NotNull Number n) {
        super.setValue(n.doubleValue());
        setProperty(n);
        sliderModelDelegate.setValue(spinnerToSlider.apply(super.getNumber()));
      }

      public void setStep(@NotNull Number n) {
        super.setStepSize(n);
      }

      public void incr(@NotNull Number n) {
        double newVal = getNumber().doubleValue() + n.doubleValue();
        double max = ((Number) getMaximum()).doubleValue();
        double min = ((Number) getMinimum()).doubleValue();
        if (spinnerWraps && (newVal < min || newVal > max)) {
          if (newVal < min) {
            newVal = newVal - min + max;
          } else {
            newVal = newVal - max + min;
          }
        } else {
          newVal = Math.min(max, Math.max(newVal, min));
        }
        setValue(newVal);
      }

      public void increment(@NotNull Number n) {
        n = MathUtil.doublePrecision(n.doubleValue(), 4);
        try {
          PropertyChangeEvent pce =
              new PropertyChangeEvent(this, "increment", super.getNumber(), n);
          vcs.fireVetoableChange(pce);
          incr(n);
          pcs.firePropertyChange(pce);
        } catch (PropertyVetoException e) {
          log.info(e.getMessage());
        }
      }

      @Override
      public Object getNextValue() {
        try {
          PropertyChangeEvent pce =
              new PropertyChangeEvent(this, "increment", super.getNumber(), super.getStepSize());
          vcs.fireVetoableChange(pce);
          incr(getStepSize());
          pcs.firePropertyChange(pce);
        } catch (PropertyVetoException e) {
          log.info(e.getMessage());
        }
        return getNumber();
      }

      @Override
      public Object getPreviousValue() {
        try {
          PropertyChangeEvent pce =
              new PropertyChangeEvent(this, "increment", super.getNumber(), super.getStepSize());
          vcs.fireVetoableChange(pce);
          incr(-getStepSize().doubleValue());
          pcs.firePropertyChange(pce);
        } catch (PropertyVetoException e) {
          log.info(e.getMessage());
        }
        return super.getNumber();
      }

      @Override
      public void setMinimum(Comparable<?> minimum) {
        minimum = MathUtil.doublePrecision(((Number) minimum).doubleValue(), 4);
        if (((Number) this.getMinimum()).doubleValue() != ((Number) minimum).doubleValue()) {
          try {
            PropertyChangeEvent pce =
                new PropertyChangeEvent(this, "minimum", super.getNumber(), minimum);
            vcs.fireVetoableChange(pce);
            setMin((Number) minimum);
            pcs.firePropertyChange(pce);
          } catch (PropertyVetoException e) {
            log.info(e.getMessage());
          }
        }
      }

      @Override
      public void setMaximum(Comparable<?> maximum) {
        maximum = MathUtil.doublePrecision(((Number) maximum).doubleValue(), 4);
        if (((Number) this.getMaximum()).doubleValue() != ((Number) maximum).doubleValue()) {
          try {
            PropertyChangeEvent pce =
                new PropertyChangeEvent(this, "maximum", super.getNumber(), maximum);
            vcs.fireVetoableChange(pce);
            setMax((Number) maximum);
            pcs.firePropertyChange(pce);
          } catch (PropertyVetoException e) {
            log.info(e.getMessage());
          }
        }
      }

      @Override
      public void setValue(Object value) {
        value = MathUtil.doublePrecision(((Number) value).doubleValue(), 4);
        if (this.getNumber().doubleValue() != ((Number) value).doubleValue()) {
          try {
            PropertyChangeEvent pce =
                new PropertyChangeEvent(this, "value", super.getNumber(), value);
            vcs.fireVetoableChange(pce);
            setVal((Number) value);
            pcs.firePropertyChange(pce);
          } catch (PropertyVetoException e) {
            log.info(e.getMessage());
          }
        }
      }

      @Override
      public String toString() {
        return "spinnerModelDelegate{"
            + "min="
            + getMinimum()
            + ", max="
            + getMaximum()
            + ", val="
            + getValue()
            + ", stepSize="
            + getStepSize()
            + '}';
      }
    }
    ;

    public NumericSliderModel sliderModelDelegate = new NumericSliderModel(0, 0, 0, 100);

    private class NumericSliderModel extends DefaultBoundedRangeModel {
      public NumericSliderModel(int value, int extent, int min, int max) {
        super(value, extent, min, max);
      }

      public void setMax(@NotNull Number n) {
        super.setMaximum(n.intValue());
        spinnerModelDelegate.setMaximum(sliderToSpinner.apply(n.intValue()).doubleValue());
      }

      public void setMin(@NotNull Number n) {
        super.setMinimum(n.intValue());
        spinnerModelDelegate.setMinimum(sliderToSpinner.apply(n.intValue()).doubleValue());
      }

      public void setVal(@NotNull Number n) {
        super.setValue(n.intValue());
        spinnerModelDelegate.setValue(sliderToSpinner.apply(n.intValue()).doubleValue());
      }

      @Override
      public void setValue(int n) {
        if (this.getValue() != n) {
          try {
            PropertyChangeEvent pce = new PropertyChangeEvent(this, "value", super.getValue(), n);
            vcs.fireVetoableChange(pce);
            setVal((Number) n);
            pcs.firePropertyChange(pce);
          } catch (PropertyVetoException e) {
            log.info(e.getMessage());
          }
        }
      }

      @Override
      public void setMinimum(int n) {
        if (this.getValue() != n) {
          try {
            PropertyChangeEvent pce = new PropertyChangeEvent(this, "value", super.getMinimum(), n);
            vcs.fireVetoableChange(pce);
            setMin((Number) n);
            pcs.firePropertyChange(pce);
          } catch (PropertyVetoException e) {
            log.info(e.getMessage());
          }
        }
      }

      @Override
      public void setMaximum(int n) {
        if (this.getValue() != n) {
          try {
            PropertyChangeEvent pce = new PropertyChangeEvent(this, "value", super.getMaximum(), n);
            vcs.fireVetoableChange(pce);
            setMax((Number) n);
            pcs.firePropertyChange(pce);
          } catch (PropertyVetoException e) {
            log.info(e.getMessage());
          }
        }
      }

      @Override
      public String toString() {
        return "sliderModelDelegate{"
            + "min="
            + super.getMinimum()
            + ", max="
            + super.getMaximum()
            + ", val="
            + super.getValue()
            + ", extent="
            + super.getExtent()
            + ", isAdjusting="
            + super.getValueIsAdjusting()
            + '}';
      }
    }
    ;

    // spotless:on
    // @formatter:on
    @Override
    public String toString() {
      return "NumericModel{"
          + spinnerModelDelegate.toString()
          + ", "
          + sliderModelDelegate.toString()
          + '}';
    }
  }

  @Override
  public String toString() {
    return "SpinnerSliderPaired{"
        + "spinnerName="
        + getLinkedSpinner().getName()
        + ", sliderName="
        + getLinkedSlider().getName()
        + ", propertySetterSet="
        + (propertySetter != null)
        + ", sliderToSpinner(0)="
        + sliderToSpinner.apply(0)
        + ", sliderToSpinner(1)="
        + sliderToSpinner.apply(1)
        + ", spinnerToSlider(0)="
        + spinnerToSlider.apply(0)
        + ", spinnerToSlider(1)="
        + spinnerToSlider.apply(1)
        + ", spinnerToSlider(0)="
        + spinnerToSlider.apply(0)
        + ", sliderToSpinner("
        + spinnerToSlider.apply(0)
        + ")="
        + sliderToSpinner.apply(spinnerToSlider.apply(0))
        + ", spinnerToSlider(1)="
        + spinnerToSlider.apply(1)
        + ", sliderToSpinner("
        + spinnerToSlider.apply(1)
        + ")="
        + sliderToSpinner.apply(spinnerToSlider.apply(1))
        + '}';
  }

  private ChangeListener spinnerEditListener =
      new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
          JSpinner spinner = (JSpinner) e.getSource();
          try {
            spinner.commitEdit();
          } catch (ParseException pe) {
            // Edited value is invalid, revert the spinner to the last valid value,
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.NumberEditor) {
              ((JSpinner.NumberEditor) editor).getTextField().setValue(spinner.getValue());
            }
            return;
          }
        }
      };
}
