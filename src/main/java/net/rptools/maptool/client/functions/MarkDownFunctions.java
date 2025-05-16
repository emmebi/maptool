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
package net.rptools.maptool.client.functions;

import com.vladsch.flexmark.ext.aside.AsideExtension;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.SubscriptExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.ins.InsExtension;
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.util.FunctionUtil;
import net.rptools.parser.Parser;
import net.rptools.parser.ParserException;
import net.rptools.parser.VariableResolver;
import net.rptools.parser.function.AbstractFunction;

/*
 * This software copyright by various authors including the RPTools.net development team, and licensed under the LGPL Version 3 or, at your option, any later version.
 *
 * Portions of this software were originally covered under the Apache Software License, Version 1.1 or Version 2.0.
 *
 * See the file LICENSE elsewhere in this distribution for license details.
 */
public class MarkDownFunctions extends AbstractFunction {

  /** The prefix used for MarkDown types in MTS Script. */
  public static final String MARKDOWN_PREFIX = "markdown.type.";

  /** Creates a new {@code MarkDownFunctions} instance. */
  public MarkDownFunctions() {
    super(0, 3, "markdownToHTML");
  }

  @Override
  public Object childEvaluate(
      Parser parser, VariableResolver resolver, String functionName, List<Object> args)
      throws ParserException {
    FunctionUtil.checkNumberParam(functionName, args, 1, 3);

    String markdownText = args.get(0).toString();
    ParserEmulationProfile profile =
        (args.size() > 1)
            ? getParserType(args.get(1).toString())
            : ParserEmulationProfile.GITHUB_DOC;
    String optionalExtensions = (args.size() > 2) ? args.get(2).toString() : "";

    List<Extension> extensions = new ArrayList<>();
    MutableDataHolder options = new MutableDataSet();

    if (profile == ParserEmulationProfile.GITHUB_DOC) {
      extensions.add(DefinitionExtension.create());
      extensions.add(TablesExtension.create());
      extensions.add(TaskListExtension.create());
      extensions.add(TocExtension.create());
      options
          .set(com.vladsch.flexmark.parser.Parser.SPACE_IN_LINK_URLS, true)
          .setFrom(ParserEmulationProfile.GITHUB_DOC)
          .set(TablesExtension.COLUMN_SPANS, false)
          .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
          .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
          .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);
    } else {
      options.setFrom(profile);
    }

    // Cleanse optionalExtensions into an array to allow exact matches later on.
    List<String> optionalExtensionsList =
        new ArrayList<>(Arrays.asList(optionalExtensions.toUpperCase().trim().split("\\s*,\\s*")));

    if (!optionalExtensionsList.isEmpty()) {
      if (optionalExtensionsList.contains("*")) {
        // Add all optional extensions.
        extensions.add(AsideExtension.create());
        extensions.add(AttributesExtension.create());
        extensions.add(DefinitionExtension.create());
        extensions.add(InsExtension.create());
        extensions.add(StrikethroughSubscriptExtension.create());
        extensions.add(SuperscriptExtension.create());
        extensions.add(TablesExtension.create());
        extensions.add(TaskListExtension.create());
        extensions.add(TocExtension.create());
      } else {
        // Add user selected optional extensions
        if (optionalExtensionsList.contains("ASIDE")) {
          extensions.add(AsideExtension.create());
        }
        if (optionalExtensionsList.contains("ATTRIBUTES")) {
          extensions.add(AttributesExtension.create());
        }
        if (optionalExtensionsList.contains("DEFINITION")) {
          extensions.add(DefinitionExtension.create());
        }
        if (optionalExtensionsList.contains("INS")) {
          extensions.add(InsExtension.create());
        }
        // Can only add the one Strikethrough and/or Subscript extension, so work out which one.
        if (optionalExtensionsList.contains("STRIKETHROUGH")
            && optionalExtensionsList.contains("SUBSCRIPT")) {
          extensions.add(StrikethroughSubscriptExtension.create());
        } else if (optionalExtensionsList.contains("STRIKETHROUGH")) {
          extensions.add(StrikethroughExtension.create());
        } else if (optionalExtensionsList.contains("SUBSCRIPT")) {
          extensions.add(SubscriptExtension.create());
        }
        if (optionalExtensionsList.contains("SUPERSCRIPT")) {
          extensions.add(SuperscriptExtension.create());
        }
        if (optionalExtensionsList.contains("TABLES")) {
          extensions.add(TablesExtension.create());
        }
        if (optionalExtensionsList.contains("TASKLIST")) {
          extensions.add(TaskListExtension.create());
        }
        if (optionalExtensionsList.contains("TOC")) {
          extensions.add(TocExtension.create());
        }
      }
    }

    if (!extensions.isEmpty()) {
      options.set(com.vladsch.flexmark.parser.Parser.EXTENSIONS, extensions);
    }

    var mdParser = com.vladsch.flexmark.parser.Parser.builder(options).build();
    HtmlRenderer renderer = HtmlRenderer.builder(options).build();

    Node document = mdParser.parse(markdownText);

    return renderer.render(document);
  }

  /**
   * Returns a {@link ParserEmulationProfile} based on the value passed from the MTS Parser.
   *
   * @param name The name of the MarkDown type starting with {@link #MARKDOWN_PREFIX} with the
   *     {@link String} value of {@link ParserEmulationProfile}.
   * @return the {@link ParserEmulationProfile} used to parse the markdown.
   * @throws ParserException if the {@code name} is invalid.
   */
  private ParserEmulationProfile getParserType(String name) throws ParserException {
    String val = name.trim().replace(MARKDOWN_PREFIX, "");
    try {
      return ParserEmulationProfile.valueOf(val);
    } catch (IllegalArgumentException ex) {
      throw new ParserException(I18N.getText("macro.function.markdown.unknownType", val));
    }
  }

  /**
   * Returns a value that can be used in MT Script to specify the type of markdown to parse.
   *
   * @param name The name of the MarkDown type.
   * @return the {@link ParserEmulationProfile} used to parse the markdown.
   * @throws ParserException if the {@code name} is invalid.
   */
  public Object getMTSTypeLabel(String name) throws ParserException {
    ParserEmulationProfile profile = getParserType(name);
    // If the above was able to convert properly return the string as is, otherwise allow the parser
    // error bubble up
    return name;
  }
}
