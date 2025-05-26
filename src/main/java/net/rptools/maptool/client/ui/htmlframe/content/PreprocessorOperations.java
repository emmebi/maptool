package net.rptools.maptool.client.ui.htmlframe.content;

import javax.annotation.Nonnull;
import org.jsoup.nodes.Document;

/**
 * This interface defines a contract for preprocessor operations that can be applied to HTML
 * documents.
 */
public interface PreprocessorOperations {

  /**
   * Applies the preprocessor operation to the given document.
   *
   * @param document The document to which the preprocessor operation will be applied.
   */
  void apply(@Nonnull Document document);

  /**
   * Returns priority of the preprocessor operation. The priority determines the order in which
   * operations are applied, lower values get applied first. If there is no specific priority,
   * return 0.
   */
  public int getPriority();
}
