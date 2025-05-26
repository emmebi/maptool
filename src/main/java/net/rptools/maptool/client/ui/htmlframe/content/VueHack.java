package net.rptools.maptool.client.ui.htmlframe.content;

import javax.annotation.Nonnull;
import org.jsoup.nodes.Document;

/**
 * This class is a workaround for Vue.js applications that don't work correclty with our setup to
 * move all script tags to the end of the body.
 */
public class VueHack implements PreprocessorOperations {
  @Override
  public void apply(@Nonnull Document document) {
    // Move all script tags to the end of the body
    var body = document.select("body").first();
    if (body == null) {
      body = document.appendElement("body");
    }
    var scripts = document.select("script");
    for (var script : scripts) {
      // Remove the script from its current position
      script.remove();
      // Append it to the end of the body
      body.appendChild(script);
    }
  }

  @Override
  public int getPriority() {
    return 0;
  }
}
