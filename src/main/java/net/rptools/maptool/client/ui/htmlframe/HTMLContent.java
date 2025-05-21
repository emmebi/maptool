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
package net.rptools.maptool.client.ui.htmlframe;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.rptools.maptool.client.ui.htmlframe.HTMLWebViewManager.JavaBridge;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.model.AssetManager;
import net.rptools.maptool.model.library.Library;
import net.rptools.maptool.model.library.LibraryManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

/**
 * Class that represents the HTML content to be displayed in the HTML pane.
 *
 * <p>This class is can also used to inject the Java bridge and the base URL into the HTML content
 * if needed.
 *
 * <p>This class is immutable and thread-safe.
 */
public class HTMLContent {

  /** Magic value that window.status must take to initiate the bridge. */
  public static final String BRIDGE_VALUE = "MY_INITIALIZING_VALUE";

  /** JS to initialize the Java bridge. Needs to be the first script of the page. */
  private static final String SCRIPT_BRIDGE =
      String.format("window.status = '%s'; window.status = '';", JavaBridge.BRIDGE_VALUE);

  /**
   * Enum that contains the content security policy directives. This is not strictly required but it
   * is better than having them in a long string as it is easier to read, maintain and document.
   */
  public enum CSPContentDirective {
    /**
     * The default-src directive is used to specify the default policy for fetching resources such
     * as JavaScript, CSS, images, fonts, AJAX requests, frames, and HTML5 media.
     */
    DEFAULT_SRC("default-src"),
    /** asset:// URL scheme */
    ASSET("asset:"),
    /** lib:// URL scheme */
    LIB("lib:"),
    /** JQuery Content Delivery Network */
    JQUERY_CDN("https://code.jquery.com"),
    /** JSDelier Content Delivery Network (bootstrap, FontAwesome, Bootswatch, Bootstrap Icons) */
    JSDELIVR_CDN("https://cdn.jsdelivr.net"),
    /** UNPKG Content Delivery Network (React, ReactDOM, React Router, etc.) */
    UNPKG_CDN("https://unpkg.com"),
    /** Cloudflare Content Delivery Network */
    CLOUDFLARE_JS_CDN("https://cdnjs.cloudflare.com"),
    /** AJAX Content Delivery Network */
    AJAX_CDN("https://ajax.googleapis.com"),
    /** Google Hosted Libraries Content Delivery Network */
    FONTS_CDN("https://fonts.googleapis.com https://fonts.gstatic.com"),
    /** Inline content */
    INLINE("'unsafe-inline'"),
    /** Evaluate content */
    EVAL("'unsafe-eval'"),
    /** Terminator for the default-src directive */
    DEFAULT_TERMINATOR(";"),
    /** Image Source Policy */
    IMG_SRC("img-src"),
    /** Image Source Policy for all images */
    IMG_ALL("*"),
    /** Image Source Policy for all images from the asset:// URL scheme */
    IMG_ASSET("asset:"),
    /** Image Source Policy for all images from the lib:// URL scheme */
    IMG_LIB("lib:"),
    /** Terminator for the img-src directive */
    IMG_TERMINATOR(";"),
    /** Font Source Policy */
    FONT_SRC("font-src"),
    /** Google Fonts */
    FONT_GOOGLE("https://fonts.gstatic.com"),
    /** Self */
    FONT_SELF("'self'");

    /**
     * Constructor for the CSPContentDirective enum.
     *
     * @param content the content of the directive
     */
    CSPContentDirective(String content) {
      this.content = content;
    }

    /**
     * Returns the content of the directive.
     *
     * @return the content of the directive
     */
    public String getContent() {
      return content;
    }

    /**
     * Returns the content of the directive.
     *
     * @return the content of the directive
     */
    private final String content;
  }

  /** Content Security Policy (CSP) for the HTML content. */
  private static final String META_CSP_CONTENT =
      Arrays.stream(CSPContentDirective.values())
          .map(CSPContentDirective::getContent)
          .collect(Collectors.joining(" "));

  /** The HTML content to be displayed. */
  @Nullable private final String htmlString;

  /** The URL of the HTML content. */
  @Nullable private final URL url;

  /** Flag to indicate if the JavaBridge has been injected. */
  private final boolean javaBridgeInjected;

  /** Flag to indicate if the base URL has been injected. */
  private final boolean baseUrlInjected;

  /**
   * Constructor for the HTMLContent class.
   *
   * @param htmlString the HTML content to be displayed
   * @param url the URL of the HTML content
   * @param javaBridgeInjected flag to indicate if the Java Bridge has been injected
   * @param baseUrlInjected flag to indicate if the base URL has been injected
   */
  private HTMLContent(
      @Nullable String htmlString,
      @Nullable URL url,
      boolean javaBridgeInjected,
      boolean baseUrlInjected) {
    this.htmlString = htmlString;
    this.url = url;
    this.javaBridgeInjected = javaBridgeInjected;
    this.baseUrlInjected = baseUrlInjected;
  }

  /**
   * Factory method to create an HTMLContent object from a string.
   *
   * @param html the HTML content to be displayed
   * @return an HTMLContent object
   */
  public static HTMLContent fromString(@Nonnull String html) {
    return new HTMLContent(html, null, false, false);
  }

  /**
   * Factory method to create an HTMLContent object from a URL.
   *
   * @param url the URL of the HTML content
   * @return an HTMLContent object
   */
  public static HTMLContent fromURL(@Nonnull URL url) {
    return new HTMLContent(null, url, false, false);
  }

  /**
   * Checks if the HTML content is a URL.
   *
   * @return true if the HTML content is a URL, false otherwise
   */
  public boolean isUrl() {
    return url != null;
  }

  /**
   * Checks if the HTML content is a string.
   *
   * @return true if the HTML content is a string, false otherwise
   */
  public boolean isHTMLString() {
    return htmlString != null;
  }

  /**
   * Returns the HTML content as a string. If the content is a URL it will return null.
   *
   * @return the HTML content as a string
   */
  public String getHtmlString() {
    return htmlString;
  }

  /**
   * Returns the URL of the HTML content. If the content is a string it will return null.
   *
   * @return the URL of the HTML content
   */
  public URL getUrl() {
    return url;
  }

  /**
   * Returns the HTML content as a data URL in base64 format.
   *
   * @return the HTML content as a data URL in base64 format.
   */
  public String getHtmlStringAsDataUrl() {
    if (isHTMLString()) {
      String encodedHtml =
          Base64.getEncoder().encodeToString(htmlString.getBytes(StandardCharsets.UTF_8));
      return "data:text/html;base64," + encodedHtml;
    } else {
      throw new IllegalStateException("HTMLContent is not a string");
    }
  }

  /**
   * Injects the base URL tag into the HTML content if it is a string.
   *
   * @return the HTMLContent object with the base URL injected.
   */
  public HTMLContent injectURLBase(@Nonnull URL baseUrl) {
    if (baseUrlInjected) {
      return this; // already injected so return the same object
    }
    if (isHTMLString()) {
      return new HTMLContent(injectURLBase(htmlString, baseUrl), url, javaBridgeInjected, true);
    } else {
      throw new IllegalStateException("HTMLContent is not a string");
    }
  }

  /**
   * Parses the HTML in the string and sets the base to the correct location.
   *
   * @param htmlString the HTML to parse.
   * @param url the origin URL to set the base relative to
   * @return a string containing the HTML with the base URL injected.
   */
  public String injectURLBase(@Nonnull String htmlString, @Nonnull URL url) {
    var document = Jsoup.parse(htmlString);
    var head = document.select("head").first();
    if (head != null) {
      addBase(document, url);
    }
    return document.html();
  }

  /**
   * Injects the Java Bridge and the base URL into the HTML content if it is a string. If the
   * content is a URL it will return the same object without any changes. If the content has already
   * been injected it will return the same object.
   *
   * <p>If url is null, the base URL will not be injected.
   *
   * @param url the URL of the HTML content
   * @return the HTMLContent object with the Java Bridge ad base URL injected.
   */
  public HTMLContent injectJavaBridgeAndBaseUrl(@Nullable URL url) {
    if (javaBridgeInjected && baseUrlInjected) {
      return this; // both already injected so return the same object
    }
    var newHtml = htmlString;
    var document = Jsoup.parse(newHtml);
    var head = document.select("head").first();
    if (head != null) {
      if (!baseUrlInjected && url != null) {
        addBase(head, url);
      }

      if (!javaBridgeInjected) {
        addCSP(head);
        addJavaBridge(head);
      }

      newHtml = document.html();
    }
    return HTMLContent.fromString(newHtml);
  }

  /**
   * Injects the Java Bridge into the HTML content if it is a string. If the content is a URL it
   * will return the same object without any changes. If the content has already been injected it
   * will return the same object.
   *
   * @return the HTMLContent object with the Java Bridge injected.
   */
  public HTMLContent injectJavaBridge() {
    if (isUrl() || javaBridgeInjected) {
      return this; // already injected so return the same object
    }

    return injectJavaBridgeAndBaseUrl(null);
  }

  /**
   * Fetches the HTML content from the URL.
   *
   * @return an HTMLContent object containing the HTML content.
   * @throws IOException if an error occurs while fetching the HTML content from the URL.
   * @throws IllegalStateException if the HTML content is a string.
   */
  public HTMLContent fetchContent() throws IOException {
    if (isHTMLString()) {
      throw new IllegalStateException("HTMLContent is not a URL");
    }
    try {
      Optional<Library> libraryOpt = new LibraryManager().getLibrary(url).get();
      if (libraryOpt.isEmpty()) {
        throw new IOException(I18N.getText("msg.error.html.loadingURL", url.toExternalForm()));
      }

      var library = libraryOpt.get();
      var assetKey = library.getAssetKey(url).get().orElse(null);
      // Check if the asset key is null, if so try reading the resource as a string from the
      // library
      if (assetKey == null) {
        String html = library.readAsString(url).get();
        if (html != null) {
          return fromString(html);
        }
      }

      var asset = AssetManager.getAsset(assetKey);
      if (asset != null) {
        return fromString(asset.getDataAsString());
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e);
    }
    throw new IOException("Unable to read location " + url.toExternalForm());
  }

  /**
   * Fetches the HTML string from the URL or returns the HTML string if the content is a string.
   *
   * @return a string containing the HTML content.
   * @throws IOException if an error occurs while fetching the HTML string from the URL.
   */
  public String fetchHTMLString() throws IOException {
    if (isHTMLString()) {
      return htmlString;
    } else {
      return fetchContent().getHtmlString();
    }
  }

  /**
   * Adds the Java bridge to the HTML content.
   *
   * @param head the head element of the HTML document
   */
  private void addJavaBridge(@Nonnull Element head) {
    var javaBridgeKludge =
        new Element(Tag.valueOf("script"), "")
            .attr("type", "text/javascript")
            .appendChild(new DataNode(SCRIPT_BRIDGE));
    if (head.children().isEmpty()) {
      head.appendChild(javaBridgeKludge);
    } else {
      head.child(0).before(javaBridgeKludge);
    }
  }

  /**
   * Adds the content security policy to the HTML content.
   *
   * @param head the head element of the HTML document
   */
  private void addCSP(@Nonnull Element head) {
    var cspElement =
        new Element(Tag.valueOf("meta"), "").attr("http-equiv", "Content-Security-Policy");
    cspElement.attr("content", META_CSP_CONTENT);
    if (head.children().isEmpty()) {
      head.appendChild(cspElement);
    } else {
      head.child(0).before(cspElement);
    }
    head.appendChild(cspElement);
  }

  /**
   * Adds the base URL to the HTML content.
   *
   * @param head the head element of the HTML document
   * @param url the URL of the HTML content
   */
  private void addBase(@Nonnull Element head, @Nonnull URL url) {
    String baseURL = url.toExternalForm().replaceFirst("\\?.*", "");
    baseURL = baseURL.substring(0, baseURL.lastIndexOf("/") + 1);
    var baseElement = new Element(Tag.valueOf("base"), "").attr("href", baseURL);
    if (head.children().isEmpty()) {
      head.appendChild(baseElement);
    } else {
      head.child(0).before(baseElement);
    }
  }
}
