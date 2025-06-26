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
import net.rptools.maptool.model.Asset;
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

    /** The content of the directive. */
    private final String content;
  }

  /** Content Security Policy (CSP) for the HTML content. */
  private static final String META_CSP_CONTENT =
      Arrays.stream(CSPContentDirective.values())
          .map(CSPContentDirective::getContent)
          .collect(Collectors.joining(" "));

  /** The enumeration that represents the type of content. */
  private enum ContentType {
    /** The content is a string, possibly HTML. */
    STRING,
    /** HTML Content in a string */
    HTML,
    /** URL that points to the content. */
    URL,
    /** The asset that represents the content if it is a binary asset. */
    ASSET
  }

  /**
   * The content type that this object represents. This is used to determine how to handle the
   * content.
   *
   * @param str content as a string.
   * @param url URL that points to the content.
   * @param asset The asset that represents the content if it is a binary asset.
   * @param type The type of content.
   */
  private record Content(
      @Nullable String str, @Nullable URL url, @Nullable Asset asset, @Nonnull ContentType type) {

    /**
     * Constructor for the Content class if it is a string containing html.
     *
     * @param str content as a string.
     * @param isHtml true if the content is HTML, false otherwise or unknown.
     */
    public Content(@Nonnull String str, boolean isHtml) {
      this(str, null, null, isHtml ? ContentType.HTML : ContentType.STRING);
    }

    /**
     * Constructor for the Content class if it is a URL.
     *
     * @param url URL that points to the content.
     */
    public Content(@Nonnull URL url) {
      this(null, url, null, ContentType.URL);
    }

    /**
     * Constructor for the Content class if it is a binary asset.
     *
     * @param asset The asset that represents the content if it is a binary asset.
     */
    public Content(@Nonnull Asset asset) {
      this(null, null, asset, ContentType.ASSET);
    }
  }

  /** The content represented. */
  Content content;

  /** Flag to indicate if the JavaBridge has been injected. */
  private final boolean javaBridgeInjected;

  /** Flag to indicate if the base URL has been injected. */
  private final boolean baseUrlInjected;

  /**
   * Constructor for the HTMLContent class.
   *
   * @param content The content represented.
   * @param javaBridgeInjected flag to indicate if the Java Bridge has been injected
   * @param baseUrlInjected flag to indicate if the base URL has been injected
   */
  private HTMLContent(Content content, boolean javaBridgeInjected, boolean baseUrlInjected) {
    this.content = content;
    this.javaBridgeInjected = javaBridgeInjected;
    this.baseUrlInjected = baseUrlInjected;
  }

  /**
   * Factory method to create an HTMLContent object from a string that is HTML.
   *
   * @param html the String content to be displayed
   * @return an HTMLContent object
   */
  public static HTMLContent htmlFromString(@Nonnull String html) {
    return new HTMLContent(new Content(html, true), false, false);
  }

  /**
   * Factory method to create an HTMLContent object from a URL.
   *
   * @param url the URL of the HTML content
   * @return an HTMLContent object
   */
  public static HTMLContent fromURL(@Nonnull URL url) {
    return new HTMLContent(new Content(url), false, false);
  }

  /**
   * Checks if the HTML content is a URL.
   *
   * @return true if the HTML content is a URL, false otherwise
   */
  public boolean isUrl() {
    return content.type == ContentType.URL;
  }

  /**
   * Checks if the HTML content is a string.
   *
   * @return true if the HTML content is a string, false otherwise
   */
  public boolean isHTMLString() {
    return content.type == ContentType.HTML;
  }

  /**
   * Returns the HTML content as a string. If the content is a URL it will return null.
   *
   * @return the HTML content as a string
   */
  public String getHtmlString() {
    return content.str;
  }

  /**
   * Returns the URL of the HTML content. If the content is a string it will return null.
   *
   * @return the URL of the HTML content
   */
  public URL getUrl() {
    return content.url;
  }

  public boolean isBinaryAsset() {
    return content.type == ContentType.ASSET;
  }

  public Asset getAsset() {
    return content.asset;
  }

  /**
   * Returns the HTML content as a data URL in base64 format.
   *
   * @return the HTML content as a data URL in base64 format.
   */
  public String getHtmlStringAsDataUrl() {
    if (isHTMLString()) {
      String encodedHtml =
          Base64.getEncoder().encodeToString(content.str.getBytes(StandardCharsets.UTF_8));
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
    if (!isHTMLString()) {
      throw new IllegalStateException("HTMLContent is not a string");
    }

    var document = Jsoup.parse(content.str);
    var head = document.select("head").first();
    if (head != null) {
      addBase(document, baseUrl);
    }

    return new HTMLContent(new Content(document.html(), true), javaBridgeInjected, true);
  }

  /**
   * Injects the Java Bridge into the HTML content if it is a string. If the content is a URL it
   * will return the same object without any changes. If the content has already been injected it
   * will return the same object.
   *
   * <p>If the content is a URL and it is not a HTML page, the Java Bridge will not be injected.
   *
   * @return the HTMLContent object with the Java Bridge injected.
   */
  public HTMLContent injectJavaBridge() {
    if (!isHTMLString() || javaBridgeInjected) {
      return this; // No need to do anything
    }

    if (!isHTMLString()) {
      throw new IllegalStateException("HTMLContent is not a string");
    }
    var newHtml = content.str;
    var document = Jsoup.parse(newHtml);
    var head = document.select("head").first();
    if (head != null) {
      addCSP(head);
      addJavaBridge(head);

      newHtml = document.html();
    }
    return new HTMLContent(new Content(newHtml, true), true, true);
  }

  /**
   * Fetches the content from the URL.
   *
   * @return an HTMLContent object containing the HTML content.
   * @throws IOException if an error occurs while fetching the HTML content from the URL.
   * @throws IllegalStateException if the content is not a URL.
   */
  public HTMLContent fetchContent() throws IOException {
    if (!isUrl()) {
      throw new IllegalStateException("HTMLContent is not a URL");
    }
    try {
      Optional<Library> libraryOpt = new LibraryManager().getLibrary(content.url).get();
      if (libraryOpt.isEmpty()) {
        throw new IOException(
            I18N.getText("msg.error.html.loadingURL", content.url.toExternalForm()));
      }

      var library = libraryOpt.get();
      var assetKey = library.getAssetKey(content.url).get().orElse(null);
      // Check if the asset key is null, if so try reading the resource as a string from the
      // library
      if (assetKey == null) {
        String html = library.readAsString(content.url).get();
        if (html != null) {
          var mediaType = Asset.getMediaType("", html.getBytes(StandardCharsets.UTF_8));
          var assetType = Asset.Type.fromMediaType(mediaType);
          return new HTMLContent(new Content(html, assetType == Asset.Type.HTML), false, false);
        }
      }

      var asset = AssetManager.getAsset(assetKey);
      if (asset != null) {
        if (asset.isStringAsset()) {
          return new HTMLContent(
              new Content(asset.getDataAsString(), asset.getType() == Asset.Type.HTML),
              false,
              false);
        } else {
          return new HTMLContent(new Content(asset), false, false);
        }
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e);
    }
    throw new IOException(I18N.getText("msg.error.html.loadingURL", content.url.toExternalForm()));
  }

  /**
   * Fetches the string content from the URL or returns the HTML string if the content is a string.
   *
   * @return a string containing the HTML content.
   * @throws IOException if an error occurs while fetching the HTML string from the URL.
   * @throws IllegalStateException if the URL points to a binary asset.
   */
  public String fetchString() throws IOException {
    if (isHTMLString()) {
      return content.str;
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
