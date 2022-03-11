import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineMarker implements LineMarkerProvider {

  boolean initSucc;
  Map<String, Set<Integer>> fileLine;

  public LineMarker() {
    fileLine = new HashMap<>();
    initSucc = parseConfig();
  }

  private boolean parseConfig() {
    File configFile = new File(System.getProperty("user.home")
        + File.separator + ".config"
        + File.separator + "line-marker"
        + File.separator + "marker");
    if (!configFile.exists()) {
      return false;
    }

    try {
      Scanner scanner = new Scanner(configFile);
      while (scanner.hasNext()) {
        String line = scanner.nextLine();
        Pattern pattern = Pattern.compile("^(?<fileName>\\S+\\.java)\\s(?<lineNumber>\\d+)$");
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
          String fileName = matcher.group("fileName");
          int lineNumber = Integer.parseInt(matcher.group("lineNumber"));
          fileLine.putIfAbsent(fileName, new HashSet<>());
          fileLine.get(fileName).add(lineNumber);
        }
      }
    } catch (IOException ioe) {
      return false;
    }
    return true;
  }

  private int getLineNumber(@Nullable PsiElement element) {
    if (element == null || element instanceof PsiFile) {
      return -1;
    }
    PsiDocumentManager manager = PsiDocumentManager.getInstance(element.getProject());
    Document doc = manager.getDocument(element.getContainingFile());
    int lineNumber = doc.getLineNumber(element.getTextOffset()) + 1;
    return lineNumber;
  }

  private boolean isInsideFile(@NotNull PsiElement element) {
    element = element.getParent();
    while (element != null) {
      if (element instanceof PsiFile) {
        return true;
      }
      element = element.getParent();
    }
    return false;
  }

  private int getLineNumberRepresented(@NotNull PsiElement element) {
    int lineNumber = getLineNumber(element);
    int parentLineNumber = getLineNumber(element.getParent());
    int prevBroLineNumber = getLineNumber(element.getPrevSibling());

    if (parentLineNumber == lineNumber || prevBroLineNumber == lineNumber) {
      return -1;
    } else {
      return lineNumber;
    }
  }

  private boolean lineToMark(String fileName, int lineNumber) {
    if (! fileLine.containsKey(fileName)) {
      return false;
    }
    return fileLine.get(fileName).contains(lineNumber);
  }

  @Override
  public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
    if (! initSucc) {
      return null;
    }

    if (! element.getLanguage().getID().equals("JAVA")) {
      return null;
    }
    if (! isInsideFile(element)) {
      return null;
    }

    int lineNumber = getLineNumberRepresented(element);
    if (lineNumber == -1) {
      // is not a representative of the line it resides in
      return null;
    }
    String fileName = element.getContainingFile().getName();

    if (lineToMark(fileName, lineNumber)) {
      NavigationGutterIconBuilder<PsiElement> builder =
          NavigationGutterIconBuilder.create(AllIcons.General.Warning)
              .setTarget(element);
      return builder.createLineMarkerInfo(element);
    }
    return null;
  }
}
