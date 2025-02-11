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

public final class OsDetection {

  /** Returns true if currently running on a Windows based operating system. */
  public static boolean WINDOWS =
      (System.getProperty("os.name").toLowerCase().startsWith("windows"));

  /** Returns true if currently running on a Mac OS X based operating system. */
  public static boolean MAC_OS_X =
      (System.getProperty("os.name").toLowerCase().startsWith("mac os x"));

  /** Returns true if currently running on Linux or other Unix/Unix like system. */
  public static boolean LINUX_OR_UNIX =
      (System.getProperty("os.name").indexOf("nix") >= 0
          || System.getProperty("os.name").indexOf("nux") >= 0
          || System.getProperty("os.name").indexOf("aix") >= 0
          || System.getProperty("os.name").indexOf("sunos") >= 0);

  private OsDetection() {
    throw new RuntimeException("OsSupport is a static class");
  }
}
