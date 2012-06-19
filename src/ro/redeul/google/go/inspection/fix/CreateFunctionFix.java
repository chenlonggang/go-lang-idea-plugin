package ro.redeul.google.go.inspection.fix;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ro.redeul.google.go.lang.psi.expressions.primary.GoCallOrConvExpression;
import ro.redeul.google.go.lang.psi.expressions.primary.GoLiteralExpression;
import ro.redeul.google.go.lang.psi.expressions.literals.GoLiteralIdentifier;
import ro.redeul.google.go.lang.psi.toplevel.GoFunctionDeclaration;
import static ro.redeul.google.go.lang.psi.utils.GoPsiUtils.findParentOfType;
import static ro.redeul.google.go.lang.psi.utils.GoPsiUtils.psiIsA;

public class CreateFunctionFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    public CreateFunctionFix(@Nullable PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return "Create function \"" + getStartElement().getText() + "\"";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Variable Declaration";
    }

    @Override
    public void invoke(@NotNull Project project,
                       @NotNull final PsiFile file,
                       @Nullable("is null when called from inspection") final Editor editor,
                       @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        final PsiElement e = startElement;
        if (!e.getContainingFile().equals(file) || isExternalFunctionNameIdentifier(e)) {
            return;
        }

        GoFunctionDeclaration fd = findParentOfType(e, GoFunctionDeclaration.class);
        final int insertPoint;
        if (fd != null) {
            insertPoint = fd.getTextRange().getEndOffset();
        } else {
            insertPoint = file.getTextRange().getEndOffset();
        }

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                Document doc = PsiDocumentManager.getInstance(e.getProject()).getDocument(file);
                doc.insertString(insertPoint, String.format("\n\nfunc %s() {\n    \n}\n", e.getText()));
                if (editor != null) {
                    int offset = doc.getLineEndOffset(doc.getLineNumber(insertPoint) + 3);
                    editor.getCaretModel().moveToOffset(offset);
                }
            }
        });
    }

    public static boolean isExternalFunctionNameIdentifier(PsiElement e) {

        if (!psiIsA(e, GoLiteralIdentifier.class))
            return false;

        GoLiteralIdentifier identifier = (GoLiteralIdentifier)e;
        if (identifier.isQualified())
            return false;

        e = e.getParent();
        if (!psiIsA(e, GoLiteralExpression.class))
            return false;

        e = e.getParent();
        if (!psiIsA(e.getParent(), GoCallOrConvExpression.class))
            return false;

        return true;
    }
}