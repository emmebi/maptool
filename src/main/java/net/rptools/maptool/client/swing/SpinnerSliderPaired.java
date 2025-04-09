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
 * have been implemented on the numeric model for the setPairValue method.
 */
public class SpinnerSliderPaired {
  // constructors
  public SpinnerSliderPaired(JSpinner spinner, JSlider slider) {
    this(spinner, slider, null);
  }

  public SpinnerSliderPaired(JSpinner spinner, JSlider slider, Consumer<Number> propertySetter) {
    this(spinner, slider, propertySetter, Number::intValue, (i) -> ((Number) i).doubleValue());
    log.debug("spinner-slider pair using default relationship.");
  }

  public SpinnerSliderPaired(
      JSpinner spinner,
      JSlider slider,
      Consumer<Number> propertySetter,
      Function<Number, Integer> spinnerToSlider,
      Function<Integer, Number> sliderToSpinner) {
    // set functional relationship
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

    log.debug("new spinner-slider pair: " + this);
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
    private JSpinner linkedSpinner;
    private JSlider linkedSlider;
    public JSpinner getLinkedSpinner(){ return linkedSpinner; }
    public JSlider getLinkedSlider(){ return linkedSlider; }
    private void setLinkedSlider(@NotNull JSlider slider){
        this.linkedSlider = slider;
        getLinkedSlider().addMouseWheelListener(
            e -> incrementPairValue((int) Math.round(e.getPreciseWheelRotation())));
    }
    private void setLinkedSpinner(JSpinner spinner){
        this.linkedSpinner = spinner;
        getLinkedSpinner().addChangeListener(spinnerEditListener);
        getLinkedSpinner().addMouseWheelListener(
            e -> incrementPairValue( e.getPreciseWheelRotation()));
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
    public void update(){ if(propertyGetter != null){ setPairValue(propertyGetter.get()); } }
    // Instance of NumericModel that ties the two controls together
    private final NumericModel commonModel = new NumericModel();
    // functions that define the relationship between spinner and slider values
    private Function<Integer, Number> sliderToSpinner;
    private Function<Number, Integer> spinnerToSlider;
    public Function<Number, Integer> getSpinnerToSlider() { return spinnerToSlider; }
    public void setSpinnerToSlider(Function<Number, Integer> function) { spinnerToSlider = function; }
    public Function<Integer, Number> getSliderToSpinner() { return sliderToSpinner; }
    public void setSliderToSpinner(Function<Integer, Number> function) { sliderToSpinner = function; }

    // public methods pointing to model methods
    public Number getPairNextValue(){ return commonModel.getModelNextValue(); }
    public Number getPairPreviousValue(){ return commonModel.getModelPreviousValue(); }
    public int getPairSliderValue(){ return commonModel.getModelNumber(true).intValue(); }
    public double getPairSpinnerValue(){ return commonModel.getModelNumber(false).doubleValue(); }
    public void incrementPairValue(Number n){ commonModel.incrementModelValue(n);}
    public void setPairMaximum(Number n){ commonModel.setModelMaximum(n);}
    public void setPairMinimum(Number n){ commonModel.setModelMinimum(n);}
    public void setPairStepSize(Number n){ commonModel.setModelStepSize(n);}
    public void setPairValue(Number n){ commonModel.setModelValue(n);}
    //@formatter:on
    // spotless:on
  // The shared model - two delegates controlled from above
  private class NumericModel {
    public NumericModel() {}

    // initialise delegates with linked spinner values
    private void setDelegateValues() {
      if (getLinkedSpinner() == null) {
        return;
      }
      if (spinnerToSlider == null) { // default 1:1 relationship
        sliderToSpinner = i -> ((Number) i).doubleValue();
        spinnerToSlider = Number::intValue;
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
    private void incrementModelValue(Number delta) {
      boolean isInt = MathUtil.isInt(delta);
      if (isInt) {
        setModelValue(getModelNumber(true).intValue() + delta.intValue());
      } else {
        setModelValue(getModelNumber(false).doubleValue() + delta.doubleValue());
      }
    }

    private void setModelStepSize(Number n) {
      spinnerModelDelegate.setStepSize(n);
    }

    private void setModelValue(Number n) {
      boolean isInt = MathUtil.isInt(n);
      PropertyChangeEvent pce;
      if (isInt) {
        int current = getModelNumber(true).intValue();
        if (n.intValue() != current) {
          pce = new PropertyChangeEvent(this, "value", current, n);
        } else {
          return;
        }
      } else {
        double curr = getModelNumber(false).doubleValue();
        if (n.doubleValue() != curr) {
          pce = new PropertyChangeEvent(this, "value", curr, n);
        } else {
          return;
        }
      }

      try {
        vcs.fireVetoableChange(pce);
        if (isInt) {
          sliderModelDelegate.setValue(n.intValue());
        } else {
          spinnerModelDelegate.setValue(n);
        }
        pcs.firePropertyChange(pce);
      } catch (PropertyVetoException e) {
        log.info(e.getMessage(), e);
      }
    }

    private void setModelMaximum(Number n) {
      PropertyChangeEvent pce =
          new PropertyChangeEvent(this, "value", getModelNumber(MathUtil.isInt(n)), n);
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

    private void setModelMinimum(Number n) {
      PropertyChangeEvent pce =
          new PropertyChangeEvent(this, "value", getModelNumber(MathUtil.isInt(n)), n);
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

    private Number getModelNextValue() {
      return (Number) spinnerModelDelegate.getNextValue();
    }

    private Number getModelPreviousValue() {
      return (Number) spinnerModelDelegate.getPreviousValue();
    }

    private Number getModelNumber(boolean asInt) {
      if (asInt) {
        return spinnerToSlider.apply(spinnerModelDelegate.getNumber());
      } else {
        return spinnerModelDelegate.getNumber();
      }
    }

    // Delegate classes with additional setters and normal setters redirected
    public NumericSpinnerModel spinnerModelDelegate = new NumericSpinnerModel(0d, 0d, 100d, 1d);

    private class NumericSpinnerModel extends SpinnerNumberModel {
      public NumericSpinnerModel(double value, double minimum, double maximum, double stepSize) {
        super(value, minimum, maximum, stepSize);
      }

      public void setSpinnerMax(@NotNull Number n) {
        if (((Number) this.getMaximum()).doubleValue() != n.doubleValue()) {
          super.setMaximum(n.doubleValue());
          sliderModelDelegate.setMaximum(spinnerToSlider.apply((Number) super.getMaximum()));
        }
      }

      public void setSpinnerMin(@NotNull Number n) {
        if (((Number) this.getMinimum()).doubleValue() != n.doubleValue()) {
          super.setMinimum(n.doubleValue());
          sliderModelDelegate.setMinimum(spinnerToSlider.apply((Number) super.getMinimum()));
        }
      }

      private boolean sentTolider = false;

      public void setSpinnerVal(@NotNull Number n, boolean fromSlider) {
        if (sentTolider && fromSlider) {
          sentTolider = false;
        } else {
          setSpinnerVal(n);
        }
      }

      public void setSpinnerVal(@NotNull Number n) {
        double newVal = n.doubleValue();
        double max = ((Number) getMaximum()).doubleValue();
        double min = ((Number) getMinimum()).doubleValue();
        if (newVal < min && spinnerWraps) {
          newVal = newVal - min + max;
        } else if (newVal > max && spinnerWraps) {
          newVal = newVal - max + min;
        } else {
          newVal = Math.clamp(newVal, min, max);
        }
        if (!MathUtil.inTolerance(newVal, this.getNumber().doubleValue(), 0.01)) {
          super.setValue(n.doubleValue());
          setProperty(n);
          sentTolider = true;
          sliderModelDelegate.setValue(spinnerToSlider.apply(super.getNumber()));
        }
      }

      public void incrSpinner(@NotNull Number n) {
        double newVal = getNumber().doubleValue() + n.doubleValue();
        setValue(newVal);
      }

      @Override
      public Object getNextValue() {
        incrSpinner(getStepSize());
        return getNumber();
      }

      @Override
      public Object getPreviousValue() {
        incrSpinner(-getStepSize().doubleValue());
        return getNumber();
      }

      @Override
      public void setMinimum(Comparable<?> minimum) {
        setSpinnerMin((Number) minimum);
      }

      @Override
      public void setMaximum(Comparable<?> maximum) {
        setSpinnerMax((Number) maximum);
      }

      @Override
      public void setValue(Object value) {
        setSpinnerVal((Number) value);
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

    public NumericSliderModel sliderModelDelegate = new NumericSliderModel(0, 0, 0, 100);

    private class NumericSliderModel extends DefaultBoundedRangeModel {
      public NumericSliderModel(int value, int extent, int min, int max) {
        super(value, extent, min, max);
      }

      @Override
      public void setValue(int n) {
        super.setValue(n);
        spinnerModelDelegate.setSpinnerVal(sliderToSpinner.apply(n).doubleValue(), true);
      }

      @Override
      public void setMinimum(int n) {
        super.setMinimum(n);
        spinnerModelDelegate.setMinimum(sliderToSpinner.apply(n).doubleValue());
      }

      @Override
      public void setMaximum(int n) {
        super.setMaximum(n);
        spinnerModelDelegate.setMaximum(sliderToSpinner.apply(n).doubleValue());
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

  private final ChangeListener spinnerEditListener =
      e -> {
        JSpinner spinner = (JSpinner) e.getSource();
        try {
          spinner.commitEdit();
        } catch (ParseException pe) {
          // Edited value is invalid, revert the spinner to the last valid value,
          JComponent editor = spinner.getEditor();
          if (editor instanceof JSpinner.NumberEditor) {
            ((JSpinner.NumberEditor) editor).getTextField().setValue(spinner.getValue());
          }
        }
      };
}
