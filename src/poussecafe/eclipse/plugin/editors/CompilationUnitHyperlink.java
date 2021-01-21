package poussecafe.eclipse.plugin.editors;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.StructuredSelection;

import static java.util.Objects.requireNonNull;

public class CompilationUnitHyperlink implements IHyperlink {

    @Override
    public IRegion getHyperlinkRegion() {
        return region;
    }

    private IRegion region;

    @Override
    public String getTypeLabel() {
        return null;
    }

    @Override
    public String getHyperlinkText() {
        return "Open " + compilationUnit.getElementName();
    }

    private ICompilationUnit compilationUnit;

    @Override
    public void open() {
        action.run(new StructuredSelection(compilationUnit));
    }

    private OpenAction action;

    public static class Builder {

        private CompilationUnitHyperlink link = new CompilationUnitHyperlink();

        public CompilationUnitHyperlink build() {
            requireNonNull(link.region);
            requireNonNull(link.compilationUnit);
            requireNonNull(link.action);
            return link;
        }

        public Builder region(IRegion region) {
            link.region = region;
            return this;
        }

        public Builder compilationUnit(ICompilationUnit compilationUnit) {
            link.compilationUnit = compilationUnit;
            return this;
        }

        public Builder action(OpenAction action) {
            link.action = action;
            return this;
        }
    }

    private CompilationUnitHyperlink() {

    }
}
