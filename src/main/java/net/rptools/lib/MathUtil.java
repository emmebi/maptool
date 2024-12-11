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
package net.rptools.lib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/* Utility class for useful mathematical methods */
public class MathUtil {
  private static final Logger log = LogManager.getLogger(MathUtil.class);

  /**
   * Faster version of absolute for integers
   *
   * @param val
   * @return absolute value of val
   */
  public static int abs(int val) {
    return (val >> 31 ^ val) - (val >> 31);
  }

  /**
   * Faster version of absolute
   *
   * @param val
   * @return absolute value of val
   */
  public static <T extends Number> T abs(T val) {
    return (T) (val.floatValue() < 0 ? -1 * val.doubleValue() : val);
  }

  /**
   * Returns a truncated double with the specified number of decimal places
   *
   * @param value to be truncated
   * @param decimalPlaces number of decimal places to use
   * @return truncated double value
   */
  public static double doublePrecision(double value, int decimalPlaces) {
    double d = Double.parseDouble(String.format("%." + decimalPlaces + "f", value));
    log.debug("value: " + value + ", decimalPlaces: " + decimalPlaces + " -> " + d);
    return d;
  }

  public static boolean isDouble(Object o) {
    return o.getClass().isAssignableFrom(Double.class);
  }

  public static boolean isFloat(Object o) {
    return o.getClass().isAssignableFrom(Float.class);
  }

  public static boolean isInt(Object o) {
    return o.getClass().isAssignableFrom(Integer.class);
  }

  public static boolean isNumber(Object o) {
    return o.getClass().isAssignableFrom(Number.class);
  }

  /**
   * Checks that a value lies within a specified tolerance. Useful for checking if a value is "close
   * enough"
   *
   * @param checkValue to be checked
   * @param referenceValue to be checked against
   * @param tolerance variance allowed
   * @return true if the value is within ± tolerance
   */
  public static boolean inTolerance(double checkValue, double referenceValue, double tolerance) {
    return checkValue <= referenceValue + tolerance && checkValue >= referenceValue - tolerance;
  }

  /**
   * Uses Generics Maps a value in one range to its equivalent in a second range
   *
   * @param valueToMap value in the first range that needs to be converted
   * @param in_min the minimum value for the original range
   * @param in_max the maximum value for the original range
   * @param out_min the minimum value for the target range
   * @param out_max the maximum value for the target range
   * @return the equivalent value of valueToMap in the target range
   * @param <T>
   */
  public static <T extends Number> T mapToRange(
      T valueToMap, T in_min, T in_max, T out_min, T out_max) {
    Number mapValue = (Number) valueToMap;
    Number inMin = (Number) in_min;
    Number inMax = (Number) in_max;
    Number outMin = (Number) out_min;
    Number outMax = (Number) out_max;
    Number result;
    if (isFloat(valueToMap)) {
      result =
          (Number)
              ((mapValue.floatValue() - inMin.floatValue())
                      * (outMax.floatValue() - outMin.floatValue())
                      / (inMax.floatValue() - inMin.floatValue())
                  + outMin.floatValue());
    } else {
      result =
          (Number)
              ((mapValue.doubleValue() - inMin.doubleValue())
                      * (outMax.doubleValue() - outMin.doubleValue())
                      / (inMax.doubleValue() - inMin.doubleValue())
                  + outMin.doubleValue());
    }
    return (T) result;
  }

  /**
   * Constrains an integer between an upper and lower limit
   *
   * @param value
   * @param lowBound
   * @param highBound
   * @return
   */
  public static int constrainInt(int value, int lowBound, int highBound) {
    return Math.max(lowBound, Math.min(highBound, value));
  }

  /**
   * Constrains a Number between an upper and lower limit
   *
   * @param value
   * @param lowBound
   * @param highBound
   * @return
   * @param <T>
   */
  public static <T extends Number> T constrainNumber(T value, T lowBound, T highBound) {
    return (T)
        (Number)
            Math.max(
                lowBound.doubleValue(), Math.min(highBound.doubleValue(), value.doubleValue()));
  }
}
