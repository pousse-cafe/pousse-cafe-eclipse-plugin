package poussecafe.eclipse.plugin.editors;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.StructuredSelection;

import static java.util.Objects.requireNonNull;

public class IMemberHyperlink implements IHyperlink {

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
        return "Open " + member.getElementName();
    }

    private IMember member;

    @Override
    public void open() {
        action.run(new StructuredSelection(member));
    }

    private OpenAction action;

    public static class Builder {

        private IMemberHyperlink link = new IMemberHyperlink();

        public IMemberHyperlink build() {
            requireNonNull(link.region);
            requireNonNull(link.member);
            requireNonNull(link.action);
            return link;
        }

        public Builder region(IRegion region) {
            link.region = region;
            return this;
        }

        public Builder member(IMember member) {
            link.member = member;
            return this;
        }

        public Builder action(OpenAction action) {
            link.action = action;
            return this;
        }
    }

    private IMemberHyperlink() {

    }
}
