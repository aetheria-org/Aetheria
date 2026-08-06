package io.hamlook.aetheria.features.chat.globalchat.util;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.*;

import javax.swing.text.Segment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless parser bridging RSyntaxTextArea tokenizers to Minecraft's FontRenderer.
 * Bypasses AWT/Swing rendering constraints to run entirely within Forge's OpenGL loops.
 * <p>
 * Token makers are instantiated directly (not via {@code TokenMakerFactory}) so the
 * shadow-relocated copies inside the mod jar keep working - the factory locates its
 * tokenizers through string-based {@code Class.forName} lookups that relocation
 * cannot rewrite.
 */
public class CodeParser {

    /** Data wrapper holding the raw text snippet and its corresponding Minecraft hex color. */
    public static class HighlightedToken {
        public final String text;
        public final int mcColor;

        public HighlightedToken(String text, int mcColor) {
            this.text = text;
            this.mcColor = mcColor;
        }
    }

    private static final Map<String, TokenMaker> MAKERS = new HashMap<>();

    private final TokenMaker currentTokenMaker;

    public CodeParser(String languageKeyword) {
        currentTokenMaker = getMakerForKeyword(languageKeyword);
    }

    /**
     * Core processing loop transforming a plain document block into an OpenGL-ready token matrix.
     * Accurately retains multiline tracking context (e.g. cross-line block comments or triple quotes).
     */
    public List<List<HighlightedToken>> parseDocument(List<String> documentLines) {
        List<List<HighlightedToken>> parsedLines = new ArrayList<>();
        if (currentTokenMaker == null) {
            for (String line : documentLines) {
                List<HighlightedToken> lineTokens = new ArrayList<>();
                if (!line.isEmpty()) {
                    lineTokens.add(new HighlightedToken(line, 0xFFFFFF));
                }
                parsedLines.add(lineTokens);
            }
            return parsedLines;
        }

        int cumulativeLineState = TokenTypes.NULL;

        for (String line : documentLines) {
            List<HighlightedToken> lineTokens = new ArrayList<>();
            char[] chars = line.toCharArray();
            Segment segment = new Segment(chars, 0, chars.length);

            Token token = currentTokenMaker.getTokenList(segment, cumulativeLineState, 0);

            while (token != null && token.isPaintable()) {
                String tokenText = token.getLexeme();
                if (tokenText != null && !tokenText.isEmpty()) {
                    lineTokens.add(new HighlightedToken(tokenText, getMinecraftColorForType(token.getType())));
                }
                token = token.getNextToken();
            }

            parsedLines.add(lineTokens);
            cumulativeLineState = currentTokenMaker.getLastTokenTypeOnLine(segment, cumulativeLineState);
        }

        return parsedLines;
    }

    /**
     * Maps RSyntaxTextArea abstract types directly to 24-bit Hex RGB values.
     * Modify these color mappings to fit your preferred editor theme design palette.
     */
    private static int getMinecraftColorForType(int tokenType) {
        switch (tokenType) {
            case TokenTypes.RESERVED_WORD:
            case TokenTypes.RESERVED_WORD_2:
                return 0xE36049; // Red / Coral (e.g., function, const, return)

            case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE:
            case TokenTypes.LITERAL_CHAR:
            case TokenTypes.LITERAL_BACKQUOTE:
            case TokenTypes.REGEX:
                return 0x859900; // Olive Green (strings and regex)

            case TokenTypes.COMMENT_EOL:
            case TokenTypes.COMMENT_MULTILINE:
            case TokenTypes.COMMENT_DOCUMENTATION:
            case TokenTypes.COMMENT_KEYWORD:
            case TokenTypes.COMMENT_MARKUP:
                return 0x586E75; // Muted Gray-Blue (comments)

            case TokenTypes.LITERAL_NUMBER_DECIMAL_INT:
            case TokenTypes.LITERAL_NUMBER_FLOAT:
            case TokenTypes.LITERAL_NUMBER_HEXADECIMAL:
            case TokenTypes.LITERAL_BOOLEAN:
                return 0x2AA198; // Cyan / Teal (numbers and booleans)

            case TokenTypes.DATA_TYPE:
                return 0xCB4B16; // Orange / Rust (built-in types / classes)

            case TokenTypes.FUNCTION:
                return 0x268BD2; // Blue (function and method names)

            case TokenTypes.OPERATOR:
                return 0x93A1A1; // Light Silver / Gray (operators)

            case TokenTypes.ANNOTATION:
                return 0xB58900; // Yellow / Gold (annotations / decorators)

            case TokenTypes.VARIABLE:
                return 0x839496; // Off-White / Soft Cyan (variables)

            case TokenTypes.PREPROCESSOR:
                return 0xD33682; // Magenta (preprocessor directives)

            default:
                return 0xDC322F; // Fallback text color / Plain text
        }
    }

    private static TokenMaker getMakerForKeyword(String keyword) {
        if (keyword == null) return null;

        String key = keyword.toLowerCase().trim();
        TokenMaker maker = MAKERS.get(key);
        if (maker != null) return maker;

        maker = createMaker(key);
        if (maker != null) {
            MAKERS.put(key, maker);
        }
        return maker;
    }

    private static TokenMaker createMaker(String key) {
        switch (key) {
            case "as3": case "actionscript": case "actionscript3":
                return new ActionScriptTokenMaker();
            case "asm": case "asm86": case "x86": case "assembly":
                return new AssemblerX86TokenMaker();
            case "asm6502": case "6502":
                return new Assembler6502TokenMaker();
            case "bbcode":
                return new BBCodeTokenMaker();
            case "c":
                return new CTokenMaker();
            case "clj": case "clojure":
                return new ClojureTokenMaker();
            case "cpp": case "c++": case "cc":
                return new CPlusPlusTokenMaker();
            case "cs": case "c#": case "csharp":
                return new CSharpTokenMaker();
            case "css":
                return new CSSTokenMaker();
            case "csv":
                return new CsvTokenMaker();
            case "d": case "dlang":
                return new DTokenMaker();
            case "dart":
                return new DartTokenMaker();
            case "delphi": case "pascal": case "pas":
                return new DelphiTokenMaker();
            case "docker": case "dockerfile":
                return new DockerTokenMaker();
            case "dtd":
                return new DtdTokenMaker();
            case "fortran": case "f77": case "f90":
                return new FortranTokenMaker();
            case "go": case "golang":
                return new GoTokenMaker();
            case "groovy": case "gvy":
                return new GroovyTokenMaker();
            case "handlebars": case "hbs":
                return new HandlebarsTokenMaker();
            case "hosts":
                return new HostsTokenMaker();
            case "htaccess":
                return new HtaccessTokenMaker();
            case "html": case "htm":
                return new HTMLTokenMaker();
            case "ini": case "properties":
                return new IniTokenMaker();
            case "java":
                return new JavaTokenMaker();
            case "js": case "javascript":
                return new JavaScriptTokenMaker();
            case "json":
                return new JsonTokenMaker();
            case "jshintrc":
                return new JshintrcTokenMaker();
            case "jsp":
                return new JSPTokenMaker();
            case "kt": case "kotlin":
                return new KotlinTokenMaker();
            case "latex": case "tex":
                return new LatexTokenMaker();
            case "less":
                return new LessTokenMaker();
            case "lisp": case "lsp":
                return new LispTokenMaker();
            case "lua":
                return new LuaTokenMaker();
            case "make": case "makefile":
                return new MakefileTokenMaker();
            case "md": case "markdown":
                return new MarkdownTokenMaker();
            case "mxml":
                return new MxmlTokenMaker();
            case "nsis":
                return new NSISTokenMaker();
            case "pl": case "perl":
                return new PerlTokenMaker();
            case "php":
                return new PHPTokenMaker();
            case "proto": case "protobuf":
                return new ProtoTokenMaker();
            case "py": case "python":
                return new PythonTokenMaker();
            case "rb": case "ruby":
                return new RubyTokenMaker();
            case "sas":
                return new SASTokenMaker();
            case "scala":
                return new ScalaTokenMaker();
            case "sh": case "bash": case "shell":
                return new UnixShellTokenMaker();
            case "sql":
                return new SQLTokenMaker();
            case "tcl":
                return new TclTokenMaker();
            case "ts": case "typescript":
                return new TypeScriptTokenMaker();
            case "vb": case "vbs": case "vbscript": case "visualbasic":
                return new VisualBasicTokenMaker();
            case "bat": case "batch": case "cmd": case "windowsbatch":
                return new WindowsBatchTokenMaker();
            case "xml":
                return new XMLTokenMaker();
            case "yaml": case "yml":
                return new YamlTokenMaker();
            default:
                return null;
        }
    }
}
